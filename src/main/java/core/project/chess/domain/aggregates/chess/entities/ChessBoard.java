package core.project.chess.domain.aggregates.chess.entities;

public class ChessBoard {

    private ChessBoard() {}

    public static ChessBoard defaultChessBoard() {
        return new ChessBoard();
    }
}
