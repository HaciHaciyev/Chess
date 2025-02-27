package core.project.chess.domain.chess.repositories;

import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.entities.Puzzle;

public interface InboundChessRepository {

    void completelySaveStartedChessGame(ChessGame chessGame);

    void completelyUpdateFinishedGame(ChessGame chessGame);

    void savePuzzle(Puzzle puzzle);
}
