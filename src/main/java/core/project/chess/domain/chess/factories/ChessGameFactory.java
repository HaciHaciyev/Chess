package core.project.chess.domain.chess.factories;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.events.SessionEvents;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.commons.containers.StatusPair;
import core.project.chess.domain.commons.tuples.Pair;
import core.project.chess.domain.user.entities.User;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ChessGameFactory {

    public ChessGame createChessGameInstance(
            final User firstPlayer,
            final GameParameters gameParameters,
            final User secondPlayer,
            final GameParameters secondGameParameters,
            final boolean isPartnershipGame) {

        final ChessGame.Time time = gameParameters.time();
        final boolean isCasualGame = isCasualGame(gameParameters, isPartnershipGame);
        final boolean firstPlayerIsWhite = gameParameters.color() != null && gameParameters.color() == Color.WHITE;
        final boolean secondPlayerIsBlack = secondGameParameters.color() != null && secondGameParameters.color() == Color.BLACK;

        if (!isPartnershipGame)
            return standardChessGame(time, firstPlayer, secondPlayer, firstPlayerIsWhite, secondPlayerIsBlack, false);

        StatusPair<Pair<String, String>> chessNotations = chessNotations(gameParameters, secondGameParameters);
        if (!chessNotations.status())
            return standardChessGame(time, firstPlayer, secondPlayer,
                    firstPlayerIsWhite, secondPlayerIsBlack, gameParameters.isCasualGame());

        final boolean isPGNBasedGame = chessNotations.orElseThrow()
                .getFirst()
                .equals("PGN");

        if (isPGNBasedGame)
            return chessGameByPGN(time, firstPlayer, secondPlayer, firstPlayerIsWhite, secondPlayerIsBlack,
                    gameParameters.isCasualGame(), chessNotations.orElseThrow().getSecond());

        return chessGameByFEN(time, firstPlayer, secondPlayer, firstPlayerIsWhite, secondPlayerIsBlack,
                gameParameters.isCasualGame(), chessNotations.orElseThrow().getSecond());
    }

    private ChessGame standardChessGame(
            ChessGame.Time time,
            User firstPlayer,
            User secondPlayer,
            boolean firstPlayerIsWhite,
            boolean secondPlayerIsBlack,
            boolean isCasualGame) {

        if (firstPlayerIsWhite && secondPlayerIsBlack)
            return ChessGame.standard(
                    UUID.randomUUID(),
                    firstPlayer,
                    secondPlayer,
                    SessionEvents.defaultEvents(),
                    time,
                    isCasualGame
            );

        return ChessGame.standard(
                UUID.randomUUID(),
                secondPlayer,
                firstPlayer,
                SessionEvents.defaultEvents(),
                time,
                isCasualGame
        );
    }

    private ChessGame chessGameByPGN(
            ChessGame.Time time,
            User firstPlayer,
            User secondPlayer,
            boolean firstPlayerIsWhite,
            boolean secondPlayerIsBlack,
            Boolean isCasualGame,
            String PGN) {

        if (firstPlayerIsWhite && secondPlayerIsBlack)
            return ChessGame.byPGN(
                    UUID.randomUUID(),
                    PGN,
                    firstPlayer,
                    secondPlayer,
                    SessionEvents.defaultEvents(),
                    time,
                    isCasualGame
            );

        return ChessGame.byPGN(
                UUID.randomUUID(),
                PGN,
                secondPlayer,
                firstPlayer,
                SessionEvents.defaultEvents(),
                time,
                isCasualGame
        );
    }

    private ChessGame chessGameByFEN(
            ChessGame.Time time,
            User firstPlayer,
            User secondPlayer,
            boolean firstPlayerIsWhite,
            boolean secondPlayerIsBlack,
            Boolean isCasualGame,
            String FEN) {

        if (firstPlayerIsWhite && secondPlayerIsBlack)
            return ChessGame.byFEN(
                    UUID.randomUUID(),
                    FEN,
                    firstPlayer,
                    secondPlayer,
                    SessionEvents.defaultEvents(),
                    time,
                    isCasualGame
            );

        return ChessGame.byFEN(
                UUID.randomUUID(),
                FEN,
                secondPlayer,
                firstPlayer,
                SessionEvents.defaultEvents(),
                time,
                isCasualGame
        );
    }

    private boolean isCasualGame(GameParameters gameParameters, boolean isPartnershipGame) {
        if (!isPartnershipGame) return false;
        return gameParameters.isCasualGame();
    }

    private static StatusPair<Pair<String, String>> chessNotations(
            GameParameters gameParameters,
            GameParameters secondGameParameters) {
        if (gameParameters.PGN() != null) return StatusPair.ofTrue(Pair.of("PGN", gameParameters.PGN()));
        if (secondGameParameters.PGN() != null) return StatusPair.ofTrue(Pair.of("PGN", secondGameParameters.PGN()));
        if (gameParameters.FEN() != null) return StatusPair.ofTrue(Pair.of("FEN", gameParameters.FEN()));
        if (secondGameParameters.FEN() != null) return StatusPair.ofTrue(Pair.of("FEN", secondGameParameters.FEN()));
        return StatusPair.ofFalse();
    }
}