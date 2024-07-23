package core.project.chess.domain.aggregates.chess.value_objects;

import org.junit.jupiter.api.Test;

class AlgebraicNotationTest {

    @Test
    void of() {
        AlgebraicNotation algebraicNotation = AlgebraicNotation.of(PieceTYPE.P, Coordinate.D2, Coordinate.D4);
        System.out.println(algebraicNotation);
    }
}