package core.project.chess.domain.subdomains.chess.value_objects;

import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.enumerations.Coordinate;

public record FromFEN(String fen,
                      Color figuresTurn,
                      Coordinate whiteKing,
                      Coordinate blackKing,
                      byte ruleOf50Moves,
                      byte countOfFullMoves,
                      byte materialAdvantageOfWhite,
                      byte materialAdvantageOfBlack,
                      boolean validWhiteShortCasting,
                      boolean validBlackShortCasting,
                      boolean validWhiteLongCasting,
                      boolean validBlackLongCasting) {}