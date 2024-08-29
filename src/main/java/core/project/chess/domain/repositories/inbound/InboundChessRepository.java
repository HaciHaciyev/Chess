package core.project.chess.domain.repositories.inbound;

import core.project.chess.domain.aggregates.chess.entities.ChessGame;

public interface InboundChessRepository {

    void saveStartedChessGame(ChessGame chessGame);

    void updateCompletedGame(ChessGame chessGame);
}
