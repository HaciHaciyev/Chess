package core.project.chess.domain.chess.services;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.MessageAddressee;
import core.project.chess.domain.chess.enumerations.UndoMoveResult;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.ChatMessage;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.infrastructure.utilities.containers.Pair;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

import java.time.Duration;
import java.util.Objects;

@ApplicationScoped
public class GameFunctionalityService {

    public boolean validateOpponentEligibility(final UserAccount player, final GameParameters gameParameters,
                                               final UserAccount opponent, final GameParameters opponentGameParameters,
                                               final boolean isPartnershipGame) {
        final boolean sameUser = player.getId().equals(opponent.getId());
        if (sameUser) {
            return false;
        }

        final boolean sameTimeControlling = gameParameters.time().equals(opponentGameParameters.time());
        if (!sameTimeControlling) {
            return false;
        }

        if (!isPartnershipGame) {
            final boolean validRatingDiff = Math.abs(player.getRating().rating() - opponent.getRating().rating()) <= 1500;
            if (!validRatingDiff) {
                return false;
            }
        }

        if (isPartnershipGame && !(gameParameters.isCasualGame().equals(opponentGameParameters.isCasualGame()))) {
            return false;
        }

        final boolean colorNotSpecified = gameParameters.color() == null || opponentGameParameters.color() == null;
        if (colorNotSpecified) {
            return true;
        }

        final boolean sameColor = gameParameters.color().equals(opponentGameParameters.color());
        return !sameColor;
    }

