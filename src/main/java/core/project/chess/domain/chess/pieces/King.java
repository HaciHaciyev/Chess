package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import core.project.chess.domain.chess.enumerations.SimpleDirection;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation.Castle;
import core.project.chess.domain.chess.value_objects.KingStatus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

public final class King implements Piece {
    private final Color color;
    private final int index;

    private static final King WHITE_KING = new King(WHITE, 5);
    private static final King BLACK_KING = new King(BLACK, 11);

    public static King of(Color color) {
        return color == WHITE ? WHITE_KING : BLACK_KING;
    }

    private King(Color color, int index) {
        this.color = color;
        this.index = index;
    }

    @Override
    public Color color() {
        return color;
    }

    @Override
    public int index() {
        return index;
    }

    @Override
    public Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Piece startField = chessBoard.piece(from);
        Piece endField = chessBoard.piece(to);

        if (startField == null) return null;
        final boolean endFieldOccupiedBySameColorPiece = endField != null && endField.color() == color;
        if (endFieldOccupiedBySameColorPiece) return null;
        if (!isValidKingMovementCoordinates(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);

        final boolean opponentPieceInEndField = endField != null;
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    public boolean safeForKing(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        ChessBoardNavigator boardNavigator = chessBoard.navigator();
        Coordinate kingPosition = color.equals(WHITE) ? chessBoard.currentWhiteKingPosition() : chessBoard.currentBlackKingPosition();

        if (kingPosition.equals(from)) {
            if (chessBoard.isCastling(chessBoard.theKing(color), from, to)) return safeToCastle(boardNavigator, from, to);
            return validateKingMovementForSafety(boardNavigator, from, to);
        }

        return validatePieceMovementForKingSafety(boardNavigator, kingPosition, from, to);
    }

    public KingStatus kingStatus(final ChessBoard chessBoard) {
        return checkOrMate(chessBoard.navigator());
    }

    public boolean stalemate(final ChessBoard chessBoard) {
        ChessBoardNavigator navigator = chessBoard.navigator();
        Coordinate kingCoordinate = navigator.kingCoordinate(color);

        KingStatus kingStatus = chessBoard.kingStatus();
        if (kingStatus != null && (kingStatus.status() == Operations.CHECK ||
                kingStatus.status() == Operations.CHECKMATE)) return false;

        List<Coordinate> enemies = kingStatus != null ? kingStatus.enemiesAttackingTheKing() : check(navigator);
        if (!enemies.isEmpty()) return false;

        List<Coordinate> surroundingFieldsOfKing = navigator.surroundingFields(kingCoordinate);

        final boolean isSurrounded = surroundingFieldsOfKing.stream()
                .allMatch(coordinate -> isFieldDangerousOrBlockedForKing(navigator, coordinate, color));
        if (!isSurrounded) return false;

        List<Coordinate> ourFields = navigator.allFriendlyFieldsExceptKing(color, kingCoordinate);
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
        if (enemies.size() == 2) return enemies;

        List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(kingCoordinate);
        for (Coordinate possibleKnight : knights) {
            Piece piece = boardNavigator.board().piece(possibleKnight);
            if (piece instanceof Knight && piece.color() == oppositeColor) {
                enemies.add(possibleKnight);
                if (enemies.size() == 2) return enemies;
            }
        }

        int countOfPotentiallyNotFounded = enemies.isEmpty() ? 2 : 1;

        List<Coordinate> enemiesFromAllDirections =
                enemiesFromAllDirections(boardNavigator, kingCoordinate, oppositeColor, countOfPotentiallyNotFounded);
        enemies.addAll(enemiesFromAllDirections);
        return enemies;
    }

    private KingStatus checkOrMate(ChessBoardNavigator boardNavigator) {
        List<Coordinate> enemies = check(boardNavigator);
        if (enemies.isEmpty()) return new KingStatus(Operations.CONTINUE, enemies);

        Coordinate kingCoordinate = boardNavigator.kingCoordinate(color);

        if (enemies.size() == 1) {
            Coordinate fieldWithEnemy = enemies.getFirst();

            if (canEat(boardNavigator, fieldWithEnemy)) return new KingStatus(Operations.CHECK, enemies);
            if (canBlock(boardNavigator, kingCoordinate, fieldWithEnemy)) return new KingStatus(Operations.CHECK, enemies);

            List<Coordinate> surroundings = boardNavigator.surroundingFields(kingCoordinate);
            Operations operation = isHaveSafetyField(surroundings, boardNavigator) ? Operations.CHECK : Operations.CHECKMATE;
            return new KingStatus(operation, enemies);
        }

        if (isValidKingMovementCoordinates(boardNavigator.board(), kingCoordinate, enemies.getFirst()) &&
                !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getFirst(), color))
            return new KingStatus(Operations.CHECK, enemies);

        if (isValidKingMovementCoordinates(boardNavigator.board(), kingCoordinate, enemies.getLast()) &&
                !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getLast(), color))
            return new KingStatus(Operations.CHECK, enemies);

