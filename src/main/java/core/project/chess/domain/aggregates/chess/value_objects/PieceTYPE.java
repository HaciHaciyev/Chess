package core.project.chess.domain.aggregates.chess.value_objects;

import lombok.Getter;

@Getter
public enum PieceTYPE {

    K("KING"),
    Q("QUEEN"),
    R("ROOK"),
    B("BISHOP"),
    N("KNIGHT"),
    P("PAWN");

    private final String value;

    PieceTYPE(String value) {
        this.value = value;
    }

}
