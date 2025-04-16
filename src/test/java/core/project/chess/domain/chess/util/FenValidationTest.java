package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.value_objects.FromFEN;
import core.project.chess.domain.commons.containers.StatusPair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FenValidationTest {

    @Test
    void testValidStandardStartingPosition() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testValidComplexPosition() {
        String fen = "r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 4 6";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testEmptyString() {
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN("");
        assertFalse(result.status());
    }

    @Test
    void testNullInput() {
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(null);
        assertFalse(result.status());
    }

    @Test
    void testInvalidPiecePlacement() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidActiveColor() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidCastlingRights() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkqz - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidEnPassantSquare() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq h9 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidHalfmoveClock() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - x 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidPieceSymbol() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNX w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidRowLength() {
        String fen = "rnbqkbnr/ppppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidNumberOfRows() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testSpecialPositions() {
        String emptyBoard = "8/8/8/8/8/8/8/8 w - - 0 1";
        StatusPair<FromFEN> result1 = ChessNotationsValidator.validateFEN(emptyBoard);
        assertFalse(result1.status());

        String onlyKings = "4k3/8/8/8/8/8/8/4K3 w - - 0 1";
        StatusPair<FromFEN> result2 = ChessNotationsValidator.validateFEN(onlyKings);
        assertFalse(result2.status());
    }

    @Test
    void testComplexCastlingRights() {
        String[] validCastling = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w K - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w q - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Kk - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"
        };

        for (String fen : validCastling) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertTrue(result.status());
        }
    }

    @Test
    void testEdgeCaseEnPassant() {
        String[] validEnPassant = {
                "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
                "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2"
        };

        for (String fen : validEnPassant) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertTrue(result.status());
        }
    }

    @Test
    void testMalformedInput() {
        String[] malformedFEN = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 extra",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
                "rnbqkbnr/pppppppp/8/8/8//PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1",
                "/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        };

        for (String fen : malformedFEN) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertFalse(result.status());
        }
    }

    @Test
    void testExtremeBoardPositions() {
        String allWhitePieces = "RNBQKBNR/PPPPPPPP/RNBQKBNR/PPPPPPPP/RNBQKBNR/PPPPPPPP/RNBQKBNR/PPPPPPPP w - - 0 1";
        StatusPair<FromFEN> result1 = ChessNotationsValidator.validateFEN(allWhitePieces);
        assertFalse(result1.status());

        String justKings = "k7/8/8/8/8/8/8/K7 w - - 0 1";
        StatusPair<FromFEN> result2 = ChessNotationsValidator.validateFEN(justKings);
        assertFalse(result2.status());
    }

    @Test
    void testBoundaryValues() {
        String[] boundaryMoves = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 999 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 9999"
        };

        for (String fen : boundaryMoves) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertTrue(result.status());
        }
    }

    @Test
    void testSpecialCharacters() {
        String[] specialChars = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\n",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1\t",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 "
        };

        for (String fen : specialChars) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertFalse(result.status());
        }
    }

    @Test
    void testInvalidEmptyBoard() {
        String fen = "8/8/8/8/8/8/8/8 w - - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidKingsOnly() {
        String fen = "4k3/8/8/8/8/8/8/4K3 w - - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testValidSicilianDefensePosition() {
        String fen = "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6 0 2";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testValidRuyLopezPosition() {
        String fen = "r1bqkbnr/pppp1ppp/2n5/1B2p3/4P3/5N2/PPPP1PPP/RNBQK2R b KQkq - 3 3";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testValidQueensGambitPosition() {
        String fen = "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b KQkq c3 0 2";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testValidKingsIndianDefensePosition() {
        String fen = "rnbqkb1r/pppppp1p/5np1/8/2PP4/8/PP2PPPP/RNBQKBNR w KQkq - 1 3";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testValidPawnEndgamePosition() {
        String fen = "8/3k4/8/2KP4/8/8/8/8 w - - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testValidRookEndgamePosition() {
        String fen = "8/8/8/3k4/8/8/3K4/4R3 w - - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testInvalidMaterial() {
        String fen = "8/8/3k4/8/2B5/8/3K4/8 w - - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testValidQueenEndgamePosition() {
        String fen = "8/8/3k4/8/2Q5/8/3K4/8 w - - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }

    @Test
    void testInvalidTooManyWhitePawns() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/PPPPPPPP w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidTooManyBlackPawns() {
        String fen = "pppppppp/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidTooManyWhiteKings() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBKKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidTooManyBlackKings() {
        String fen = "rnbkkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidTooManyWhiteQueens() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQQBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidNoWhiteKing() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidNoBlackKing() {
        String fen = "rnbq1bnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidPawnsOnFirstRank() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/8/PPPPPPPP w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidPawnsOnLastRank() {
        String fen = "pppppppp/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidEnPassantSquareFormat() {
        String[] invalidEnPassant = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e9 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq i4 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq 4e 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq ee 0 1"
        };
        for (String fen : invalidEnPassant) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertFalse(result.status());
        }
    }

    @Test
    @Disabled("Disable until fen regex will fixed.")
    void testInvalidCastlingRightsFormat() {
        String[] invalidCastling = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkqKQ - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w kqKQ - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w ABC - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq+ - 0 1"
        };
        for (String fen : invalidCastling) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertFalse(result.status());
        }
    }

    @Test
    void testInvalidRowSeparators() {
        String[] invalidSeparators = {
                "rnbqkbnr\\pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr|pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr,pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        };
        for (String fen : invalidSeparators) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertFalse(result.status());
        }
    }

    @Test
    void testInvalidPieceNumbers() {
        String[] invalidPieceNumbers = {
                "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/0/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "rnbqkbnr/pppppppp/18/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
        };
        for (String fen : invalidPieceNumbers) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertFalse(result.status());
        }
    }

    @Test
    void testValidVariousCastlingRights() {
        String[] validCastling = {
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w K - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Q - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w k - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w q - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Kk - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w Qq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQk - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQq - 0 1",
                "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1"
        };
        for (String fen : validCastling) {
            StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
            assertTrue(result.status());
        }
    }

    @Test
    void testInvalidMissingSpaces() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR wKQkq-01";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidExtraSpaces() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR  w  KQkq  -  0  1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidWrongSectionCount() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidCharactersInBoard() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQXBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidTooManySquaresInRow() {
        String fen = "rnbqkbnr/pppppppp/9/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidTooFewSquaresInRow() {
        String fen = "rnbqkbnr/ppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidCastlingRights2() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkqA - 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testInvalidEnPassantSquare2() {
        String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq a9 0 1";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertFalse(result.status());
    }

    @Test
    void testFENPosition() {
        String fen = "rnbqkbnr/ppp2ppp/4p3/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R w KQkq - 0 3";
        StatusPair<FromFEN> result = ChessNotationsValidator.validateFEN(fen);
        assertTrue(result.status());
    }
}