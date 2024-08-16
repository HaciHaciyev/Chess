package core.project.chess.application.controller;

import core.project.chess.application.model.ChessMovementForm;
import core.project.chess.application.model.GamePreparationMessageType;
import core.project.chess.application.model.GameSearchProcessMessage;
import core.project.chess.application.model.InboundGameParameters;
import core.project.chess.application.service.ChessGameService;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.chess.value_objects.Color;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.Result;
import core.project.chess.infrastructure.utilities.StatusPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/chess")
@RequiredArgsConstructor
public class ChessGameController {

    private final ChessGameService chessGameService;

    private final SimpMessagingTemplate messagingTemplate;

    private final OutboundUserRepository outboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private final Map<UUID, ChessGame> chessGameMap = new ConcurrentHashMap<>();

    private final Map<UserAccount, InboundGameParameters> pendingGames = new ConcurrentHashMap<>();

    private static final String USER_DESTINATION_ADDRESS = "/party-search-process/%s";

    @MessageMapping("/create-game")
    @SendTo("/game-process/{gameId}")
    final void preparingForChessGame(@Payload final UUID userId, @Payload InboundGameParameters gameParameters) {

        Objects.requireNonNull(userId);
        Objects.requireNonNull(gameParameters);

        final UserAccount firstPlayer = outboundUserRepository
                .findById(userId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "This account was not found.")
                );

        if (!firstPlayer.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This account is not enabled.");
        }

        final var statusPair = findOpponent(firstPlayer, gameParameters);
        if (!statusPair.status()) {

            pendingGames.put(firstPlayer, gameParameters);

            final var message = new GameSearchProcessMessage(
                    GamePreparationMessageType.WAITING_FOR_THE_OPPONENT, "We are currently searching for an opponent that suits your requirements."
            );

            messagingTemplate.convertAndSendToUser(firstPlayer.getUsername(), USER_DESTINATION_ADDRESS.formatted(firstPlayer.getId()), message);
            return;
        }

        final ChessGame chessGame;
        final UserAccount secondPlayer = statusPair.valueOrElseThrow().getFirst();
        final InboundGameParameters secondPlayerGameParameters = statusPair.valueOrElseThrow().getSecond();
        final ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());
        final ChessGame.TimeControllingTYPE timeControlling = gameParameters.timeControllingTYPE();
        final boolean firstPlayerIsWhite = gameParameters.color() != null && gameParameters.color().equals(Color.WHITE);
        final boolean secondPlayerIsBlack = secondPlayerGameParameters.color() != null && secondPlayerGameParameters.color().equals(Color.BLACK);

        pendingGames.remove(secondPlayer);
        if (firstPlayerIsWhite && secondPlayerIsBlack) {

            chessGame = Result.ofThrowable(
                    () -> ChessGame.of(UUID.randomUUID(), chessBoard, firstPlayer, secondPlayer, SessionEvents.defaultEvents(), timeControlling)
            ).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data for chess game creation.")
            );

        } else {

            chessGame = Result.ofThrowable(
                    () -> ChessGame.of(UUID.randomUUID(), chessBoard, secondPlayer, firstPlayer, SessionEvents.defaultEvents(), timeControlling)
            ).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid data for chess game creation.")
            );

        }

        chessGameMap.put(chessGame.getChessGameId(), chessGame);
        inboundChessRepository.saveStarterChessGame(chessGame);

        final var message = new GameSearchProcessMessage(
                GamePreparationMessageType.CHESS_GAME_STARTED, "A partner for your chess game has been successfully found. Have a nice game!."
        );

        messagingTemplate.convertAndSendToUser(firstPlayer.getUsername(), USER_DESTINATION_ADDRESS.formatted(firstPlayer.getId()), message);
        messagingTemplate.convertAndSendToUser(firstPlayer.getUsername(), USER_DESTINATION_ADDRESS.formatted(secondPlayer.getId()), message);
        messagingTemplate.convertAndSend(chessGame.getChessBoard());
    }

    private StatusPair<Pair<UserAccount, InboundGameParameters>> findOpponent(final UserAccount player, final InboundGameParameters gameParameters) {
        for (var entry : pendingGames.entrySet()) {
            final UserAccount potentialOpponent = entry.getKey();
            final InboundGameParameters potentialOpponentGameParameters = entry.getValue();

            if (potentialOpponentGameParameters.waitingTime() > 3) {
                final var message = new GameSearchProcessMessage(
                        GamePreparationMessageType.UNABLE_TO_FIND_A_PARTNER, "A partner for your chess game was not found. You can try again."
                );

                messagingTemplate.convertAndSendToUser(potentialOpponent.getUsername(), USER_DESTINATION_ADDRESS.formatted(potentialOpponent.getId()), message);
            }

            final boolean opponent = chessGameService.isOpponent(player, gameParameters, potentialOpponent, potentialOpponentGameParameters);
            if (!opponent) {
                continue;
            }

            return StatusPair.ofTrue(Pair.of(potentialOpponent, potentialOpponentGameParameters));
        }

        return StatusPair.ofFalse();
    }

    @MessageMapping("/process-movement/{gameId}")
    @SendTo("/game-process/{gameId}")
    final void processMovement(
            @Payload final UUID userId,@Payload final ChessMovementForm move, @PathVariable("gameId") final UUID gameId
    ) throws IllegalAccessException {

        Objects.requireNonNull(userId);
        Objects.requireNonNull(move);
        Objects.requireNonNull(gameId);

        final ChessGame chessGame = chessGameMap.get(gameId);

        if (chessGame == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Game not exists.");
        }

        chessGame.makeMovement(userId, move.from(), move.to(), move.inCaseOfPromotion());

        if (chessGame.gameResult().isPresent()) {
            chessGameMap.remove(gameId);
            inboundChessRepository.updateCompletedGame(chessGame);
        }
    }
}
