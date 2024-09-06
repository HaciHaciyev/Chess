package core.project.chess.domain.aggregates.chess.pieces;

import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation.Castle;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.ChessBoardNavigator;
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

        if (!(startField.pieceOptional().orElseThrow() instanceof King(var kingColor))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() && endField.pieceOptional().orElseThrow().color().equals(kingColor);
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

        final boolean opponentPieceInEndField = endField.pieceOptional().isPresent() && !endField.pieceOptional().orElseThrow().color().equals(kingColor);
        if (opponentPieceInEndField) {
            setOfOperations.add(Operations.CAPTURE);
        }

        return StatusPair.ofTrue(setOfOperations);
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
     * @return <code>true</code> if the move is valid (either an adjacent move or a castling move); <code>false</code> otherwise.
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

    /**
     * Checks if the king will be safe after executing the proposed move.
     *
     * @param chessBoard   The current state of the chess board.
     * @param kingPosition The current position of the king.
     * @param from         The coordinate from which the piece is being moved.
     * @param to           The target coordinate where the piece is intended to move.
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

        ChessBoardNavigator boardNavigator = new ChessBoardNavigator(chessBoard);

        if (kingPosition.equals(from)) {
            if (chessBoard.isCastling(chessBoard.theKing(color), from, to)) {
                return safeToCastle(boardNavigator, from, to);
            }

            return validateKingMovementForSafety(boardNavigator, from, to);
        }

        return validatePieceMovementForKingSafety(boardNavigator, kingPosition, from, to);
    }

    /**
     * Evaluates the status of the king after a proposed move.
     *
     * @param chessBoard The current state of the chess board.
     * @param kingColor  The color of King that need to be checked for his status(safe, check, checkmate).
     * @return An instance of the {@link Operations} enum indicating the status of the king:
     * - {Operations.CHECK} if the king is in check after the move,
     * - {Operations.CHECKMATE} if the king is in checkmate after the move,
     * - {Operations.EMPTY} if the king is not in check or checkmate.
     */
    public Operations kingStatus(final ChessBoard chessBoard, final Color kingColor) {
        ChessBoardNavigator boardNavigator = new ChessBoardNavigator(chessBoard);

        if (checkmate(boardNavigator, kingColor)) {
            return Operations.CHECKMATE;
        }

        if (!check(boardNavigator, kingColor).isEmpty()) {
            return Operations.CHECK;
        }

        return Operations.EMPTY;
    }

    /**
     * Determines if the current position is a stalemate.
     *
     * @param chessBoard The current state of the chess board.
     * @param color      color of the king
     * @return true if the position is a stalemate, false otherwise.
     * <p>
     * This method checks if the current position is a stalemate, meaning the player has no legal moves
     * and their king is not in check. It evaluates the surrounding fields of the king to determine if
     * they are blocked or dangerous, and checks all friendly fields to see if any legal moves are available.
     */
    public boolean stalemate(ChessBoard chessBoard, Color color) {
        ChessBoardNavigator boardNavigator = new ChessBoardNavigator(chessBoard);
        Coordinate kingCoordinate = boardNavigator.kingCoordinate(color);

        List<Field> enemies = check(boardNavigator, color);

        if (!enemies.isEmpty()) {
            return false;
        }

        List<Field> surroundingFieldsOfKing = boardNavigator.surroundingFields(kingCoordinate);

        boolean isSurrounded = surroundingFieldsOfKing.stream().allMatch(field -> fieldIsBlockedOrDangerous(boardNavigator, field, color));

        if (!isSurrounded) {
            return false;
        }


        List<Field> ourFields = boardNavigator.allFriendlyFields(color, field -> !field.getCoordinate().equals(kingCoordinate));

        for (Field ourField : ourFields) {
            boolean stalemate = processStalemate(boardNavigator, kingCoordinate, ourField);

            if (!stalemate) {
                return false;
            }
        }

        return true;
    }

    private boolean processStalemate(ChessBoardNavigator boardNavigator, Coordinate kingCoordinate, Field ourField) {
        Piece piece = ourField.pieceOptional().orElseThrow();
        Coordinate coordinate = ourField.getCoordinate();
        ChessBoard board = boardNavigator.board();

        if (piece instanceof Pawn pawn) {

            final List<Field> pawnCoordinates = boardNavigator.coordinatesThreatenedByPawn(coordinate, color);
            Optional<Field> forwardMove = boardNavigator.forwardField(coordinate, Field::isEmpty);
            forwardMove.ifPresent(pawnCoordinates::add);

            for (final Field coord : pawnCoordinates) {

                if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                    continue;
                }

                if (pawn.validate(boardNavigator.board(), new LinkedHashSet<>(), ourField, coord).status()) {
                    return !safeForKing(board, kingCoordinate, ourField.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Knight knight) {
            final List<Field> coords = boardNavigator.knightAttackPositions(kingCoordinate);

            for (Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                    continue;
                }

                if (knight.knightMove(ourField.getCoordinate(), coord.getCoordinate())) {
                    return !safeForKing(board, kingCoordinate, ourField.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Bishop bishop) {
            final List<Field> coords = boardNavigator.fieldsInDirections(Direction.diagonalDirections(), coordinate);

            for (final Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                    continue;
                }

                if (bishop.validate(board, ourField, coord)) {
                    return !safeForKing(board, kingCoordinate, ourField.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Rook rook) {
            final List<Field> coords = boardNavigator.fieldsInDirections(Direction.horizontalVerticalDirections(), coordinate);

            for (Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                    continue;
                }

                if (rook.validate(board, ourField, coord)) {
                    return !safeForKing(board, kingCoordinate, ourField.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Queen queen) {
            final List<Field> coords = boardNavigator.fieldsInDirections(Direction.allDirections(), coordinate);

            for (final Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                    continue;
                }

                if (queen.validate(board, ourField, coord)) {
                    return !safeForKing(board, kingCoordinate, ourField.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        return true;
    }

    private List<Field> check(final ChessBoardNavigator boardNavigator, Color kingColor) {
        Coordinate kingCoordinate = boardNavigator.kingCoordinate(kingColor);
        Color oppositeColor = kingColor.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
        List<Field> enemies = new ArrayList<>();


        List<Field> pawns = boardNavigator.pawnsThreateningCoordinate(kingCoordinate, oppositeColor);
        if (!pawns.isEmpty()) {
            enemies.addAll(pawns);
        }

        List<Field> knights = boardNavigator.knightAttackPositions(kingCoordinate);
        for (Field possibleKnight : knights) {
            Piece piece = possibleKnight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && piece.color().equals(oppositeColor)) {
                enemies.add(possibleKnight);
            }
        }

        List<Field> enemiesFromAllDirections = enemiesFromAllDirections(boardNavigator, kingCoordinate, oppositeColor);
        enemies.addAll(enemiesFromAllDirections);

        return enemies;
    }

    private List<Field> enemiesFromAllDirections(ChessBoardNavigator boardNavigator, Coordinate kingCoordinate, Color oppositeColor)  {
        List<Field> enemies = new ArrayList<>();

        List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), kingCoordinate);
        for (Field diagonalField : diagonalFields) {
            Piece piece = diagonalField.pieceOptional().orElseThrow();

            boolean isBishopOrQueen = piece instanceof Bishop || piece instanceof Queen;
            boolean isEnemy = piece.color().equals(oppositeColor);
            if (isBishopOrQueen && isEnemy) {
                enemies.add(diagonalField);
            }
        }

        List<Field> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), kingCoordinate);
        for (Field horizontalVerticalField : horizontalVerticalFields) {
            Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            boolean isRookOrQueen = piece instanceof Rook || piece instanceof Queen;
            boolean isEnemy = piece.color().equals(oppositeColor);

            if (isRookOrQueen && isEnemy) {
                enemies.add(horizontalVerticalField);
            }
        }

        return enemies;
    }

    private boolean checkmate(ChessBoardNavigator boardNavigator, Color kingColor) {
        List<Field> enemies = check(boardNavigator, kingColor);

        if (enemies.isEmpty()) {
            return false;
        }
        Coordinate kingCoordinate = boardNavigator.kingCoordinate(kingColor);

        List<Field> surroundings = boardNavigator.surroundingFields(kingCoordinate);
        boolean surrounded = surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(boardNavigator, field, kingColor));

        if (enemies.size() > 1 && surrounded) {
            return true;
        }

        if (enemies.size() == 1 && !surrounded) {
            return false;
        }

        for (Field enemy : enemies) {
            boolean canEat = canEat(boardNavigator, kingCoordinate, enemy, kingColor);
            boolean canBlock = canBlock(boardNavigator, kingCoordinate, enemy, kingColor);

            if (!canEat && !canBlock) {
                return true;
            }
        }

        return false;
    }

    private boolean safeToCastle(final ChessBoardNavigator boardNavigator, final Coordinate presentKingPosition, final Coordinate futureKingPosition) {
        final Castle castle;
        if (presentKingPosition.getColumn() < futureKingPosition.getColumn()) {
            castle = Castle.SHORT_CASTLING;
        } else {
            castle = Castle.LONG_CASTLING;
        }

        final boolean ableToCastle = boardNavigator.board().ableToCastling(color, castle);
        if (!ableToCastle) {
            return false;
        }

        final List<Field> fieldsToCastle = boardNavigator.castlingFields(presentKingPosition, futureKingPosition);
        for (final Field field : fieldsToCastle) {
            boolean canCastle = processCastling(boardNavigator, field);

            if (!canCastle) {
                return false;
            }
        }

        return true;
    }

    private boolean processCastling(ChessBoardNavigator boardNavigator, Field field) {
        if (field.isPresent() && !(field.pieceOptional().orElseThrow() instanceof King)) {
            return false;
        }

        Color oppositeColor = color.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
        List<Field> pawns = boardNavigator.pawnsThreateningCoordinate(field.getCoordinate(), oppositeColor);
        for (Field pawn : pawns) {
            Piece piece = pawn.pieceOptional().orElseThrow();
            if (!piece.color().equals(this.color)) {
                return false;
            }
        }

        List<Field> knights = boardNavigator.knightAttackPositions(field.getCoordinate());
        for (Field knight : knights) {
            Piece piece = knight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && !piece.color().equals(this.color)) {
                return false;
            }
        }

        List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), field.getCoordinate());
        for (Field diagonalField : diagonalFields) {
            Piece piece = diagonalField.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(this.color)) {
                return false;
            }
        }

        List<Field> horizontalVertical = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), field.getCoordinate());
        for (Field horizontalField : horizontalVertical) {
            Piece piece = horizontalField.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(this.color)) {
                return false;
            }
        }

        List<Field> surroundings = boardNavigator.surroundingFields(field.getCoordinate(), Field::isEmpty);
        for (Field surroundingField : surroundings) {
            Piece piece = surroundingField.pieceOptional().orElseThrow();

            if (piece instanceof King && !piece.color().equals(this.color)) {
                return false;
            }
        }

        return true;
    }

    private boolean validateKingMovementForSafety(final ChessBoardNavigator boardNavigator, final Coordinate previousKing, final Coordinate futureKing) {
        final List<ChessBoard.Field> pawns = boardNavigator.pawnsThreateningCoordinate(futureKing, color);
        for (final Field possiblePawn : pawns) {
            final Piece pawn = possiblePawn.pieceOptional().orElseThrow();

            final boolean isEnemyPawn = pawn instanceof Pawn && !pawn.color().equals(color);
            if (isEnemyPawn) {
                return false;
            }
        }

        final List<ChessBoard.Field> knights = boardNavigator.knightAttackPositions(futureKing);
        for (final Field possibleKnight : knights) {
            final Piece knight = possibleKnight.pieceOptional().orElseThrow();

            final boolean isEnemyKnight = knight instanceof Knight && !knight.color().equals(color);
            if (isEnemyKnight) {
                return false;
            }
        }

        final List<ChessBoard.Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.diagonalDirections(), futureKing, field -> !field.getCoordinate().equals(previousKing)
        );

        for (final Field field : diagonalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final boolean isDangerPiece = (piece instanceof Bishop || piece instanceof Queen || piece instanceof King) && !piece.color().equals(color);
            if (isDangerPiece) {
                return false;
            }
        }

        final List<ChessBoard.Field> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(), futureKing, field -> !field.getCoordinate().equals(previousKing)
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

    private boolean validatePieceMovementForKingSafety(final ChessBoardNavigator boardNavigator, final Coordinate kingPosition,
                                                       final Coordinate from, final Coordinate to) {

        final List<Field> pawnsThreateningCoordinates = boardNavigator.pawnsThreateningCoordinate(kingPosition, color);
        for (final Field possiblePawn : pawnsThreateningCoordinates) {
            final Pawn pawn = (Pawn) possiblePawn.pieceOptional().orElseThrow();

            if (!pawn.color().equals(color)) {
                return false;
            }
        }

        final List<Field> potentialKnightAttackPositions = boardNavigator.knightAttackPositions(kingPosition);
        for (final Field potentialKnightAttackPosition : potentialKnightAttackPositions) {
            final Piece piece = potentialKnightAttackPosition.pieceOptional().orElseThrow();

            boolean isEaten = potentialKnightAttackPosition.getCoordinate().equals(to);

            final boolean isEnemyKnight = piece instanceof Knight && !piece.color().equals(color);
            if (isEnemyKnight && !isEaten) {
                return false;
            }
        }

        final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.diagonalDirections(),
                kingPosition,
                coordinate -> coordinate.equals(from),
                coordinate -> coordinate.equals(to),
                field -> true
        );

        for (final Field field : diagonalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final boolean isEnemyBishopOrQueen = (piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color);
            if (isEnemyBishopOrQueen) {
                return false;
            }
        }

        final List<Field> horizontalVertical = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(),
                kingPosition,
                coordinate -> coordinate.equals(from),
                coordinate -> coordinate.equals(to),
                field -> true
        );
        for (final Field field : horizontalVertical) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final boolean isEnemyRookOrQueen = (piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color);
            if (isEnemyRookOrQueen) {
                return false;
            }
        }

        return true;
    }

    private boolean canEat(ChessBoardNavigator boardNavigator, Coordinate king, Field enemyField, Color kingColor) {
        final List<Field> possiblePawns = boardNavigator.pawnsThreateningCoordinate(enemyField.getCoordinate(), kingColor);
        for (final Field possiblePawn : possiblePawns) {
            if (safeForKing(boardNavigator.board(), king, possiblePawn.getCoordinate(), enemyField.getCoordinate())) {
                return true;
            }
        }

        final List<Field> knights = boardNavigator.knightAttackPositions(enemyField.getCoordinate());
        for (final Field knight : knights) {
            if (knight.pieceOptional().orElseThrow().color().equals(kingColor)) {
                return safeForKing(boardNavigator.board(), king, knight.getCoordinate(), enemyField.getCoordinate());
            }
        }

        final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), enemyField.getCoordinate());
        for (final Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(kingColor)) {
                return safeForKing(boardNavigator.board(), king, diagonalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        final List<Field> horizontalVertical = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), enemyField.getCoordinate());
        for (final Field horizontalVerticalField : horizontalVertical) {
            final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(kingColor)) {
                return safeForKing(boardNavigator.board(), king, horizontalVerticalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        return false;
    }

    private boolean canBlock(ChessBoardNavigator boardNavigator, Coordinate king, Field enemyField, Color kingColor) {
        List<Field> path = boardNavigator.fieldsInPath(king, enemyField.getCoordinate(), false);

        for (final Field field : path) {
            final Coordinate currentCoordinate = field.getCoordinate();

            final StatusPair<Coordinate> possibleCoordinate;
            if (Color.WHITE.equals(kingColor)) {
                possibleCoordinate = Coordinate.of(currentCoordinate.getRow() - 1, currentCoordinate.columnToInt());
            } else {
                possibleCoordinate = Coordinate.of(currentCoordinate.getRow() + 1, currentCoordinate.columnToInt());
            }

            if (possibleCoordinate.status()) {
                final Coordinate pawnCoordinate = possibleCoordinate.orElseThrow();
                final Field possiblePawn = boardNavigator.board().field(pawnCoordinate);

                if (possiblePawn.isPresent() && possiblePawn.pieceOptional().orElseThrow().color().equals(kingColor)) {
                    return safeForKing(boardNavigator.board(), king, pawnCoordinate, currentCoordinate);
                }
            }

            final List<Field> knights = boardNavigator.knightAttackPositions(currentCoordinate);
            for (final Field knight : knights) {
                Piece piece = knight.pieceOptional().orElseThrow();
                if (piece instanceof Knight && piece.color().equals(kingColor)) {
                    return safeForKing(boardNavigator.board(), king, knight.getCoordinate(), currentCoordinate);
                }
            }

            final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), enemyField.getCoordinate());
            for (final Field diagonalField : diagonalFields) {
                final Piece piece = diagonalField.pieceOptional().orElseThrow();

                if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(kingColor)) {
                    return safeForKing(boardNavigator.board(), king, diagonalField.getCoordinate(), currentCoordinate);
                }
            }

            final List<Field> horizontalVertical = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), enemyField.getCoordinate());
            for (final Field horizontalVerticalField : horizontalVertical) {
                final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

                if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(kingColor)) {
                    return safeForKing(boardNavigator.board(), king, horizontalVerticalField.getCoordinate(), currentCoordinate);
                }
            }
        }

        return false;
    }

    private boolean fieldIsBlockedOrDangerous(ChessBoardNavigator boardNavigator, Field field, Color kingColor) {
        if (field.isPresent() && field.pieceOptional().orElseThrow().color().equals(kingColor)) {
            return true;
        }

        Color oppositeColor = kingColor.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;

        final List<Field> pawns = boardNavigator.pawnsThreateningCoordinate(field.getCoordinate(), oppositeColor);
        if (!pawns.isEmpty()) {
            return true;
        }

        final List<Field> knights = boardNavigator.knightAttackPositions(field.getCoordinate());
        for (final Field possibleKnight : knights) {
            final Piece piece = possibleKnight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && !piece.color().equals(kingColor)) {
                return true;
            }
        }

        final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), field.getCoordinate());
        for (final Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if (piece instanceof Bishop && !piece.color().equals(kingColor)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(kingColor)) {
                return true;
            }
        }

        final List<Field> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), field.getCoordinate(), field1 -> {
            Piece piece = field1.pieceOptional().orElseThrow();

            return !(piece instanceof King) || !piece.color().equals(kingColor);
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