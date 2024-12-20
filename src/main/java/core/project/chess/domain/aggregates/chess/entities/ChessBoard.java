package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.enumerations.Color;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import core.project.chess.domain.aggregates.chess.enumerations.GameResultMessage;
import core.project.chess.domain.aggregates.chess.pieces.*;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import jakarta.annotation.Nullable;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.*;

import static core.project.chess.domain.aggregates.chess.entities.ChessBoard.Operations.*;

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
 * @version 2.2
 */
public class ChessBoard {
    private final @Getter UUID chessBoardId;
    private Color figuresTurn;
    private byte ruleOf50MovesForWhite;
    private byte ruleOf50MovesForBlack;
    private byte materialAdvantageOfWhite;
    private byte materialAdvantageOfBlack;
    private boolean validWhiteShortCasting;
    private boolean validWhiteLongCasting;
    private boolean validBlackShortCasting;
    private boolean validBlackLongCasting;
    private Coordinate currentWhiteKingPosition;
    private Coordinate currentBlackKingPosition;
    private final Map<Coordinate, Field> fieldMap;
    private final Map<String, Byte> hashCodeOfBoard;
    private final ArrayList<String> fenRepresentationsOfBoard;
    private final List<AlgebraicNotation> listOfAlgebraicNotations;
    private final List<Piece> capturedWhitePieces = new ArrayList<>();
    private final List<Piece> capturedBlackPieces = new ArrayList<>();
    private static final Coordinate initialWhiteKingPosition = Coordinate.e1;
    private static final Coordinate initialBlackKingPosition = Coordinate.e8;
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
        Objects.requireNonNull(algebraicNotations);


        this.chessBoardId = chessBoardId;
        this.initializationTYPE = initializationTYPE;
        this.listOfAlgebraicNotations = algebraicNotations;
        this.fieldMap = new EnumMap<>(Coordinate.class);
        this.fenRepresentationsOfBoard = new ArrayList<>(10);
        this.hashCodeOfBoard = new HashMap<>(10, 0.75f);

        this.figuresTurn = Color.WHITE;
        this.currentWhiteKingPosition = initialWhiteKingPosition;
        this.currentBlackKingPosition = initialBlackKingPosition;