    public Pair<MessageAddressee, Message> move(Message move, Pair<String, Session> usernameSession, ChessGame chessGame) {
        final String username = usernameSession.getFirst();

        try {
            Log.infof("%s moving from %s to %s", username, move.from(), move.to());
            chessGame.makeMovement(
                    username,
                    move.from(),
                    move.to(),
                    Objects.isNull(move.inCaseOfPromotion()) ? null : AlgebraicNotation.fromSymbol(move.inCaseOfPromotion())
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            Log.infof("Movement failed: %s.", e.getMessage());
            return Pair.of(MessageAddressee.ONLY_ADDRESSER,
                    Message.builder(MessageType.ERROR)
                    .message("Invalid chess movement: %s".formatted(e.getMessage()))
                    .gameID(chessGame.getChessGameId().toString())
                    .build()
            );
        }

        return Pair.of(MessageAddressee.FOR_ALL, Message.builder(MessageType.FEN_PGN)
                .gameID(chessGame.getChessGameId().toString())
                .FEN(chessGame.getChessBoard().actualRepresentationOfChessBoard())
                .PGN(chessGame.getChessBoard().pgn())
                .timeLeft(remainingTimeAsString(chessGame))
                .isThreeFoldActive(chessGame.isThreeFoldActive())
                .build()
        );
    }

    private String remainingTimeAsString(ChessGame cg) {
        if (cg.getChessBoard().countOfHalfMoves() == 0 || cg.getChessBoard().countOfHalfMoves() == 1) {
            return "W -> 02:59:59 | B -> 03:00:00";
        }

        Duration whiteRemaining = cg.remainingTimeForWhite();

        long wHH = whiteRemaining.toHours();
        int wMM = whiteRemaining.toMinutesPart();
        int wSS = whiteRemaining.toSecondsPart();

        String wTime = "W -> %02d:%02d:%02d".formatted(wHH, wMM, wSS);

        Duration blackRemaining = cg.remainingTimeForBlack();

        long bHH = blackRemaining.toHours();
        int bMM = blackRemaining.toMinutesPart();
        int bSS = blackRemaining.toSecondsPart();

        String bTime = "B -> %02d:%02d:%02d".formatted(bHH, bMM, bSS);

        return wTime + " | " + bTime;
    }

    public Pair<MessageAddressee, Message> chat(Message message, Pair<String, Session> usernameSession, ChessGame chessGame) {
        final String username = usernameSession.getFirst();

        try {
            ChatMessage chatMsg = new ChatMessage(message.message());
            chessGame.addChatMessage(username, chatMsg);

            final Message msg = Message.builder(MessageType.MESSAGE)
                    .gameID(chessGame.getChessGameId().toString())
                    .message(chatMsg.message())
                    .build();

            return Pair.of(MessageAddressee.FOR_ALL, msg);
        } catch (IllegalArgumentException | NullPointerException e) {
            Message errorMessage = Message.builder(MessageType.ERROR)
                    .message("Invalid message.")
                    .gameID(chessGame.getChessGameId().toString())
                    .build();

            return Pair.of(MessageAddressee.ONLY_ADDRESSER, errorMessage);
        }
    }

    public Pair<MessageAddressee, Message> returnOfMovement(Pair<String, Session> usernameAndSession, ChessGame chessGame) {
        final String username = usernameAndSession.getFirst();
        final UndoMoveResult undoMoveResult;

        try {
            undoMoveResult = chessGame.returnMovement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Message message = Message.builder(MessageType.ERROR)
                    .message("Can`t return a move.")
                    .gameID(chessGame.getChessGameId().toString())
                    .build();

            return Pair.of(MessageAddressee.ONLY_ADDRESSER, message);
        }

        return switch (undoMoveResult) {
            case SUCCESSFUL_UNDO -> {
                final Message message = Message.builder(MessageType.FEN_PGN)
                        .gameID(chessGame.getChessGameId().toString())
                        .FEN(chessGame.getChessBoard().actualRepresentationOfChessBoard())
                        .PGN(chessGame.getChessBoard().pgn())
                        .timeLeft(remainingTimeAsString(chessGame))
                        .isThreeFoldActive(chessGame.isThreeFoldActive())
                        .build();

                yield Pair.of(MessageAddressee.FOR_ALL, message);
            }
            case FAILED_UNDO -> {
                Message message = Message.builder(MessageType.ERROR)
                        .message("Can`t return a move.")
                        .gameID(chessGame.getChessGameId().toString())
                        .build();

                yield Pair.of(MessageAddressee.ONLY_ADDRESSER, message);
            }
            case UNDO_REQUESTED -> {
                Message message = Message.builder(MessageType.RETURN_MOVE)
                        .message("Player {%s} requested for move returning.".formatted(username))
                        .gameID(chessGame.getChessGameId().toString())
                        .build();

                yield Pair.of(MessageAddressee.FOR_ALL, message);
            }
        };
    }

    public Pair<MessageAddressee, Message> resignation(final Pair<String, Session> usernameAndSession, final ChessGame chessGame) {
        final String username = usernameAndSession.getFirst();

        try {
            chessGame.resignation(username);

            final Message message = Message.builder(MessageType.GAME_ENDED)
                    .gameID(chessGame.getChessGameId().toString())
                    .message("Game is ended by result {%s}".formatted(chessGame.gameResult().orElseThrow().toString()))
                    .build();

            return Pair.of(MessageAddressee.FOR_ALL, message);
        } catch (IllegalArgumentException e) {
            Message message = Message.builder(MessageType.ERROR)
                    .message("Not a player.")
                    .gameID(chessGame.getChessGameId().toString())
                    .build();

            return Pair.of(MessageAddressee.ONLY_ADDRESSER, message);
        }
    }

    public Pair<MessageAddressee, Message> threeFold(final Pair<String, Session> usernameAndSession, final ChessGame chessGame) {
        final String username = usernameAndSession.getFirst();

        try {
            chessGame.endGameByThreeFold(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Message message = Message.builder(MessageType.ERROR)
                    .message("Can`t end game by ThreeFold")
                    .gameID(chessGame.getChessGameId().toString())
                    .build();

            return Pair.of(MessageAddressee.ONLY_ADDRESSER, message);
        }

        final Message message = Message.builder(MessageType.GAME_ENDED)
                .gameID(chessGame.getChessGameId().toString())
                .message("Game is ended by ThreeFold rule, game result is: {%s}".formatted(chessGame.gameResult().orElseThrow().toString()))
                .build();

        return Pair.of(MessageAddressee.FOR_ALL, message);
    }

    public Pair<MessageAddressee, Message> agreement(final Pair<String, Session> usernameAndSession, final ChessGame chessGame) {
        final String username = usernameAndSession.getFirst();

        try {
            chessGame.agreement(username);
        } catch (IllegalArgumentException | IllegalStateException e) {
            Message message = Message.builder(MessageType.ERROR)
                    .message("Not a player. Illegal access.")
                    .gameID(chessGame.getChessGameId().toString())
                    .build();

            return Pair.of(MessageAddressee.ONLY_ADDRESSER, message);
        }

        if (!chessGame.isAgreementAvailable()) {
            final Message message = Message.builder(MessageType.AGREEMENT)
                    .gameID(chessGame.getChessGameId().toString())
                    .message("Player {%s} requested for agreement.".formatted(username))
                    .build();

            return Pair.of(MessageAddressee.FOR_ALL, message);
        }

        final Message message = Message.builder(MessageType.GAME_ENDED)
                .gameID(chessGame.getChessGameId().toString())
                .message("Game is ended by agreement, game result is {%s}".formatted(chessGame.gameResult().orElseThrow().toString()))
                .build();

        return Pair.of(MessageAddressee.FOR_ALL, message);
    }
}
