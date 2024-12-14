package core.project.chess.domain.subdomains.chess.value_objects;

import core.project.chess.domain.subdomains.chess.entities.AlgebraicNotation;
import core.project.chess.domain.subdomains.chess.enumerations.Color;
import core.project.chess.domain.subdomains.chess.enumerations.Coordinate;

import java.util.List;
import java.util.Map;

public record FromFEN(String fen,
                      Color figuresTurn,
                      Coordinate whiteKing,
                      Coordinate blackKing,
                      byte ruleOf50MovesForWhite,
                      byte ruleOf50MovesForBlack,
                      byte materialAdvantageOfWhite,
                      byte materialAdvantageOfBlack,
                      boolean validWhiteShortCasting,
                      boolean validBlackShortCasting,
                      boolean validWhiteLongCasting,
                      boolean validBlackLongCasting,
                      List<AlgebraicNotation> pgn,
                      Map<String, Byte> hashCodeOfBoard,
                      List<String> fenRepresentationsOfBoard) {}