        List<Coordinate> surroundings = boardNavigator.surroundingFields(kingCoordinate);
        Operations operation = isHaveSafetyField(surroundings, boardNavigator) ? Operations.CHECK : Operations.CHECKMATE;
        return new KingStatus(operation, enemies);
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

        KingStatus kingStatus = boardNavigator.board().kingStatus();
        if (kingStatus.status() != Operations.CONTINUE) return false;

        List<Coordinate> fieldsToCastle = boardNavigator.castlingFields(castle, color);
        for (Coordinate field : fieldsToCastle) {
            if (field.column() == 5) continue;
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

        List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(pivot);
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

        List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(futureKing);
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

        KingStatus kingStatus = board.kingStatus();
        if (kingStatus != null) {
            List<Coordinate> enemies = kingStatus.enemiesAttackingTheKing();
            if (enemies.size() == 2) return false;

            SimpleDirection direction = getSimpleDirection(kingPosition, from);
            if (enemies.isEmpty() && direction == null) return true;
            if (enemies.isEmpty()) return checkIfTheAttackIsNotOpen(boardNavigator, kingPosition, from, to, direction);
            if (direction != null && !checkIfTheAttackIsNotOpen(boardNavigator, kingPosition, from, to, direction)) return false;

            Coordinate enemyField = enemies.getFirst();
            Piece enemy = board.piece(enemyField);

            if (enemy instanceof Pawn) return isPawnEaten(to, enemyField, boardNavigator);
            if (enemy instanceof Knight) return to == enemyField;
            final boolean surround = Math.abs(kingPosition.row() - enemyField.row()) <= 1 &&
                    Math.abs(kingPosition.column() - enemyField.column()) <= 1;
            if (surround) return to == enemyField;
            final boolean isEaten = to == enemyField;
            if (isEaten) return true;

            // Сan block from vertical
            final boolean vertical = kingPosition.row() != enemyField.row() && kingPosition.column() == enemyField.column();
            if (vertical) {
                if (to.column() != kingPosition.column()) return false;

                return kingPosition.row() < enemyField.row() ?
                        to.row() > kingPosition.row() && to.row() < enemyField.row() :
                        to.row() < kingPosition.row() && to.row() > enemyField.row();
            }

            // Сan block from horizontal
            final boolean horizontal = kingPosition.row() == enemyField.row() && kingPosition.column() != enemyField.column();
            if (horizontal) {
                if (to.row() != kingPosition.row()) return false;

                return kingPosition.column() < enemyField.column() ?
                        to.column() > kingPosition.column() && to.column() < enemyField.column() :
                        to.column() < kingPosition.column() && to.column() > enemyField.column();
            }

            // Сan block from diagonal
            if (Math.abs(to.row() - kingPosition.row()) != Math.abs(to.column() - kingPosition.column())) return false;

            final boolean isFromTop = kingPosition.row() < enemyField.row();
            if (isFromTop) {
                final boolean isFromRight = kingPosition.column() < enemyField.column();
                if (isFromRight) {
                    return kingPosition.row() < to.row() &&
                            to.row() < enemyField.row() &&
                            kingPosition.column() < to.column() &&
                            to.column() < enemyField.column();
                }

                return kingPosition.row() < to.row() &&
                        to.row() < enemyField.row() &&
                        kingPosition.column() > to.column() &&
                        to.column() > enemyField.column();
            }

            final boolean isFromRight = kingPosition.column() < enemyField.column();
            if (isFromRight) {
                return kingPosition.row() > to.row() &&
                        to.row() > enemyField.row() &&
                        kingPosition.column() < to.column() &&
                        to.column() < enemyField.column();
            }

            return kingPosition.row() > to.row() &&
                    to.row() > enemyField.row() &&
                    kingPosition.column() > to.column() &&
                    to.column() > enemyField.column();
        }

        List<Coordinate> pawnsThreateningCoordinates = boardNavigator.pawnsThreateningTheCoordinateOf(kingPosition, oppositeColor);
        for (Coordinate possiblePawn : pawnsThreateningCoordinates) {
            if (!isPawnEaten(to, possiblePawn, boardNavigator)) return false;
        }

        List<Coordinate> potentialKnightAttackPositions = boardNavigator.knightAttackPositionsNonNull(kingPosition);
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

    private static SimpleDirection getSimpleDirection(Coordinate start, Coordinate end) {
        SimpleDirection direction = null;
        if (start.column() == end.column() && start.row() != end.row()) direction =
                SimpleDirection.VERTICAL;
        if (start.column() != end.column() && start.row() == end.row()) direction =
                SimpleDirection.HORIZONTAL;
        if (Math.abs(start.row() - end.row()) == Math.abs(start.column() - end.column()))
            direction = SimpleDirection.DIAGONAL;
        return direction;
    }

    private boolean checkIfTheAttackIsNotOpen(ChessBoardNavigator boardNavigator, Coordinate kingPosition,
                                              Coordinate from, Coordinate to,
                                              SimpleDirection direction) {
        return switch (direction) {
            case DIAGONAL -> {
                final boolean isFromTop = kingPosition.row() < from.row();
                if (isFromTop) {
                    final boolean isTopRight = kingPosition.column() < from.column();
                    final Direction direction1 = isTopRight ? Direction.TOP_RIGHT : Direction.TOP_LEFT;

                    Coordinate occupiedCoordinate = boardNavigator.occupiedFieldInDirection(
                            direction1,
                            kingPosition,
                            coordinate -> from == coordinate,
                            coordinate -> to == coordinate,
                            coordinate -> false
                    );

                    if (occupiedCoordinate == null) yield true;

                    Piece pieceOnOccupiedField = boardNavigator.board().piece(occupiedCoordinate);

                    final boolean isEnemy = pieceOnOccupiedField.color() != color;
                    if (!isEnemy) yield  true;

                    if (pieceOnOccupiedField instanceof Bishop) yield false;
                    yield  !(pieceOnOccupiedField instanceof Queen);
                }

                final boolean isBottomRight = kingPosition.column() < from.column();
                final Direction direction1 = isBottomRight ? Direction.BOTTOM_RIGHT : Direction.BOTTOM_LEFT;

                Coordinate occupiedCoordinate = boardNavigator.occupiedFieldInDirection(
                        direction1,
                        kingPosition,
                        coordinate -> from == coordinate,
                        coordinate -> to == coordinate,
                        coordinate -> true
                );

                if (occupiedCoordinate == null) yield true;

                Piece pieceOnOccupiedField = boardNavigator.board().piece(occupiedCoordinate);

                final boolean isEnemy = pieceOnOccupiedField.color() != color;
                if (!isEnemy) yield  true;

                if (pieceOnOccupiedField instanceof Bishop) yield false;
                yield  !(pieceOnOccupiedField instanceof Queen);
            }
            case VERTICAL -> {
                final boolean isFromTop = kingPosition.row() < from.row();
                final Direction direction1 = isFromTop ? Direction.TOP : Direction.BOTTOM;

                Coordinate occupiedCoordinate = boardNavigator.occupiedFieldInDirection(
                        direction1,
                        kingPosition,
                        coordinate -> from == coordinate,
                        coordinate -> to == coordinate,
                        coordinate -> true
                );

                if (occupiedCoordinate == null) yield true;

                Piece pieceOnOccupiedField = boardNavigator.board().piece(occupiedCoordinate);

                final boolean isEnemy = pieceOnOccupiedField.color() != color;
                if (!isEnemy) yield  true;

                if (pieceOnOccupiedField instanceof Rook) yield false;
                yield  !(pieceOnOccupiedField instanceof Queen);
            }
            case HORIZONTAL -> {
                final boolean isFromRight = kingPosition.column() < from.column();
                final Direction direction1 = isFromRight ? Direction.RIGHT : Direction.LEFT;

                Coordinate occupiedCoordinate = boardNavigator.occupiedFieldInDirection(
                        direction1,
                        kingPosition,
                        coordinate -> from == coordinate,
                        coordinate -> to == coordinate,
                        coordinate -> true
                );

                if (occupiedCoordinate == null) yield true;

                Piece pieceOnOccupiedField = boardNavigator.board().piece(occupiedCoordinate);

                final boolean isEnemy = pieceOnOccupiedField.color() != color;
                if (!isEnemy) yield  true;

                if (pieceOnOccupiedField instanceof Rook) yield false;
                yield  !(pieceOnOccupiedField instanceof Queen);
            }
        };
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

        List<Coordinate> knightsThatPotentiallyCanEatEnemyPiece = boardNavigator.knightAttackPositionsNonNull(enemyField);
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

            List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(field);
            for (Coordinate knight : knights) {
                Piece piece = boardNavigator.board().piece(knight);

                final boolean isOurKnight = piece.color() == color && piece instanceof Knight;
                if (isOurKnight && safeForKing(boardNavigator.board(), knight, field)) return true;
            }

            List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), field);
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
        Coordinate kingCoordinate = boardNavigator.kingCoordinate(kingColor);
        Piece pivotPiece = board.piece(pivot);
        Color oppositeColor = kingColor == WHITE ? BLACK : WHITE;

