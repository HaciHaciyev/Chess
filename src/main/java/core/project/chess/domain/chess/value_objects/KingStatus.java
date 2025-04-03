package core.project.chess.domain.chess.value_objects;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;

import java.util.List;

public record KingStatus(ChessBoard.Operations status,
                         List<Coordinate> enemiesAttackingTheKing) {}