package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation.PieceTYPE;

@Disabled
class AlgebraicNotationTest {

    @Test
    void pawnMovementValidation() {
        /** Valid pawn passage*/
        var simplePawnMovement = AlgebraicNotation.of(PieceTYPE.P, Collections.EMPTY_SET, Coordinate.E2, Coordinate.E4, null);
        assertEquals("E2-E4", simplePawnMovement.algebraicNotation());

        /** Invalid move distance.*/
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E2, Coordinate.E6, null)
        );

        var diagonalCapture = AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E2, Coordinate.D3, null);
        assertEquals("E2XD3", diagonalCapture.algebraicNotation());

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E2, Coordinate.G5, null)
        );

        /** Invalid diagonal move distance.*/
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E2, Coordinate.C4, null)
        );

        /** Promotion.*/

        /** Forget to set operation Promotion in the Set<Operations>.*/
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E7, Coordinate.E8, null)
        );

        /** Forget to put PieceType for promotion.*/
        assertThrows(
                NullPointerException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E7, Coordinate.E8, null)
        );

        /** Invalid piece type for pawn promotion.*/
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E7, Coordinate.E8, PieceTYPE.K)
        );

        /** Invalid coordinates for promotion.*/
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E2, Coordinate.E4, PieceTYPE.Q)
        );

        /** Capture*/

        var captureOperation = AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E4, Coordinate.D5, null);
        assertEquals("E4XD5", captureOperation.algebraicNotation());

        var setForMultiplyOperations = Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.PROMOTION, ChessBoard.Operations.CHECKMATE);
        var multiplyOperations = AlgebraicNotation.of(PieceTYPE.P, setForMultiplyOperations, Coordinate.E7, Coordinate.D8, PieceTYPE.Q);
        assertEquals("E7XD8=Q#", multiplyOperations.algebraicNotation());

        /** Invalid distance by diagonal*/
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E4, Coordinate.D6, null)
        );
    }

    @Test
    void coordinates() {
        var simplePawnMovement = AlgebraicNotation.of(PieceTYPE.P, Collections.EMPTY_SET, Coordinate.E2, Coordinate.E4, null);

        var coordinates = simplePawnMovement.coordinates();
        Log.info("From : {%s}".formatted(coordinates.getFirst()));
        Log.info("To : {%s}".formatted(coordinates.getSecond()));
    }

    @Test
    void figureMovementValidation() {
        var simpleQueenMovement = AlgebraicNotation.of(PieceTYPE.Q, Collections.emptySet(), Coordinate.C5, Coordinate.C8, null);
        assertEquals("QC5-C8", simpleQueenMovement.algebraicNotation());

        var setOfCaptureAndCheck = Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.CHECK);
        var simpleQueenMovementWithCaptureAndCheck = AlgebraicNotation.of(PieceTYPE.Q, setOfCaptureAndCheck, Coordinate.C5, Coordinate.C8, null);
        assertEquals("QC5XC8+", simpleQueenMovementWithCaptureAndCheck.algebraicNotation());

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.Q, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E7, Coordinate.E8, PieceTYPE.N)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.fromRepository("QC7-C8=N")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.Q, Collections.emptySet(), Coordinate.E7, Coordinate.G6, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.Q, Set.of(ChessBoard.Operations.CHECK, ChessBoard.Operations.CHECKMATE), Coordinate.E7, Coordinate.E8, null)
        );

        var simpleKnightMovement = AlgebraicNotation.of(PieceTYPE.N, Collections.emptySet(), Coordinate.C5, Coordinate.D7, null);
        assertEquals("NC5-D7", simpleKnightMovement.algebraicNotation());

        var setOfCaptureAndStalemate = Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.STALEMATE);
        var simpleKnightMovementPlusCaptureAndStalemate = AlgebraicNotation.of(PieceTYPE.N, setOfCaptureAndStalemate, Coordinate.C5, Coordinate.D7, null);
        assertEquals("NC5XD7.", simpleKnightMovementPlusCaptureAndStalemate.algebraicNotation());

        var setOfCaptureAndCheckmate = Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.CHECKMATE);
        var simpleRookMovement = AlgebraicNotation.of(PieceTYPE.R, setOfCaptureAndCheckmate, Coordinate.A1, Coordinate.A8, null);
        assertEquals("RA1XA8#", simpleRookMovement.algebraicNotation());

        var simpleBishopMovement = AlgebraicNotation.of(PieceTYPE.B, setOfCaptureAndCheckmate, Coordinate.C1, Coordinate.G5, null);
        assertEquals("BC1XG5#", simpleBishopMovement.algebraicNotation());
    }

    @Test
    void kingMovementValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.fromRepository("O-O-OX")
        );

        var shortCastingPlusCheck = AlgebraicNotation.of(PieceTYPE.K, Set.of(ChessBoard.Operations.CHECK), Coordinate.E1, Coordinate.G1, null);
        assertEquals("O-O+", shortCastingPlusCheck.algebraicNotation());

        var longCastingPlusCheck = AlgebraicNotation.of(PieceTYPE.K, Set.of(ChessBoard.Operations.CHECK), Coordinate.E8, Coordinate.C8, null);
        assertEquals("O-O-O+", longCastingPlusCheck.algebraicNotation());

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.K, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E2, Coordinate.E1, PieceTYPE.Q)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.fromRepository("KE2-E1=Q")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.K, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E1, Coordinate.G1, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.fromRepository("OXO=Q")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.K, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E1, Coordinate.G1, null)
        );

        var setOfCaptureAndPromotion = Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.PROMOTION);
        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.K, setOfCaptureAndPromotion, Coordinate.E1, Coordinate.G1, null)
        );

        var simpleKingMovementWithCapture = AlgebraicNotation.of(PieceTYPE.K, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E7, Coordinate.F8, null);
        assertEquals("KE7XF8", simpleKingMovementWithCapture.algebraicNotation());

        assertThrows(
                IllegalArgumentException.class,
                () -> AlgebraicNotation.of(PieceTYPE.K, Collections.emptySet(), Coordinate.B3, Coordinate.B5, null)
        );

        var setOfCaptureAndCheckmate = Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.CHECKMATE);
        var simpleMovementWithCaptureAndCheckmate = AlgebraicNotation.of(PieceTYPE.K, setOfCaptureAndCheckmate, Coordinate.E7, Coordinate.F8, null);
        assertEquals("KE7XF8#", simpleMovementWithCaptureAndCheckmate.algebraicNotation());
    }
}