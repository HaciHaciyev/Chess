package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.*;
import core.project.chess.infrastructure.utilities.StatusPair;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.data.util.Pair;

import java.util.*;

/**
 * The `ChessBoard` class represents the central entity of the Chess Aggregate. It encapsulates the state and behavior of a chess board,
 * serving as the entry point for all chess-related operations within the domain.
 * <p>
 * The `ChessBoard` is responsible for managing the placement and movement of chess pieces, enforcing the rules of the game,
 * and tracking the history of moves made on the board. It provides a well-defined API for interacting with the chess board,
 * ensuring that all operations are performed in a consistent and valid manner.
 * <p>
 * The `ChessBoard` is the root entity of the Chess Aggregate, meaning that it owns and is responsible for the lifecycle of all
 * other entities and value objects within the aggregate, such as `Piece`, `Coordinate`, and `AlgebraicNotation`. This ensures
 * that the aggregate remains in a valid and consistent state at all times.
 * <p>
 * The `ChessBoard` class encapsulates the following key responsibilities:
 * <p>
 * 1. **Piece Placement and Movement**: The `reposition()` method allows for the movement of pieces on the board, handling
 *    various operations such as capturing, promotion, and castling, while ensuring the validity of each move,
 *    also allow to revert last made move by using 'returnOfTheMovement()'.
 * <p>
 * 2. **Castling Management**: The `castling()` method handles the specific logic for castling moves, including the movement of the rook
 *    also allow to revert last made move by using 'revertCastling()'.
 * <p>
 * 3. **Move History Tracking**: The `listOfAlgebraicNotations` property and associated methods allow for the recording and retrieval
 *    of the move history, represented using algebraic notation.
 * <p>
 * 4. **King Position Tracking**: The `isKingMoved()` and `changedKingPosition()` methods are used to track the movement of the white and black kings,
 *    which is crucial for validating the legality of moves and castling.
 * <p>
 * 5. **King Safety Validation**: The `safeForKing()` method checks if a proposed move is safe for the king, considering the current position
 *    of the king and the potential threats on the board.
 * <p>
 * 6. **Piece Validation**: The `ChessBoard` class delegates the validation of piece movements to the individual `Piece` implementations,
 *    ensuring that each piece type can enforce its own unique movement rules.
 * <p>
 * 7. **Chess Board Initialization**: The `standardInitializer()` method sets up the initial state of the chess board according to the standard chess rules,
 *    ensuring a consistent starting point for each game.
 *
 *
 * @author Hadzhyiev Hadzhy
 * @version 1.2
 */
public class ChessBoard {
    private final @Getter UUID chessBoardId;
    private Color figuresTurn;
    private boolean validWhiteShortCasting;
    private boolean validWhiteLongCasting;
    private boolean validBlackShortCasting;
    private boolean validBlackLongCasting;
    private Coordinate currentWhiteKingPosition;
    private Coordinate currentBlackKingPosition;
    private final Map<Coordinate, Field> fieldMap;
    private final List<AlgebraicNotation> listOfAlgebraicNotations;
    private static final Coordinate initialWhiteKingPosition = Coordinate.E1;
    private static final Coordinate initialBlackKingPosition = Coordinate.E8;
    private final @Getter(AccessLevel.PROTECTED) InitializationTYPE initializationTYPE;

    /**
     * Constructs a new `ChessBoard` instance with the given parameters.
     *
     * @param chessBoardId             The unique identifier of the chess board.
     * @param initialWhiteKingPosition The initial position of the white king.
     * @param initialBlackKingPosition The initial position of the black king.
     * @param initializationTYPE       The type of initialization for the chess board.
     */
    private ChessBoard(
            final UUID chessBoardId, final Coordinate initialWhiteKingPosition,
            final Coordinate initialBlackKingPosition, final InitializationTYPE initializationTYPE,
            final List<AlgebraicNotation> algebraicNotations
    ) {
        Objects.requireNonNull(chessBoardId);
        Objects.requireNonNull(initialWhiteKingPosition);
        Objects.requireNonNull(initialBlackKingPosition);
        Objects.requireNonNull(initializationTYPE);

        this.chessBoardId = chessBoardId;
        this.initializationTYPE = initializationTYPE;
        this.listOfAlgebraicNotations = algebraicNotations;
        this.fieldMap = new HashMap<>();

        this.figuresTurn = Color.WHITE;
        this.currentWhiteKingPosition = initialWhiteKingPosition;
        this.currentBlackKingPosition = initialBlackKingPosition;

        /** These fields are intended to determine whether castling is possible from the point of view
         * of the pieces intended for castling not having been used earlier in the movement.
         * This is not complete validation of castling of course.*/
        this.validWhiteShortCasting = true;
        this.validWhiteLongCasting = true;
        this.validBlackShortCasting = true;
        this.validBlackLongCasting = true;

        /**
         * Checks if the initialization type is set to STANDARD then we create new chess game,
         * also we start from zero position for reading the ended chess game if initialization type set to READER_MODE.
         * If true, the `standardInitializer()` method is called to set up the initial state of the chess board.
         */
        final boolean standardInit =
                initializationTYPE.equals(InitializationTYPE.STANDARD) || initializationTYPE.equals(InitializationTYPE.READER_MODE);
        if (standardInit) {
            standardInitializer();
        }

    }

    /**
     * Factory method.
     * Creates a new `ChessBoard` instance with the standard chess board initialization.
     *
     * @param chessBoardId The unique identifier of the chess board.
     * @return A new `ChessBoard` instance with the standard chess board initialization.
     */
    public static ChessBoard starndardChessBoard(final UUID chessBoardId) {
        return new ChessBoard(
                chessBoardId, initialWhiteKingPosition, initialBlackKingPosition, InitializationTYPE.STANDARD, new LinkedList<>());
    }

    /**
     * Retrieves the `Field` object at the specified coordinate on the chess board.
     *
     * @param coordinate The coordinate of the field to retrieve.
     * @return A new `Field` object representing the field at the specified coordinate.
     */
    public Field field(final Coordinate coordinate) {
        Field field = fieldMap.get(coordinate);
        return new Field(
                field.getCoordinate(), field.pieceOptional().orElse(null)
        );
    }

