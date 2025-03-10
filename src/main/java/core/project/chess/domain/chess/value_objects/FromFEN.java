package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.containers.Pair;

import java.util.Optional;

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