        this.ruleOf50MovesForWhite = 0;
        this.ruleOf50MovesForBlack = 0;
        this.materialAdvantageOfWhite = 39;
        this.materialAdvantageOfBlack = 39;

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
        final boolean standardInit = initializationTYPE.equals(InitializationTYPE.STANDARD);
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
                chessBoardId, initialWhiteKingPosition, initialBlackKingPosition, InitializationTYPE.STANDARD, new LinkedList<>()
        );
    }

    /**
     * Retrieves the `Field` object at the specified coordinate on the chess board.
     *
     * @param coordinate The coordinate of the field to retrieve.
     * @return A new `Field` object representing the field at the specified coordinate.
     */
    public Field field(final Coordinate coordinate) {
        Objects.requireNonNull(coordinate);

        return fieldMap.get(coordinate);
    }

    /**
     * Retrieves a list of pieces that have been captured by the white player.
     * <p>
     * This method accesses the collection of captured black pieces and returns the list.
     *
     * @return A list of {@link Piece} objects representing the pieces captured by the
     *         white player.
     */
    public List<Piece> whiteCaptures() {
        return capturedBlackPieces.stream().toList();
    }

    /**
     * Retrieves a list of pieces that have been captured by the black player.
     * <p>
     * This method accesses the collection of captured white pieces, and returns list.
     *
     * @return A list of {@link Piece} objects representing the pieces captured by the
     *         black player.
     */
    public List<Piece> blackCaptures() {
        return capturedWhitePieces.stream().toList();
    }

    /**
     * Returns the current position of the white king on the chess board.
     *
     * @return a new Coordinate object representing the current position of the white king
     */
    public Coordinate currentWhiteKingPosition() {
        return Coordinate.valueOf(currentWhiteKingPosition.toString());
    }

    /**
     * Returns the current position of the black king on the chess board.
     *
     * @return a new Coordinate object representing the current position of the white king
     */
    public Coordinate currentBlackKingPosition() {
        return Coordinate.valueOf(currentBlackKingPosition.toString());
    }

    /**
     * Retrieves a list of algebraic notations representing the moves made on the chess board.
     *
     * @return algebraic notations in type of AlgebraicNotation.
     */
    public List<String> listOfAlgebraicNotations() {
        return listOfAlgebraicNotations.stream().map(AlgebraicNotation::algebraicNotation).toList();
    }

    /**
     * Retrieves the last algebraic notation representing the moves made on the chess board.
     *
     * @return An algebraic notation in type of String.
     */
    public Optional<AlgebraicNotation> lastAlgebraicNotation() {
        if (listOfAlgebraicNotations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(listOfAlgebraicNotations.getLast());
    }

    /**
     * Retrieves a last generating toString() for ChessBoard.
     *
     * @return String representation of ChessBoard.
     */
    public String actualRepresentationOfChessBoard() {
        if (fenRepresentationsOfBoard.isEmpty()) {
            return this.toString();
        }

        return fenRepresentationsOfBoard.getLast();
    }

    /**
     * Generates a Portable Game Notation (PGN) string representation of the chess game.
     *
     * <p>This method constructs a PGN string by iterating through a list of algebraic notations
     * representing the moves made in the game. Each move is formatted according to PGN standards,
     * with move numbers and corresponding notations for both players.</p>
     *
     * <p>The method assumes that the list of algebraic notations contains pairs of moves,
     * where each pair consists of a move by White followed by a move by Black.</p>
     *
     * @return a string representing the PGN of the chess game, formatted with move numbers
     *         and corresponding algebraic notations for each turn.
     *
     * @example
     * <pre>
     *     // Assuming listOfAlgebraicNotations contains valid move notations
     *     String pgnString = pgn();
     *     // Output might look like: "1. e2-e4 e7-e5 2. Ng1-f3 Nb1-c6 ..."
     * </pre>
     */
    public String pgn() {
        final StringBuilder stringBuilder = new StringBuilder();

        int number = 1;
        for (int i = 0; i < listOfAlgebraicNotations.size(); i += 2) {
            final String notation = listOfAlgebraicNotations.get(i).algebraicNotation();

            final String secondNotation;
            if (i + 1 <= listOfAlgebraicNotations.size() - 1) {
                secondNotation = listOfAlgebraicNotations.get(i + 1).algebraicNotation();
            } else {
                secondNotation = "...";
            }

            stringBuilder.append(number)
                    .append(". ")
                    .append(notation)
                    .append(" ")
                    .append(secondNotation)
                    .append(" ");

            number++;
        }

        return stringBuilder.toString();
    }

    /**
     * Converts the collection of FEN (Forsyth-Edwards Notation) keys
     * associated with the hash code of the board into an array of strings.
     *
     * <p>This method retrieves the current FEN keys stored in the
     * {@code fenKeysOfHashCodeOfBoard} collection and converts them
     * into a standard Java array of strings. The resulting array can
     * be used for various purposes, such as exporting the FEN
     * representations of the board state or for further processing.</p>
     *
     * @return an array of strings containing the FEN keys. If the
     *         collection is empty, an empty array will be returned.
     */
    public String[] arrayOfFEN() {
        return fenRepresentationsOfBoard.toArray(String[]::new);
    }

    /**
     * Retrieves the latest movement on the chess board, represented as a pair of coordinates.
     * <p>
     * If the latest movement was a castling move, the method will return the coordinates of the King's position before and after the castling move.
     *
     * @return An Optional containing the pair of coordinates representing the latest movement, or an empty Optional if no movement has been made.
     */
    public Optional<Pair<Coordinate, Coordinate>> latestMovement() {
        if (listOfAlgebraicNotations.isEmpty()) {
            return Optional.empty();
        }

        AlgebraicNotation algebraicNotation = listOfAlgebraicNotations.getLast();

        StatusPair<AlgebraicNotation.Castle> statusPair = AlgebraicNotation.isCastling(algebraicNotation);
        if (statusPair.status()) {

            return Optional.of(algebraicNotation.castlingCoordinates(statusPair.orElseThrow(), figuresTurn));
        }

        return Optional.of(algebraicNotation.coordinates());
    }

    /**
     * Returns the number of movements based on the size of the
     * {@code listOfAlgebraicNotations}, assuming each movement
     * is represented by a pair of notations.
     *
     * @return the number of movements (size / 2).
     */
    public int countOfMovement() {
        return listOfAlgebraicNotations.size() / 2;
    }

    /**
     * Checks if the move from the specified 'from' coordinate to the 'to' coordinate is safe for the king.
     *
     * @param from  The coordinate the piece is moving from.
     * @param to    The coordinate the piece is moving to.
     * @return True if the move is safe for the king, false otherwise.
     */
    public boolean safeForKing(final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        Color kingColor = fieldMap.get(from).pieceOptional().orElseThrow().color();
        King king = theKing(kingColor);

        return king.safeForKing(this, kingColor, from, to);
    }

    /**
     * Retrieves the king object based on the specified color.
     *
     * @param kingColor The color of the king to retrieve.
     * @return The king object.
     * @throws IllegalStateException If the king object cannot be found.
     */
    public King theKing(final Color kingColor) {
        Objects.requireNonNull(kingColor);
        return kingColor.equals(Color.WHITE) ?

                (King) fieldMap
                        .get(currentWhiteKingPosition)
                        .pieceOptional()
                        .orElseThrow(
                                () -> new IllegalStateException("Unexpected exception.")
                        )
                :
                (King) fieldMap
                        .get(currentBlackKingPosition)
                        .pieceOptional()
                        .orElseThrow(
                                () -> new IllegalStateException("Unexpected exception.")
                        );
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
    private boolean validateFiguresTurnAndPieceExisting(final Coordinate coordinate) {
        final Color colorOfFiguresThatTryToMove = this.field(coordinate)
                .pieceOptional()
                .orElseThrow(
                        () -> new IllegalArgumentException("Invalid move. No piece for movement.")
                )
                .color();

        return colorOfFiguresThatTryToMove == figuresTurn;
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
     * Updates the material advantage based on the removal of a piece from the board.
     * <p>
     * This method adjusts the material advantage for the player whose piece has been removed.
     * It calculates the price of the removed piece using the materialAdvantageOfFigure method
     * and subtracts that value from the respective player's material advantage.
     *
     * @param removedPiece the piece that has been removed from the board.
     */
    private void changeInMaterialAdvantage(final Piece removedPiece) {
        final byte price = materialAdvantageOfFigure(removedPiece);

        if (removedPiece.color().equals(Color.WHITE)) {
            materialAdvantageOfWhite -= price;
        }

        if (removedPiece.color().equals(Color.BLACK)) {
            materialAdvantageOfBlack -= price;
        }
    }

    /**
     * Updates the material advantage in case of a pawn promotion.
     * <p>
     * This method adjusts the material advantage for both players when a pawn is promoted
     * to another piece. It ensures that the promoted piece is neither a King nor a Pawn,
     * and then updates the material advantages accordingly.
     *
     * @param promotionFigure the piece that the pawn is promoted to.
     * @throws IllegalArgumentException if the promotionFigure is a King or a Pawn.
     */
    private void changeInMaterialAdvantageInCaseOfPromotion(final Piece promotionFigure) {
        if (promotionFigure instanceof King || promotionFigure instanceof Pawn) {
            throw new IllegalArgumentException("Unexpected situation.");
        }

        final byte price = materialAdvantageOfFigure(promotionFigure);

        if (promotionFigure.color().equals(Color.WHITE)) {
            materialAdvantageOfWhite -= 1;
            materialAdvantageOfBlack += price;
        }

        if (promotionFigure.color().equals(Color.BLACK)) {
            materialAdvantageOfBlack -= 1;
            materialAdvantageOfWhite += price;
        }
    }

    public byte materialAdvantageOfFigure(final Piece piece) {
        return switch (piece) {
            case Queen q -> 9;
            case Rook r -> 5;
            case Knight n -> 3;
            case Bishop b -> 3;
            case Pawn p -> 1;
            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
    }

    /**
     * Determines if a pawn can perform a capture en passant on the specified coordinate.
     *
     * <p>This method checks if the given piece is a pawn and if its previous move was a
     * double advance (two squares forward) from its starting position. If so, it verifies
     * whether the target coordinate is directly adjacent to the pawn's last move's ending
     * position, allowing for an en passant capture.</p>
     *
     * @param piece The piece to check, which should be an instance of {@link Pawn}.
     * @param to The target coordinate where the pawn is attempting to capture en passant.
     * @return {@code true} if the pawn can capture en passant at the specified coordinate;
     *         {@code false} otherwise.
     *
     * @throws NoSuchElementException if there is no latest movement recorded.
     */
    private boolean isCaptureOnPassage(final Piece piece, final Coordinate to) {
        if (piece instanceof Pawn pawn && pawn.previousMoveWasPassage(this)) {
            final Coordinate lastMoveEnd = latestMovement().orElseThrow().getSecond();

            if (lastMoveEnd.columnToInt() != to.columnToInt()) {
                return false;
            }

            if (piece.color().equals(Color.WHITE) && lastMoveEnd.getRow() - to.getRow() == -1) {
                return true;
            }

            return piece.color().equals(Color.BLACK) && lastMoveEnd.getRow() - to.getRow() == 1;
        }

        return false;
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
        if (!(piece instanceof Rook) && !(piece instanceof King)) {
            throw new IllegalStateException("Invalid method usage, check documentation. Only kings and rooks available for this function.");
        }

        final Color color = piece.color();

        final boolean whiteColorFigure = color.equals(Color.WHITE);
        if (whiteColorFigure) {

            if (piece instanceof King) {
                this.validWhiteShortCasting = false;
                this.validWhiteLongCasting = false;
                return;
            }

            if (from.equals(Coordinate.a1)) {
                validWhiteLongCasting = false;
            }

            if (from.equals(Coordinate.h1)) {
                validWhiteShortCasting = false;
            }

        } else {

            if (piece instanceof King) {
                this.validBlackShortCasting = false;
                this.validBlackLongCasting = false;
                return;
            }

            if (from.equals(Coordinate.a8)) {
                validBlackLongCasting = false;
            }

            if (from.equals(Coordinate.h8)) {
                validBlackShortCasting = false;
            }

        }
    }

    /**
     * This method is responsible for updating the castling ability of a piece during a revert move.
     *
     * @param piece  The piece being moved.
     * @throws IllegalStateException if the provided piece is not a King or Rook.
     */
    private void changeOfCastlingAbilityInRevertMove(final Piece piece) {
        if (!(piece instanceof King) && !(piece instanceof Rook)) {
            throw new IllegalStateException("Invalid method usage, check documentation. Only kings and rooks available for this function.");
        }

        final String lastFEN = fenRepresentationsOfBoard.getLast();
        final String aboutCastlingAbility = lastFEN.substring(lastFEN.indexOf(' '));

        if (aboutCastlingAbility.contains("K")) {
            validWhiteShortCasting = true;
        } else {
            validWhiteShortCasting = false;
        }

        if (aboutCastlingAbility.contains("Q")) {
            validWhiteLongCasting = true;
        } else {
            validWhiteLongCasting = false;
        }

        if (aboutCastlingAbility.contains("k")) {
            validBlackShortCasting = true;
        } else {
            validBlackShortCasting = false;
        }

        if (aboutCastlingAbility.contains("q")) {
            validBlackLongCasting = true;
        } else {
            validBlackLongCasting = false;
        }
    }

    /**
     * Updates the count of moves for the 50-move rule based on the current piece and operations performed.
     * <p>
     * The 50-move rule states that a player can claim a draw if no pawn has been moved and no capture has been made
     * in the last 50 moves. This method checks the current piece and the operations performed during the turn to
     * determine whether to increment the move counters for the respective player (White or Black).
     *
     * @param piece The piece that is currently being moved. This is used to check if the piece is a pawn.
     * @param operations A set of operations that were performed during the turn. This is used to check if a capture
     *                   occurred.
     *
     * <p>
     * The method works as follows:
     * <ul>
     *     <li>If the current piece is a pawn and no capture operation is present, the method increments the move
     *         counter for the respective player (White or Black).</li>
     *     <li>If the current piece is a pawn or a capture operation has occurred, the method resets the move counter
     *         for the respective player to zero.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Note: The counters for the 50-move rule are maintained separately for White and Black, allowing each player
     * to track their own move counts independently.
     * </p>
     */
    private void ruleOf50MovesAbility(final Piece piece, final Set<Operations> operations) {
        if (!operations.contains(CAPTURE) && piece instanceof Pawn) {

            if (figuresTurn.equals(Color.WHITE)) {
                ruleOf50MovesForWhite++;
            }

            if (figuresTurn.equals(Color.BLACK)) {
                ruleOf50MovesForBlack++;
            }
        }

        if (piece instanceof Pawn || operations.contains(CAPTURE)) {

            if (figuresTurn.equals(Color.WHITE)) {
                ruleOf50MovesForWhite = 0;
            }

            if (figuresTurn.equals(Color.BLACK)) {
                ruleOf50MovesForBlack = 0;
            }
        }
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
                return Pair.of(Coordinate.e1, Coordinate.h1);
            } else {
                return Pair.of(Coordinate.e8, Coordinate.h8);
            }
        }

        if (figuresTurn.equals(Color.WHITE)) {
            return Pair.of(Coordinate.e1, Coordinate.a1);
        } else {
            return Pair.of(Coordinate.e1, Coordinate.a8);
        }
    }

    /**
     * Determines whether the given move represents a castling move for the specified piece.
     *
     * @param piece the piece that is being moved
     * @param from the starting coordinate of the piece
     * @param to the ending coordinate of the piece
     * @return true if the move represents a valid castling move, false otherwise
     */
    public boolean isCastling(final Piece piece, final Coordinate from, final Coordinate to) {
        Objects.requireNonNull(piece);
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        final boolean king = piece instanceof King;
        if (!king) {
            return false;
        }

        return from.equals(Coordinate.e1) && (to.equals(Coordinate.g1) || to.equals(Coordinate.c1))
                || from.equals(Coordinate.e8) && (to.equals(Coordinate.g8) || to.equals(Coordinate.c8));
    }

    /**
     * Checks if there is at least one pawn on the chessboard.
     * <p>
     * This method iterates through all possible coordinates on the board,
     * checking each field to see if it contains a piece. If it finds a piece
     * that is an instance of the Pawn class, it returns true. If no pawns
     * are found after checking all fields, it returns false.
     *
     * @return true if at least one pawn is present on the board, false otherwise.
     */
    public boolean isAtLeastOnePawnOnBoard() {
        for (final Coordinate coordinate : Coordinate.values()) {
            final Field field = fieldMap.get(coordinate);

            if (field.isPresent() && field.pieceOptional().orElseThrow() instanceof Pawn) {
                return true;
            }
        }

        return false;
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
    public boolean ableToCastling(final Color color, final AlgebraicNotation.Castle castle) {
        Objects.requireNonNull(color);
        Objects.requireNonNull(castle);

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
     * Processes a piece repositioning on the chess board.
     *
     * @param from                  The coordinate the piece is moving from.
     * @param to                    The coordinate the piece is moving to.
     * @param inCaseOfPromotion     The piece to promote to in case of a pawn promotion, or null if no promotion.
     * @return The operations performed during the repositioning.
     * @throws IllegalArgumentException If the move is invalid.
     */
    protected final GameResultMessage reposition(final Coordinate from, final Coordinate to, final @Nullable Piece inCaseOfPromotion) {
        /** Preparation of necessary data and validation.*/
        Objects.requireNonNull(from);
        Objects.requireNonNull(to);

        if (!validateFiguresTurnAndPieceExisting(from)) {
            throw new IllegalArgumentException(
                    String.format("At the moment, the player for %s must move and not the opponent", figuresTurn)
            );
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move. Coordinate 'from' can`t be equal to coordinate 'to'");
        }

        final Field startField = fieldMap.get(from);
        final Field endField = fieldMap.get(to);
        final Piece piece = startField.pieceOptional().orElseThrow();

        /** Delegate the operation to another method if necessary.*/
        if (isCastling(piece, from, to)) {
            return castling(from, to);
        }

        /** Validation.*/
        final StatusPair<Set<Operations>> statusPair = piece.isValidMove(this, from, to);
        if (!statusPair.status()) {
            throw new IllegalArgumentException("Invalid move. Failed validation for %s movement.".formatted(piece.toString()));
        }

        final boolean promotionOperation = statusPair.orElseThrow().contains(PROMOTION);
        if (promotionOperation) {

            Pawn pawn = (Pawn) piece;

            final boolean isValidPieceForPromotion = pawn.isValidPromotion(pawn, inCaseOfPromotion);
            if (!isValidPieceForPromotion) {
                throw new IllegalArgumentException("Mismatch in color of figures for pawn promotion. Failed validation.");
            }

        }

        /** Process operations from StatusPair. All validation need to be processed before that.*/
        final Set<Operations> operations = statusPair.orElseThrow();

        startField.removeFigure();

        if (operations.contains(CAPTURE)) {

            boolean captureOnPassage = isCaptureOnPassage(piece, to);

            if (captureOnPassage) {
                Field field = fieldMap.get(latestMovement().orElseThrow().getSecond());

                if (piece.color().equals(Color.WHITE)) {
                    capturedWhitePieces.add(field.piece);
                }

                if (piece.color().equals(Color.BLACK)) {
                    capturedBlackPieces.add(field.piece);
                }

                changeInMaterialAdvantage(field.piece);
                field.removeFigure();
            }

            if (piece.color().equals(Color.WHITE) && !captureOnPassage) {
                capturedWhitePieces.add(endField.piece);
            }

            if (piece.color().equals(Color.BLACK) && !captureOnPassage) {
                capturedBlackPieces.add(endField.piece);
            }

            changeInMaterialAdvantage(endField.piece);
            endField.removeFigure();
        }

        if (operations.contains(PROMOTION)) {
            if (!endField.isEmpty()) {
                endField.removeFigure();
            }

            changeInMaterialAdvantageInCaseOfPromotion(inCaseOfPromotion);
            endField.addFigure(inCaseOfPromotion);
        } else {
            endField.addFigure(piece);
        }

        /** Check for Checkmate, Stalemate, Check after move executed...*/
        final King opponentKing = theKing(piece.color().equals(Color.WHITE) ? Color.BLACK : Color.WHITE);

        operations.add(
                opponentKing.kingStatus(this, opponentKing.color(), Pair.of(from, to))
        );

        final boolean isStalemate = countOfMovement() + 1 >= 10 && opponentKing.stalemate(this, opponentKing.color(), Pair.of(from, to));
        if (isStalemate) {
            operations.add(STALEMATE);
        }

        /** Monitor opportunities for castling, switch players.*/
        if (piece instanceof King king) {
            changedKingPosition(king, to);
            changeOfCastlingAbility(from, king);
        }

        if (piece instanceof Rook rook) {
            changeOfCastlingAbility(from, rook);
        }

        switchFiguresTurn();
        ruleOf50MovesAbility(piece, operations);

        /** Recording the move made in algebraic notation and Fen.*/
        final String currentPositionHash = this.toString();
        fenRepresentationsOfBoard.add(currentPositionHash);

        hashCodeOfBoard.put(currentPositionHash, (byte) (hashCodeOfBoard.getOrDefault(currentPositionHash, (byte) 0) + 1));

        final var inCaseOfPromotionPieceType = inCaseOfPromotion == null ? null : AlgebraicNotation.pieceToType(inCaseOfPromotion);
        listOfAlgebraicNotations.add(AlgebraicNotation.of(AlgebraicNotation.pieceToType(piece), operations, from, to, inCaseOfPromotionPieceType));

        /** Retrieve message about game result.*/
        final Operations opponentKingStatus = AlgebraicNotation.opponentKingStatus(operations);

        if (opponentKingStatus.equals(STALEMATE)) {
            return GameResultMessage.Stalemate;
        }

        if (opponentKingStatus.equals(CHECKMATE)) {
            return GameResultMessage.Checkmate;
        }

        if (opponentKingStatus.equals(CHECK)) {
            return GameResultMessage.Continue;
        }

        if (materialAdvantageOfWhite <= 3 && materialAdvantageOfBlack <= 3 && !isAtLeastOnePawnOnBoard()) {
            return GameResultMessage.InsufficientMatingMaterial;
        }

        if (ruleOf50MovesForWhite == 50 && ruleOf50MovesForBlack == 50) {
            return GameResultMessage.RuleOf50Moves;
        }

        if (hashCodeOfBoard.get(currentPositionHash) == 3) {
            return GameResultMessage.RuleOf3EqualsPositions;
        }

        return GameResultMessage.Continue;
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
    private GameResultMessage castling(final Coordinate from, final Coordinate to) {
        /** Preparation of necessary data and validation.*/
        final Field kingStartedField = fieldMap.get(from);
        final Field kingEndField = fieldMap.get(to);
        final Piece piece = kingStartedField.pieceOptional().orElseThrow(() -> new IllegalArgumentException("Invalid move. No piece for movement."));

        if (!(piece instanceof King king)) {
            throw new IllegalStateException("Invalid method usage, check the documentation.");
        }

        final Color color = king.color();
        if (!ableToCastling(color, AlgebraicNotation.castle(to))) {
            throw new IllegalArgumentException("Invalid move. One or both of the pieces to be castled have made moves, castling is not possible.");
        }

        final StatusPair<Set<Operations>> statusPair = king.isValidMove(this, kingStartedField.getCoordinate(), kingEndField.getCoordinate());
        if (!statusPair.status()) {
            throw new IllegalArgumentException("Invalid move. Failed validation.");
        }

        final Set<Operations> operations = statusPair.orElseThrow();

        /**Process operations from StatusPair. All validation need to be processed before that.*/
        kingStartedField.removeFigure();
        kingEndField.addFigure(king);

        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(AlgebraicNotation.castle(to));
        if (shortCasting) {
            moveRookInShortCastling(to);
        } else {
            moveRookInLongCastling(to);
        }

        /** Check for Checkmate, Stalemate, Check after move executed...*/
        final King opponentKing = theKing(piece.color().equals(Color.WHITE) ? Color.BLACK : Color.WHITE);

        operations.add(
                opponentKing.kingStatus(this, opponentKing.color(), Pair.of(from, to))
        );

        final boolean isStalemate = countOfMovement() + 1 >= 10 && opponentKing.stalemate(this, opponentKing.color(), Pair.of(from, to));
        if (isStalemate) {
            operations.add(STALEMATE);
        }

        /** Monitor opportunities for castling and switch players.*/
        changedKingPosition(king, to);
        changeOfCastlingAbility(from, king);

        switchFiguresTurn();
        ruleOf50MovesAbility(piece, operations);

        /** Recording the move made in algebraic notation.*/
        final String currentPositionHash = toString();
        fenRepresentationsOfBoard.add(currentPositionHash);

        hashCodeOfBoard.put(currentPositionHash, (byte) (hashCodeOfBoard.getOrDefault(currentPositionHash, (byte) 0) + 1));

        listOfAlgebraicNotations.add(AlgebraicNotation.of(AlgebraicNotation.pieceToType(piece), operations, from, to, null));

        /** Retrieve message about game result.*/
        final Operations opponentKingStatus = AlgebraicNotation.opponentKingStatus(operations);

        if (opponentKingStatus.equals(STALEMATE)) {
            return GameResultMessage.Stalemate;
        }

        if (opponentKingStatus.equals(CHECKMATE)) {
            return GameResultMessage.Checkmate;
        }

        if (ruleOf50MovesForWhite == 50 && ruleOf50MovesForBlack == 50) {
            return GameResultMessage.RuleOf50Moves;
        }

        if (hashCodeOfBoard.get(currentPositionHash) == 3) {
            return GameResultMessage.RuleOf3EqualsPositions;
        }

        return GameResultMessage.Continue;
    }

    /**
     * Processes the movement of the rook during a short castling.
     *
     * @param to The coordinate the king is moving to during the castling.
     */
    private void moveRookInShortCastling(final Coordinate to) {
        final boolean isWhiteCastling = to.getRow() == 1;

        if (isWhiteCastling) {
            final Field startField = fieldMap.get(Coordinate.h1);
            final Field endField = fieldMap.get(Coordinate.f1);
            final Rook rook = (Rook) startField.pieceOptional().orElseThrow();

            startField.removeFigure();
            endField.addFigure(rook);
            return;
        }

        final Field startField = fieldMap.get(Coordinate.h8);
        final Field endField = fieldMap.get(Coordinate.f8);
        final Rook rook = (Rook) startField.pieceOptional().orElseThrow();

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
            final Field startField = fieldMap.get(Coordinate.a1);
            final Field endField = fieldMap.get(Coordinate.d1);
            final Rook rook = (Rook) startField.pieceOptional().orElseThrow();

            startField.removeFigure();
            endField.addFigure(rook);
            return;
        }

        final Field startField = fieldMap.get(Coordinate.a8);
        final Field endField = fieldMap.get(Coordinate.d8);
        final Rook rook = (Rook) startField.pieceOptional().orElseThrow();

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

        final String currentPositionHash = fenRepresentationsOfBoard.getLast();
        final AlgebraicNotation lastMovement = listOfAlgebraicNotations.getLast();
        final StatusPair<AlgebraicNotation.Castle> isCastling = AlgebraicNotation.isCastling(lastMovement);

        if (isCastling.status()) {
            revertCastling(isCastling.orElseThrow());
            return true;
        }

        final var movementPair = lastMovement.coordinates();
        final Coordinate from = movementPair.getFirst();
        final Coordinate to = movementPair.getSecond();

        final Field startedField = fieldMap.get(from);
        final Field endedField = fieldMap.get(to);
        final Piece piece = endedField.pieceOptional().orElseThrow();

        endedField.removeFigure();
        startedField.addFigure(piece);

        final boolean isCapture = lastMovement.algebraicNotation().contains("x");
        if (isCapture) {
             revertCapture(startedField, endedField, piece);
        }

        if (!isCapture && !(piece instanceof Pawn)){

            if (this.figuresTurn.equals(Color.WHITE)) {
                ruleOf50MovesForBlack--;
            } else {
                ruleOf50MovesForWhite--;
            }
        }

        fenRepresentationsOfBoard.removeLast();
        listOfAlgebraicNotations.removeLast();
        final byte newValue = (byte) (hashCodeOfBoard.get(currentPositionHash) - 1);
        if (newValue == 0) {
            hashCodeOfBoard.remove(currentPositionHash);
        } else {
            hashCodeOfBoard.put(currentPositionHash, newValue);
        }

        if (piece instanceof King king) {
            changedKingPosition(king, from);
            changeOfCastlingAbilityInRevertMove(king);
        }

        if (piece instanceof Rook rook) {
            changeOfCastlingAbilityInRevertMove(rook);
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
        final String currentPositionHash = toString();

        final var movementPair = castlingCoordinates(castle);
        final Coordinate from = movementPair.getFirst();
        final Coordinate to = movementPair.getSecond();

        final Field kingStartedField = fieldMap.get(from);
        final Field kingEndedField = fieldMap.get(to);
        final King king = (King) kingEndedField.pieceOptional().orElseThrow();

        kingEndedField.removeFigure();
        kingStartedField.addFigure(king);

        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(castle);
        if (shortCasting) {
            revertRookInShortCastling(to);
        } else {
            revertRookInLongCastling(to);
        }

        listOfAlgebraicNotations.removeLast();

        final byte newValue = (byte) (hashCodeOfBoard.get(currentPositionHash) - 1);
        if (newValue == 0) {
            hashCodeOfBoard.remove(currentPositionHash);
        } else {
            hashCodeOfBoard.put(currentPositionHash, newValue);
        }

        changedKingPosition(king, from);
        changeOfCastlingAbilityInRevertMove(king);

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
            final Field startField = fieldMap.get(Coordinate.h1);
            final Field endField = fieldMap.get(Coordinate.f1);
            final Rook rook = (Rook) endField.pieceOptional().orElseThrow();

            endField.removeFigure();
            startField.addFigure(rook);
            return;
        }

        final Field startField = fieldMap.get(Coordinate.h8);
        final Field endField = fieldMap.get(Coordinate.f8);
        final Rook rook = (Rook) endField.pieceOptional().orElseThrow();

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
            final Field startField = fieldMap.get(Coordinate.a1);
            final Field endField = fieldMap.get(Coordinate.d1);
            final Rook rook = (Rook) endField.pieceOptional().orElseThrow();

            endField.removeFigure();
            startField.addFigure(rook);
            return;
        }

        final Field startField = fieldMap.get(Coordinate.a8);
        final Field endField = fieldMap.get(Coordinate.d8);
        final Rook rook = (Rook) endField.pieceOptional().orElseThrow();

        endField.removeFigure();
        startField.addFigure(rook);
    }

    private void revertCapture(final Field startedField, final Field endedField, final Piece piece) {
        if (revertPotentialCaptureOnPassage(startedField, endedField, piece)) {
            return;
        }

        final Piece previouslyCapturedPiece;
        if (figuresTurn.equals(Color.WHITE)) {
            previouslyCapturedPiece = capturedBlackPieces.removeLast();
            endedField.addFigure(previouslyCapturedPiece);
            return;
        }

        previouslyCapturedPiece = capturedWhitePieces.removeLast();
        endedField.addFigure(previouslyCapturedPiece);
    }

    private boolean revertPotentialCaptureOnPassage(final Field startedField, final Field endedField, final Piece piece) {
        if (!(piece instanceof Pawn)) {
            return false;
        }

        final AlgebraicNotation penultimateMove = listOfAlgebraicNotations.get(listOfAlgebraicNotations.size() - 2);

        final boolean isTheMoveBeforeLastWasPassage = Pawn.isPassage(penultimateMove);
        if (!isTheMoveBeforeLastWasPassage) {
            return false;
        }

        final int startColumn = startedField.getCoordinate().columnToInt();
        final int startRow = startedField.getCoordinate().getRow();
        final int endColumn = endedField.getCoordinate().columnToInt();
        final int endRow = endedField.getCoordinate().getRow();

        final boolean isDiagonalMove = Math.abs(startColumn - endColumn) == 1 && Math.abs(startRow - endRow) == 1;
        if (!isDiagonalMove) {
            return false;
        }

        final Coordinate passageEnd = penultimateMove.coordinates().getSecond();

        if (endColumn != passageEnd.columnToInt()) {
            return false;
        }

        if (piece.color().equals(Color.WHITE)) {
            final boolean isValidRows = endRow - passageEnd.getRow() == 1;
            if (!isValidRows) {
                return false;
            }

            fieldMap.get(passageEnd).addFigure(new Pawn(Color.BLACK));
            return true;
        }

        final boolean isValidRows = endRow - passageEnd.getRow() == -1;
        if (!isValidRows) {
            return false;
        }

        fieldMap.get(passageEnd).addFigure(new Pawn(Color.WHITE));
        return true;
    }

    /**
     * Represents the different types of initialization for a chess board.
     */
    private enum InitializationTYPE {
        STANDARD, DURING_THE_GAME
    }

    /**
     * Represents the different operations that can be performed during a chess move,
     * such as capture, promotion, check, checkmate, and stalemate or empty if operation not exists.
     */
    @Getter
    public enum Operations {
        PROMOTION("="),
        CAPTURE("x"),
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

            return Optional.of(piece);
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
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChessBoard that = (ChessBoard) o;
        return validWhiteShortCasting == that.validWhiteShortCasting &&
                validWhiteLongCasting == that.validWhiteLongCasting &&
                validBlackShortCasting == that.validBlackShortCasting &&
                validBlackLongCasting == that.validBlackLongCasting &&
                Objects.equals(chessBoardId, that.chessBoardId) &&
                figuresTurn == that.figuresTurn &&
                currentWhiteKingPosition == that.currentWhiteKingPosition &&
                currentBlackKingPosition == that.currentBlackKingPosition &&
                Objects.equals(fieldMap, that.fieldMap) &&
                Objects.equals(listOfAlgebraicNotations, that.listOfAlgebraicNotations) &&
                initializationTYPE == that.initializationTYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                chessBoardId,
                figuresTurn,
                validWhiteShortCasting,
                validWhiteLongCasting,
                validBlackShortCasting,
                validBlackLongCasting,
                currentWhiteKingPosition,
                currentBlackKingPosition,
                fieldMap,
                listOfAlgebraicNotations,
                initializationTYPE
        );
    }

    /**
     * Generates a string representation of the current position on the chessboard.
     * <p>
     * This method iterates over all the fields of the board, represented as coordinates,
     * and creates a string where each field is associated with its coordinate
     * and the corresponding piece (if present). If the field is empty,
     * no piece information is added for that field.
     *
     * @return A string representing the current position on the board by FEN concepts, where each
     *         coordinate is mapped to a piece representation or an empty string.
     */
    @Override
    public final String toString() {
        var fen = new StringBuilder();

        int row = 8;
        int countOfEmptyFields = 0;
        for (final Coordinate coordinate : Coordinate.values()) {

            if (coordinate.getRow() == row - 1) {
                row -= 1;

                if (countOfEmptyFields == 0) {
                    fen.append("/");
                } else {
                    fen
                            .append(countOfEmptyFields)
                            .append("/");

                    countOfEmptyFields = 0;
                }
            }

            final Field field = fieldMap.get(coordinate);

            if (field.isEmpty()) {
                countOfEmptyFields++;
            }

            if (countOfEmptyFields == 8) {
                fen.append(countOfEmptyFields);
                countOfEmptyFields = 0;
            }

            else if (field.isPresent()) {

                if (countOfEmptyFields != 0) {
                    fen
                            .append(countOfEmptyFields)
                            .append(
                                    convertPieceToHash(field.pieceOptional().orElseThrow())
                            );

                    countOfEmptyFields = 0;
                } else {
                    fen
                            .append(
                                    convertPieceToHash(field.pieceOptional().orElseThrow())
                            );
                }
            }
        }

        if (countOfEmptyFields != 0) {
            fen.append(countOfEmptyFields);
        }

        fen.append(" ");
        if (figuresTurn.equals(Color.WHITE)) {
            fen.append("w");
        } else {
            fen.append("b");
        }

        fen.append(" ");

        if (validWhiteShortCasting) {
            fen.append("K");
        }
        if (validWhiteLongCasting) {
            fen.append("Q");
        }
        if (!validWhiteShortCasting && !validWhiteLongCasting) {
            fen.append("- ");
        }

        if (validBlackShortCasting) {
            fen.append("k");
        }
        if (validBlackLongCasting) {
            fen.append("q");
        }
        if (!validBlackShortCasting && !validBlackLongCasting) {
            fen.append("- ");
        }

        final var lastMovement = latestMovement();
        if (latestMovement().isPresent() && new Pawn(Color.WHITE).previousMoveWasPassage(this)) {
            final Coordinate to = lastMovement.orElseThrow().getSecond();

            final Coordinate intermediateFieldOfPassage = Coordinate
                    .of(
                            to.getRow() == 4 ? to.getRow() - 1 : to.getRow() + 1,
                            to.columnToInt()
                    )
                    .orElseThrow();

            final String result = " " + intermediateFieldOfPassage.getColumn() + intermediateFieldOfPassage.getRow();
            fen.append(result);
        }

        return fen.toString();
    }

    private String convertPieceToHash(final Piece piece) {
        return switch (piece) {
            case King(Color color) -> color.equals(Color.WHITE) ? "K" : "k";
            case Queen(Color color) -> color.equals(Color.WHITE) ? "Q" : "q";
            case Rook(Color color) -> color.equals(Color.WHITE) ? "R" : "r";
            case Bishop(Color color) -> color.equals(Color.WHITE) ? "B" : "b";
            case Knight(Color color) -> color.equals(Color.WHITE) ? "N" : "n";
            case Pawn(Color color) -> color.equals(Color.WHITE) ? "P" : "p";
        };
    }

    /**
     * Initializes the standard chess board setup.
     * <p>
     * This method populates the {@code fieldMap} with the initial positions of the chess pieces on the board.
     * The white pieces are placed on the first and second rows, while the black pieces are placed on the seventh and eighth rows.
     * The remaining fields are left empty.
     */
    private void standardInitializer() {
        fieldMap.put(Coordinate.a1, new Field(Coordinate.a1, new Rook(Color.WHITE)));
        fieldMap.put(Coordinate.b1, new Field(Coordinate.b1, new Knight(Color.WHITE)));
        fieldMap.put(Coordinate.c1, new Field(Coordinate.c1, new Bishop(Color.WHITE)));
        fieldMap.put(Coordinate.d1, new Field(Coordinate.d1, new Queen(Color.WHITE)));
        fieldMap.put(Coordinate.e1, new Field(Coordinate.e1, new King(Color.WHITE)));
        fieldMap.put(Coordinate.f1, new Field(Coordinate.f1, new Bishop(Color.WHITE)));
        fieldMap.put(Coordinate.g1, new Field(Coordinate.g1, new Knight(Color.WHITE)));
        fieldMap.put(Coordinate.h1, new Field(Coordinate.h1, new Rook(Color.WHITE)));
        fieldMap.put(Coordinate.a8, new Field(Coordinate.a8, new Rook(Color.BLACK)));
        fieldMap.put(Coordinate.b8, new Field(Coordinate.b8, new Knight(Color.BLACK)));
        fieldMap.put(Coordinate.c8, new Field(Coordinate.c8, new Bishop(Color.BLACK)));
        fieldMap.put(Coordinate.d8, new Field(Coordinate.d8, new Queen(Color.BLACK)));
        fieldMap.put(Coordinate.e8, new Field(Coordinate.e8, new King(Color.BLACK)));
        fieldMap.put(Coordinate.f8, new Field(Coordinate.f8, new Bishop(Color.BLACK)));
        fieldMap.put(Coordinate.g8, new Field(Coordinate.g8, new Knight(Color.BLACK)));
        fieldMap.put(Coordinate.h8, new Field(Coordinate.h8, new Rook(Color.BLACK)));

        for (Coordinate coordinate : Coordinate.values()) {
            if (coordinate.getRow() == 1 || coordinate.getRow() == 8) {
                continue;
            }

            switch (coordinate.getRow()) {
                case 2 -> fieldMap.put(coordinate, new Field(coordinate, new Pawn(Color.WHITE)));
                case 7 -> fieldMap.put(coordinate, new Field(coordinate, new Pawn(Color.BLACK)));
                default -> fieldMap.put(coordinate, new Field(coordinate, null));
            }
        }
    }

    private int pieceRank(Piece piece) {
        return switch (piece) {
            case Pawn pawn -> 1;
            case Knight knight -> 2;
            case Bishop bishop -> 3;
            case Rook rook -> 4;
            case Queen queen -> 5;
            default -> throw new IllegalStateException("Unexpected value: " + piece);
        };
    }

}