        final boolean blocked = pivotPiece != null && pivotPiece.color() == kingColor;
        if (blocked) return true;

        List<Coordinate> pawns = boardNavigator.pawnsThreateningTheCoordinateOf(pivot, oppositeColor);
        if (!pawns.isEmpty()) return true;

        List<Coordinate> knights = boardNavigator.knightAttackPositionsNonNull(pivot);
        for (Coordinate possibleKnight : knights) {
            Piece piece = board.piece(possibleKnight);
            if (piece.color() != kingColor && piece instanceof Knight) return true;
        }

        final boolean isKingOpposition = boardNavigator.findKingOpposition(pivot, this.color == WHITE ? BLACK : WHITE);
        if (isKingOpposition) return true;

        List<Coordinate> diagonalFields = boardNavigator
                .occupiedFieldsInDirections(Direction.diagonalDirections(),
                        pivot,
                        coordinate -> coordinate == kingCoordinate,
                        coordinate -> false
                );

        for (Coordinate coordinate : diagonalFields) {
            Piece piece = board.piece(coordinate);
            if (piece.color() != kingColor && (piece instanceof Bishop || piece instanceof Queen)) return true;
        }

        List<Coordinate> horizontalVerticalFields = boardNavigator
                .occupiedFieldsInDirections(Direction.horizontalVerticalDirections(),
                        pivot,
                        coordinate -> coordinate == kingCoordinate,
                        coordinate -> false
                );

