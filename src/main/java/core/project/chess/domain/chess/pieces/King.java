package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.entities.ChessBoard.Field;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation.Castle;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import jakarta.annotation.Nullable;

import java.util.*;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

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

        final boolean endFieldOccupiedBySameColorPiece = endField.pieceOptional().isPresent() &&
                endField.pieceOptional().orElseThrow().color().equals(kingColor);

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
        final int startColumn = from.column();
        final int endColumn = to.column();
        final int startRow = from.row();
        final int endRow = to.row();

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
     * @param from         The coordinate from which the piece is being moved.
     * @param to           The target coordinate where the piece is intended to move.
     * @return true if the king will be safe after the move, false otherwise.
     * <p>
     * This method checks whether the king will remain in a safe position after the move is executed.
     * If the piece being moved is the king, it checks for castling and validates the king's movement
     * for safety. If the piece is not the king, it checks whether the king would be in danger after
     * the proposed move.
     */
    public boolean safeForKing(final ChessBoard chessBoard, final Color color, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        final ChessBoardNavigator boardNavigator = new ChessBoardNavigator(chessBoard);
        final Coordinate kingPosition = color.equals(WHITE) ? chessBoard.currentWhiteKingPosition() : chessBoard.currentBlackKingPosition();

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
    public Operations kingStatus(final ChessBoard chessBoard, final Color kingColor, @Nullable final Pair<Coordinate, Coordinate> latestMovement) {
        ChessBoardNavigator boardNavigator = new ChessBoardNavigator(chessBoard);
        return checkOrMate(boardNavigator, kingColor, latestMovement);
    }

    /**
     * Determines if the current position is a stalemate.
     *
     * @param chessBoard The current state of the chess board.
     * @param color      The color of the king whose stalemate status is being checked.
     * @param latestMovement The latest movement made on the chessboard, represented as a pair of coordinates,
     *                       or {@code null} if the last move is guaranteed not to involve a pawn's en passant.
     * @return true if the position is a stalemate, false otherwise.
     * <p>
     * This method checks if the current position is a stalemate, meaning the player has no legal moves
     * and their king is not in check. It evaluates the surrounding fields of the king to determine if
     * they are blocked or dangerous, and checks all friendly fields to see if any legal moves are available.
     * <p>
     * Note: The parameter {@code latestMovement} is used solely for validating pawn moves,
     * particularly for en passant captures. It has no impact on other pieces' movement checks.
     */
    public boolean stalemate(final ChessBoard chessBoard, final Color color, @Nullable final Pair<Coordinate, Coordinate> latestMovement) {
        Objects.requireNonNull(chessBoard);
        Objects.requireNonNull(color);

        final ChessBoardNavigator boardNavigator = new ChessBoardNavigator(chessBoard);
        final Coordinate kingCoordinate = boardNavigator.kingCoordinate(color);

        final List<Field> piecesThatAttackingOurKing = check(boardNavigator, color);
        if (!piecesThatAttackingOurKing.isEmpty()) {
            return false;
        }

        final List<Field> surroundingFieldsOfKing = boardNavigator.surroundingFields(kingCoordinate);

        final boolean isSurrounded = surroundingFieldsOfKing.stream().allMatch(field -> isFieldDangerousOrBlockedForKing(boardNavigator, field, color));
        if (!isSurrounded) {
            return false;
        }

        final List<Field> ourFields = boardNavigator.allFriendlyFields(color, field -> !field.getCoordinate().equals(kingCoordinate));
        for (Field ourField : ourFields) {

            final boolean stalemate = processStalemate(boardNavigator, ourField, latestMovement);
            if (!stalemate) {
                return false;
            }
        }

        return true;
    }

    private boolean processStalemate(final ChessBoardNavigator boardNavigator, final Field ourField,
                                     @Nullable final Pair<Coordinate, Coordinate> latestMovement) {
        final Piece piece = ourField.pieceOptional().orElseThrow();
        final Coordinate coordinate = ourField.getCoordinate();
        final ChessBoard board = boardNavigator.board();

        return switch (piece) {
            case Pawn pawn -> pawn.isPawnOnStalemate(boardNavigator, ourField, pawn.color(), latestMovement, pawn);
            case Knight knight -> {
                final List<Field> coords = boardNavigator.knightAttackPositions(coordinate, f -> true);

                for (final Field coord : coords) {
                    if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(knight.color())) {
                        continue;
                    }

                    if (
                            knight.knightMove(ourField.getCoordinate(), coord.getCoordinate()) &&
                            safeForKing(board, knight.color(), ourField.getCoordinate(), coord.getCoordinate())
                    ) {
                        yield false;
                    }
                }

                yield true;
            }
            case Bishop bishop -> {
                final List<Field> coords = boardNavigator.fieldsInDirections(Direction.diagonalDirections(), coordinate);

                for (final Field coord : coords) {
                    if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                        continue;
                    }

                    if (
                            bishop.validate(board, ourField, coord) &&
                            safeForKing(board, bishop.color(), ourField.getCoordinate(), coord.getCoordinate())
                    ) {
                        yield false;
                    }
                }

                yield true;
            }
            case Rook rook -> {
                final List<Field> coords = boardNavigator.fieldsInDirections(Direction.horizontalVerticalDirections(), coordinate);

                for (final Field coord : coords) {
                    if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                        continue;
                    }

                    if (
                            rook.validate(board, ourField, coord) &&
                            safeForKing(board, rook.color(), ourField.getCoordinate(), coord.getCoordinate())
                    ) {
                        yield false;
                    }
                }

                yield true;
            }
            case Queen queen -> {
                final List<Field> coords = boardNavigator.fieldsInDirections(Direction.allDirections(), coordinate);

                for (final Field coord : coords) {
                    if (coord.isPresent() && coord.pieceOptional().orElseThrow().color().equals(color)) {
                        continue;
                    }

                    if (
                            queen.validate(board, ourField, coord) &&
                            safeForKing(board, queen.color(), ourField.getCoordinate(), coord.getCoordinate())
                    ) {
                        yield false;
                    }
                }

                yield true;
            }
            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
    }

    private List<Field> check(final ChessBoardNavigator boardNavigator, final Color kingColor) {
        final Coordinate kingCoordinate = boardNavigator.kingCoordinate(kingColor);
        final Color oppositeColor = kingColor.equals(WHITE) ? BLACK : WHITE;

        final List<Field> enemies = new ArrayList<>();

        final List<Field> pawns = boardNavigator.pawnsThreateningCoordinate(kingCoordinate, oppositeColor);
        if (!pawns.isEmpty()) {
            enemies.addAll(pawns);
        }

        final List<Field> knights = boardNavigator.knightAttackPositions(kingCoordinate, Field::isPresent);
        for (Field possibleKnight : knights) {
            final Piece piece = possibleKnight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && piece.color().equals(oppositeColor)) {
                enemies.add(possibleKnight);
            }
        }

        final List<Field> enemiesFromAllDirections = enemiesFromAllDirections(boardNavigator, kingCoordinate, oppositeColor);
        enemies.addAll(enemiesFromAllDirections);
        return enemies;
    }

    private Operations checkOrMate(final ChessBoardNavigator boardNavigator,
                                   final Color kingColor,
                                   @Nullable final Pair<Coordinate, Coordinate> latestMovement) {
        final List<Field> enemies = check(boardNavigator, kingColor);
        if (enemies.isEmpty()) {
            return Operations.EMPTY;
        }

        final Coordinate kingCoordinate = boardNavigator.kingCoordinate(kingColor);

        if (enemies.size() == 1) {
            final Field fieldWithEnemy = enemies.getFirst();

            final boolean canEat = canEat(boardNavigator, fieldWithEnemy, kingColor, latestMovement);
            if (canEat) {
                return Operations.CHECK;
            }

            final boolean canBlock = canBlock(boardNavigator, kingCoordinate, fieldWithEnemy, kingColor);
            if (canBlock) {
                return Operations.CHECK;
            }

            final List<Field> surroundings = boardNavigator.surroundingFields(kingCoordinate);
            if (isHaveSafetyField(surroundings, boardNavigator, kingColor)) {
                return Operations.CHECK;
            }

            return Operations.CHECKMATE;
        }

        if (enemies.size() == 2) {
            final Field fieldWithKing = boardNavigator.board().field(kingCoordinate);

            final boolean kingCanCaptureFirstFigure = !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getFirst(), kingColor) ;
            if (kingCanCaptureFirstFigure && isValidKingMovementCoordinates(boardNavigator.board(), fieldWithKing, enemies.getFirst())) {

                final boolean isCastling = boardNavigator.board().isCastling(this, fieldWithKing.getCoordinate(), enemies.getFirst().getCoordinate());
                if (isCastling && safeToCastle(boardNavigator, fieldWithKing.getCoordinate(), enemies.getFirst().getCoordinate())) {
                    return Operations.CHECK;
                }

                if (!isCastling) {
                    return Operations.CHECK;
                }
            }

            final boolean kingCanCaptureSecondFigure = !isFieldDangerousOrBlockedForKing(boardNavigator, enemies.getLast(), kingColor);
            if (kingCanCaptureSecondFigure && isValidKingMovementCoordinates(boardNavigator.board(), fieldWithKing, enemies.getLast())) {

                final boolean isCastling = boardNavigator.board().isCastling(this, fieldWithKing.getCoordinate(), enemies.getLast().getCoordinate());
                if (isCastling && safeToCastle(boardNavigator, fieldWithKing.getCoordinate(), enemies.getLast().getCoordinate())) {
                    return Operations.CHECK;
                }

                if (!isCastling) {
                    return Operations.CHECK;
                }
            }

            final List<Field> surroundings = boardNavigator.surroundingFields(kingCoordinate);
            return isHaveSafetyField(surroundings, boardNavigator, kingColor) ? Operations.CHECK : Operations.CHECKMATE;
        }

        final List<Field> surroundings = boardNavigator.surroundingFields(kingCoordinate);
        return isHaveSafetyField(surroundings, boardNavigator, kingColor) ? Operations.CHECK : Operations.CHECKMATE;
    }

    private boolean isHaveSafetyField(final List<Field> fields, final ChessBoardNavigator boardNavigator, final Color kingColor) {
        for (final Field field : fields) {

            final boolean isFieldSafe = !isFieldDangerousOrBlockedForKing(boardNavigator, field, kingColor);
            if (isFieldSafe) {
                return true;
            }
        }

        return false;
    }

    private boolean safeToCastle(final ChessBoardNavigator boardNavigator, final Coordinate presentKingPosition, final Coordinate futureKingPosition) {
        final Castle castle;
        if (presentKingPosition.column() < futureKingPosition.column()) {
            castle = Castle.SHORT_CASTLING;
        } else {
            castle = Castle.LONG_CASTLING;
        }

        final boolean ableToCastle = boardNavigator.board().ableToCastling(color, castle);
        if (!ableToCastle) {
            return false;
        }

        final List<Field> fieldsToCastle = boardNavigator.castlingFields(castle, color);
        for (final Field field : fieldsToCastle) {
            boolean canCastle = processCastling(boardNavigator, field);

            if (!canCastle) {
                return false;
            }
        }

        return true;
    }

    private boolean processCastling(final ChessBoardNavigator boardNavigator, final Field field) {
        if (field.isPresent() && !(field.pieceOptional().orElseThrow() instanceof King)) {
            return false;
        }

        final Color oppositeColor = color.equals(WHITE) ? BLACK : WHITE;

        final List<Field> pawns = boardNavigator.pawnsThreateningCoordinate(field.getCoordinate(), oppositeColor);
        for (Field pawn : pawns) {
            final Piece piece = pawn.pieceOptional().orElseThrow();

            if (!piece.color().equals(this.color)) {
                return false;
            }
        }

        final List<Field> knights = boardNavigator.knightAttackPositions(field.getCoordinate(), Field::isPresent);
        for (Field knight : knights) {
            final Piece piece = knight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && !piece.color().equals(this.color)) {
                return false;
            }
        }

        final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), field.getCoordinate());
        for (Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if ((piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(this.color)) {
                return false;
            }
        }

        final List<Field> horizontalVertical = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), field.getCoordinate());
        for (Field horizontalField : horizontalVertical) {
            final Piece piece = horizontalField.pieceOptional().orElseThrow();

            if ((piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(this.color)) {
                return false;
            }
        }

        final List<Field> surroundings = boardNavigator.surroundingFields(field.getCoordinate(), Field::isEmpty);
        for (Field surroundingField : surroundings) {
            final Piece piece = surroundingField.pieceOptional().orElseThrow();

            if (piece instanceof King && !piece.color().equals(this.color)) {
                return false;
            }
        }

        return true;
    }

    private boolean validateKingMovementForSafety(final ChessBoardNavigator boardNavigator, final Coordinate previousKing, final Coordinate futureKing) {
        final Color oppositeColor = color.equals(WHITE) ? BLACK : WHITE;

        final List<Field> pawns = boardNavigator.pawnsThreateningCoordinate(futureKing, oppositeColor);
        for (final Field possiblePawn : pawns) {
            final Piece pawn = possiblePawn.pieceOptional().orElseThrow();

            final boolean isEnemyPawn = pawn instanceof Pawn && !pawn.color().equals(color);
            if (isEnemyPawn) {
                return false;
            }
        }

        final List<Field> knights = boardNavigator.knightAttackPositions(futureKing, Field::isPresent);
        for (final Field possibleKnight : knights) {
            final Piece knight = possibleKnight.pieceOptional().orElseThrow();

            final boolean isEnemyKnight = knight instanceof Knight && !knight.color().equals(color);
            if (isEnemyKnight) {
                return false;
            }
        }

        final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.diagonalDirections(), futureKing, field -> !field.getCoordinate().equals(previousKing)
        );

        for (final Field field : diagonalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final int enemyRow = field.getCoordinate().row();
            final int enemyColumn = field.getCoordinate().column();

            final boolean surroundField = Math.abs(futureKing.row() - enemyRow) <= 1 && Math.abs(futureKing.column() - enemyColumn) <= 1;

            final boolean isOppositionOfKing = (piece instanceof King) && !piece.color().equals(this.color) && surroundField;
            if (isOppositionOfKing) {
                return false;
            }

            final boolean isDangerPiece = (piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color);
            if (isDangerPiece) {
                return false;
            }
        }

        final List<Field> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(), futureKing, field -> !field.getCoordinate().equals(previousKing)
        );

        for (final Field field : horizontalVerticalFields) {
            final Piece piece = field.pieceOptional().orElseThrow();

            final int enemyRow = field.getCoordinate().row();
            final int enemyColumn = field.getCoordinate().column();

            final boolean surroundField = Math.abs(futureKing.row() - enemyRow) <= 1 && Math.abs(futureKing.column() - enemyColumn) <= 1;

            final boolean isOppositionOfKing = (piece instanceof King) && !piece.color().equals(this.color) && surroundField;
            if (isOppositionOfKing) {
                return false;
            }

            final boolean isEnemyRookOrQueenOrKing = (piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color);
            if (isEnemyRookOrQueenOrKing) {
                return false;
            }
        }

        return true;
    }

    private boolean validatePieceMovementForKingSafety(final ChessBoardNavigator boardNavigator, final Coordinate kingPosition,
                                                       final Coordinate from, final Coordinate to) {
        final Color oppositeColor = color.equals(WHITE) ? BLACK : WHITE;

        final List<Field> pawnsThreateningCoordinates = boardNavigator.pawnsThreateningCoordinate(kingPosition, oppositeColor);
        for (final Field possiblePawn : pawnsThreateningCoordinates) {
            final Pawn pawn = (Pawn) possiblePawn.pieceOptional().orElseThrow();

            final boolean isEaten = isPawnEaten(to, possiblePawn, boardNavigator);
            if (!isEaten && !pawn.color().equals(color)) {
                return false;
            }
        }

        final List<Field> potentialKnightAttackPositions = boardNavigator.knightAttackPositions(kingPosition, Field::isPresent);
        for (final Field potentialKnightAttackPosition : potentialKnightAttackPositions) {
            final Piece piece = potentialKnightAttackPosition.pieceOptional().orElseThrow();

            final boolean isEaten = potentialKnightAttackPosition.getCoordinate().equals(to);

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

            final boolean isEaten = field.getCoordinate().equals(to);

            final boolean isEnemyBishopOrQueen = (piece instanceof Bishop || piece instanceof Queen) && !piece.color().equals(color);
            if (!isEaten && isEnemyBishopOrQueen) {
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

            final boolean isEaten = field.getCoordinate().equals(to);

            final boolean isEnemyRookOrQueen = (piece instanceof Rook || piece instanceof Queen) && !piece.color().equals(color);
            if (!isEaten && isEnemyRookOrQueen) {
                return false;
            }
        }

        return true;
    }

    private static boolean isPawnEaten(Coordinate to, Field possiblePawn, ChessBoardNavigator boardNavigator) {
        Optional<Coordinate> enPassaunt = boardNavigator.board().enPassaunt();
        Coordinate opponentPawn = possiblePawn.getCoordinate();
        if (enPassaunt.isPresent() && enPassaunt.get().equals(to)) {
            if (opponentPawn.equals(to)) {
                return true;
            }

            if (opponentPawn.column() != to.column()) {
                return false;
            }

            int row = to.row();
            int opponentRow = opponentPawn.row();
            if (possiblePawn.pieceOptional().orElseThrow().color().equals(BLACK)) {
                return row - opponentRow == 1;
            }

            return row - opponentRow == -1;
        }

        return opponentPawn.equals(to);
    }

    private boolean canEat(final ChessBoardNavigator boardNavigator,
                           final Field enemyField,
                           final Color kingColor,
                           @Nullable final Pair<Coordinate, Coordinate> latestMovement) {

        final boolean kingAttackedByPawn = enemyField.pieceOptional().orElseThrow() instanceof Pawn;
        if (kingAttackedByPawn && Objects.nonNull(latestMovement)) {
            final Coordinate startOfLastMove = latestMovement.getFirst();
            final Coordinate endOfLastMove = latestMovement.getSecond();

            final boolean sameColumn = startOfLastMove.column() == endOfLastMove.column();

            final boolean isLastMoveWasPassage = sameColumn && Math.abs(startOfLastMove.row() - endOfLastMove.row()) == 2;
            if (isLastMoveWasPassage) {

                final Coordinate intermediateCoordinateOnPassage;
                if (kingColor.equals(WHITE)) {
                    intermediateCoordinateOnPassage = Coordinate
                            .of(enemyField.getCoordinate().row() + 1, enemyField.getCoordinate().column());
                } else {
                    intermediateCoordinateOnPassage = Coordinate
                            .of(enemyField.getCoordinate().row() - 1, enemyField.getCoordinate().column());
                }

                final List<Field> surroundedPawns = boardNavigator.pawnsThreateningCoordinate(intermediateCoordinateOnPassage, kingColor);
                for (final Field possiblePawn : surroundedPawns) {

                    final boolean canCaptureOnPassage = safeForKing(boardNavigator.board(),
                            kingColor,
                            possiblePawn.getCoordinate(),
                            enemyField.getCoordinate());

                    if (canCaptureOnPassage) {
                        return true;
                    }

                }
            }
        }

        final List<Field> pawnsThatPotentiallyCanEatEnemyPiece = boardNavigator.pawnsThreateningCoordinate(enemyField.getCoordinate(), kingColor);
        for (final Field possiblePawn : pawnsThatPotentiallyCanEatEnemyPiece) {

            final boolean canSimpleDiagonalCapture = safeForKing(boardNavigator.board(), kingColor, possiblePawn.getCoordinate(), enemyField.getCoordinate());
            if (canSimpleDiagonalCapture) {
                return true;
            }
        }

        final List<Field> knightsThatPotentiallyCanEatEnemyPiece = boardNavigator.knightAttackPositions(enemyField.getCoordinate(), Field::isPresent);
        for (final Field knight : knightsThatPotentiallyCanEatEnemyPiece) {
            final Piece piece = knight.pieceOptional().orElseThrow();

            final boolean isOurKnight = piece instanceof Knight && piece.color().equals(kingColor);
            if (isOurKnight && safeForKing(boardNavigator.board(), kingColor, knight.getCoordinate(), enemyField.getCoordinate())) {
                return true;
            }
        }

        final List<Field> firstPiecesFromDiagonalVectors = boardNavigator
                .occupiedFieldsInDirections(
                        Direction.diagonalDirections(), enemyField.getCoordinate()
                );

        for (final Field diagonalField : firstPiecesFromDiagonalVectors) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            final boolean canEatFromDiagonalPosition = (piece instanceof Bishop || piece instanceof Queen)
                    && piece.color().equals(kingColor) && safeForKing(boardNavigator.board(), kingColor, diagonalField.getCoordinate(), enemyField.getCoordinate());
            if (canEatFromDiagonalPosition) {
                return true;
            }

        }

        final List<Field> firstPiecesFromHorizontalAndVerticalVectors = boardNavigator
                .occupiedFieldsInDirections(
                        Direction.horizontalVerticalDirections(), enemyField.getCoordinate()
                );

        for (final Field horizontalVerticalField : firstPiecesFromHorizontalAndVerticalVectors) {
            final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            final boolean canEatFromHorizontalAndVerticalPositions = (piece instanceof Rook || piece instanceof Queen) && piece.color().equals(kingColor)
                    && safeForKing(boardNavigator.board(), kingColor, horizontalVerticalField.getCoordinate(), enemyField.getCoordinate());
            if (canEatFromHorizontalAndVerticalPositions) {
                return true;
            }
        }

        return false;
    }

    private boolean canBlock(ChessBoardNavigator boardNavigator, Coordinate king, Field enemyField, Color kingColor) {
        if (enemyField.isPresent() && enemyField.pieceOptional().orElseThrow() instanceof Knight) {
            return false;
        }

        final Coordinate enemyCoord = enemyField.getCoordinate();

        final boolean surround = Math.abs(king.row() - enemyCoord.row()) <= 1 && Math.abs(king.column() - enemyCoord.column()) <= 1;
        if (surround) {
            return false;
        }

        final List<Field> path = boardNavigator.fieldsInPath(king, enemyField.getCoordinate(), false);
        for (final Field field : path) {
            final Coordinate currentCoordinate = field.getCoordinate();

            final Coordinate potentialPawnThatCanBlockAttackBySimpleMove;
            if (WHITE.equals(kingColor)) {
                potentialPawnThatCanBlockAttackBySimpleMove = Coordinate.of(currentCoordinate.row() - 1, currentCoordinate.column());
            } else {
                potentialPawnThatCanBlockAttackBySimpleMove = Coordinate.of(currentCoordinate.row() + 1, currentCoordinate.column());
            }

            if (potentialPawnThatCanBlockAttackBySimpleMove != null) {
                final Field possiblePawn = boardNavigator.board().field(potentialPawnThatCanBlockAttackBySimpleMove);

                if (possiblePawn.isPresent()) {
                    final boolean isPawn = possiblePawn.pieceOptional().orElseThrow() instanceof Pawn;

                    final boolean isFriendly = possiblePawn.pieceOptional().orElseThrow().color().equals(kingColor);

                    final boolean pawnCanBlock = isPawn && isFriendly &&
                            safeForKing(boardNavigator.board(), kingColor, potentialPawnThatCanBlockAttackBySimpleMove, currentCoordinate);
                    if (pawnCanBlock) {
                        return true;
                    }
                }
            }

            final boolean potentiallyCanBeBlockedByPawnPassage = currentCoordinate.row() == 5 && kingColor.equals(BLACK)
                    || currentCoordinate.row() == 4 && kingColor.equals(WHITE);

            if (potentiallyCanBeBlockedByPawnPassage) {
                final Coordinate potentialPawnCoordinate;
                final Coordinate secondPassageCoordinate;

                if (WHITE.equals(kingColor)) {
                    potentialPawnCoordinate = Coordinate.of(2, currentCoordinate.column());
                    secondPassageCoordinate = Coordinate.of(5, currentCoordinate.column());
                } else {
                    potentialPawnCoordinate = Coordinate.of(7, currentCoordinate.column());
                    secondPassageCoordinate = Coordinate.of(4, currentCoordinate.column());
                }

                final Field field2 = boardNavigator.board().field(potentialPawnCoordinate);

                final boolean isFriendlyPawnExists = field2.isPresent() &&
                        field2.pieceOptional().orElseThrow() instanceof Pawn (Color pawnColor) && pawnColor.equals(kingColor);

                final boolean isPathClear = clearPath(boardNavigator.board(), potentialPawnCoordinate, secondPassageCoordinate);

                final boolean canBlockByPassage = isFriendlyPawnExists &&
                        isPathClear &&
                        safeForKing(boardNavigator.board(), kingColor, potentialPawnCoordinate, currentCoordinate);

                if (canBlockByPassage) return true;
            }

            final List<Field> knights = boardNavigator.knightAttackPositions(currentCoordinate, Field::isPresent);
            for (final Field knight : knights) {
                final Piece piece = knight.pieceOptional().orElseThrow();

                final boolean isOurKnight = piece instanceof Knight && piece.color().equals(kingColor);
                if (isOurKnight && safeForKing(boardNavigator.board(), kingColor, knight.getCoordinate(), currentCoordinate)) {
                    return true;
                }
            }

            final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), currentCoordinate);
            for (final Field diagonalField : diagonalFields) {
                final Piece piece = diagonalField.pieceOptional().orElseThrow();

                final boolean figureThatCanBlock = (piece instanceof Bishop || piece instanceof Queen) && piece.color().equals(kingColor);
                if (figureThatCanBlock && safeForKing(boardNavigator.board(), kingColor, diagonalField.getCoordinate(), currentCoordinate)) {
                    return true;
                }
            }

            final List<Field> horizontalVertical = boardNavigator.occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), currentCoordinate);
            for (final Field horizontalVerticalField : horizontalVertical) {
                final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

                if ((piece instanceof Rook || piece instanceof Queen) && piece.color().equals(kingColor)
                        && safeForKing(boardNavigator.board(), kingColor, horizontalVerticalField.getCoordinate(), currentCoordinate)) {
                    return true;
                }

            }
        }

        return false;
    }

    private boolean isFieldDangerousOrBlockedForKing(final ChessBoardNavigator boardNavigator, final Field field, final Color kingColor) {
        final Color oppositeColor = kingColor.equals(WHITE) ? BLACK : WHITE;

        final boolean blocked = field.isPresent() && field.pieceOptional().orElseThrow().color().equals(kingColor);
        if (blocked) {
            return true;
        }

        final List<Field> pawns = boardNavigator.pawnsThreateningCoordinate(field.getCoordinate(), oppositeColor);
        if (!pawns.isEmpty()) {
            return true;
        }

        final List<Field> knights = boardNavigator.knightAttackPositions(field.getCoordinate(), Field::isPresent);
        for (final Field possibleKnight : knights) {
            final Piece piece = possibleKnight.pieceOptional().orElseThrow();

            if (piece instanceof Knight && !piece.color().equals(kingColor)) {
                return true;
            }
        }

        final List<Field> surroundings = boardNavigator.surroundingFields(
                field.getCoordinate(),
                f -> !(
                        f.isPresent() && f.pieceOptional().orElseThrow() instanceof King && !f.pieceOptional().orElseThrow().color().equals(kingColor)
                )
        );

        if (!surroundings.isEmpty()) {
            return true;
        }

        final List<Field> diagonalFields = boardNavigator
                .occupiedFieldsInDirections(
                        Direction.diagonalDirections(),
                        field.getCoordinate(),
                        field1 -> {
                            final Piece piece = field1.pieceOptional().orElseThrow();
                            return !(piece instanceof King) || !piece.color().equals(kingColor);
                        }
                );

        for (final Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            if (piece instanceof Bishop && !piece.color().equals(kingColor)) {
                return true;
            }
            if (piece instanceof Queen && !piece.color().equals(kingColor)) {
                return true;
            }
        }

        final List<Field> horizontalVerticalFields = boardNavigator.occupiedFieldsInDirections(
                Direction.horizontalVerticalDirections(),
                field.getCoordinate(),
                field1 -> {
                    final Piece piece = field1.pieceOptional().orElseThrow();
                    return !(piece instanceof King) || !piece.color().equals(kingColor);
                }
        );

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

    private List<Field> enemiesFromAllDirections(final ChessBoardNavigator boardNavigator, final Coordinate kingCoordinate, final Color oppositeColor)  {
        final List<Field> enemies = new ArrayList<>();

        final List<Field> diagonalFields = boardNavigator.occupiedFieldsInDirections(Direction.diagonalDirections(), kingCoordinate);
        for (Field diagonalField : diagonalFields) {
            final Piece piece = diagonalField.pieceOptional().orElseThrow();

            final boolean isBishopOrQueen = piece instanceof Bishop || piece instanceof Queen;
            final boolean isEnemy = piece.color().equals(oppositeColor);
            if (isBishopOrQueen && isEnemy) {
                enemies.add(diagonalField);
            }
        }

        final List<Field> horizontalVerticalFields = boardNavigator
                .occupiedFieldsInDirections(Direction.horizontalVerticalDirections(), kingCoordinate);
        for (Field horizontalVerticalField : horizontalVerticalFields) {
            final Piece piece = horizontalVerticalField.pieceOptional().orElseThrow();

            final boolean isRookOrQueen = piece instanceof Rook || piece instanceof Queen;
            final boolean isEnemy = piece.color().equals(oppositeColor);
            if (isRookOrQueen && isEnemy) {
                enemies.add(horizontalVerticalField);
            }
        }

        return enemies;
    }
}