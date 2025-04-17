package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.commons.tuples.Pair;

import java.util.Optional;

/**
 * Represents the FEN (Forsyth-Edwards Notation) of a chess position.
 */
public record FromFEN(String fen,
                      Color figuresTurn,
                      Coordinate whiteKing,
                      Coordinate blackKing,
                      byte materialAdvantageOfWhite,
                      byte materialAdvantageOfBlack,
                      boolean validWhiteShortCasting,
                      boolean validBlackShortCasting,
                      boolean validWhiteLongCasting,
                      boolean validBlackLongCasting,
                      Optional<Pair<Coordinate, Coordinate>> isLastMovementWasPassage) {}