        for (Coordinate coordinate : horizontalVerticalFields) {
            Piece piece = board.piece(coordinate);
            if (piece.color() != kingColor && (piece instanceof Rook || piece instanceof Queen)) return true;
        }

        return false;
    }

    private List<Coordinate> enemiesFromAllDirections(ChessBoardNavigator boardNavigator,
                                                      Coordinate kingCoordinate,
                                                      Color oppositeColor,
                                                      int requiredEnemiesCount)  {
        List<Coordinate> enemies = new ArrayList<>();

        List<Coordinate> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), kingCoordinate);

        for (Coordinate diagonalField : diagonalFields) {
            Piece piece = boardNavigator.board().piece(diagonalField);
            if (piece.color() == oppositeColor && (piece instanceof Bishop || piece instanceof Queen)) {
                enemies.add(diagonalField);
                if (enemies.size() == requiredEnemiesCount) return enemies;
            }
        }

        requiredEnemiesCount -= enemies.size();

        List<Coordinate> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(),
                kingCoordinate
        );

        for (Coordinate horizontalVerticalField : horizontalVerticalFields) {
            Piece piece = boardNavigator.board().piece(horizontalVerticalField);
            if (piece.color() == oppositeColor && (piece instanceof Rook || piece instanceof Queen)) {
                enemies.add(horizontalVerticalField);
                if (enemies.size() == requiredEnemiesCount) return enemies;
            }
        }

        return enemies;
    }
}