package core.project.chess.domain.entities;

import core.project.chess.domain.chess.entities.ChessBoard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChessBoardByPGNTest {

    static Stream<String> validPGNProvider() {
        return Stream.of(
                "1. e2-e3 ... ",
                "1. e2-e4 e7-e5 ",
                "1. e2-e3 Nb8-c6 "
        );
    }

    @ParameterizedTest
    @MethodSource("validPGNProvider")
    @DisplayName("Test initialization of chess board by FEN.")
    void testFromPosition(String validPGN) {
        assertDoesNotThrow(() -> {
            ChessBoard board = ChessBoard.pureChessFromPGN(validPGN);
            assertNotNull(board);
        });
    }
}
