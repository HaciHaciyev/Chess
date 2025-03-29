package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation.Castle;

import java.util.*;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

public record King(Color color) implements Piece {

    @Override
    public Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (from.equals(to)) {
            return null;
        }

        Piece startField = chessBoard.piece(from);
        Piece endField = chessBoard.piece(to);

        if (startField == null) return null;
        final boolean endFieldOccupiedBySameColorPiece = endField != null && endField.color() == color;
        if (endFieldOccupiedBySameColorPiece) return null;
        if (!isValidKingMovementCoordinates(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = new HashSet<>();

        final boolean opponentPieceInEndField = endField != null;
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    public boolean safeForKing(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        ChessBoardNavigator boardNavigator = chessBoard.navigator();
        Coordinate kingPosition = color.equals(WHITE) ? chessBoard.currentWhiteKingPosition() : chessBoard.currentBlackKingPosition();

        if (kingPosition.equals(from)) {
            if (chessBoard.isCastling(chessBoard.theKing(color), from, to)) return safeToCastle(boardNavigator, from, to);
            return validateKingMovementForSafety(boardNavigator, from, to);
        }

        return validatePieceMovementForKingSafety(boardNavigator, kingPosition, from, to);
    }

    public Operations kingStatus(final ChessBoard chessBoard) {
        return checkOrMate(chessBoard.navigator());
    }

    public boolean stalemate(ChessBoard chessBoard) {
        Objects.requireNonNull(chessBoard);

        ChessBoardNavigator navigator = chessBoard.navigator();
        Coordinate kingCoordinate = navigator.kingCoordinate(color);

        List<Coordinate> enemies = check(navigator);
        if (!enemies.isEmpty()) return false;

        List<Coordinate> surroundingFieldsOfKing = navigator.surroundingFields(kingCoordinate);

        final boolean isSurrounded = surroundingFieldsOfKing.stream()
                .allMatch(coordinate -> isFieldDangerousOrBlockedForKing(navigator, coordinate, color));
        if (!isSurrounded) return false;

        List<Coordinate> ourFields = navigator.allFriendlyFields(color, coordinate -> coordinate != kingCoordinate);
        for (Coordinate ourField : ourFields) {
            final boolean stalemate = processStalemate(navigator, ourField);
            if (!stalemate) return false;
        }

        return true;
    }

    private boolean isValidKingMovementCoordinates(ChessBoard chessBoard, Coordinate startField, Coordinate endField) {
        final int startColumn = startField.column();
        final int endColumn = endField.column();
        final int startRow = startField.row();
        final int endRow = endField.row();

        final boolean surroundField = Math.abs(startColumn - endColumn) <= 1 && Math.abs(startRow - endRow) <= 1;
        if (surroundField) return true;

        return chessBoard.isCastling(this, startField, endField);
    }

    private boolean processStalemate(ChessBoardNavigator navigator, Coordinate pivot) {
        Piece piece = navigator.board().piece(pivot);
        ChessBoard board = navigator.board();

        return switch (piece) {
            case Pawn pawn -> pawn.isPawnOnStalemate(navigator, pivot);
            case Knight knight -> {
                List<Coordinate> coords = navigator.knightAttackPositions(pivot);
                for (Coordinate endCoordinate : coords) {
                    Piece endPosition = board.piece(endCoordinate);
                    if (endPosition != null && endPosition.color() == color) continue;
                    if (knight.knightMove(pivot, endCoordinate) && safeForKing(board, pivot, endCoordinate)) yield false;
                }

                yield true;
            }
            case Bishop bishop -> {
                List<Coordinate> coords = navigator.fieldsInDirections(Direction.diagonalDirections(), pivot);
                for (Coordinate endCoordinate : coords) {
                    Piece endPosition = board.piece(endCoordinate);
                    if (endPosition != null && endPosition.color() == color) continue;
                    if (bishop.bishopMove(board, pivot, endCoordinate) && safeForKing(board, pivot, endCoordinate)) yield false;
                }

                yield true;
            }
            case Rook rook -> {
                List<Coordinate> coords = navigator.fieldsInDirections(Direction.horizontalVerticalDirections(), pivot);
                for (Coordinate endCoordinate : coords) {
                    Piece endPosition = board.piece(endCoordinate);
                    if (endPosition != null && endPosition.color() == color) continue;
                    if (rook.rookMove(board, pivot, endCoordinate) && safeForKing(board, pivot, endCoordinate)) yield false;
                }

                yield true;
            }
            case Queen queen -> {
                List<Coordinate> coords = navigator.fieldsInDirections(Direction.allDirections(), pivot);
                for (Coordinate endCoordinate : coords) {
                    Piece endPosition = board.piece(endCoordinate);
                    if (endPosition != null && endPosition.color() == color) continue;
                    if (queen.queenMove(board, pivot, endCoordinate) && safeForKing(board, pivot, endCoordinate)) yield false;
                }

                yield true;
            }
            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
    }

    private List<Coordinate> check(ChessBoardNavigator boardNavigator) {
        Coordinate kingCoordinate = boardNavigator.kingCoordinate(color);
        Color oppositeColor = color == WHITE ? BLACK : WHITE;

        List<Coordinate> enemies = new ArrayList<>();

        List<Coordinate> pawns = boardNavigator.pawnsThreateningTheCoordinateOf(kingCoordinate, oppositeColor);
        if (!pawns.isEmpty()) enemies.addAll(pawns);

        List<Coordinate> knights = boardNavigator.knightAttackPositions(kingCoordinate, Objects::nonNull);
        for (Coordinate possibleKnight : knights) {
            Piece piece = boardNavigator.board().piece(possibleKnight);
            if (piece instanceof Knight && piece.color() == oppositeColor) enemies.add(possibleKnight);
        }

        List<Coordinate> enemiesFromAllDirections = enemiesFromAllDirections(boardNavigator, kingCoordinate, oppositeColor);
        enemies.addAll(enemiesFromAllDirections);
        return enemies;
    }

    private Operations checkOrMate(ChessBoardNavigator boardNavigator) {
        List<Coordinate> enemies = check(boardNavigator);
        if (enemies.isEmpty()) return Operations.CONTINUE;

        Coordinate kingCoordinate = boardNavigator.kingCoordinate(color);

        switch (enemies.size()) {
            case 1 -> {
                Coordinate fieldWithEnemy = enemies.getFirst();

                if (canEat(boardNavigator, fieldWithEnemy)) return Operations.CHECK;
                if (canBlock(boardNavigator, kingCoordinate, fieldWithEnemy)) return Operations.CHECK;

                List<Coordinate> surroundings = boardNavigator.surroundingFields(kingCoordinate);
                return isHaveSafetyField(surroundings, boardNavigator) ? Operations.CHECK : Operations.CHECKMATE;
            }
            case 2 -> {
                if (isValidKingMovementCoordinates(boardNavigator.board(), kingCoordinate, enemies.getFirst()) &&
                        !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getFirst(), color)) return Operations.CHECK;

                if (isValidKingMovementCoordinates(boardNavigator.board(), kingCoordinate, enemies.getLast()) &&
                        !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getLast(), color)) return Operations.CHECK;

                List<Coordinate> surroundings = boardNavigator.surroundingFields(kingCoordinate);
                return isHaveSafetyField(surroundings, boardNavigator) ? Operations.CHECK : Operations.CHECKMATE;
            }
            default -> {
                List<Coordinate> surroundings = boardNavigator.surroundingFields(kingCoordinate);
                return isHaveSafetyField(surroundings, boardNavigator) ? Operations.CHECK : Operations.CHECKMATE;
            }
        }
    }

    private boolean isHaveSafetyField(List<Coordinate> fields, ChessBoardNavigator boardNavigator) {
        for (Coordinate field : fields) {
            if (!isFieldDangerousOrBlockedForKing(boardNavigator, field, color)) return true;
        }

        return false;
    }

    private boolean safeToCastle(ChessBoardNavigator boardNavigator, Coordinate presentKingPosition, Coordinate futureKingPosition) {
        Castle castle;
        if (presentKingPosition.column() < futureKingPosition.column()) castle = Castle.SHORT_CASTLING;
        else castle = Castle.LONG_CASTLING;

        if (!boardNavigator.board().ableToCastling(color, castle)) return false;

        List<Coordinate> fieldsToCastle = boardNavigator.castlingFields(castle, color);
        for (Coordinate field : fieldsToCastle) {
            if (!processCastling(boardNavigator, field)) return false;
        }

        return true;
    }

    private boolean processCastling(ChessBoardNavigator boardNavigator, Coordinate pivot) {
        Piece piece = boardNavigator.board().piece(pivot);
        if (piece != null && !(piece instanceof King)) return false;

        Color oppositeColor = color == WHITE ? BLACK : WHITE;

        List<Coordinate> pawns = boardNavigator.pawnsThreateningTheCoordinateOf(pivot, oppositeColor);
        for (Coordinate coordinate : pawns) {
            Piece pawn = boardNavigator.board().piece(coordinate);
            if (pawn.color() != this.color) return false;
        }

        List<Coordinate> knights = boardNavigator.knightAttackPositions(pivot, Objects::nonNull);
        for (Coordinate coordinate : knights) {
            Piece knight = boardNavigator.board().piece(coordinate);
            if (knight instanceof Knight && knight.color() != this.color) return false;
        }

        List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), pivot);
        for (Coordinate coordinate : diagonalFields) {
            Piece pieceDiagonal = boardNavigator.board().piece(coordinate);
            if ((pieceDiagonal instanceof Bishop || pieceDiagonal instanceof Queen) && pieceDiagonal.color() != this.color) return false;
        }

