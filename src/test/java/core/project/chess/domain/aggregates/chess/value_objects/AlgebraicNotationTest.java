package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.infrastructure.utilities.Result;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Set;

import core.project.chess.domain.aggregates.chess.value_objects.AlgebraicNotation.PieceTYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
class AlgebraicNotationTest {

    @Test
    void pawnMovementValidation() {
        /** Valid pawn passage*/
        var simplePawnMovement = AlgebraicNotation.of(
                PieceTYPE.P, Collections.EMPTY_SET, Coordinate.E2, Coordinate.E4, null
        );

        assertThat(simplePawnMovement.algebraicNotation()).isEqualTo("E2-E4");

        /** Invalid move distance.*/
        Result<AlgebraicNotation, IllegalArgumentException> invalidMoveDistance = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E2, Coordinate.E6, null)
        );

        assertThatThrownBy(invalidMoveDistance::orElseThrow).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid algebraic notation.");

        var diagonalCapture = AlgebraicNotation.of(
                PieceTYPE.P, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E2, Coordinate.D3, null
        );

        Result<AlgebraicNotation, IllegalArgumentException> invalidMovement = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E2, Coordinate.G5, null)
        );

        assertThatThrownBy(invalidMovement::orElseThrow).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid algebraic notation.");

        assertThat(diagonalCapture.algebraicNotation()).isEqualTo("E2XD3");

        /** Invalid diagonal move distance.*/
        Result<AlgebraicNotation, IllegalArgumentException> invalidDiagonalMoveDistance = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E2, Coordinate.C4, null)
        );

        assertThatThrownBy(invalidDiagonalMoveDistance::orElseThrow).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid algebraic notation.");

        /** Promotion.*/

        /** Forget to set operation Promotion in the Set<Operations>.*/
        Result<AlgebraicNotation, IllegalArgumentException> forgetPromotionOperation = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Collections.emptySet(), Coordinate.E7, Coordinate.E8, null)
        );

        assertThatThrownBy(forgetPromotionOperation::orElseThrow).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid algebraic notation.");

        /** Forget to put PieceType for promotion.*/
        Result<AlgebraicNotation, NullPointerException> invalidPromotionPieceType = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E7, Coordinate.E8, null)
        );

        assertThatThrownBy(invalidPromotionPieceType::orElseThrow).isInstanceOf(NullPointerException.class);

        /** Invalid piece type for pawn promotion.*/
        Result<AlgebraicNotation, NullPointerException> invalidPromotionPieceType2 = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E7, Coordinate.E8, PieceTYPE.K)
        );

        assertThatThrownBy(invalidPromotionPieceType2::orElseThrow).isInstanceOf(IllegalArgumentException.class);

        /** Invalid coordinates for promotion.*/
        Result<AlgebraicNotation, IllegalArgumentException> invalidCoordinatesForPromotion = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E2, Coordinate.E4, PieceTYPE.Q)
        );

        assertThatThrownBy(invalidCoordinatesForPromotion::orElseThrow).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Invalid algebraic notation.");

        /** Capture*/

        var captureOperation = AlgebraicNotation.of(
                PieceTYPE.P, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E4, Coordinate.D5, null
        );

        assertThat(captureOperation.algebraicNotation()).isEqualTo("E4XD5");

        var multiplyOperations = AlgebraicNotation.of(
                PieceTYPE.P, Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.PROMOTION, ChessBoard.Operations.CHECKMATE), Coordinate.E7, Coordinate.D8, PieceTYPE.Q
        );

        assertThat(multiplyOperations.algebraicNotation()).isEqualTo("E7XD8=Q#");

        /** Invalid distance by diagonal*/
        Result<AlgebraicNotation, IllegalArgumentException> invalidCaptureOperation = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.P, Set.of(ChessBoard.Operations.CAPTURE), Coordinate.E4, Coordinate.D6, null)
        );

        assertThatThrownBy(invalidCaptureOperation::orElseThrow).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void coordinates() {
        var simplePawnMovement = AlgebraicNotation.of(
                PieceTYPE.P, Collections.EMPTY_SET, Coordinate.E2, Coordinate.E4, null
        );

        var coordinates = simplePawnMovement.coordinates();
        log.info("From : {}", coordinates.getFirst());
        log.info("To : {}", coordinates.getSecond());
    }

    @Test
    void queenMovementValidation() {
        var simpleQueenMovement = AlgebraicNotation.of(
                PieceTYPE.Q, Collections.emptySet(), Coordinate.C5, Coordinate.C8, null
        );

        assertThat(simpleQueenMovement.algebraicNotation()).isNotNull().isEqualTo("QC5-C8");

        var simpleQueenMovementWithCaptureAndCheck = AlgebraicNotation.of(
                PieceTYPE.Q, Set.of(ChessBoard.Operations.CAPTURE, ChessBoard.Operations.CHECK), Coordinate.C5, Coordinate.C8, null
        );

        assertThat(simpleQueenMovementWithCaptureAndCheck.algebraicNotation()).isNotNull().isEqualTo("QC5XC8+");

        var invalidPromotion = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.Q, Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E7, Coordinate.E8, PieceTYPE.N)
        );

        assertThatThrownBy(invalidPromotion::orElseThrow).isInstanceOf(IllegalArgumentException.class);

        var invalidQueenMovement = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.Q, Collections.emptySet(), Coordinate.E7, Coordinate.G6, null)
        );

        assertThatThrownBy(invalidQueenMovement::orElseThrow).isInstanceOf(IllegalArgumentException.class);

        var invalidQueenOperationsOnOpponentKing = Result.ofThrowable(
                () -> AlgebraicNotation.of(PieceTYPE.Q, Set.of(ChessBoard.Operations.CHECK, ChessBoard.Operations.CHECKMATE), Coordinate.E7, Coordinate.E8, null)
        );

        assertThatThrownBy(invalidQueenOperationsOnOpponentKing::orElseThrow).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A move must have only one operation involving the enemy King or stalemate, an invalid set of operations.");
    }
}