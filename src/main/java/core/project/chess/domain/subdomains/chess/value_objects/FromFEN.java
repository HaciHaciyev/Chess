package core.project.chess.domain.subdomains.chess.value_objects;

import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.containers.Pair;

import java.util.Map;
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
                      Map<Coordinate.Column, Integer> countOfPawnsOnEveryColumnForWhites,
                      int countOfWhitePieces,
                      Map<Coordinate.Column, Integer> countOfPawnsOnEveryColumnForBlack,
                      int countOfBlackPieces,
                      Optional<Pair<Coordinate, Coordinate>> isLastMovementWasPassage) {}