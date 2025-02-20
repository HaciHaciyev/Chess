package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;

class ChessBoardNavigatorTest {

    @Test
    void fieldsInDirections() {
        ChessBoard chessBoard = ChessBoard.starndardChessBoard();
        ChessBoardNavigator navigator = new ChessBoardNavigator(chessBoard);

        Log.infof("Coordinates in directions: {%s}", navigator.fieldsInDirections(Direction.allDirections(), Coordinate.e4)
                .stream()
                .map(ChessBoard.Field::getCoordinate)
                .toList()
                .toString());
    }
}