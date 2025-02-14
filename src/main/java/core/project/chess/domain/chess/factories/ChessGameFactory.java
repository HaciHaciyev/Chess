package core.project.chess.domain.chess.factories;

import com.esotericsoftware.minlog.Log;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.user.entities.UserAccount;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;
import java.util.UUID;

@ApplicationScoped
public class ChessGameFactory {

    public ChessGame createChessGameInstance(final UserAccount firstPlayer, final GameParameters gameParameters,
                                              final UserAccount secondPlayer, final GameParameters secondGameParameters,
                                              final boolean isPartnershipGame) {
        ChessBoard chessBoard;
        boolean isCasualGame;
        try {
            Pair<ChessBoard, Boolean> chessBoardInstance = createChessBoardInstance(gameParameters, secondGameParameters, isPartnershipGame);
            chessBoard = chessBoardInstance.getFirst();
            isCasualGame = chessBoardInstance.getSecond();
        } catch (Exception e) {
            Log.error("Can`t create chess game.", e.getMessage());
            chessBoard = ChessBoard.starndardChessBoard();
            isCasualGame = gameParameters.isCasualGame();
        }

        final ChessGame.Time timeControlling = gameParameters.time();
        final boolean firstPlayerIsWhite = Objects.nonNull(gameParameters.color()) && gameParameters.color().equals(Color.WHITE);
        final boolean secondPlayerIsBlack = Objects.nonNull(secondGameParameters.color()) && secondGameParameters.color().equals(Color.BLACK);

        if (firstPlayerIsWhite && secondPlayerIsBlack) {
            return ChessGame.of(
                    UUID.randomUUID(),
                    chessBoard,
                    firstPlayer,
                    secondPlayer,
                    SessionEvents.defaultEvents(),
                    timeControlling,
                    isCasualGame
            );
        }

        return ChessGame.of(
                UUID.randomUUID(),
                chessBoard,
                secondPlayer,
                firstPlayer,
                SessionEvents.defaultEvents(),
                timeControlling,
                isCasualGame
        );
    }

    private static Pair<ChessBoard, Boolean> createChessBoardInstance(GameParameters gameParameters, GameParameters secondGameParameters,
                                                                      boolean isPartnershipGame) {
        if (!isPartnershipGame) {
            return Pair.of(ChessBoard.starndardChessBoard(), false);
        }

        StatusPair<Pair<String, String>> chessNotations = chessNotations(gameParameters, secondGameParameters);
        if (!chessNotations.status()) {
            return Pair.of(ChessBoard.starndardChessBoard(), gameParameters.isCasualGame());
        }

        final boolean isPGNBasedGame = chessNotations.orElseThrow().getFirst().equals("PGN");
        if (isPGNBasedGame) {
            return Pair.of(ChessBoard.fromPGN(chessNotations.orElseThrow().getSecond()), gameParameters.isCasualGame());
        }

        return Pair.of(ChessBoard.fromPosition(chessNotations.orElseThrow().getSecond()), gameParameters.isCasualGame());
    }

    private static StatusPair<Pair<String, String>> chessNotations(GameParameters gameParameters, GameParameters secondGameParameters) {
        if (gameParameters.PGN() != null) {
            return StatusPair.ofTrue(Pair.of("PGN", gameParameters.PGN()));
        }
        if (secondGameParameters.PGN() != null) {
            return StatusPair.ofTrue(Pair.of("PGN", secondGameParameters.PGN()));
        }
        if (gameParameters.FEN() != null) {
            return StatusPair.ofTrue(Pair.of("FEN", gameParameters.FEN()));
        }
        if (secondGameParameters.FEN() != null) {
            return StatusPair.ofTrue(Pair.of("FEN", secondGameParameters.FEN()));
        }
        return StatusPair.ofFalse();
    }
}