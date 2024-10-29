package core.project.chess.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.gamesession.*;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.events.SessionEvents;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundChessRepository;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundChessRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import core.project.chess.infrastructure.utilities.containers.Triple;
import core.project.chess.infrastructure.utilities.json.JsonUtilities;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.websocket.Session;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static core.project.chess.infrastructure.utilities.web.WSUtilities.closeSession;
import static core.project.chess.infrastructure.utilities.web.WSUtilities.sendMessage;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ChessGameService {

    private final ObjectMapper objectMapper;

    private final InboundUserRepository inboundUserRepository;

    private final InboundChessRepository inboundChessRepository;

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    private static final ConcurrentHashMap<Username, Pair<Session, UserAccount>> sessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Pair<ChessGame, HashSet<Session>>> gameSessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Username, List<Pair<UserAccount, GameParameters>>> invitations = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Username, Triple<Session, UserAccount, GameParameters>> waitingForTheGame = new ConcurrentHashMap<>();

    private static final String INVITATION_MESSAGE = "User %s invite you for a chess game with parameters: figures color for you = %s, time control = %s.";

    public void handleOnOpen(Session session, Username username) {
        CompletableFuture.runAsync(() -> {
            Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(username);
            if (!result.success()) {
                sendMessage(session, "This account is do not founded.");
                return;
            }
            sessions.put(username, Pair.of(session, result.value()));
            if (invitations.containsKey(username)) {
                invitations.get(username)
                        .forEach(p -> {
                            UserAccount partner = p.getFirst();
                            GameParameters gameParameters = p.getSecond();
                            String color = Objects.isNull(gameParameters.color()) ? "RANDOM" : gameParameters.color().equals(Color.BLACK) ? "WHITE" : "BLACK";
                            String message = String.format(INVITATION_MESSAGE, partner.getUsername().username(), color, gameParameters.timeControllingTYPE());
                            sendMessage(session, message);
                        });
            }
        });
    }

    public void handleOnMessage(Session session, Username username, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            sendMessage(session, "message can't be null or blank");
            return;
        }

        final Result<MessageType, Throwable> messageType = JsonUtilities.chessMessageType(message);
        if (!messageType.success()) {
            sendMessage(session, "Invalid message type.");
            return;
        }

        final boolean isGameInitialization = messageType.value().equals(MessageType.GAME_INIT);
        if (isGameInitialization) {
            gameInitialization(session, username, message);
            return;
        }

        final String gameId = session.getUserProperties().get("game-id").toString();
        if (Objects.isNull(gameId)) {
            return;
        }

        final Pair<ChessGame, HashSet<Session>> gamePlusSessions = ChessGameService.gameSessions.get(UUID.fromString(gameId));
        if (Objects.isNull(gamePlusSessions)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("This game session is not exits.").build());
        }

        final Result<JsonNode, Throwable> messageNode = JsonUtilities.jsonTree(message);
        if (!messageNode.success()) {
            sendMessage(session, "Invalid message.");
            return;
        }

        CompletableFuture.runAsync(
                () -> handleWebSocketMessage(session, username.username(), messageNode.value(), messageType.value(), gamePlusSessions)
        );
    }

    public void handleOnClose(Session session) {
        final String gameId = session.getUserProperties().get("game-id").toString();
        if (Objects.isNull(gameId)) {
            return;
        }

        final UUID gameUuid = UUID.fromString(gameId);

        final boolean isGameSessionExists = gameSessions.containsKey(gameUuid);
        if (!isGameSessionExists) {
            sendMessage(session, "Game session with id {%s} does not exist".formatted(gameId));
            return;
        }

        final Pair<ChessGame, HashSet<Session>> pair = gameSessions.get(gameUuid);

        final ChessGame chessGame = pair.getFirst();
        if (chessGame.gameResult().isEmpty()) {
            return;
        }

        final Set<Session> sessionHashSet = pair.getSecond();
        final String messageInCaseOfGameEnding = "Game ended. Because of %s".formatted(chessGame.gameResult().get().toString());
        closeSession(session, messageInCaseOfGameEnding);

        sessionHashSet.remove(session);
        if (sessionHashSet.isEmpty()) {
            gameSessions.remove(gameUuid);
        }
    }

    private void gameInitialization(Session session, Username username, String message) {
        CompletableFuture.runAsync(() -> {
            final Result<GameInit, Throwable> parameters = JsonUtilities.gameInit(message);
            if (!parameters.success()) {
                sendMessage(session, "Invalid game initialization parameters");
                return;
            }

            final boolean connectToExistedGame = Objects.nonNull(parameters.value().gameId());
            if (connectToExistedGame) {
                connectToExistedGame(session, parameters.value().gameId());
                return;
            }

            final boolean partnershipGame = Objects.nonNull(parameters.value().nameOfPartner());
            if (partnershipGame) {
                partnershipGame(session, username, parameters.value());
                return;
            }

            gameInit(session, username, parameters.value());
        });
    }

    private void handleWebSocketMessage(final Session session, final String username, final JsonNode jsonNode,
                                        final MessageType type, final Pair<ChessGame, HashSet<Session>> gameSessions) {
        switch (type) {
            case MOVE -> this.move(jsonNode, Pair.of(username, session), gameSessions);
            case MESSAGE -> this.chat(jsonNode, Pair.of(username, session), gameSessions);
            case RETURN_MOVE -> this.returnOfMovement(Pair.of(username, session), gameSessions);
            case RESIGNATION -> this.resignation(Pair.of(username, session), gameSessions);
            case TREE_FOLD -> this.threeFold(Pair.of(username, session), gameSessions);
            case AGREEMENT -> this.agreement(Pair.of(username, session), gameSessions);
            default -> sendMessage(session, "Invalid message type.");
        }
    }

    private void move(JsonNode jsonNode, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameSessions) {
        final String username = usernameSession.getFirst();
        final ChessGame chessGame = gameSessions.getFirst();
        final Result<ChessMovementForm, Throwable> move = JsonUtilities.movementFormMessage(Objects.requireNonNull(jsonNode));
        if (!move.success()) {
            sendMessage(usernameSession.getSecond(), "Invalid chess movement json.");
            return;
        }

        try {
            chessGame.makeMovement(username, move.value().from(), move.value().to(), move.value().inCaseOfPromotion());
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameSession.getSecond(), "Invalid chess movement.");
            return;
        }

        final String message = JsonUtilities.chessGameToString(chessGame).orElseThrow();
        for (Session currentSession : gameSessions.getSecond()) {
            sendMessage(currentSession, message);
        }

        if (chessGame.gameResult().isPresent()) {
            gameOverOperationsExecutor(chessGame);

            for (Session currentSession : gameSessions.getSecond()) {
                sendMessage(currentSession, "Game is ended by result: {%s}.".formatted(chessGame.gameResult().orElseThrow().toString()));
            }
        }
    }

    public void chat(JsonNode jsonNode, Pair<String, Session> usernameSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameSession.getFirst();
        final Result<Message, Throwable> message = JsonUtilities.messageRecord(jsonNode);
        if (!message.success()) {
            sendMessage(usernameSession.getSecond(), "Invalid message");
            return;
        }

        try {
            gameAndSessions.getFirst().addChatMessage(username, message.value());
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, message.value().message()));

        } catch (IllegalArgumentException | NullPointerException e) {
            sendMessage(usernameSession.getSecond(), "Invalid message.");
        }
    }

    public void returnOfMovement(Pair<String, Session> usernameAndSession, Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.returnMovement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), "Can`t return a move.");
            return;
        }

        if (!chessGame.isLastMoveWasUndo()) {
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, "Player {%s} requested for move returning.".formatted(username)));
            return;
        }

        final String message = JsonUtilities.chessGameToString(chessGame).orElseThrow();
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    public void resignation(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.resignation(username);

            gameOverOperationsExecutor(chessGame);

            String message = "Game is ended by result {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
            for (Session currentSession : gameAndSessions.getSecond()) {
                sendMessage(currentSession, message);
            }
        } catch (IllegalArgumentException e) {
            sendMessage(usernameAndSession.getSecond(), "Not a player.");
        }
    }

    public void threeFold(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.endGameByThreeFold(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), "Can`t end game by ThreeFold");
            return;
        }

        gameOverOperationsExecutor(chessGame);

        String message = "Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    public void agreement(final Pair<String, Session> usernameAndSession, final Pair<ChessGame, HashSet<Session>> gameAndSessions) {
        final String username = usernameAndSession.getFirst();
        final ChessGame chessGame = gameAndSessions.getFirst();

        try {
            chessGame.agreement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            sendMessage(usernameAndSession.getSecond(), "Not a player. Illegal access.");
            return;
        }

        if (!chessGame.isAgreementAvailable()) {
            String message = "Player {%s} requested for agreement.".formatted(username);
            gameAndSessions.getSecond().forEach(session -> sendMessage(session, message));

            return;
        }

        gameOverOperationsExecutor(chessGame);

        String message = "Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().orElseThrow().toString());
        for (Session currentSession : gameAndSessions.getSecond()) {
            sendMessage(currentSession, message);
        }
    }

    private void connectToExistedGame(Session session, UUID gameId) {
        final Pair<ChessGame, HashSet<Session>> pair = gameSessions.get(gameId);
        pair.getSecond().add(session);

        sendMessage(session, JsonUtilities.chessGameToString(pair.getFirst()).orElseThrow());
    }

    private void gameInit(Session session, Username username, GameInit parameters) {
        final UserAccount firstPlayer = outboundUserRepository.findByUsername(username).orElseThrow();
        sendMessage(session, "Process for opponent finding.");

        final GameParameters gameParameters = new GameParameters(parameters.color(), parameters.time(), LocalDateTime.now());

        final StatusPair<Triple<Session, UserAccount, GameParameters>> potentialOpponent = findOpponent(firstPlayer, gameParameters);
        if (!potentialOpponent.status()) {
            waitingForTheGame.put(username, Triple.of(session, firstPlayer, gameParameters));
            sendMessage(session, "Try to find opponent for you %s.".formatted(username.username()));
            return;
        }

        final Session secondSession = potentialOpponent.orElseThrow().getFirst();
        final UserAccount secondPlayer = potentialOpponent.orElseThrow().getSecond();
        final GameParameters secondGameParameters = potentialOpponent.orElseThrow().getThird();
        waitingForTheGame.remove(secondPlayer.getUsername());

        chessGameInitialization(session, firstPlayer, gameParameters, secondSession, secondPlayer, secondGameParameters);
    }

    private void partnershipGame(Session session, Username username, GameInit parameters) {
        if (Objects.isNull(parameters.nameOfPartner())) {
            throw new IllegalArgumentException("Invalid method usage.");
        }

        if (!outboundUserRepository.isUsernameExists(parameters.nameOfPartner())) {
            sendMessage(session, "User %s do not exists.".formatted(parameters.nameOfPartner().username()));
            return;
        }

        final UserAccount user = outboundUserRepository.findByUsername(username).orElseThrow();
        final UserAccount partner = Objects.requireNonNullElseGet(
                sessions.get(parameters.nameOfPartner()).getSecond(), () -> outboundUserRepository.findByUsername(parameters.nameOfPartner()).orElseThrow()
        );

        final boolean haveNoPartnership = outboundUserRepository.havePartnership(user, partner);
        if (haveNoPartnership) {
            sendMessage(session, "You can`t invite someone who`s have not partnership with you.");
            return;
        }

        final GameParameters gameParameters = getGameParameters(parameters);
        invitations.computeIfAbsent(partner.getUsername(), k -> new LinkedList<>()).add(Pair.of(user, gameParameters));

        final StatusPair<GameParameters> isResponse = isValidPartnershipGame(user, gameParameters, partner);
        if (isResponse.status()) {
            partnershipGameInit(
                    sessions.get(username).getFirst(), user, gameParameters, sessions.get(partner.getUsername()).getFirst(), partner, isResponse.orElseThrow()
            );

            removeInvitations(user, partner);
        }
    }

    private static void removeInvitations(UserAccount user, UserAccount partner) {
        final List<Pair<UserAccount, GameParameters>> userInvitations = invitations.get(user.getUsername());
        for (int i = 0; i < userInvitations.size(); i++) {
            final Pair<UserAccount, GameParameters> userInvitation = userInvitations.get(i);

            final boolean isPartner = userInvitation.getFirst().getUsername().equals(partner.getUsername());
            if (isPartner) {
                userInvitations.remove(i);
                break;
            }
        }

        final List<Pair<UserAccount, GameParameters>> partnerInvitations = invitations.get(partner.getUsername());
        for (int i = 0; i < partnerInvitations.size(); i++) {
            final Pair<UserAccount, GameParameters> userInvitation = partnerInvitations.get(i);

            final boolean isPartner = userInvitation.getFirst().getUsername().equals(user.getUsername());
            if (isPartner) {
                partnerInvitations.remove(i);
                break;
            }
        }
    }

    private boolean isPartnershipGameAgreed(UserAccount user, UserAccount partner) {
        final boolean haveRequestFromPartner = !invitations.get(user.getUsername()).stream()
                .filter(p -> p.getFirst().getUsername().equals(partner.getUsername()))
                .toList().isEmpty();

        if (!haveRequestFromPartner) {
            return false;
        }

        return !invitations.get(partner.getUsername()).stream().filter(p -> p.getFirst().getUsername().equals(user.getUsername())).toList().isEmpty();
    }

    private void chessGameInitialization(Session session, UserAccount firstPlayer, GameParameters gameParameters,
                                         Session secondSession,  UserAccount secondPlayer, GameParameters secondGameParameters) {
        final ChessGame chessGame = loadChessGame(firstPlayer, gameParameters, secondPlayer, secondGameParameters);
        gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, new HashSet<>(Arrays.asList(session, secondSession))));

        session.getUserProperties().put("game-id", chessGame.getChessGameId().toString());
        secondSession.getUserProperties().put("game-id", chessGame.getChessGameId().toString());

        final Result<String, Throwable> message = JsonUtilities.chessGameToString(chessGame);
        sendMessage(session, message.orElseThrow());
        sendMessage(secondSession, message.orElseThrow());

        inboundChessRepository.completelySaveStartedChessGame(chessGame);
        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();
    }

    private void partnershipGameInit(Session session, UserAccount firstPlayer, GameParameters gameParameters,
                                     Session secondSession, UserAccount secondPlayer, GameParameters secondGameParameters) {
        Objects.requireNonNull(session);
        if (Objects.isNull(secondSession)) {
            sendMessage(session, "The request timed out. Can`t initialize a game.");
            return;
        }

        final ChessGame chessGame = loadChessGame(firstPlayer, gameParameters, secondPlayer, secondGameParameters);
        gameSessions.put(chessGame.getChessGameId(), Pair.of(chessGame, new HashSet<>(Arrays.asList(session, secondSession))));

        final Result<String, Throwable> message = JsonUtilities.chessGameToString(chessGame);
        sendMessage(session, message.orElseThrow());
        sendMessage(secondSession, message.orElseThrow());

        session.getUserProperties().put("game-id", chessGame.getChessGameId().toString());
        secondSession.getUserProperties().put("game-id", chessGame.getChessGameId().toString());

        inboundChessRepository.completelySaveStartedChessGame(chessGame);
        ChessGameSpectator spectator = new ChessGameSpectator(chessGame);
        spectator.start();
    }

    public StatusPair<GameParameters> isValidPartnershipGame(UserAccount user, GameParameters gameParameters, UserAccount partner) {
        List<Pair<UserAccount, GameParameters>> invitationList = invitations.get(user.getUsername());
        if (Objects.isNull(invitationList)) {
            return StatusPair.ofFalse();
        }

        for (Pair<UserAccount, GameParameters> pair : invitationList) {

            if (pair.getFirst().getUsername().equals(partner.getUsername())) {
                final boolean isOpponent = this.isOpponent(user, gameParameters, partner, pair.getSecond(), false);
                if (isOpponent) {
                    return StatusPair.ofFalse();
                }

                return StatusPair.ofTrue(pair.getSecond());
            }

        }

        return StatusPair.ofFalse();
    }

    @Transactional
    void gameOverOperationsExecutor(final ChessGame chessGame) {
        if (outboundChessRepository.isChessHistoryPresent(chessGame.getChessBoard().getChessBoardId())) {
            Log.errorf("History of game %s is already present", chessGame.getChessGameId());
            return;
        }

        Log.infof("Saving finished game %s and changing ratings", chessGame.getChessGameId());
        inboundChessRepository.completelyUpdateFinishedGame(chessGame);
        inboundUserRepository.updateOfRating(chessGame.getPlayerForWhite());
        inboundUserRepository.updateOfRating(chessGame.getPlayerForBlack());
    }

    private static GameParameters getGameParameters(GameInit parameters) {
        return new GameParameters(parameters.color(), parameters.time(), LocalDateTime.now());
    }

    private StatusPair<Triple<Session, UserAccount, GameParameters>> findOpponent(final UserAccount firstPlayer, final GameParameters gameParameters) {
        for (var entry : waitingForTheGame.entrySet()) {
            final UserAccount potentialOpponent = entry.getValue().getSecond();
            final GameParameters gameParametersOfPotentialOpponent = entry.getValue().getThird();

            if (potentialOpponent.getId().equals(firstPlayer.getId())) {
                continue;
            }

            final boolean isOpponent = this.isOpponent(firstPlayer, gameParameters, potentialOpponent, gameParametersOfPotentialOpponent, true);
            if (isOpponent) {
                return StatusPair.ofTrue(entry.getValue());
            }

            final boolean waitToLong = gameParametersOfPotentialOpponent.waitingTime() > 9;
            if (waitToLong) {
                waitingForTheGame.remove(potentialOpponent.getUsername());
            }
        }

        return StatusPair.ofFalse();
    }

    private boolean isOpponent(final UserAccount player, final GameParameters gameParameters,
                               final UserAccount opponent, final GameParameters opponentGameParameters,
                               final boolean isRatingDifferenceRequired) {
        final boolean sameUser = player.getId().equals(opponent.getId());
        if (sameUser) {
            return false;
        }

        final boolean sameTimeControlling = gameParameters.timeControllingTYPE().equals(opponentGameParameters.timeControllingTYPE());
        if (!sameTimeControlling) {
            return false;
        }

        if (isRatingDifferenceRequired) {
            final boolean validRatingDiff = Math.abs(player.getRating().rating() - opponent.getRating().rating()) <= 1500;
            if (!validRatingDiff) {
                return false;
            }
        }

        final boolean colorNotSpecified = gameParameters.color() == null || opponentGameParameters.color() == null;
        if (colorNotSpecified) {
            return true;
        }

        final boolean sameColor = gameParameters.color().equals(opponentGameParameters.color());
        return !sameColor;
    }

    private ChessGame loadChessGame(final UserAccount firstPlayer, final GameParameters gameParameters,
                                    final UserAccount secondPlayer, final GameParameters secondGameParameters) {

        final ChessBoard chessBoard = ChessBoard.starndardChessBoard(UUID.randomUUID());
        final ChessGame.TimeControllingTYPE timeControlling = gameParameters.timeControllingTYPE();
        final boolean firstPlayerIsWhite = gameParameters.color() != null && gameParameters.color().equals(Color.WHITE);
        final boolean secondPlayerIsBlack = secondGameParameters.color() != null && secondGameParameters.color().equals(Color.BLACK);

        final ChessGame chessGame;
        if (firstPlayerIsWhite && secondPlayerIsBlack) {

            chessGame = Result.ofThrowable(
                    () -> ChessGame.of(UUID.randomUUID(), chessBoard, firstPlayer, secondPlayer, SessionEvents.defaultEvents(), timeControlling)
            ).orElseThrow(
                    () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid data for chess game creation.").build())
            );

        } else {

            chessGame = Result.ofThrowable(
                    () -> ChessGame.of(UUID.randomUUID(), chessBoard, secondPlayer, firstPlayer, SessionEvents.defaultEvents(), timeControlling)
            ).orElseThrow(
                    () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid data for chess game creation.").build())
            );

        }
        Log.infof("Created chess game {%s} | Players: {%s}(%s), {%s}(%s) | Time controlling type: {%s}",
                chessGame.getChessBoard().getChessBoardId(),
                firstPlayer.getUsername().username(), firstPlayer.getRating().rating(),
                secondPlayer.getUsername().username(), secondPlayer.getRating().rating(),
                timeControlling.toString()
        );

        return chessGame;
    }

    private class ChessGameSpectator implements Runnable {
        private final ChessGame game;
        private final AtomicBoolean isRunning;
        private final ExecutorService executor;

        public ChessGameSpectator(ChessGame game) {
            this.game = game;
            this.isRunning = new AtomicBoolean(false);
            this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Spectator Thread"));
        }

        @Override
        public void run() {
            while (isRunning.get()) {
                game.gameResult().ifPresent(gameResult -> {
                    Log.infof("Game is over by result {%s}", gameResult);
                    Log.debugf("Removing game {%s}", game.getChessGameId());
                    var gameAndSessions = gameSessions.remove(game.getChessGameId());

                    for (Session session : gameAndSessions.getSecond()) {
                        Log.infof("Sending game result {%s} to session {%s}", gameResult, session.getId());
                        sendMessage(session, "Game is over by result {%s}".formatted(gameResult));
                    }

                    gameOverOperationsExecutor(game);
                    isRunning.set(false);
                });
            }
            Log.info("Spectator shutting down");
        }

        public void start() {
            if (isRunning.get()) {
                Log.debug("Spectator is already running");
            }

            Log.info("Starting spectator");
            isRunning.set(true);
            executor.submit(this);
        }
    }
}
