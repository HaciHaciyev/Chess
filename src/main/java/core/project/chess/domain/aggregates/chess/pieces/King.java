package core.project.chess.domain.aggregates.chess.pieces;

import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation.Castle;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.ChessBoardUtils;
import core.project.chess.infrastructure.utilities.Direction;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.*;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations;

public record King(Color color)
        implements Piece {

    @Override
    public StatusPair<Set<Operations>> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);
        if (from.equals(to)) {
            return StatusPair.ofFalse();
        }

        Field startField = chessBoard.field(from);
        Field endField = chessBoard.field(to);

        final boolean pieceNotExists = startField.pieceOptional().isEmpty();
        if (pieceNotExists) {
            return StatusPair.ofFalse();
        }

        if (!(startField.pieceOptional().get() instanceof King(var kingColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().get().color().equals(kingColor);
        if (endFieldOccupiedBySameColorPiece) {
            return StatusPair.ofFalse();
        }

        final boolean validKingMovement = isValidKingMovementCoordinates(chessBoard, startField, endField);
        if (!validKingMovement) {
            return StatusPair.ofFalse();
        }

        final boolean isSafeForTheKing = chessBoard.safeForKing(from, to);
        if (!isSafeForTheKing) {
            return StatusPair.ofFalse();
        }

        var setOfOperations = new LinkedHashSet<Operations>();

        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && !endField.pieceOptional().get().color().equals(kingColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        return StatusPair.ofTrue(setOfOperations);
    }

    /**
     * Checks if the king will be safe after executing the proposed move.
     *
     * @param chessBoard The current state of the chess board.
     * @param kingPosition The current position of the king.
     * @param from The coordinate from which the piece is being moved.
     * @param to The target coordinate where the piece is intended to move.
     * @return true if the king will be safe after the move, false otherwise.
     * <p>
     * This method checks whether the king will remain in a safe position after the move is executed.
     * If the piece being moved is the king, it checks for castling and validates the king's movement
     * for safety. If the piece is not the king, it checks whether the king would be in danger after
     * the proposed move.
     */
    public boolean safeForKing(final ChessBoard chessBoard, final Coordinate kingPosition, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (kingPosition.equals(from)) {
            if (chessBoard.isCastling(chessBoard.theKing(color), from, to)) {
                return safeToCastle(chessBoard, from, to);
            }

            return validateKingMovementForSafety(chessBoard, from, to);
        }

        return validatePieceMovementForKingSafety(chessBoard, kingPosition, from, to);
    }


    /**
     * Determines if the current position is a stalemate.
     *
     * @param chessBoard The current state of the chess board.
     * @param from The coordinate from which the piece is being moved.
     * @param to The target coordinate where the piece is intended to move.
     * @return true if the position is a stalemate, false otherwise.
     * <p>
     * This method checks if the current position is a stalemate, meaning the player has no legal moves
     * and their king is not in check. It evaluates the surrounding fields of the king to determine if
     * they are blocked or dangerous, and checks all friendly fields to see if any legal moves are available.
     */
    /** TODO for AinGrace.*/
    public boolean stalemate() {
        return false;
    }

    /**
     * Evaluates the status of the king after a proposed move.
     *
     * @param chessBoard The current state of the chess board.
     * @param kingColor The color of King that need to be checked for his status(safe, check, checkmate).
     * @return An instance of the {@link Operations} enum indicating the status of the king:
     *         - {Operations.CHECK} if the king is in check after the move,
     *         - {Operations.CHECKMATE} if the king is in checkmate after the move,
     *         - {Operations.EMPTY} if the king is not in check or checkmate.
     */
    /** TODO for AinGrace.*/
    public Operations kingStatus(final ChessBoard chessBoard, final Color kingColor) {
        if (checkmate(chessBoard, kingColor)) {
            return Operations.CHECKMATE;
        }

        if (!check(chessBoard, kingColor).isEmpty()) {
            return Operations.CHECK;
        }

        return Operations.EMPTY;
    }

    private List<Field> check(final ChessBoard chessBoard, Color kingColor) {
        Coordinate kingCoordinate = ChessBoardUtils.getKingCoordinate(chessBoard, kingColor);
        Color oppositeColor = kingColor.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;

        List<Field> enemies = new ArrayList<>();

        List<Field> pawns = ChessBoardUtils.pawnsThreateningCoordinate(chessBoard, kingCoordinate, oppositeColor);
        if (!pawns.isEmpty()) {
            enemies.addAll(pawns);
        }

        List<Field> knights = ChessBoardUtils.knightAttackPositions(chessBoard, kingCoordinate);
        for (Field possibleKnight : knights) {
            Piece piece = possibleKnight.pieceOptional().get();

            if (piece instanceof Knight && piece.color().equals(oppositeColor)) {
                enemies.add(possibleKnight);
            }
        }

        List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, kingCoordinate);
        for (Field diagonalField : diagonalFields) {
            Piece piece = diagonalField.pieceOptional().get();

            boolean isBishopOrQueen = piece instanceof Bishop || piece instanceof Queen;
            boolean isEnemy = piece.color().equals(oppositeColor);
            if (isBishopOrQueen && isEnemy) {
                enemies.add(diagonalField);
            }
        }

        List<Field> horizontalVerticalFields = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, kingCoordinate);
        for (Field horizontalVerticalField : horizontalVerticalFields) {
            Piece piece = horizontalVerticalField.pieceOptional().get();

            boolean isRookOrQueen = piece instanceof Rook || piece instanceof Queen;
            boolean isEnemy = piece.color().equals(oppositeColor);

            if (isRookOrQueen && isEnemy) {
                enemies.add(horizontalVerticalField);
            }
        }

        return enemies;
    }

    private boolean checkmate(ChessBoard chessBoard, Color kingColor) {
        List<Field> enemies = check(chessBoard, kingColor);

        if (enemies.isEmpty()) {
            return false;
        }
        Coordinate kingCoordinate = ChessBoardUtils.getKingCoordinate(chessBoard, kingColor);

        List<Field> surroundings = ChessBoardUtils.surroundingFields(chessBoard, kingCoordinate);
        boolean surrounded = surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field, kingColor));

        if (enemies.size() > 1 && surrounded) {
            return true;
        }

        if (enemies.size() == 1 && !surrounded) {
            return false;
        }

        for (Field enemy : enemies) {
            boolean canEat = canEat(chessBoard, kingCoordinate, enemy, kingColor);
            boolean canBlock = canBlock(chessBoard, kingCoordinate, enemy, kingColor);

            if (!canEat && !canBlock) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validates whether the movement from the start field to the end field is a valid move for a King on the chessboard.
     *
     * <p>
     * This method checks if the King is moving to an adjacent square (one square in any direction) or if the move is a castling move.
     * A King can move one square in any direction, including diagonally, as long as the destination square is not under attack.
     * If the move is valid, the method returns <code>true</code>; otherwise, it checks if the move is a castling move.
     * </p>
     *
     * <p>
     * Preconditions:
     * <ul>
     *     <li>The caller must ensure that the method <code>safeForKing(...)</code> has been called prior to invoking this method.
     *         This is to confirm that the move does not place the king in check.</li>
     *     <li>The caller must check that neither <code>chessBoard</code> nor <code>startField</code> nor <code>endField</code> is <code>null</code>.</li>
     *     <li>The caller must verify that the <code>endField</code> is not occupied by a piece of the same color as the piece being moved.
     *         This is to ensure that the move does not violate the rules of chess regarding capturing pieces.</li>
     * </ul>
     * </p>
     *
     * @param chessBoard The chessboard on which the move is being validated. This object contains the current state of the board,
     *                   including the positions of all pieces.
     * @param startField The field from which the King is moving. This field should contain the King that is being moved.
     * @param endField   The field to which the King is moving. This field is the target location for the move.
     *
     * @return <code>true</code> if the move is valid (either an adjacent move or a castling move); <code>false</code> otherwise.
     *
     * @throws NoSuchElementException if the starting field does not contain a piece (the King).
     */
    private boolean isValidKingMovementCoordinates(final ChessBoard chessBoard, final Field startField, final Field endField) {
        final Coordinate from = startField.getCoordinate();
        final Coordinate to = endField.getCoordinate();
        final int startColumn = from.columnToInt();
        final int endColumn = to.columnToInt();
        final int startRow = from.getRow();
        final int endRow = to.getRow();

        final boolean surroundField = Math.abs(startColumn - endColumn) <= 1 && Math.abs(startRow - endRow) <= 1;
        if (surroundField) {
            return true;
        }

        return chessBoard.isCastling(startField.pieceOptional().orElseThrow(), from, to);
    }

    private boolean safeToCastle(final ChessBoard chessBoard, final Coordinate presentKingPosition, final Coordinate futureKingPosition) {

        final Castle castle;
        if (presentKingPosition.getColumn() < futureKingPosition.getColumn()) {
            castle = Castle.SHORT_CASTLING;
        } else {
            castle = Castle.LONG_CASTLING;
        }

        final boolean ableToCastle = chessBoard.ableToCastling(color, castle);
        if (!ableToCastle) {
            return false;
        }

        final List<Field> fieldsToCastle = ChessBoardUtils.getCastlingFields(chessBoard, presentKingPosition, futureKingPosition);
        for (final Field field : fieldsToCastle) {

            if (field.isPresent() && !(field.pieceOptional().orElseThrow() instanceof King)) {
                return false;
            }

            final List<Field> pawns = ChessBoardUtils.pawnsThreateningCoordinate(chessBoard, field.getCoordinate(), color);
            for (final Field fieldWithPawn : pawns) {
                final Pawn pawn = (Pawn) fieldWithPawn.pieceOptional().orElseThrow();

                if (!pawn.color().equals(this.color)) {
                    return false;
                }
            }

            final List<Field> knights = ChessBoardUtils.knightAttackPositions(chessBoard, field.getCoordinate());
            for (final Field fieldWithKnight : knights) {
                final Piece piece = fieldWithKnight.pieceOptional().orElseThrow();

                if (piece instanceof Knight && !piece.color().equals(this.color)) {
                    return false;
                }
            }

            final List<Field> occupiedDiagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, field.getCoordinate());
            for (final Field diagonalField : occupiedDiagonalFields) {
                final Piece piece = diagonalField.pieceOptional().orElseThrow();

                if ((piece instanceof Bishop || piece instanceof Queen || piece instanceof King) && !piece.color().equals(this.color)) {
                    return false;
                }
            }

            final List<Field> horizontalVerticalFields = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, field.getCoordinate());
            for (final Field horizontalField : horizontalVerticalFields) {
                final Piece piece = horizontalField.pieceOptional().orElseThrow();

                if ((piece instanceof Rook || piece instanceof Queen || piece instanceof King) && !piece.color().equals(this.color)) {
                    return false;
                }
            }

        }

        return true;
    }

    private boolean validateKingMovementForSafety(final ChessBoard chessBoard, final Coordinate previousKing, final Coordinate futureKing) {

        final List<ChessBoard.Field> pawns = ChessBoardUtils.pawnsThreateningCoordinate(chessBoard, futureKing, color);
        for (final Field possiblePawn : pawns) {
            final Piece pawn = possiblePawn.pieceOptional().orElseThrow();

            final boolean isEnemyPawn = pawn instanceof Pawn && !pawn.color().equals(color);
            if (isEnemyPawn) {
                return false;
            }
        }

        final List<ChessBoard.Field> knights = ChessBoardUtils.knightAttackPositions(chessBoard, futureKing);
        for (final Field possibleKnight : knights) {
            final Piece knight = possibleKnight.pieceOptional().orElseThrow();

            final boolean isEnemyKnight = knight instanceof Knight && !knight.color().equals(color);
            if (isEnemyKnight) {
                return false;
            }
        }

        final List<ChessBoard.Field> diagonalFields = Direction
                .occupiedFieldsFromDiagonalDirections(
                        chessBoard, futureKing, field -> field.isPresent() && !field.getCoordinate().equals(previousKing)
                );

        for (final Field field : diagonalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final boolean isDangerPiece = (piece instanceof Bishop || piece instanceof Queen || piece instanceof King) && !piece.color().equals(color);
            if (isDangerPiece) {
                return false;
            }
        }

        final List<ChessBoard.Field> horizontalVerticalFields = Direction
                .occupiedFieldsFromHorizontalVerticalDirections(
                        chessBoard, futureKing, field -> field.isPresent() && !field.getCoordinate().equals(previousKing)
                );

        for (final Field field : horizontalVerticalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final boolean isEnemyRookOrQueenOrKing = (piece instanceof Rook || piece instanceof Queen || piece instanceof King) && !piece.color().equals(color);
            if (isEnemyRookOrQueenOrKing) {
                return false;
            }
        }

        return true;
    }

    private boolean validatePieceMovementForKingSafety(final ChessBoard chessBoard, final Coordinate kingPosition,
                                                       final Coordinate from, final Coordinate to) {

        final List<Field> pawnsThreateningCoordinates = ChessBoardUtils.pawnsThreateningCoordinate(chessBoard, kingPosition, color);
        for (final Field possiblePawn : pawnsThreateningCoordinates) {
            final Pawn pawn = (Pawn) possiblePawn.pieceOptional().orElseThrow();

            if (!pawn.color().equals(color)) {
                return false;
            }
        }

        final List<Field> potentialKnightAttackPositions = ChessBoardUtils.knightAttackPositions(chessBoard, kingPosition);
        for (final Field potentialKnightAttackPosition : potentialKnightAttackPositions) {
            final Piece piece = potentialKnightAttackPosition.pieceOptional().orElseThrow();

            final boolean isEnemyKnight = piece instanceof Knight && !piece.color().equals(color);
            if (isEnemyKnight) {
                return false;
            }
        }

        final List<Field> occupiedDiagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, kingPosition, from, to);
        for (final Field field : occupiedDiagonalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final boolean isEnemyBishopOrQueen = (piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color);
            if (isEnemyBishopOrQueen) {
                return false;
            }
        }

        final List<Field> occupiedHorizontalAndVerticalFields = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, kingPosition, from, to);
        for (final Field field : occupiedHorizontalAndVerticalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final boolean isEnemyRookOrQueen = (piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color);
            if (isEnemyRookOrQueen) {
                return false;
            }
        }

        return true;
    }

    private boolean canEat(ChessBoard chessBoard, Coordinate king, Field enemyField, Color kingColor) {
        final List<Field> possiblePawns = ChessBoardUtils.pawnsThreateningCoordinate(chessBoard, enemyField.getCoordinate(), kingColor);
        for (final Field possiblePawn : possiblePawns) {
            if (safeForKing(chessBoard, king, possiblePawn.getCoordinate(), enemyField.getCoordinate())) {
                return true;
            }
        }

        final List<Field> knights = ChessBoardUtils.knightAttackPositions(chessBoard, enemyField.getCoordinate());
        for (final Field knight : knights) {
            if (knight.pieceOptional().orElseThrow().color().equals(kingColor)) {
                return safeForKing(chessBoard, king, knight.getCoordinate(), enemyField.getCoordinate());
            }
        }

        final List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, enemyField.getCoordinate());
        for (final Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(kingColor)) {
                return safeForKing(chessBoard, king, diagonalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        final List<Field> horizontalVertical = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, enemyField.getCoordinate());
        for (final Field horizontalVerticalField : horizontalVertical) {
            final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(kingColor)) {
                return safeForKing(chessBoard, king, horizontalVerticalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        return false;
    }

    private boolean canBlock(ChessBoard chessBoard, Coordinate king, Field enemyField, Color kingColor) {
        List<Field> path = Direction.fieldsOfPathExclusive(chessBoard, king, enemyField.getCoordinate());

        for (final Field field : path) {
            final Coordinate currentCoordinate = field.getCoordinate();

            final StatusPair<Coordinate> possibleCoordinate;
            if (Color.WHITE.equals(kingColor)) {
                possibleCoordinate = Coordinate.coordinate(currentCoordinate.getRow() - 1, currentCoordinate.columnToInt());
            } else {
                possibleCoordinate = Coordinate.coordinate(currentCoordinate.getRow() + 1, currentCoordinate.columnToInt());
            }

            if (possibleCoordinate.status()) {
                final Coordinate pawnCoordinate = possibleCoordinate.orElseThrow();
                final Field possiblePawn = chessBoard.field(pawnCoordinate);

                if (possiblePawn.isPresent() && possiblePawn.pieceOptional().orElseThrow().color().equals(kingColor)) {
                    return safeForKing(chessBoard, king, pawnCoordinate, currentCoordinate);
                }
            }

            final List<Field> knights = ChessBoardUtils.knightAttackPositions(chessBoard, currentCoordinate);
            for (final Field knight : knights) {
                Piece piece = knight.pieceOptional().orElseThrow();
                if (piece instanceof Knight && piece.color().equals(kingColor)) {
                    return safeForKing(chessBoard, king, knight.getCoordinate(), currentCoordinate);
                }
            }

            final List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, currentCoordinate);
            for (final Field diagonalField : diagonalFields) {
                final Piece piece = diagonalField.pieceOptional().orElseThrow();

                if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(kingColor)) {
                    return safeForKing(chessBoard, king, diagonalField.getCoordinate(), currentCoordinate);
                }
            }

            final List<Field> horizontalVertical = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, currentCoordinate);
            for (final Field horizontalVerticalField : horizontalVertical) {
                final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

                if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(kingColor)) {
                    return safeForKing(chessBoard, king, horizontalVerticalField.getCoordinate(), currentCoordinate);
                }
            }
        }

        return false;
    }

    private boolean fieldIsBlockedOrDangerous(ChessBoard chessBoard, Field field, Color kingColor) {
        if (field.isPresent() && field.pieceOptional().orElseThrow().color().equals(kingColor)) {
            return true;
        }

        Color oppositeColor = kingColor.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;

        final List<Field> pawns = ChessBoardUtils.pawnsThreateningCoordinate(chessBoard, field.getCoordinate(), oppositeColor);
        if (!pawns.isEmpty()) {
            return true;
        }

        final List<Field> knights = ChessBoardUtils.knightAttackPositions(chessBoard, field.getCoordinate());
        for (final Field possibleKnight : knights) {
            final Piece piece = possibleKnight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && !piece.color().equals(kingColor)) {
                return true;
            }
        }

        final List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, field.getCoordinate());
        for (final Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if (piece instanceof Bishop && !piece.color().equals(kingColor)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(kingColor)) {
                return true;
            }
        }

        final List<Field> horizontalVerticalFields = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, field.getCoordinate(), field1 -> {
            if (field1.isPresent()) {
                Piece piece = field1.pieceOptional().get();

                return !(piece instanceof King) || !piece.color().equals(kingColor);
            }
            return false;
        });

        for (final Field horizontalVerticalField : horizontalVerticalFields) {
            final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            if (piece instanceof Rook && !piece.color().equals(kingColor)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(kingColor)) {
                return true;
            }
        }

        return false;
    }
}