    /**
     * Retrieves a list of algebraic notations representing the moves made on the chess board.
     *
     * @return A list of algebraic notations in type of String.
     */
    public List<String> listOfAlgebraicNotations() {
        return listOfAlgebraicNotations.stream().map(AlgebraicNotation::algebraicNotation).toList();
    }

    /**
     * Retrieves the latest movement on the chess board, represented as a pair of coordinates.
     *
     * @return An Optional containing the pair of coordinates representing the latest movement, or an empty Optional if no movement has been made.
     */
    public Optional<Pair<Coordinate, Coordinate>> latestMovement() {
        AlgebraicNotation algebraicNotation = listOfAlgebraicNotations.getLast();
        if (algebraicNotation == null) {
            return Optional.empty();
        }

        StatusPair<AlgebraicNotation.Castle> statusPair = AlgebraicNotation.isCastling(algebraicNotation);
        if (statusPair.status()) {
            return Optional.of(castlingCoordinates(statusPair.valueOrElseThrow()));
        }

        return Optional.of(algebraicNotation.coordinates());
    }

    private void switchFiguresTurn() {
        if (figuresTurn.equals(Color.WHITE)) {
            figuresTurn = Color.BLACK;
        } else {
            figuresTurn = Color.WHITE;
        }
    }

    /** This is not a complete validation of which player should play at this point.
     * This validation rather checks what color pieces should be moved.
     * Finally, validation of the question of who should walk can only be carried out in the controller.*/
    private boolean validateFiguresTurn(final Coordinate coordinate) {
        Color figuresThatTryToMove = this.field(coordinate).pieceOptional().orElseThrow().color();
        if (figuresThatTryToMove != figuresTurn) {
            return false;
        }
        return true;
    }

    /**
     * Updates the position of the king on the chess board.
     *
     * @param king        The king piece that has moved.
     * @param coordinate  The new coordinate of the king.
     */
    private void changedKingPosition(final King king, final Coordinate coordinate) {
        if (king.color().equals(Color.WHITE)) {
            this.currentWhiteKingPosition = coordinate;
        } else {
            this.currentBlackKingPosition = coordinate;
        }
    }

    /**
     * Updates the castling ability based on the movement of a Rook or King piece.
     * <p>
     * This method modifies the following class-level variables:
     * - `validWhiteShortCasting`
     * - `validWhiteLongCasting`
     * - `validBlackShortCasting`
     * - `validBlackLongCasting`
     *
     * @param from The coordinate from which the piece was moved.
     * @param piece The piece that was moved.
     * @throws IllegalStateException if the provided piece is not a Rook or King.
     */
    private void changeOfCastlingAbility(final Coordinate from, final Piece piece) {
        if (!(piece instanceof Rook) || !(piece instanceof King (Color color))) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        final boolean whiteColorFigure = color.equals(Color.WHITE);
        if (whiteColorFigure) {

            if (piece instanceof King) {
                this.validWhiteShortCasting = false;
                this.validWhiteLongCasting = false;
            }

            if (from.equals(Coordinate.A1)) {
                validWhiteLongCasting = false;
            } else {
                validWhiteShortCasting = false;
            }

        } else {

            if (piece instanceof King) {
                this.validBlackShortCasting = false;
                this.validBlackLongCasting = false;
            }

            if (from.equals(Coordinate.A8)) {
                validBlackLongCasting = false;
            } else {
                validBlackShortCasting = false;
            }

        }
    }

    /**
     * This method is responsible for updating the castling ability of a piece during a revert move.
     *
     * @param piece  The piece being moved.
     * @param castle The castling ability associated with the move, if any.
     * @throws IllegalStateException if the provided piece is not a King or Rook.
     */
    private void changeOfCastlingAbilityInRevertMove(
            final Piece piece, final @Nullable AlgebraicNotation.Castle castle
    ) {
        if (!(piece instanceof King (Color color)) || !(piece instanceof Rook)) {
            throw new IllegalStateException("Invalid method usage, check documentation.");
        }

        if (!Objects.isNull(castle)) {
            changeOfCastlingAbilityInRevertCastling((King) piece, castle);
        }

        if (color.equals(Color.BLACK)) {
            processHistoryForBlackFigures(piece);
        } else {
            processHistoryForWhiteFigures(piece);
        }
    }

    /**
     * This method is responsible for updating the castling ability of white figures based on the move history.
     *
     * @param piece The white piece being moved.
     */
    private void processHistoryForWhiteFigures(final Piece piece) {
        for (int i = 0; i < listOfAlgebraicNotations.size(); i++) {
            final Color color = i % 10 == 2 ? Color.WHITE : Color.BLACK;
            final AlgebraicNotation algebraicNotation = listOfAlgebraicNotations.get(i);

            if (!color.equals(piece.color())) {
                continue;
            }

            final boolean king = algebraicNotation.algebraicNotation().charAt(0) == 'K';
            if (king) {
                validWhiteShortCasting = false;
                validWhiteLongCasting = false;
            }

            final Coordinate from = algebraicNotation.coordinates().getFirst();

            final boolean leftRookMoved = algebraicNotation.algebraicNotation().charAt(0) == 'R' && from.equals(Coordinate.A1);
            if (leftRookMoved) {
                validWhiteLongCasting = false;
            }

            final boolean rightRookMoved = algebraicNotation.algebraicNotation().charAt(0) == 'R' && from.equals(Coordinate.H1);
            if (rightRookMoved) {
                validWhiteShortCasting = false;
            }
        }
    }

