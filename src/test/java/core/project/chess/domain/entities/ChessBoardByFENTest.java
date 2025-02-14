package core.project.chess.domain.entities;

import core.project.chess.domain.chess.entities.ChessBoard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChessBoardByFENTest {

    static Stream<String> validFenProvider() {
        return Stream.of(
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "r1bqkbnr/pppp1ppp/2n5/4p3/2B5/5N2/PPPPPPPP/RNBQK2R w KQkq - 2 3",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq - 0 1",
                "rnbqkb1r/pppppppp/7n/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 1 2",
                "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2",
                "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2",
                "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3",
                "rnbqk2r/ppppppbp/5np1/8/8/5NP1/PPPPPPBP/RNBQK2R w KQkq - 2 4",
                "2r3k1/p4p1p/1p1r2p1/nR5R/3p4/1P4P1/P1P2P1P/6K1 w - - 0 1"
        );
    }

    @ParameterizedTest
    @MethodSource("validFenProvider")
    @DisplayName("Test initialization of chess board by FEN.")
    void testFromPosition(String validFEN) {
        assertDoesNotThrow(() -> {
            ChessBoard board = ChessBoard.fromPosition(validFEN);
            assertNotNull(board);
        });
    }
}
