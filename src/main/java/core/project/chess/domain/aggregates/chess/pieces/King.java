package core.project.chess.domain.aggregates.chess.pieces;

import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation;
import core.project.chess.domain.aggregates.chess.entities.AlgebraicNotation.Castle;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard;
import core.project.chess.domain.aggregates.chess.entities.ChessBoard.Field;
import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.utilities.Direction;
import core.project.chess.infrastructure.utilities.StatusPair;

import java.util.*;
import java.util.stream.Stream;

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
        setOfOperations.add(influenceOnTheOpponentKing(chessBoard, from, to));

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
     * Checks if the king will be in check after executing the move.
     *
     * @param chessBoard The current state of the chess board.
     * @param from The coordinate from which the piece is being moved.
     * @param to The target coordinate where the piece is intended to move.
     * @return true if the king will be in check after the move, false otherwise.
     * <p>
     * This method determines whether the move made by the specified piece will result in the king
     * being in check. It evaluates the type of piece being moved and applies the appropriate logic
     * to determine if the king is threatened after the proposed move.
     */
    public boolean check(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        final Piece piece = chessBoard.field(from).pieceOptional().orElseThrow();
        final Coordinate king = getKingCoordinate(chessBoard);

        return switch (piece) {
            case Pawn x -> !pawnMovedCheck(chessBoard, king, from, to).isEmpty();
            case Knight x -> !knightMovedCheck(chessBoard, king, from, to).isEmpty();
            case Bishop x -> !validateDirectionsCheck(chessBoard, king, from, to, Bishop.class).isEmpty();
            case Rook x -> !validateDirectionsCheck(chessBoard, king, from, to, Rook.class).isEmpty();
            case Queen x -> !validateDirectionsCheck(chessBoard, king, from, to, Queen.class).isEmpty();
            case King x -> !validateDirectionsCheck(chessBoard, king, from, to, King.class).isEmpty();
        };
    }

    /**
     * Determines if the current move results in checkmate.
     *
     * @param chessBoard The current state of the chess board.
     * @param from The coordinate from which the piece is being moved.
     * @param to The target coordinate where the piece is intended to move.
     * @return true if the move results in checkmate, false otherwise.
     * <p>
     * This method checks if the move made by the specified piece will result in the opponent's king
     * being in checkmate. It evaluates the type of piece being moved and applies the appropriate
     * logic to determine if the opponent's king has no legal moves left and no one from allies pieces can block attack and King is in check.
     */
    public boolean checkmate(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        final Piece piece = chessBoard.field(from).pieceOptional().orElseThrow();
        final Coordinate king = getKingCoordinate(chessBoard);

        return switch (piece) {
            case Pawn x -> pawnMovedCheckmate(chessBoard, king, from, to);
            case Knight x -> knightMovedCheckmate(chessBoard, king, from, to);
            case Bishop x -> validateDirectionsCheckmate(chessBoard, king, from, to, Bishop.class);
            case Rook x -> validateDirectionsCheckmate(chessBoard, king, from, to, Rook.class);
            case Queen x -> validateDirectionsCheckmate(chessBoard, king, from, to, Queen.class);
            case King x -> validateDirectionsCheckmate(chessBoard, king, from, to, King.class);
        };
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
    public boolean stalemate(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        final Coordinate king = getKingCoordinate(chessBoard);

        final boolean check = check(chessBoard, from, to);
        if (check) {
            return false;
        }

        final List<Field> surroundings = surroundingFields(chessBoard, king);

        final boolean surrounded = surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));
        if (!surrounded) {
            return false;
        }

        final List<Field> fields = getAllFriendlyFields(chessBoard);

        for (final Field field : fields) {
            final boolean result = processFields(chessBoard, king, field);

            if (!result) {
                return false;
            }
        }

        return true;
    }

    private boolean processFields(ChessBoard chessBoard, Coordinate king, Field field) {
        final Piece piece = field.pieceOptional().orElseThrow();
        final Coordinate coordinate = field.getCoordinate();

        if (piece instanceof Pawn pawn) {

            //TODO imitate a move for pawn to see
            // in case of stalemate match pawn can't see the Queen in front of it


            final List<Field> pawnCoordinates = coordinatesThreatenedByPawn(chessBoard, coordinate, color);

            Optional<Field> forwardMove = getForwardMove(chessBoard, coordinate);
            forwardMove.ifPresent(pawnCoordinates::add);

            for (final Field coord : pawnCoordinates) {

                if (coord.isPresent() && coord.pieceOptional().get().color().equals(color)) {
                    continue;
                }

                if (pawn.validate(chessBoard, new LinkedHashSet<>(), field, coord).status()) {
                    return !safeForKing(chessBoard, king, field.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Knight knight) {
            final List<Field> coords = knightAttackPositions(chessBoard, king);

            for (Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().get().color().equals(color)) {
                    continue;
                }

                if (knight.knightMove(field.getCoordinate(), coord.getCoordinate())) {
                    return !safeForKing(chessBoard, king, field.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Bishop bishop) {
            final List<Field> coords = Direction.fieldsFromDiagonalDirections(chessBoard, coordinate);

            for (final Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().get().color().equals(color)) {
                    continue;
                }

                if (bishop.validate(chessBoard, field, coord)) {
                    return !safeForKing(chessBoard, king, field.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Rook rook) {

            final List<Field> coords = Direction.fieldsFromHorizontalAndVerticalDirections(chessBoard, coordinate);

            for (Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().get().color().equals(color)) {
                    continue;
                }

                if (rook.validate(chessBoard, field, coord)) {
                    return !safeForKing(chessBoard, king, field.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        if (piece instanceof Queen queen) {
            final List<Field> coords = Direction.fieldsFromAllDirections(chessBoard, coordinate);

            for (final Field coord : coords) {

                if (coord.isPresent() && coord.pieceOptional().get().color().equals(color)) {
                    continue;
                }

                if (queen.validate(chessBoard, field, coord)) {
                    return !safeForKing(chessBoard, king, field.getCoordinate(), coord.getCoordinate());
                }
            }
        }

        return true;
    }

    private Optional<Field> getForwardMove(ChessBoard chessBoard, Coordinate coordinate) {
        Field pawnField = chessBoard.field(coordinate);
        Piece piece = pawnField.pieceOptional().orElseThrow();

        if (piece.color().equals(Color.WHITE)) {

            final StatusPair<Coordinate> possibleForwardCoordinate = Coordinate.coordinate(
                            coordinate.getRow() + 1, coordinate.getColumnAsInt()
                    );

            if (possibleForwardCoordinate.status()) {
                Field field = chessBoard.field(possibleForwardCoordinate.orElseThrow());
                return Optional.of(field);
            }

        } else {

            final StatusPair<Coordinate> possibleForwardCoordinate = Coordinate
                    .coordinate(
                            coordinate.getRow() - 1, coordinate.getColumnAsInt()
                    );

            if (possibleForwardCoordinate.status()) {
                Field field = chessBoard.field(possibleForwardCoordinate.orElseThrow());
                return Optional.of(field);
            }
        }

        return Optional.empty();
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
        final int startColumn = columnToInt(from.getColumn());
        final int endColumn = columnToInt(to.getColumn());
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

        final List<Field> fieldsToCastle = getCastlingFields(chessBoard, presentKingPosition, futureKingPosition);
        for (final Field field : fieldsToCastle) {

            if (field.isPresent() && !(field.pieceOptional().orElseThrow() instanceof King)) {
                return false;
            }

            final List<Field> pawns = pawnsThreateningCoordinate(chessBoard, field.getCoordinate(), color);
            for (final Field fieldWithPawn : pawns) {
                final Pawn pawn = (Pawn) fieldWithPawn.pieceOptional().orElseThrow();

                if (!pawn.color().equals(this.color)) {
                    return false;
                }
            }

            final List<Field> knights = knightAttackPositions(chessBoard, field.getCoordinate());
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

    private List<Field> getCastlingFields(final ChessBoard chessBoard, final Coordinate presentKing, final Coordinate futureKingPosition) {
        final char from = presentKing.getColumn();
        final char to = futureKingPosition.getColumn();

        final List<Field> fields = new ArrayList<>();
        fields.add(chessBoard.field(presentKing));

        final boolean shortCastling = from < to;
        if (shortCastling) {

            int row = presentKing.getRow();
            int column = AlgebraicNotation.columnToInt(presentKing.getColumn()) + 1;

            do {
                final Coordinate coordinate = Coordinate
                        .coordinate(row, column)
                        .orElseThrow(
                                () -> new IllegalStateException("Can`t create coordinate. The method needs repair.")
                        );

                if (coordinate.equals(futureKingPosition)) {
                    fields.add(chessBoard.field(coordinate));
                    return fields;
                }

                fields.add(chessBoard.field(coordinate));
                column++;
            } while (true);

        }

        final boolean longCastling = from > to;
        if (longCastling) {

            int row = presentKing.getRow();
            int column = AlgebraicNotation.columnToInt(presentKing.getColumn()) - 1;

            do {
                final Coordinate coordinate = Coordinate
                        .coordinate(row, column)
                        .orElseThrow(
                                () -> new IllegalStateException("Can`t create coordinate. The method needs repair.")
                        );

                if (coordinate.equals(futureKingPosition)) {
                    fields.add(chessBoard.field(coordinate));
                    return fields;
                }

                fields.add(chessBoard.field(coordinate));
                column--;
            } while (true);

        }

        return fields;
    }

    private boolean validateKingMovementForSafety(final ChessBoard chessBoard, final Coordinate previousKing, final Coordinate futureKing) {

        final List<ChessBoard.Field> pawns = pawnsThreateningCoordinate(chessBoard, futureKing, color);
        for (final Field possiblePawn : pawns) {
            final Piece pawn = possiblePawn.pieceOptional().orElseThrow();

            final boolean isEnemyPawn = pawn instanceof Pawn && !pawn.color().equals(color);
            if (isEnemyPawn) {
                return false;
            }
        }

        final List<ChessBoard.Field> knights = knightAttackPositions(chessBoard, futureKing);
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

        final List<Field> pawnsThreateningCoordinates = pawnsThreateningCoordinate(chessBoard, kingPosition, color);
        for (final Field possiblePawn : pawnsThreateningCoordinates) {
            final Pawn pawn = (Pawn) possiblePawn.pieceOptional().orElseThrow();

            if (!pawn.color().equals(color)) {
                return false;
            }
        }

        final List<Field> potentialKnightAttackPositions = knightAttackPositions(chessBoard, kingPosition);
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

    private List<Field> pawnMovedCheck(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        final List<Field> checkFields = new ArrayList<>();

        final List<ChessBoard.Field> possibleKings = coordinatesThreatenedByPawn(chessBoard, to, color);
        for (final Field possibleKing : possibleKings) {
            if (possibleKing.isPresent() && possibleKing.getCoordinate().equals(king)) {
                checkFields.add(new Field(to, oppositePiece(Pawn.class)));
            }
        }

        final List<Field> checksFromOtherDirections = validateDirectionsCheck(chessBoard, king, from, to, Pawn.class);
        if (!checksFromOtherDirections.isEmpty()) {
            checkFields.addAll(checksFromOtherDirections);
            return checkFields;
        }

        return checkFields;
    }

    private List<Field> knightMovedCheck(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        final List<Field> checkFields = new ArrayList<>();

        final List<ChessBoard.Field> possibleKings = knightAttackPositions(chessBoard, to);
        for (Field possibleKing : possibleKings) {
            if (possibleKing.getCoordinate().equals(king)) {
                checkFields.add(new Field(to, oppositePiece(Knight.class)));
            }
        }

        final List<Field> checksFromOtherDirections = validateDirectionsCheck(chessBoard, king, from, to, Knight.class);
        if (!checksFromOtherDirections.isEmpty()) {
            checkFields.addAll(checksFromOtherDirections);
            return checkFields;
        }

        return checkFields;
    }

    private List<Field> validateDirectionsCheck(ChessBoard chessBoard, Coordinate king,
                                                Coordinate from, Coordinate to,
                                                Class<? extends Piece> classType) {

        final List<Field> checkFields = new ArrayList<>();

        final List<ChessBoard.Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, king, to, oppositePiece(classType));
        for (final Field field : diagonalFields) {

            if (field.getCoordinate().equals(from)) {
                continue;
            }


            final Piece piece = field.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color)) {
                checkFields.add(new Field(to, piece));
            }
        }

        final List<ChessBoard.Field> horizontalAndVerticalFields =
                Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, king, to, oppositePiece(classType));
        for (final Field field : horizontalAndVerticalFields) {
            if (field.getCoordinate().equals(from)) {
                continue;
            }

            final Piece piece = field.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color)) {
                checkFields.add(new Field(to, piece));
            }
        }

        return checkFields;
    }

    private boolean pawnMovedCheckmate(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        final List<Field> enemies = pawnMovedCheck(chessBoard, king, from, to);
        if (enemies.size() != 1) {
            return false;
        }

        final List<Field> surroundings = surroundingFields(chessBoard, king, enemies.getFirst());

        final boolean surrounded = surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));
        if (!surrounded) {
            return false;
        }

        final Field enemyField = enemies.getFirst();

        final Piece enemy = enemyField.pieceOptional().orElseThrow();
        if (enemy instanceof Pawn) {
            return true;
        }

        if (enemy instanceof Bishop || enemy instanceof Rook || enemy instanceof Queen) {

            final List<Field> fields = Direction.fieldsOfPathExclusive(chessBoard, king, enemyField.getCoordinate());

            final boolean block = canBlock(chessBoard, fields, king);
            final boolean eat = canEat(chessBoard, king, enemyField);

            return block || eat;
        }

        return false;
    }

    private boolean knightMovedCheckmate(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to) {
        final List<Field> enemies = knightMovedCheck(chessBoard, king, from, to);
        if (enemies.isEmpty()) {
            return false;
        }

        final List<Field> surroundings = surroundingFields(chessBoard, king, enemies);

        if (enemies.size() > 1) {
            return surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));
        }

        final Field enemyField = enemies.getFirst();
        final Piece enemy = enemyField.pieceOptional().orElseThrow();

        if (enemy instanceof Bishop || enemy instanceof Rook || enemy instanceof Queen) {

            final List<Field> fields = Direction.fieldsOfPathExclusive(chessBoard, king, enemyField.getCoordinate());

            final boolean block = canBlock(chessBoard, fields, king);
            final boolean eat = canEat(chessBoard, king, enemyField);

            return block || eat;
        }

        return false;
    }

    private boolean validateDirectionsCheckmate(ChessBoard chessBoard, Coordinate king, Coordinate from, Coordinate to, Class<? extends Piece> classType) {
        final List<Field> enemies = validateDirectionsCheck(chessBoard, king, from, to, classType);
        if (enemies.isEmpty()) {
            return false;
        }

        final List<Field> surroundings = surroundingFields(chessBoard, king, enemies);

        boolean surrounded = surroundings.stream().allMatch(field -> fieldIsBlockedOrDangerous(chessBoard, field));
        if (enemies.size() > 1) {
            return surrounded;
        }

        if (!surrounded) {
            return false;
        }

        final Field enemyField = enemies.getFirst();
        final Piece enemy = enemyField.pieceOptional().orElseThrow();

        if (enemy instanceof Bishop || enemy instanceof Rook || enemy instanceof Queen) {

            final List<Field> fields = Direction.fieldsOfPathExclusive(chessBoard, king, enemyField.getCoordinate());

            final boolean block = canBlock(chessBoard, fields, king);
            final boolean eat = canEat(chessBoard, king, enemyField);

            return !block || eat;
        }

        return false;
    }

    private List<Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot, List<Field> replace) {
        int row = pivot.getRow();
        char column = pivot.getColumn();
        var up = Coordinate.coordinate(row + 1, AlgebraicNotation.columnToInt(column));
        var down = Coordinate.coordinate(row - 1, AlgebraicNotation.columnToInt(column));
        var left = Coordinate.coordinate(row, AlgebraicNotation.columnToInt(column) - 1);
        var right = Coordinate.coordinate(row, AlgebraicNotation.columnToInt(column) + 1);
        var downLeft = Coordinate.coordinate(row - 1, AlgebraicNotation.columnToInt(column) - 1);
        var downRight = Coordinate.coordinate(row - 1, AlgebraicNotation.columnToInt(column) + 1);
        var upperLeft = Coordinate.coordinate(row + 1, AlgebraicNotation.columnToInt(column) - 1);
        var upperRight = Coordinate.coordinate(row + 1, AlgebraicNotation.columnToInt(column) + 1);

        return Stream.of(up, down, left, right, upperLeft, upperRight, downLeft, downRight)
                .filter(StatusPair::status)
                .map(StatusPair::orElseThrow)
                .map(chessBoard::field)
                .map(field -> {
                    for (Field toReplace : replace) {
                        if (field.getCoordinate().equals(toReplace.getCoordinate())) {
                            return toReplace;
                        }
                    }

                    return field;
                }).toList();
    }

    private boolean canEat(ChessBoard chessBoard, Coordinate king, Field enemyField) {
        final List<Field> possiblePawns = pawnsThreateningCoordinate(chessBoard, enemyField.getCoordinate(), color);
        for (final Field possiblePawn : possiblePawns) {
            if (safeForKing(chessBoard, king, possiblePawn.getCoordinate(), enemyField.getCoordinate())) {
                return true;
            }
        }

        final List<Field> knights = knightAttackPositions(chessBoard, enemyField.getCoordinate());
        for (final Field knight : knights) {
            if (knight.pieceOptional().orElseThrow().color().equals(color)) {
                return safeForKing(chessBoard, king, knight.getCoordinate(), enemyField.getCoordinate());
            }
        }

        final List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, enemyField.getCoordinate());
        for (final Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(color)) {
                return safeForKing(chessBoard, king, diagonalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        final List<Field> horizontalVertical = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, enemyField.getCoordinate());
        for (final Field horizontalVerticalField : horizontalVertical) {
            final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(color)) {
                return safeForKing(chessBoard, king, horizontalVerticalField.getCoordinate(), enemyField.getCoordinate());
            }
        }

        return false;
    }

    private boolean canBlock(ChessBoard chessBoard, List<Field> fields, Coordinate king) {

        for (final Field field : fields) {
            final Coordinate currentCoordinate = field.getCoordinate();

            final StatusPair<Coordinate> possibleCoordinate;
            if (Color.WHITE.equals(color)) {
                possibleCoordinate = Coordinate.coordinate(currentCoordinate.getRow() - 1, AlgebraicNotation.columnToInt(currentCoordinate.getColumn()));
            } else {
                possibleCoordinate = Coordinate.coordinate(currentCoordinate.getRow() + 1, AlgebraicNotation.columnToInt(currentCoordinate.getColumn()));
            }

            if (possibleCoordinate.status()) {
                final Coordinate pawnCoordinate = possibleCoordinate.orElseThrow();
                final Field possiblePawn = chessBoard.field(pawnCoordinate);

                if (possiblePawn.isPresent() && possiblePawn.pieceOptional().orElseThrow().color().equals(color)) {
                    return safeForKing(chessBoard, king, pawnCoordinate, currentCoordinate);
                }
            }

            final List<Field> knights = knightAttackPositions(chessBoard, currentCoordinate);
            for (final Field knight : knights) {
                if (knight.pieceOptional().orElseThrow().color().equals(color)) {
                    return safeForKing(chessBoard, king, knight.getCoordinate(), currentCoordinate);
                }
            }

            final List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, currentCoordinate);
            for (final Field diagonalField : diagonalFields) {
                final Piece piece = diagonalField.pieceOptional().orElseThrow();

                if ((piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(color)) {
                    return safeForKing(chessBoard, king, diagonalField.getCoordinate(), currentCoordinate);
                }
            }

            final List<Field> horizontalVertical = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, currentCoordinate);
            for (final Field horizontalVerticalField : horizontalVertical) {
                final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

                if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(color)) {
                    return safeForKing(chessBoard, king, horizontalVerticalField.getCoordinate(), currentCoordinate);
                }
            }
        }

        return false;
    }

    private List<Field> getAllFriendlyFields(ChessBoard chessBoard) {
        final Coordinate kingCoordinate = getKingCoordinate(chessBoard);
        final Coordinate[] coordinates = Coordinate.values();

        return Arrays.stream(coordinates)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .filter(field -> !field.getCoordinate().equals(kingCoordinate))
                .filter(field -> field.pieceOptional().orElseThrow().color().equals(color))
                .toList();
    }

    private List<Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot) {
        int row = pivot.getRow();
        char column = pivot.getColumn();
        var up = Coordinate.coordinate(row + 1, column);
        var down = Coordinate.coordinate(row - 1, column);
        var left = Coordinate.coordinate(row, column - 1);
        var right = Coordinate.coordinate(row, column + 1);
        var downLeft = Coordinate.coordinate(row - 1, column - 1);
        var downRight = Coordinate.coordinate(row - 1, column + 1);
        var upperLeft = Coordinate.coordinate(row + 1, column - 1);
        var upperRight = Coordinate.coordinate(row + 1, column + 1);

        return Stream.of(up, down, left, right, upperLeft, upperRight, downLeft, downRight)
                .filter(StatusPair::status)
                .map(StatusPair::orElseThrow)
                .map(chessBoard::field)
                .toList();
    }

    private List<Field> knightAttackPositions(final ChessBoard chessBoard, final Coordinate pivot) {
        int row = pivot.getRow();
        char col = pivot.getColumn();
        var knightPos1 = Coordinate.coordinate(row + 1, col - 2);
        var knightPos2 = Coordinate.coordinate(row + 2, col - 1);
        var knightPos3 = Coordinate.coordinate(row + 2, col + 1);
        var knightPos4 = Coordinate.coordinate(row + 1, col + 2);
        var knightPos5 = Coordinate.coordinate(row - 1, col + 2);
        var knightPos6 = Coordinate.coordinate(row - 2, col + 1);
        var knightPos7 = Coordinate.coordinate(row - 2, col - 1);
        var knightPos8 = Coordinate.coordinate(row - 1, col - 2);

        return Stream.of(knightPos1, knightPos2, knightPos3, knightPos4, knightPos5, knightPos6, knightPos7, knightPos8)
                .filter(StatusPair::status)
                .map(StatusPair::orElseThrow)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .toList();
    }

    private List<Field> pawnsThreateningCoordinate(ChessBoard chessBoard, Coordinate pivot, Color color) {

        final List<StatusPair<Coordinate>> coordinates = new ArrayList<>(2);
        if (Color.WHITE.equals(color)) {
            coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() - 1));
            coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumn() + 1));
        } else {
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumn() - 1));
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumn() + 1));
        }

        return coordinates.stream()
                .filter(StatusPair::status)
                .map(StatusPair::orElseThrow)
                .map(chessBoard::field)
                .filter(Field::isPresent)
                .filter(field -> field.pieceOptional().orElseThrow() instanceof Pawn)
                .toList();
    }

    private List<Field> coordinatesThreatenedByPawn(ChessBoard chessBoard, Coordinate pivot, Color color) {

        final List<StatusPair<Coordinate>> coordinates = new ArrayList<>(2);
        if (Color.WHITE.equals(color)) {
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumnAsInt() - 1));
            coordinates.add(Coordinate.coordinate(pivot.getRow() + 1, pivot.getColumnAsInt() + 1));
        } else {
            coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumnAsInt() - 1));
            coordinates.add(Coordinate.coordinate(pivot.getRow() - 1, pivot.getColumnAsInt() + 1));
        }

        return coordinates.stream()
                .filter(StatusPair::status)
                .map(StatusPair::orElseThrow)
                .map(chessBoard::field)
                .toList();
    }

    private List<Field> surroundingFields(ChessBoard chessBoard, Coordinate pivot, Field toReplace) {
        int row = pivot.getRow();
        char column = pivot.getColumn();
        var up = Coordinate.coordinate(row + 1, column);
        var down = Coordinate.coordinate(row - 1, column);
        var left = Coordinate.coordinate(row, column - 1);
        var right = Coordinate.coordinate(row, column + 1);
        var downLeft = Coordinate.coordinate(row - 1, column - 1);
        var downRight = Coordinate.coordinate(row - 1, column + 1);
        var upperLeft = Coordinate.coordinate(row + 1, column - 1);
        var upperRight = Coordinate.coordinate(row + 1, column + 1);

        return Stream.of(up, down, left, right, upperLeft, upperRight, downLeft, downRight)
                .filter(StatusPair::status)
                .map(StatusPair::orElseThrow)
                .map(chessBoard::field)
                .map(field -> {
                    if (toReplace != null && field.getCoordinate().equals(toReplace.getCoordinate())) {
                        return toReplace;
                    }

                    return field;
                }).toList();
    }

    private boolean fieldIsBlockedOrDangerous(ChessBoard chessBoard, Field field) {
        if (field.isPresent() && field.pieceOptional().orElseThrow().color().equals(color)) {
            return true;
        }

        final List<Field> pawns = pawnsThreateningCoordinate(chessBoard, field.getCoordinate(), color);
        if (!pawns.isEmpty()) {
            return true;
        }

        final List<Field> knights = knightAttackPositions(chessBoard, field.getCoordinate());
        for (final Field possibleKnight : knights) {
            final Piece piece = possibleKnight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && !piece.color().equals(color)) {
                return true;
            }
        }

        final List<Field> diagonalFields = Direction.occupiedFieldsFromDiagonalDirections(chessBoard, field.getCoordinate());
        for (final Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if (piece instanceof Bishop && !piece.color().equals(color)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(color)) {
                return true;
            }
        }

        final List<Field> horizontalVerticalFields = Direction.occupiedFieldsFromHorizontalVerticalDirections(chessBoard, field.getCoordinate());
        for (final Field horizontalVerticalField : horizontalVerticalFields) {
            final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            if (piece instanceof Rook && !piece.color().equals(color)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(color)) {
                return true;
            }
        }

        return false;
    }

    private Coordinate getKingCoordinate(ChessBoard board) {
        if (this.color.equals(Color.WHITE)) {
            return board.currentWhiteKingPosition();
        } else {
            return board.currentBlackKingPosition();
        }
    }

    private Piece oppositePiece(Class<? extends Piece> type) {
        final Color oppositeColor = color.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;

        try {
            return type.getDeclaredConstructor(Color.class).newInstance(oppositeColor);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create opposite piece", e);
        }
    }

}