    /**
     * This method is responsible for updating the castling ability of black figures based on the move history.
     *
     * @param piece The white piece being moved.
     */
    private void processHistoryForBlackFigures(final Piece piece) {
        for (int i = 0; i < listOfAlgebraicNotations.size(); i++) {
            final Color color = i % 10 == 2 ? Color.WHITE : Color.BLACK;
            final AlgebraicNotation algebraicNotation = listOfAlgebraicNotations.get(i);

            if (!color.equals(piece.color())) {
                continue;
            }

            final boolean king = algebraicNotation.algebraicNotation().charAt(0) == 'K';
            if (king) {
                validBlackShortCasting = false;
                validBlackLongCasting = false;
            }

            final Coordinate from = algebraicNotation.coordinates().getFirst();

            final boolean leftRookMoved = algebraicNotation.algebraicNotation().charAt(0) == 'R' && from.equals(Coordinate.H8);
            if (leftRookMoved) {
                validBlackShortCasting = false;
            }

            final boolean rightRookMoved = algebraicNotation.algebraicNotation().charAt(0) == 'R' && from.equals(Coordinate.A8);
            if (rightRookMoved) {
                validBlackLongCasting = false;
            }
        }
    }

    /**
     * This method is responsible for updating the castling ability of a King during a revert castling move.
     *
     * @param king    The King involved in the castling move.
     * @param castle  The castling ability associated with the move.
     */
    private void changeOfCastlingAbilityInRevertCastling(
            final King king, final AlgebraicNotation.Castle castle
    ) {
        final boolean whiteFigures = king.color().equals(Color.WHITE);
        final boolean shortCastling = castle.equals(AlgebraicNotation.Castle.SHORT_CASTLING);
        if (shortCastling) {

            if (whiteFigures) {
                validWhiteShortCasting = true;
            } else {
                validBlackShortCasting = true;
            }

        } else {

            if (whiteFigures) {
                validWhiteLongCasting = true;
            } else {
                validBlackLongCasting = true;
            }

        }

        changeCastlingAbilityOfSecondRookInRevertCastling(king.color(), castle);
    }

    /**
     * This method is responsible for updating the castling ability of the second Rook involved in a revert castling move.
     *
     * @param color   The color of the King involved in the castling move.
     * @param castle  The castling ability associated with the move.
     */
    private void changeCastlingAbilityOfSecondRookInRevertCastling(
            final Color color, final AlgebraicNotation.Castle castle
    ) {
        for (int i = 0; i < listOfAlgebraicNotations.size(); i++) {
            final AlgebraicNotation algebraicNotation = listOfAlgebraicNotations.get(i);
            final Color figureColor = i % 10 == 2 ? Color.WHITE : Color.BLACK;
            final boolean rook = algebraicNotation.algebraicNotation().charAt(0) == 'R';

            final boolean ourRookIsMoved = rook && figureColor.equals(color);
            if (ourRookIsMoved) {
                return;
            }
        }

        changeAbility(color, castle);
    }

    /**
     * This method is responsible for updating the castling ability based on the color of the King and the type of castling.
     *
     * @param color   The color of the King.
     * @param castle  The castling ability associated with the move.
     */
    private void changeAbility(final Color color, final AlgebraicNotation.Castle castle) {
        if (color.equals(Color.WHITE)) {
            if (castle.equals(AlgebraicNotation.Castle.SHORT_CASTLING)) {
                validWhiteLongCasting = true;
            } else {
                validWhiteShortCasting = true;
            }
        }

        if (castle.equals(AlgebraicNotation.Castle.SHORT_CASTLING)) {
            validBlackLongCasting = true;
        }

        validBlackShortCasting = true;
    }

    /**
     * Checks if the player with the given color is able to perform the specified castling move.
     * <p>
     * This method checks the current state of the game to determine if the requested castling move is valid.
     * It takes into account the type of castling (short or long) and the color of the player.
     * <p>
     * Note that this validation is not final, as it only checks if the pieces required for castling
     * have not moved previously during the game. If either the king or the rook have been moved
     * at any point, castling will not be possible.
     *
     * @param color The color of the player performing the castling move.
     * @param castle The type of castling move (short or long).
     * @return `true` if the player is able to perform the specified castling move, `false` otherwise.
     */
    private boolean ableToCastling(final Color color, final AlgebraicNotation.Castle castle) {
        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(castle);
        if (shortCasting) {

            if (color.equals(Color.WHITE)) {
                return validWhiteShortCasting;
            }

            if (color.equals(Color.BLACK)) {
                return validBlackShortCasting;
            }
        }

        if (color.equals(Color.WHITE)) {
            return validWhiteLongCasting;
        }

        return validBlackLongCasting;
    }

    /**
     * Checks if the move from the specified 'from' coordinate to the 'to' coordinate is safe for the king.
     *
     * @param from  The coordinate the piece is moving from.
     * @param to    The coordinate the piece is moving to.
     * @return True if the move is safe for the king, false otherwise.
     */
    public boolean safeForKing(final Coordinate from, final Coordinate to) {
        Color kingColor = fieldMap.get(from).pieceOptional().orElseThrow().color();
        King king = theKing(kingColor);

        return kingColor == Color.WHITE ?
                king.safeForKing(this, currentWhiteKingPosition, from, to) :
                king.safeForKing(this, currentBlackKingPosition, from, to);
    }

    /**
     * Retrieves the king object based on the specified color.
     *
     * @param kingColor The color of the king to retrieve.
     * @return The king object.
     * @throws IllegalStateException If the king object cannot be found.
     */
    private King theKing(final Color kingColor) {
        return kingColor.equals(Color.WHITE) ?

                (King) fieldMap
                        .get(currentWhiteKingPosition)
                        .pieceOptional()
                        .orElseThrow(
                                () -> new IllegalStateException("Invalid method usage, check documentation.")
                        )
                :
                (King) fieldMap
                        .get(currentBlackKingPosition)
                        .pieceOptional()
                        .orElseThrow(
                                () -> new IllegalStateException("Invalid method usage, check documentation.")
                        );
    }

