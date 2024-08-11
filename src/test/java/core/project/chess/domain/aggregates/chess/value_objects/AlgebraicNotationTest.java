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

    }
}