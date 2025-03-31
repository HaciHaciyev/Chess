package core.project.chess.domain.chess.value_objects;

public record CastlingAbility(boolean whiteShortCastling,
                              boolean whiteLongCastling,
                              boolean blackShortCastling,
                              boolean blackLongCastling) {}
