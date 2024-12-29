package core.project.chess.domain.chess.repositories;

import core.project.chess.domain.chess.entities.ChessGame;

public interface InboundChessRepository {

    void completelySaveStartedChessGame(ChessGame chessGame);

    void completelyUpdateFinishedGame(ChessGame chessGame);
}
