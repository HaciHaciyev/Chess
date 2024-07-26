package core.project.chess.domain.aggregates.chess.value_objects;

import org.junit.jupiter.api.Test;

class AlgebraicNotationTest {

    @Test
    void of() {
        AlgebraicNotation algebraicNotation = AlgebraicNotation.of(
                new Pawn(Color.WHITE), AlgebraicNotation.Operations.EMPTY, Coordinate.D2, Coordinate.D4, null
        );
        System.out.println(algebraicNotation);
    }
}