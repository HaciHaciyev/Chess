package core.project.chess.domain.aggregates.chess.value_objects;

import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class AlgebraicNotationTest {

    @Test
    void pawnMovementValidation() {

        var simplePawnMovement = AlgebraicNotation.of(
                new Pawn(Color.WHITE), Collections.EMPTY_SET, Coordinate.E2, Coordinate.E4, null
        );

        log.info("{}", simplePawnMovement);
        assertThat(simplePawnMovement.algebraicNotation()).isEqualTo("E2-E4");

        try {
            var invalidSimplePawnMovement = AlgebraicNotation.of(
                    new Pawn(Color.WHITE), Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E2, Coordinate.E4, null
            );

            log.info("{}", invalidSimplePawnMovement);
        } catch (NullPointerException e) {
            log.info("{}", e.getMessage());
        }

        try {
            var invalidSimplePawnMovement = AlgebraicNotation.of(
                    new Pawn(Color.WHITE), Set.of(ChessBoard.Operations.PROMOTION), Coordinate.E2, Coordinate.E4, AlgebraicNotation.PieceTYPE.Q
            );

            log.info("{}", invalidSimplePawnMovement);
        } catch (IllegalArgumentException e) {
            log.info("{}", e.getMessage());
        }
    }
}