    /**
     * Processes a piece repositioning on the chess board.
     *
     * @param from                  The coordinate the piece is moving from.
     * @param to                    The coordinate the piece is moving to.
     * @param inCaseOfPromotion     The piece to promote to in case of a pawn promotion, or null if no promotion.
     * @return The operations performed during the repositioning.
     * @throws IllegalArgumentException If the move is invalid.
     */
    protected final Operations reposition(
            final Coordinate from, final Coordinate to, final @Nullable Piece inCaseOfPromotion
    ) {
        /** Preparation of necessary data and validation.*/
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (!validateFiguresTurn(from)) {
            throw new IllegalArgumentException(
                    String.format("At the moment, the player for %s must move and not the opponent", figuresTurn)
            );
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        Field startField = fieldMap.get(from);
        Field endField = fieldMap.get(to);
        Piece piece = startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        /** Delegate the operation to another method if necessary.*/
        if (AlgebraicNotation.isCastling(piece, from, to)) {
            return castling(from, to);
        }

        /** Validation.*/
        StatusPair<LinkedHashSet<Operations>> statusPair = piece.isValidMove(this, from, to);
        if (!statusPair.status() || !safeForKing(from, to)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        /** Process operations from StatusPair. All validation need to be processed before that.*/
        LinkedHashSet<Operations> operations = statusPair.valueOrElseThrow();

        startField.removeFigure();

        if (operations.contains(Operations.CAPTURE)) {
            endField.removeFigure();
        }

        if (operations.contains(Operations.PROMOTION)) {
            if (!endField.isEmpty()) {
                endField.removeFigure();
            }

            endField.addFigure(inCaseOfPromotion);
        } else {
            endField.addFigure(piece);
        }

        /** Monitor opportunities for castling and switch players.*/
        if (piece instanceof King king) {
            changedKingPosition(king, to);
            changeOfCastlingAbility(from, king);
        }

        if (piece instanceof Rook rook) {
            changeOfCastlingAbility(from, rook);
        }

        switchFiguresTurn();

        /** Recording the move made in algebraic notation.*/
        listOfAlgebraicNotations.add(AlgebraicNotation.of(piece, operations, from, to, inCaseOfPromotion));

        return AlgebraicNotation.opponentKingStatus(operations);
    }

    /**
     * Processes a castling move on the chess board.
     *
     * @param from The coordinate the king is moving from.
     * @param to   The coordinate the king is moving to.
     * @return The operations performed during the castling.
     * @throws IllegalArgumentException If the castling move is invalid.
     * @throws IllegalStateException    If the method is used incorrectly.
     */
    private Operations castling(final Coordinate from, final Coordinate to) {
        /** Preparation of necessary data and validation.*/
        Field kingStartedField = fieldMap.get(from);
        Field kingEndField = fieldMap.get(to);
        Piece piece = kingStartedField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        if (!(piece instanceof King king)) {
            throw new IllegalStateException("Invalid method usage, check the documentation.");
        }

        final Color color = king.color();
        if (!ableToCastling(color, AlgebraicNotation.castle(to))) {
            throw new IllegalArgumentException("Invalid move.");
        }

        StatusPair<LinkedHashSet<Operations>> statusPair = king.canCastle(this, kingStartedField, kingEndField);
        if (!statusPair.status() || !safeForKing(from, to)) {
            throw new IllegalArgumentException("Invalid move.");
        }

        /**Process operations from StatusPair. All validation need to be processed before that.*/
        kingStartedField.removeFigure();
        kingEndField.addFigure(king);

        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(AlgebraicNotation.castle(to));
        if (shortCasting) {
            moveRookInShortCastling(to);
        } else {
            moveRookInLongCastling(to);
        }

        /** Monitor opportunities for castling and switch players.*/
        changedKingPosition(king, to);
        changeOfCastlingAbility(from, king);

        switchFiguresTurn();

        /** Recording the move made in algebraic notation.*/
        LinkedHashSet<Operations> operations = statusPair.valueOrElseThrow();
        listOfAlgebraicNotations.add(AlgebraicNotation.of(piece, operations, from, to, null));

        return AlgebraicNotation.opponentKingStatus(operations);
    }

    /**
     * Processes the movement of the rook during a short castling.
     *
     * @param to The coordinate the king is moving to during the castling.
     */
    private void moveRookInShortCastling(final Coordinate to) {
        final boolean isWhiteCastling = to.getRow() == 1;

        if (isWhiteCastling) {
            Field startField = fieldMap.get(Coordinate.H1);
            Field endField = fieldMap.get(Coordinate.F1);
            Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            startField.removeFigure();
            endField.addFigure(rook);
            return;
        }

        Field startField = fieldMap.get(Coordinate.H8);
        Field endField = fieldMap.get(Coordinate.F8);
        Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        startField.removeFigure();
        endField.addFigure(rook);
    }

    /**
     * Processes the movement of the rook during a long castling.
     *
     * @param to The coordinate the king is moving to during the castling.
     */
    private void moveRookInLongCastling(final Coordinate to) {
        final boolean isWhiteCastling = to.getRow() == 1;

        if (isWhiteCastling) {
            Field startField = fieldMap.get(Coordinate.A1);
            Field endField = fieldMap.get(Coordinate.D1);
            Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            startField.removeFigure();
            endField.addFigure(rook);
            return;
        }

        Field startField = fieldMap.get(Coordinate.A8);
        Field endField = fieldMap.get(Coordinate.D8);
        Rook rook = (Rook) startField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        startField.removeFigure();
        endField.addFigure(rook);
    }

    /**
     * Reverts the last move made in the game.
     *
     * @return `true` if the last move was successfully reverted, `false` otherwise.
     */
    protected final boolean returnOfTheMovement() {
        if (listOfAlgebraicNotations.isEmpty()) {
            return false;
        }

        AlgebraicNotation lastMovement = listOfAlgebraicNotations.getLast();
        StatusPair<AlgebraicNotation.Castle> statusPair = AlgebraicNotation.isCastling(lastMovement);
        if (statusPair.status()) {
            revertCastling(statusPair.valueOrElseThrow());
            return true;
        }

        var movementPair = lastMovement.coordinates();
        Coordinate from = movementPair.getFirst();
        Coordinate to = movementPair.getSecond();

        Field startedField = fieldMap.get(from);
        Field endedField = fieldMap.get(to);
        Piece piece = endedField.pieceOptional().orElseThrow();

        endedField.removeFigure();
        startedField.addFigure(piece);

        listOfAlgebraicNotations.removeLast();

        if (piece instanceof King king) {
            changedKingPosition(king, from);
            changeOfCastlingAbilityInRevertMove(king, null);
        }

        switchFiguresTurn();
        return true;
    }

    /**
     * Reverts a castling move.
     *
     * @param castle the castling information
     */
    private void revertCastling(final AlgebraicNotation.Castle castle) {
        var movementPair = castlingCoordinates(castle);
        Coordinate from = movementPair.getFirst();
        Coordinate to = movementPair.getSecond();

        Field kingStartedField = fieldMap.get(from);
        Field kingEndedField = fieldMap.get(to);
        King king = (King) kingEndedField.pieceOptional().orElseThrow();

        kingEndedField.removeFigure();
        kingStartedField.addFigure(king);

        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(castle);
        if (shortCasting) {
            revertRookInShortCastling(to);
        } else {
            revertRookInLongCastling(to);
        }

        listOfAlgebraicNotations.removeLast();

        changedKingPosition(king, from);
        changeOfCastlingAbilityInRevertMove(king, castle);

        switchFiguresTurn();
    }

    /**
     * Reverts the rook's move in a short castling.
     *
     * @param to the coordinate where the rook ended up after the castling
     */
    private void revertRookInShortCastling(Coordinate to) {
        final boolean isWhiteCastling = to.getRow() == 1;

        if (isWhiteCastling) {
            Field startField = fieldMap.get(Coordinate.H1);
            Field endField = fieldMap.get(Coordinate.F1);
            Rook rook = (Rook) endField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            endField.removeFigure();
            startField.addFigure(rook);
            return;
        }

        Field startField = fieldMap.get(Coordinate.H8);
        Field endField = fieldMap.get(Coordinate.F8);
        Rook rook = (Rook) endField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        endField.removeFigure();
        startField.addFigure(rook);
    }

    /**
     * Reverts the rook's move in a long castling.
     *
     * @param to the coordinate where the rook ended up after the castling
     */
    private void revertRookInLongCastling(Coordinate to) {
        final boolean isWhiteCastling = to.getRow() == 1;

        if (isWhiteCastling) {
            Field startField = fieldMap.get(Coordinate.A1);
            Field endField = fieldMap.get(Coordinate.D1);
            Rook rook = (Rook) endField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

            endField.removeFigure();
            startField.addFigure(rook);
            return;
        }

        Field startField = fieldMap.get(Coordinate.A8);
        Field endField = fieldMap.get(Coordinate.D8);
        Rook rook = (Rook) endField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move."));

        endField.removeFigure();
        startField.addFigure(rook);
    }

    /**
     * Retrieves the pair of coordinates representing a castling move.
     *
     * @param castle The type of castling move (short or long).
     * @return A Pair of Coordinates representing the castling move.
     */
    private Pair<Coordinate, Coordinate> castlingCoordinates(final AlgebraicNotation.Castle castle) {
        final boolean shortCastling = castle.equals(AlgebraicNotation.Castle.SHORT_CASTLING);
        if (shortCastling) {
            if (figuresTurn.equals(Color.WHITE)) {
                return Pair.of(Coordinate.E1, Coordinate.H1);
            } else {
                return Pair.of(Coordinate.E8, Coordinate.H8);
            }
        }

        if (figuresTurn.equals(Color.WHITE)) {
            return Pair.of(Coordinate.E1, Coordinate.A1);
        } else {
            return Pair.of(Coordinate.E1, Coordinate.A8);
        }
    }

    /**
     * Represents the different types of initialization for a chess board.
     */
    private enum InitializationTYPE {
        STANDARD, DURING_THE_GAME, READER_MODE
    }

    /**
     * Represents the different operations that can be performed during a chess move,
     * such as capture, promotion, check, checkmate, and stalemate or empty if operation not exists.
     */
    @Getter
    public enum Operations {
        PROMOTION("="),
        CAPTURE("X"),
        CHECK("+"),
        STALEMATE("."),
        CHECKMATE("#"),
        EMPTY("");

        private final String algebraicNotation;

        Operations(String algebraicNotation) {
            this.algebraicNotation = algebraicNotation;
        }
    }

    /**
     * Represents a single field on a chess board.
     * Each field has a coordinate and can either be empty or contain a chess piece.
     */
    public static class Field {
        private Piece piece;
        private final @Getter Coordinate coordinate;

        public Field(Coordinate coordinate, Piece piece) {
            Objects.requireNonNull(coordinate);
            this.coordinate = coordinate;
            this.piece = piece;
        }

        public boolean isEmpty() {
            return piece == null;
        }

        public boolean isPresent() {
            return piece != null;
        }

        public Optional<Piece> pieceOptional() {
            if (piece == null) {
                return Optional.empty();
            }

            Color color = piece.color();
            return switch (piece) {
                case King _ -> Optional.of(new King(color));
                case Queen _ -> Optional.of(new Queen(color));
                case Rook _ -> Optional.of(new Rook(color));
                case Bishop _ -> Optional.of(new Bishop(color));
                case Knight _ -> Optional.of(new Knight(color));
                default -> Optional.of(new Pawn(color));
            };
        }

        private void removeFigure() {
            this.piece = null;
        }

        private void addFigure(final Piece piece) {
            if (this.piece == null) {
                this.piece = piece;
            }
        }
    }

    /**
     * The `AlgebraicNotation` class is responsible for generating the algebraic notation representation of a chess move.
     * Algebraic notation is a standard way of recording chess moves, where each square on the chessboard is assigned a unique
     * coordinate, and the moves are described using these coordinates.
     * <p>
     * This class provides a set of static methods that take in various parameters related to a chess move, such as the piece
     * being moved, the set of operations performed during the move (e.g., capture, promotion), the starting and ending
     * coordinates of the move, and the piece being promoted to (if applicable), and generates the corresponding algebraic
     * notation representation.
     * <p>
     * The class also includes some helper methods, such as `isCastling()` and `castle()`, which are used to determine
     * whether a move is a castling move and to get the appropriate algebraic notation for it.
     */
    private record AlgebraicNotation(String algebraicNotation) {

        public AlgebraicNotation {
            Objects.requireNonNull(algebraicNotation);
            if (algebraicNotation.isBlank()) {
                throw new IllegalArgumentException("Algebraic notation can`t be black.");
            }
        }

        /**
         * Represents a simple pawn movement, where the pawn moves forward without capturing any piece.
         * The last symbol of the notation indicates an operation related to the enemy king or the end of the game as a whole, like
         * check ('+'), checkmate ('#'), stalemate ('.'), or just an empty string if not operation with opponent king ('').
         * Examples:
         * "e2-e4"
         * "e2-e4+"
         */
        private static final String SIMPLE_PAWN_MOVEMENT_FORMAT = "%s%s%s";

        /**
         * Represents a simple movement of a chess piece (other than a pawn), where the piece moves without capturing any piece.
         * Examples:
         * "Nf3-g5"
         * "Nf3-g5#"
         */
        private static final String SIMPLE_FIGURE_MOVEMENT_FORMAT = "%s%s%s%s";

        /**
         * Represents a pawn capture operation, where a pawn captures an opponent's piece.
         * Examples:
         * "e2xd3"
         * "e2xd3."
         */
        private static final String PAWN_CAPTURE_OPERATION_FORMAT = "%s%s%s%s";

        /**
         * Represents a capture operation by a chess piece (other than a pawn), where the piece captures an opponent's piece.
         * Examples:
         * "Nf3xd4"
         * "Nf3xd4+"
         */
        private static final String FIGURE_CAPTURE_OPERATION_FORMAT = "%s%s%s%s%s";

        /**
         * Represents a castling move, where the king and the rook move together.
         * Examples:
         * "O-O" (short castling) or "O-O-O" (long castling)
         * "O-O+" (short castling) or "O-O-O+" (long castling)
         */
        private static final String CASTLE_PLUS_OPERATION_FORMAT = "%s%s";

        /**
         * Represents a pawn promotion, where a pawn is promoted to a different piece (e.g., queen, rook, bishop, or knight).
         * Examples:
         * "a7-a8=Q"
         * "a7-a8=Q+"
         */
        private static final String PROMOTION_FORMAT = "%s%s=%s%s";

        /**
         * Represents a pawn promotion that also includes a capture operation.
         * Examples:
         * "a7xb8=Q"
         * "a7xb8=Q#"
         */
        private static final String PROMOTION_PLUS_OPERATION_FORMAT = "%s%s%s=%s%s";

        /**
         * Generates the algebraic notation representation of a chess move.
         *
         * @param piece The piece being moved.
         * @param operationsSet The set of operations performed during the move (e.g., capture, promotion, check, checkmate, stalemate).
         * @param from The starting coordinate of the move.
         * @param to The ending coordinate of the move.
         * @param inCaseOfPromotion The piece that the pawn is being promoted to (if applicable).
         * @return An `AlgebraicNotation` object representing the algebraic notation of the move.
         * @throws NullPointerException if any of the required parameters are null.
         */
        public static AlgebraicNotation of(
                Piece piece, Set<Operations> operationsSet, Coordinate from, Coordinate to, @Nullable Piece inCaseOfPromotion
        ) {
            Objects.requireNonNull(piece);
            Objects.requireNonNull(operationsSet);
            Objects.requireNonNull(from);
            Objects.requireNonNull(to);

            final boolean castle = isCastling(piece, from, to);
            if (castle) {
                return castlingRecording(operationsSet, to);
            }

            final boolean promotion = operationsSet.contains(Operations.PROMOTION);
            if (promotion) {
                Objects.requireNonNull(inCaseOfPromotion);
                return promotionRecording(operationsSet, from, to, inCaseOfPromotion);
            }

            final boolean capture = operationsSet.contains(Operations.CAPTURE);
            if (capture) {
                if (piece instanceof Pawn) {
                    return pawnCaptureRecording(operationsSet, from, to);
                }
                return figureCaptureRecording(piece, operationsSet, from, to);
            }

            return simpleMovementRecording(piece, operationsSet,from, to);
        }

        /**
         * Generates the algebraic notation representation of a castling move.
         *
         * @param operationsSet The set of operations performed during the move.
         * @param finalCoordinate The ending coordinate of the castling move.
         * @return An `AlgebraicNotation` object representing the algebraic notation of the castling move.
         */
        private static AlgebraicNotation castlingRecording(Set<Operations> operationsSet, Coordinate finalCoordinate) {
            Operations opponentKingStatus = opponentKingStatus(operationsSet);
            String algebraicNotation = CASTLE_PLUS_OPERATION_FORMAT.formatted(
                    castle(finalCoordinate).getAlgebraicNotation(), opponentKingStatus.getAlgebraicNotation()
            );
            return new AlgebraicNotation(algebraicNotation);
        }

        /**
         * Generates the algebraic notation representation of a pawn capture operation.
         *
         * @param operationsSet The set of operations performed during the move.
         * @param from The starting coordinate of the move.
         * @param to The ending coordinate of the move.
         * @return An `AlgebraicNotation` object representing the algebraic notation of the pawn capture operation.
         */
        private static AlgebraicNotation pawnCaptureRecording(Set<Operations> operationsSet, Coordinate from, Coordinate to) {
            Operations opponentKingStatus = opponentKingStatus(operationsSet);
            String algebraicNotation = String.format(
                    PAWN_CAPTURE_OPERATION_FORMAT, from, Operations.CAPTURE, to, opponentKingStatus.getAlgebraicNotation()
            );
            return new AlgebraicNotation(algebraicNotation);
        }

        /**
         * Generates the algebraic notation representation of a capture operation by a chess piece (other than a pawn).
         *
         * @param piece The piece being moved.
         * @param operationsSet The set of operations performed during the move.
         * @param from The starting coordinate of the move.
         * @param to The ending coordinate of the move.
         * @return An `AlgebraicNotation` object representing the algebraic notation of the figure capture operation.
         */
        private static AlgebraicNotation figureCaptureRecording(
                Piece piece, Set<Operations> operationsSet, Coordinate from, Coordinate to
        ) {
            Operations opponentKingStatus = opponentKingStatus(operationsSet);
            String algebraicNotation = FIGURE_CAPTURE_OPERATION_FORMAT.formatted(
                    pieceToType(piece), from, Operations.CAPTURE, to, opponentKingStatus.getAlgebraicNotation()
            );
            return new AlgebraicNotation(algebraicNotation);
        }

        /**
         * Generates the algebraic notation representation of a simple movement of a chess piece, where the piece moves without capturing any piece.
         *
         * @param piece The piece being moved.
         * @param operationsSet The set of operations performed during the move.
         * @param from The starting coordinate of the move.
         * @param to The ending coordinate of the move.
         * @return An `AlgebraicNotation` object representing the algebraic notation of the simple movement.
         */
        private static AlgebraicNotation simpleMovementRecording(
                Piece piece, Set<Operations> operationsSet, Coordinate from, Coordinate to
        ) {
            if (piece instanceof Pawn) {
                String algebraicNotation = SIMPLE_PAWN_MOVEMENT_FORMAT.formatted(
                        from, to, opponentKingStatus(operationsSet).getAlgebraicNotation()
                );
                return new AlgebraicNotation(algebraicNotation);
            }

            String algebraicNotation = SIMPLE_FIGURE_MOVEMENT_FORMAT.formatted(
                    pieceToType(piece), from, to, opponentKingStatus(operationsSet).getAlgebraicNotation()
            );
            return new AlgebraicNotation(algebraicNotation);
        }

        /**
         * Generates the algebraic notation representation of a pawn promotion, where a pawn is promoted to a different piece (e.g., queen, rook, bishop, or knight).
         *
         * @param operationsSet The set of operations performed during the move.
         * @param from The starting coordinate of the move.
         * @param to The ending coordinate of the move.
         * @param inCaseOfPromotion The piece that the pawn is being promoted to.
         * @return An `AlgebraicNotation` object representing the algebraic notation of the pawn promotion.
         */
        private static AlgebraicNotation promotionRecording(
                Set<Operations> operationsSet, Coordinate from, Coordinate to, Piece inCaseOfPromotion
        ) {
            String algebraicNotation;
            Operations opponentKingStatus = opponentKingStatus(operationsSet);

            if (operationsSet.contains(Operations.CAPTURE)) {
                algebraicNotation = PROMOTION_PLUS_OPERATION_FORMAT.formatted(
                        from, Operations.CAPTURE, to, inCaseOfPromotion, opponentKingStatus.getAlgebraicNotation()
                );
                return new AlgebraicNotation(algebraicNotation);
            }

            algebraicNotation = PROMOTION_FORMAT.formatted(from, to, inCaseOfPromotion, opponentKingStatus.getAlgebraicNotation());
            return new AlgebraicNotation(algebraicNotation);
        }

        /**
         * Determines the status of the opponent's king based on the set of operations performed during the move.
         *
         * @param operationsSet The set of operations performed during the move.
         * @return The status of the opponent's king.
         */
        private static Operations opponentKingStatus(Set<Operations> operationsSet) {
            if (operationsSet.contains(Operations.STALEMATE)) {
                return Operations.STALEMATE;
            }
            if (operationsSet.contains(Operations.CHECKMATE)) {
                return Operations.CHECKMATE;
            }
            if (operationsSet.contains(Operations.CHECK)) {
                return Operations.CHECK;
            }

            return Operations.EMPTY;
        }

        /**
         * Converts a piece to its corresponding algebraic notation type.
         *
         * @param piece The piece to be converted.
         * @return The algebraic notation type of the piece.
         */
        private static String pieceToType(Piece piece) {
            return switch (piece) {
                case King _ -> "K";
                case Queen _ -> "Q";
                case Rook _ -> "R";
                case Bishop _ -> "B";
                case Knight _ -> "N";
                default -> "";
            };
        }

        /**
         * Determines the type of castling move (short or long) based on the ending coordinate.
         *
         * @param to The ending coordinate of the castling move.
         * @return The type of castling move (short or long).
         */
        public static Castle castle(Coordinate to) {
            final boolean isShortCasting = to.equals(Coordinate.G1) || to.equals(Coordinate.G8);
            if (isShortCasting) {
                return Castle.SHORT_CASTLING;
            }

            return Castle.LONG_CASTLING;
        }

        /**
         * Checks if the given algebraic notation represents a castling move.
         *
         * @param algebraicNotation the algebraic notation to be checked
         * @return a {@link StatusPair} containing a boolean value indicating whether the
         *         given algebraic notation represents a castling move, and if so, the
         *         corresponding {@link Castle} instance (either {@link Castle#SHORT_CASTLING}
         *         or {@link Castle#LONG_CASTLING}).
         */
        public static StatusPair<Castle> isCastling(AlgebraicNotation algebraicNotation) {
            String algebraicNotationSTR = algebraicNotation.algebraicNotation();

            final boolean shortCasting = algebraicNotationSTR.equals(Castle.SHORT_CASTLING.getAlgebraicNotation()) ||
                    algebraicNotationSTR.substring(0, algebraicNotationSTR.length() - 1).equals(Castle.SHORT_CASTLING.getAlgebraicNotation());
            if (shortCasting) {
                return StatusPair.ofTrue(Castle.SHORT_CASTLING);
            }

            final boolean longCasting = algebraicNotationSTR.equals(Castle.LONG_CASTLING.getAlgebraicNotation()) ||
                    algebraicNotationSTR.substring(0, algebraicNotationSTR.length() - 1).equals(Castle.LONG_CASTLING.getAlgebraicNotation());
            if (longCasting) {
                return StatusPair.ofTrue(Castle.LONG_CASTLING);
            }

            return StatusPair.ofFalse();
        }

        /**
         * This function can only be used to predetermine the user's intention to make castling,
         * However, this is by no means a final validation of this operation.
         */
        public static boolean isCastling(Piece piece, Coordinate from, Coordinate to) {
            final boolean isKing = (piece instanceof King);
            if (!isKing) {
                return false;
            }

            final boolean isValidKingPosition = from.equals(Coordinate.E1) || from.equals(Coordinate.E8);
            if (!isValidKingPosition) {
                return false;
            }

            final boolean isCastle = to.equals(Coordinate.C1) || to.equals(Coordinate.G1) ||
                    to.equals(Coordinate.C8) || to.equals(Coordinate.G8);
            if (!isCastle) {
                return false;
            }

            return true;
        }

        /**
         * Extracts the "from" and "to" coordinates from the algebraic notation of a chess move.
         *
         * @return a {@link Pair} containing the "from" and "to" coordinates of the move.
         * @throws IllegalStateException if the algebraic notation represents a castling move, as the coordinates cannot be extracted in the same way.
         */
        public Pair<Coordinate, Coordinate> coordinates() {
            if (AlgebraicNotation.isCastling(this).status()) {
                throw new IllegalStateException("Invalid method usage, check the documentation.");
            }

            final Coordinate from;
            final Coordinate to;
            String algebraicNotation = this.algebraicNotation();

            final boolean startFromFigureType = Character.isLetter(algebraicNotation.charAt(0)) && Character.isLetter(algebraicNotation.charAt(1));
            if (startFromFigureType) {

                from = Coordinate.valueOf(algebraicNotation.substring(1, 3));

                final boolean containsCaptureOperation = containsCaptureOperation(algebraicNotation.charAt(3));
                if (containsCaptureOperation) {
                    to = Coordinate.valueOf(algebraicNotation.substring(4, 6));
                } else {
                    to = Coordinate.valueOf(algebraicNotation.substring(3, 5));
                }
            } else  {

                from = Coordinate.valueOf(algebraicNotation.substring(0, 2));

                final boolean containsCaptureOperation = containsCaptureOperation(algebraicNotation.charAt(2));
                if (containsCaptureOperation) {
                    to = Coordinate.valueOf(algebraicNotation.substring(3, 5));
                } else {
                    to = Coordinate.valueOf(algebraicNotation.substring(2, 4));
                }
            }

            return Pair.of(from, to);
        }

        private boolean containsCaptureOperation(char c) {
            if (c == 'X') {
                return true;
            }

            return false;
        }

        /**
         * Represents the two types of castling moves: short castling and long castling.
         */
        @Getter
        public enum Castle {
            SHORT_CASTLING("O-O"), LONG_CASTLING("O-O-O");

            private final String algebraicNotation;

            Castle(String algebraicNotation) {
                this.algebraicNotation = algebraicNotation;
            }
        }
    }

    /**
     * Initializes the standard chess board setup.
     * <p>
     * This method populates the {@code fieldMap} with the initial positions of the chess pieces on the board.
     * The white pieces are placed on the first and second rows, while the black pieces are placed on the seventh and eighth rows.
     * The remaining fields are left empty.
     */
    private void standardInitializer() {
        fieldMap.put(Coordinate.A1, new Field(Coordinate.A1, new Rook(Color.WHITE)));
        fieldMap.put(Coordinate.B1, new Field(Coordinate.B1, new Knight(Color.WHITE)));
        fieldMap.put(Coordinate.C1, new Field(Coordinate.C1, new Bishop(Color.WHITE)));
        fieldMap.put(Coordinate.D1, new Field(Coordinate.D1, new Queen(Color.WHITE)));
        fieldMap.put(Coordinate.E1, new Field(Coordinate.E1, new King(Color.WHITE)));
        fieldMap.put(Coordinate.F1, new Field(Coordinate.F1, new Bishop(Color.WHITE)));
        fieldMap.put(Coordinate.G1, new Field(Coordinate.G1, new Knight(Color.WHITE)));
        fieldMap.put(Coordinate.H1, new Field(Coordinate.H1, new Rook(Color.WHITE)));
        fieldMap.put(Coordinate.A8, new Field(Coordinate.A8, new Rook(Color.BLACK)));
        fieldMap.put(Coordinate.B8, new Field(Coordinate.B8, new Knight(Color.BLACK)));
        fieldMap.put(Coordinate.C8, new Field(Coordinate.C8, new Bishop(Color.BLACK)));
        fieldMap.put(Coordinate.D8, new Field(Coordinate.D8, new Queen(Color.BLACK)));
        fieldMap.put(Coordinate.E8, new Field(Coordinate.E8, new King(Color.BLACK)));
        fieldMap.put(Coordinate.F8, new Field(Coordinate.F8, new Bishop(Color.BLACK)));
        fieldMap.put(Coordinate.G8, new Field(Coordinate.G8, new Knight(Color.BLACK)));
        fieldMap.put(Coordinate.H8, new Field(Coordinate.H8, new Rook(Color.BLACK)));

        for (Coordinate coordinate : Coordinate.values()) {
            final boolean fieldForWhitePawn = coordinate.getRow() == 2;
            if (fieldForWhitePawn) {
                fieldMap.put(
                        coordinate, new Field(coordinate, new Pawn(Color.WHITE))
                );
            }

            final boolean fieldForBlackPawn = coordinate.getRow() == 7;
            if (fieldForBlackPawn) {
                fieldMap.put(
                        coordinate, new Field(coordinate, new Pawn(Color.BLACK))
                );
            }

            final boolean fieldMustBeEmpty = coordinate.getRow() != 1 && coordinate.getRow() != 2 &&
                    coordinate.getRow() != 7 && coordinate.getRow() != 8;
            if (fieldMustBeEmpty) {
                fieldMap.put(
                        coordinate, new Field(coordinate, null)
                );
            }
        }
    }
}