        List<Coordinate> horizontalVertical = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), pivot);
        for (Coordinate hvField : horizontalVertical) {
            Piece pieceFromHV = boardNavigator.board().piece(hvField);
            if ((piece instanceof Rook || piece instanceof Queen) && pieceFromHV.color() != this.color) return false;
        }

        List<Coordinate> surroundings = boardNavigator.surroundingFields(pivot);
        for (Coordinate surroundingField : surroundings) {
            Piece surroundingPiece = boardNavigator.board().piece(surroundingField);
            if (surroundingPiece instanceof King && surroundingPiece.color() != this.color) return false;
        }

        return true;
    }

    private boolean validateKingMovementForSafety(ChessBoardNavigator boardNavigator, Coordinate previousKing, Coordinate futureKing) {
        ChessBoard board = boardNavigator.board();
        Color oppositeColor = color.equals(WHITE) ? BLACK : WHITE;

        List<Coordinate> pawns = boardNavigator.pawnsThreateningTheCoordinateOf(futureKing, oppositeColor);
        if (!pawns.isEmpty()) return false;

        List<Coordinate> knights = boardNavigator.knightAttackPositions(futureKing, Objects::nonNull);
        for (Coordinate possibleKnight : knights) {
            final Piece knight = board.piece(possibleKnight);
            if (knight instanceof Knight && knight.color() != color) return false;
        }

        List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.diagonalDirections(), futureKing, field -> field != previousKing
        );

        for (Coordinate field : diagonalFields) {
            final Piece piece = board.piece(field);

            final int enemyRow = field.row();
            final int enemyColumn = field.column();

            final boolean isEnemy = piece.color() != this.color;
            if (isEnemy && (piece instanceof Bishop || piece instanceof Queen)) return false;

            final boolean surroundField = Math.abs(futureKing.row() - enemyRow) <= 1 && Math.abs(futureKing.column() - enemyColumn) <= 1;

            final boolean isOppositionOfKing = surroundField && (piece instanceof King);
            if (isOppositionOfKing) return false;
        }

        List<Coordinate> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(), futureKing, field -> field != previousKing
        );

        for (Coordinate field : horizontalVerticalFields) {
            final Piece piece = board.piece(field);

            final int enemyRow = field.row();
            final int enemyColumn = field.column();

            final boolean isEnemy = piece.color() != this.color;
            if (isEnemy && (piece instanceof Rook || piece instanceof Queen)) return false;

            final boolean surroundField = Math.abs(futureKing.row() - enemyRow) <= 1 && Math.abs(futureKing.column() - enemyColumn) <= 1;

            final boolean isOppositionOfKing = surroundField && (piece instanceof King);
            if (isOppositionOfKing) return false;
        }

        return true;
    }

    private boolean validatePieceMovementForKingSafety(ChessBoardNavigator boardNavigator, Coordinate kingPosition,
                                                       Coordinate from, Coordinate to) {
        ChessBoard board = boardNavigator.board();
        Color oppositeColor = color.equals(WHITE) ? BLACK : WHITE;

        List<Coordinate> pawnsThreateningCoordinates = boardNavigator.pawnsThreateningTheCoordinateOf(kingPosition, oppositeColor);
        for (Coordinate possiblePawn : pawnsThreateningCoordinates) {
            if (!isPawnEaten(to, possiblePawn, boardNavigator)) return false;
        }

        List<Coordinate> potentialKnightAttackPositions = boardNavigator.knightAttackPositions(kingPosition, Objects::nonNull);
        for (Coordinate potentialKnightAttackPosition : potentialKnightAttackPositions) {
            Piece piece = board.piece(potentialKnightAttackPosition);

            if (piece.color() == color) continue;

            final boolean isEaten = potentialKnightAttackPosition.equals(to);
            final boolean isEnemyKnight = piece instanceof Knight;
            if (isEnemyKnight && !isEaten) return false;
        }

        List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.diagonalDirections(),
                kingPosition,
                coordinate -> coordinate.equals(from),
                coordinate -> coordinate.equals(to)
        );

        for (Coordinate field : diagonalFields) {
            Piece piece = board.piece(field);

            if (piece.color() == color) continue;

            final boolean isEaten = field.equals(to);
            final boolean isEnemyBishopOrQueen = (piece instanceof Bishop || piece instanceof Queen);
            if (!isEaten && isEnemyBishopOrQueen) return false;
        }

        List<Coordinate> horizontalVertical = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(),
                kingPosition,
                coordinate -> coordinate.equals(from),
                coordinate -> coordinate.equals(to)
        );

        for (Coordinate field : horizontalVertical) {
            Piece piece = board.piece(field);

            if (piece.color() == color) continue;

            final boolean isEaten = field.equals(to);
            final boolean isEnemyRookOrQueen = (piece instanceof Rook || piece instanceof Queen);
            if (!isEaten && isEnemyRookOrQueen) return false;
        }

        return true;
    }

    private static boolean isPawnEaten(Coordinate to, Coordinate opponentPawn, ChessBoardNavigator boardNavigator) {
        Coordinate enPassaunt = boardNavigator.board().enPassaunt();
        if (enPassaunt != null && enPassaunt == to) {
            if (opponentPawn.equals(to)) return true;

            if (opponentPawn.column() != to.column()) return false;

            int row = to.row();
            int opponentRow = opponentPawn.row();
            if (boardNavigator.board().piece(opponentPawn).color() == BLACK) return row - opponentRow == 1;

            return row - opponentRow == -1;
        }

        return opponentPawn.equals(to);
    }

    private boolean canEat(ChessBoardNavigator boardNavigator, Coordinate enemyField) {
        Piece enemyPiece = boardNavigator.board().piece(enemyField);
        Coordinate enPassaunt = boardNavigator.board().enPassaunt();

        if (enemyPiece instanceof Pawn && enPassaunt != null) {
            List<Coordinate> surroundedByEnPassauntPawns = boardNavigator.pawnsThreateningTheCoordinateOf(enPassaunt, color);
            for (Coordinate possiblePawn : surroundedByEnPassauntPawns) {
                if (safeForKing(boardNavigator.board(), possiblePawn, enPassaunt)) return true;
            }
        }

        List<Coordinate> pawnsThatPotentiallyCanEatEnemyPiece = boardNavigator.pawnsThreateningTheCoordinateOf(enemyField, color);
        for (Coordinate possiblePawn : pawnsThatPotentiallyCanEatEnemyPiece) {
            if (safeForKing(boardNavigator.board(), possiblePawn, enemyField)) return true;
        }

        List<Coordinate> knightsThatPotentiallyCanEatEnemyPiece = boardNavigator.knightAttackPositions(enemyField, Objects::nonNull);
        for (Coordinate knight : knightsThatPotentiallyCanEatEnemyPiece) {
            Piece piece = boardNavigator.board().piece(knight);

            final boolean isOurKnight = piece.color() == color && piece instanceof Knight;
            if (isOurKnight && safeForKing(boardNavigator.board(), knight, enemyField)) return true;
        }

        List<Coordinate> firstPiecesFromDiagonalVectors = boardNavigator
                .occupiedFieldsInDirections(Direction.diagonalDirections(), enemyField);

        for (Coordinate diagonalField : firstPiecesFromDiagonalVectors) {
            Piece piece = boardNavigator.board().piece(diagonalField);

            final boolean canEatFromDiagonalPosition = (piece instanceof Bishop || piece instanceof Queen) &&
                    piece.color() == color &&
                    safeForKing(boardNavigator.board(), diagonalField, enemyField);

            if (canEatFromDiagonalPosition) return true;
        }

        List<Coordinate> firstPiecesFromHorizontalAndVerticalVectors = boardNavigator
                .occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), enemyField);

        for (Coordinate horizontalVerticalField : firstPiecesFromHorizontalAndVerticalVectors) {
            Piece piece = boardNavigator.board().piece(horizontalVerticalField);

            final boolean canEatFromHorizontalAndVerticalPositions = (piece instanceof Rook || piece instanceof Queen) &&
                    piece.color() == color &&
                    safeForKing(boardNavigator.board(), horizontalVerticalField, enemyField);

            if (canEatFromHorizontalAndVerticalPositions) return true;
        }

        return false;
    }

    private boolean canBlock(ChessBoardNavigator boardNavigator, Coordinate pivot, Coordinate enemyField) {
        Piece opponentPiece = boardNavigator.board().piece(enemyField);
        if (opponentPiece instanceof Knight) return false;

        final boolean surround = Math.abs(pivot.row() - enemyField.row()) <= 1 && Math.abs(pivot.column() - enemyField.column()) <= 1;
        if (surround) return false;

        final boolean vertical = pivot.column() == enemyField.column() && pivot.row() != enemyField.row();
        List<Coordinate> path = boardNavigator.fieldsInPath(pivot, enemyField, false);

        for (Coordinate field : path) {
            if (!vertical && pawnCanBlock(boardNavigator, field)) return true;

            List<Coordinate> knights = boardNavigator.knightAttackPositions(field, Objects::nonNull);
            for (Coordinate knight : knights) {
                Piece piece = boardNavigator.board().piece(knight);

                final boolean isOurKnight = piece.color() == color && piece instanceof Knight;
                if (isOurKnight && safeForKing(boardNavigator.board(), knight, field)) return true;
            }

            final List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), field);
            for (Coordinate diagonalField : diagonalFields) {
                Piece piece = boardNavigator.board().piece(diagonalField);

                final boolean figureThatCanBlock = piece.color() == color && (piece instanceof Bishop || piece instanceof Queen);
                if (figureThatCanBlock && safeForKing(boardNavigator.board(), diagonalField, field)) return true;
            }

            List<Coordinate> horizontalVertical = boardNavigator
                    .occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), field);

            for (Coordinate horizontalVerticalField : horizontalVertical) {
                Piece piece = boardNavigator.board().piece(horizontalVerticalField);

                if (piece.color() == color &&  (piece instanceof Rook || piece instanceof Queen) &&
                        safeForKing(boardNavigator.board(), horizontalVerticalField, field)) return true;
            }
        }
        return false;
    }

    private boolean pawnCanBlock(ChessBoardNavigator boardNavigator, Coordinate field) {
        Coordinate potentialPawnThatCanBlockAttackBySimpleMove;
        if (color == WHITE) {
            potentialPawnThatCanBlockAttackBySimpleMove = Coordinate.of(field.row() - 1, field.column());
        } else {
            potentialPawnThatCanBlockAttackBySimpleMove = Coordinate.of(field.row() + 1, field.column());
        }

        if (potentialPawnThatCanBlockAttackBySimpleMove != null) {
            Piece possiblePawn = boardNavigator.board().piece(potentialPawnThatCanBlockAttackBySimpleMove);

            if (possiblePawn != null) {
                final boolean pawnCanBlock = possiblePawn.color() == color &&
                        possiblePawn instanceof Pawn &&
                        safeForKing(boardNavigator.board(), potentialPawnThatCanBlockAttackBySimpleMove, field);

                if (pawnCanBlock) return true;
            }
        }

        final boolean potentiallyCanBeBlockedByPawnPassage = field.row() == 5 && color == BLACK ||
                field.row() == 4 && color == WHITE;

        if (potentiallyCanBeBlockedByPawnPassage) {
            Coordinate potentialPawnCoordinate;
            Coordinate secondPassageCoordinate;

            if (color == WHITE) {
                potentialPawnCoordinate = Coordinate.of(2, field.column());
                secondPassageCoordinate = Coordinate.of(5, field.column());
            } else {
                potentialPawnCoordinate = Coordinate.of(7, field.column());
                secondPassageCoordinate = Coordinate.of(4, field.column());
            }

            final Piece potentialPawn = boardNavigator.board().piece(potentialPawnCoordinate);

            final boolean isFriendlyPawnExists = potentialPawn != null &&
                    potentialPawn.color() == color &&
                    potentialPawn instanceof Pawn;

            return isFriendlyPawnExists &&
                    clearPath(boardNavigator.board(), potentialPawnCoordinate, secondPassageCoordinate) &&
                    safeForKing(boardNavigator.board(), potentialPawnCoordinate, secondPassageCoordinate);
        }
        return false;
    }

    private boolean isFieldDangerousOrBlockedForKing(ChessBoardNavigator boardNavigator, Coordinate pivot, Color kingColor) {
        ChessBoard board = boardNavigator.board();
        Piece pivotPiece = board.piece(pivot);
        Color oppositeColor = kingColor == WHITE ? BLACK : WHITE;

        final boolean blocked = pivotPiece != null && pivotPiece.color() == kingColor;
        if (blocked) return true;

        List<Coordinate> pawns = boardNavigator.pawnsThreateningTheCoordinateOf(pivot, oppositeColor);
        if (!pawns.isEmpty()) return true;

        List<Coordinate> knights = boardNavigator.knightAttackPositions(pivot, Objects::nonNull);
        for (Coordinate possibleKnight : knights) {
            Piece piece = board.piece(possibleKnight);
            if (piece.color() != kingColor && piece instanceof Knight) return true;
        }

        final boolean kingOppositionNotExists = boardNavigator
                .surroundingFields(pivot, coordinate -> {
                    Piece piece = board.piece(coordinate);
                    return piece != null && piece.color() != kingColor && piece instanceof King;
                })
                .isEmpty();

        if (!kingOppositionNotExists) return true;

        List<Coordinate> diagonalFields = boardNavigator
                .occupiedFieldsInDirections(
                        Direction.diagonalDirections(),
                        pivot,
                        coord -> {
                            Piece piece = board.piece(coord);
                            return piece.color() != kingColor && (piece instanceof Bishop || piece instanceof Queen);
                        }
                );
        if (!diagonalFields.isEmpty()) return true;

        List<Coordinate> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(),
                pivot,
                coord -> {
                    Piece piece = board.piece(coord);
                    return piece.color() != kingColor && (piece instanceof Rook || piece instanceof Queen);
                }
        );
        return !horizontalVerticalFields.isEmpty();
    }

    private List<Coordinate> enemiesFromAllDirections(ChessBoardNavigator boardNavigator,
                                                      Coordinate kingCoordinate,
                                                      Color oppositeColor)  {
        List<Coordinate> enemies = new ArrayList<>();

        List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), kingCoordinate);
        for (Coordinate diagonalField : diagonalFields) {
            Piece piece = boardNavigator.board().piece(diagonalField);
            if (piece.color() == oppositeColor && (piece instanceof Bishop || piece instanceof Queen)) enemies.add(diagonalField);
        }

        List<Coordinate> horizontalVerticalFields = boardNavigator
                .occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), kingCoordinate);

        for (Coordinate horizontalVerticalField : horizontalVerticalFields) {
            Piece piece = boardNavigator.board().piece(horizontalVerticalField);
            if (piece.color() == oppositeColor && (piece instanceof Rook || piece instanceof Queen)) enemies.add(horizontalVerticalField);
        }

        return enemies;
    }
}