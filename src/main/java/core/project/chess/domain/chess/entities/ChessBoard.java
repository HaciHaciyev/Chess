package core.project.chess.domain.chess.entities;

import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.Direction;
import core.project.chess.domain.chess.enumerations.GameResultMessage;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.util.ChessBoardNavigator;
import core.project.chess.domain.chess.util.ChessNotationsValidator;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.FromFEN;
import core.project.chess.domain.chess.value_objects.PlayerMove;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import jakarta.annotation.Nullable;
import lombok.Getter;

import java.util.*;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations.*;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

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
 * 7. **Chess Board Initialization**: The `standardInitializer()` method sets up the initial state of the chess board
 *    according to the standard chess rules, ensuring a consistent starting point for each game.
 *    The `ChessBoard` can also be initialized using FEN or PGN notation, allowing for custom board setups
 *    and game continuation from specific positions. Additionally, it can be configured as `pureChess`,
 *    which disables the following chess rules:
 *      - insufficient mating material;
 *      - threefold repetition rule;
 *      - fifty-move rule.
 *
 * @author Hadzhyiev Hadzhy
 * @version 3.0
 */
public class ChessBoard {
    private final UUID chessBoardId;

    private Color figuresTurn;

    private byte ruleOf50Moves;
    private byte countOfHalfMoves;

    /**
     * Flag indicating whether the game is in "pure chess" mode, disabling certain chess rules:
     * - insufficient mating material;
     * - threefold repetition rule;
     * - fifty-move rule.
     */
    private final boolean isPureChess;

    private byte materialAdvantageOfWhite;
    private byte materialAdvantageOfBlack;
    private boolean validWhiteShortCasting;
    private boolean validWhiteLongCasting;
    private boolean validBlackShortCasting;
    private boolean validBlackLongCasting;
    private Coordinate currentWhiteKingPosition;
    private Coordinate currentBlackKingPosition;

    /**
     * Map representing the current state of the board, where each coordinate is linked to a field.
     * Field is a nested class of ChessBoard and contains fields such as coordinates and, if available, a chess piece.
     */
    private final Map<Coordinate, Field> fieldMap;

    /**
     * Hash codes representing unique board positions for repetition detection.
     * Similar to FEN representations, but eliminates move counting and the 50-move rule at the end of FEN.
     * Necessary for counting identical positions on the board.
     * It is MANDATORY to take into account that toString() returns exactly this so-called hashCodeОfBoard, and not FEN.
     */
    private final Map<String, Integer> hashCodeOfBoard;

    /**
     * List of FEN representations of previous board states, used for game history tracking.
     */
    private final ArrayList<String> fenRepresentationsOfBoard;

    /**
     * List of moves recorded in algebraic notation for game replay and analysis.
     */
    private final List<AlgebraicNotation> listOfAlgebraicNotations;

    private final List<Piece> capturedWhitePieces = new ArrayList<>();
    private final List<Piece> capturedBlackPieces = new ArrayList<>();

    /**
     * Constructs a new `ChessBoard` instance with the given parameters.
     *
     * @param chessBoardId        The unique identifier of the chess board.
     * @param inCaseOfInitFromFEN The data for initialization of chess board from FEN.
     * @param isPureChess         The data which disables the following chess rules:
     *      - insufficient mating material;
     *      - three-fold rule;
     *      - fifty moves rule.
     */
    private ChessBoard(final UUID chessBoardId, @Nullable final FromFEN inCaseOfInitFromFEN,
                       final boolean isPureChess, @Nullable final List<AlgebraicNotation> listOfAlgebraicNotations) {
        Objects.requireNonNull(chessBoardId);

        this.chessBoardId = chessBoardId;
        this.isPureChess = isPureChess;

        this.ruleOf50Moves = 0;
        this.countOfHalfMoves = 0;
        this.fieldMap = new EnumMap<>(Coordinate.class);
        this.listOfAlgebraicNotations = new ArrayList<>();
        this.fenRepresentationsOfBoard = new ArrayList<>(10);
        this.hashCodeOfBoard = new HashMap<>(10, 0.75f);

        if (Objects.isNull(inCaseOfInitFromFEN)) {
            this.figuresTurn = WHITE;
            this.currentWhiteKingPosition = Coordinate.e1;
            this.currentBlackKingPosition = Coordinate.e8;

            this.materialAdvantageOfWhite = 39;
            this.materialAdvantageOfBlack = 39;

            this.validWhiteShortCasting = true;
            this.validWhiteLongCasting = true;
            this.validBlackShortCasting = true;
            this.validBlackLongCasting = true;

            standardInitializer();

            final String currentBoard = this.toString();
            this.hashCodeOfBoard.put(currentBoard, 1);
            this.fenRepresentationsOfBoard.add(FEN(currentBoard));

            if (Objects.nonNull(listOfAlgebraicNotations)) {
                validateAndForward(listOfAlgebraicNotations);
            }

            return;
        }

        Objects.requireNonNull(inCaseOfInitFromFEN);
        String FEN = inCaseOfInitFromFEN.fen();

        String currentPositionHash = fenToHashCodeOfBoard(FEN);
        hashCodeOfBoard.put(currentPositionHash, hashCodeOfBoard.getOrDefault(currentPositionHash, 0) + 1);
        fenRepresentationsOfBoard.add(FEN);

        this.figuresTurn = inCaseOfInitFromFEN.figuresTurn();
        this.currentWhiteKingPosition = inCaseOfInitFromFEN.whiteKing();
        this.currentBlackKingPosition = inCaseOfInitFromFEN.blackKing();

        this.materialAdvantageOfWhite = inCaseOfInitFromFEN.materialAdvantageOfWhite();
        this.materialAdvantageOfBlack = inCaseOfInitFromFEN.materialAdvantageOfBlack();

        this.validWhiteShortCasting = inCaseOfInitFromFEN.validWhiteShortCasting();
        this.validWhiteLongCasting = inCaseOfInitFromFEN.validWhiteLongCasting();
        this.validBlackShortCasting = inCaseOfInitFromFEN.validBlackShortCasting();
        this.validBlackLongCasting = inCaseOfInitFromFEN.validBlackLongCasting();

        initializerFromFEN(FEN);
        validateStalemateAndCheckmate(inCaseOfInitFromFEN);
    }

    /**
     * Factory method.
     * Creates a new `ChessBoard` instance with the standard chess board initialization.
     *
     * @return A new `ChessBoard` instance with the standard chess board initialization.
     */
    public static ChessBoard starndardChessBoard() {
        return new ChessBoard(UUID.randomUUID(), null, false, null);
    }

    /**
     *  Factory method.
     *   Creates a new `ChessBoard` instance with the chess board which will ignore next logic:
     *      - insufficient mating material;
     *      - three-fold rule;
     *      - fifty moves rule.
     *
     *  @return A new `ChessBoard` instance with the standard chess board initialization.
     */
    public static ChessBoard pureChess() {
        return new ChessBoard(UUID.randomUUID(), null, true, null);
    }

    /**
     * Factory method.
     * Creates a new ChessBoard instance from a specific position defined by FEN notation.
     *
     * @param fen The FEN notation representing the current position of the board.
     * @return A new ChessBoard instance initialized from the provided FEN notation.
     *
     * @throws IllegalArgumentException If the provided FEN notation are invalid.
     */
    public static ChessBoard fromPosition(final String fen) {
        StatusPair<FromFEN> isValidFEN = ChessNotationsValidator.validateFEN(fen);
        if (!isValidFEN.status()) {
            throw new IllegalArgumentException("Invalid FEN.");
        }

        return new ChessBoard(UUID.randomUUID(), isValidFEN.orElseThrow(), false, null);
    }

    /**
     * Factory method.
     * Creates a new ChessBoard instance from a specific position defined by FEN notation which will ignore next logic:
     *      - insufficient mating material;
     *      - three-fold rule;
     *      - fifty moves rule.
     *final UUID chessBoardId,
     * @param fen The FEN notation representing the current position of the board.
     * @return A new ChessBoard instance initialized from the provided FEN notation.
     * @throws IllegalArgumentException If the provided FEN notation are invalid.
     */
    public static ChessBoard pureChessFromPosition(final String fen) {
        StatusPair<FromFEN> isValidFEN = ChessNotationsValidator.validateFEN(fen);
        if (!isValidFEN.status()) {
            throw new IllegalArgumentException("Invalid FEN.");
        }

        return new ChessBoard(UUID.randomUUID(), isValidFEN.orElseThrow(), true, null);
    }

    /**
     * Factory method.
     * Creates a new `ChessBoard` instance from a specific position defined by PGN notation.
     * <p>
     * The created board will follow all standard chess rules.
     *
     * @param pgn The PGN notation representing the sequence of moves leading to the current position of the board.
     * @return A new `ChessBoard` instance initialized from the provided PGN notation.
     * @throws IllegalArgumentException If the provided PGN notation is invalid.
     */
    public static ChessBoard fromPGN(final String pgn) {
        List<AlgebraicNotation> listOfAlgebraicNotations = ChessNotationsValidator.listOfAlgebraicNotations(pgn);
        if (listOfAlgebraicNotations.isEmpty()) {
            throw new IllegalArgumentException("Invalid PGN");
        }

        return new ChessBoard(UUID.randomUUID(), null, false, listOfAlgebraicNotations);
    }

    /**
     * Factory method.
     * Creates a new `ChessBoard` instance from a specific position defined by PGN notation,
     * with certain rules ignored:
     *   - insufficient mating material;
     *   - three-fold repetition rule;
     *   - fifty-move rule.
     * <p>
     * This allows the creation of chess boards for scenarios where such rules are not enforced.
     *
     * @param pgn The PGN notation representing the sequence of moves leading to the current position of the board.
     * @return A new `ChessBoard` instance initialized from the provided PGN notation.
     * @throws IllegalArgumentException If the provided PGN notation is invalid.
     */
    public static ChessBoard pureChessFromPGN(final String pgn) {
        List<AlgebraicNotation> listOfAlgebraicNotations = ChessNotationsValidator.listOfAlgebraicNotations(pgn);
        if (listOfAlgebraicNotations.isEmpty()) {
            throw new IllegalArgumentException("Invalid PGN");
        }

        return new ChessBoard(UUID.randomUUID(), null, true, listOfAlgebraicNotations);
    }

    public UUID ID() {
        return chessBoardId;
    }

    public int countOfFullMoves() {
        return countOfHalfMoves == 1 || countOfHalfMoves == 0 ? 1 :countOfHalfMoves / 2;
    }

    public boolean isPureChess() {
        return isPureChess;
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

    public AlgebraicNotation[] arrayOfAlgebraicNotations() {
        return this.listOfAlgebraicNotations.toArray(new AlgebraicNotation[0]);
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
            return FEN();
        }

        return fenRepresentationsOfBoard.getLast();
    }

    private String FEN() {
        String stringOfBoard = this.toString();
        var sb = new StringBuilder(stringOfBoard);
        if (stringOfBoard.charAt(stringOfBoard.length() - 1) != ' ') {
            sb.append(' ');
        }

        return sb.append(this.ruleOf50Moves)
                .append(" ")
                .append(this.countOfFullMoves())
                .toString();
    }

    private String FEN(final String hashCodeOfBoard) {
        var sb = new StringBuilder(hashCodeOfBoard);
        if (hashCodeOfBoard.charAt(hashCodeOfBoard.length() - 1) != ' ') {
            sb.append(' ');
        }

        return sb.append(this.ruleOf50Moves)
                .append(" ")
                .append(this.countOfFullMoves())
                .toString();
    }

    /**
     * Checks whether the threefold repetition rule is active.
     * <p>
     * In chess, the threefold repetition rule states that if the same position
     * appears three times with the same possible moves for both players, the game
     * can be declared a draw. However, if all three repetitions occur while the
     * king is in check, the rule does not apply. This method analyzes the current
     * board position and determines whether it has occurred three times under
     * valid conditions.
     *
     * @return {@code true} if the position has been repeated three times under valid conditions, {@code false} otherwise.
     */
    public boolean isThreeFoldActive() {
        final String currentPositionHash = this.toString();
        return !isPureChess && hashCodeOfBoard.get(currentPositionHash) == 3;
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
     * Counts the number of half-moves (plies) made in the game.
     * <p>
     * A half-move, or plies, refers to a single move made by either player. In
     * chess terminology, a full move consists of two half-moves, one made by
     * White and one made by Black. This method calculates the total number of
     * half-moves by dividing the size of the list of algebraic notations by 2,
     * as each algebraic notation represents one half-move.
     *
     * @return the total number of half-moves (plies) made in the game.
     */
    public int countOfHalfMoves() {
        return this.countOfHalfMoves;
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

    private void validateStalemateAndCheckmate(FromFEN fromFEN) {
        final Color activeColor = fromFEN.figuresTurn();
        final Optional<Pair<Coordinate, Coordinate>> lastMove = fromFEN.isLastMovementWasPassage();
        final Pair<Coordinate, Coordinate> latestMovement = lastMove.isEmpty() ? null : lastMove.orElseThrow();
        final King whiteKing = theKing(WHITE);
        final King blackKing = theKing(BLACK);

        final Operations checkOrMateForWhite = whiteKing.kingStatus(this, WHITE, latestMovement);
        if (checkOrMateForWhite.equals(CHECKMATE) || !activeColor.equals(WHITE) && checkOrMateForWhite.equals(CHECK)) {
            throw new IllegalArgumentException("Invalid FEN. Checkmate position.");
        }

        final Operations checkOrMateForBlack = blackKing.kingStatus(this, BLACK, latestMovement);
        if (checkOrMateForBlack.equals(CHECKMATE) || !activeColor.equals(BLACK) && checkOrMateForBlack.equals(CHECK)) {
            throw new IllegalArgumentException("Invalid FEN. Checkmate position.");
        }

        final boolean stalemateForWhite = activeColor.equals(WHITE) && whiteKing.stalemate(this, WHITE, latestMovement);
        if (stalemateForWhite) {
            throw new IllegalArgumentException("Invalid FEN. Stalemate position.");
        }

        final boolean stalemateForBlack = activeColor.equals(BLACK) && blackKing.stalemate(this, BLACK, latestMovement);
        if (stalemateForBlack) {
            throw new IllegalArgumentException("Invalid FEN. Stalemate position.");
        }
    }

    private void validateAndForward(final List<AlgebraicNotation> listOfAlgebraicNotations) {
        for (final AlgebraicNotation algebraicNotation : listOfAlgebraicNotations) {
            final Pair<Coordinate, Coordinate> coordinates = coordinates(algebraicNotation);

            final Coordinate from = coordinates.getFirst();
            final Coordinate to = coordinates.getSecond();
            final Piece inCaseOfPromotion = getInCaseOfPromotion(algebraicNotation);

            GameResultMessage message;
            try {
                message = reposition(from, to, inCaseOfPromotion);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid PGN.");
            }

            final boolean isGameOver = message.equals(GameResultMessage.Checkmate) ||
                    message.equals(GameResultMessage.Stalemate) ||
                    message.equals(GameResultMessage.RuleOf50Moves) ||
                    message.equals(GameResultMessage.InsufficientMatingMaterial);

            if (isGameOver) {
                throw new IllegalArgumentException("Invalid PGN, You can`t start a game with ended position.");
            }
        }
    }

    Pair<Coordinate, Coordinate> coordinates(AlgebraicNotation algebraicNotation) {
        Pair<Coordinate, Coordinate> coordinates;

        final StatusPair<AlgebraicNotation.Castle> isCastling = AlgebraicNotation.isCastling(algebraicNotation);
        if (isCastling.status()) {
            coordinates = algebraicNotation.castlingCoordinates(isCastling.orElseThrow(), figuresTurn);
        } else {
            coordinates = algebraicNotation.coordinates();
        }
        return coordinates;
    }

    Piece getInCaseOfPromotion(AlgebraicNotation algebraicNotation) {
        StatusPair<AlgebraicNotation.PieceTYPE> promotion = algebraicNotation.promotionType();

        Piece inCaseOfPromotion = null;
        if (promotion.status()) {
            inCaseOfPromotion = AlgebraicNotation.fromSymbol(promotion.orElseThrow(), figuresTurn);
        }

        return inCaseOfPromotion;
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
        return kingColor.equals(WHITE) ?

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
        if (figuresTurn.equals(WHITE)) {
            figuresTurn = BLACK;
        } else {
            figuresTurn = WHITE;
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
        if (king.color().equals(WHITE)) {
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

        if (removedPiece.color().equals(WHITE)) {
            materialAdvantageOfWhite -= price;
        }

        if (removedPiece.color().equals(BLACK)) {
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

        if (promotionFigure.color().equals(WHITE)) {
            materialAdvantageOfWhite -= 1;
            materialAdvantageOfBlack += price;
        }

        if (promotionFigure.color().equals(BLACK)) {
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

            if (piece.color().equals(WHITE) && lastMoveEnd.getRow() - to.getRow() == -1) {
                return true;
            }

            return piece.color().equals(BLACK) && lastMoveEnd.getRow() - to.getRow() == 1;
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

        final boolean whiteColorFigure = color.equals(WHITE);
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

            return;
        }

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

        this.validWhiteShortCasting = aboutCastlingAbility.contains("K");
        this.validWhiteLongCasting = aboutCastlingAbility.contains("Q");
        this.validBlackShortCasting = aboutCastlingAbility.contains("k");
        this.validBlackLongCasting = aboutCastlingAbility.contains("q");
    }

    /**
     * Updates the count of moves for the 50-move rule based on the current piece and operations performed.
     * <p>
     * The 50-move rule states that a player can claim a draw if no pawn has been moved and no capture has been made
     * in the last 50 full moves. This method checks the current piece and the operations performed during the turn to
     * determine whether to increment the move counters.
     *
     * @param piece The piece that is currently being moved. This is used to check if the piece is a pawn.
     * @param operations A set of operations that were performed during the turn. This is used to check if a capture
     *                   occurred.
     * </p>
     */
    private void ruleOf50MovesAbility(final Piece piece, final Set<Operations> operations) {
        if (!operations.contains(CAPTURE) && !(piece instanceof Pawn)) {
            this.ruleOf50Moves++;
        }

        if (piece instanceof Pawn || operations.contains(CAPTURE)) {
            this.ruleOf50Moves = 0;
        }
    }

    /**
     * Retrieves the pair of coordinates representing a castling move for ONLY Undo move.
     *
     * @param castle The type of castling move (short or long).
     * @return A Pair of Coordinates representing the castling move.
     */
    private Pair<Coordinate, Coordinate> castlingCoordinatesForUndoMove(final AlgebraicNotation.Castle castle) {
        final boolean shortCastling = castle.equals(AlgebraicNotation.Castle.SHORT_CASTLING);
        if (shortCastling) {
            if (figuresTurn.equals(WHITE)) {
                return Pair.of(Coordinate.e8, Coordinate.g8);
            } else {
                return Pair.of(Coordinate.e1, Coordinate.g1);
            }
        }

        if (figuresTurn.equals(WHITE)) {
            return Pair.of(Coordinate.e8, Coordinate.c8);
        } else {
            return Pair.of(Coordinate.e1, Coordinate.c1);
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

            if (color.equals(WHITE)) {
                return validWhiteShortCasting;
            }

            if (color.equals(BLACK)) {
                return validBlackShortCasting;
            }
        }

        if (color.equals(WHITE)) {
            return validWhiteLongCasting;
        }

        return validBlackLongCasting;
    }

    /**
     * Generates a list of all valid moves for the current board state.
     * <p>
     * This method analyzes the positions of pieces and chess rules to return only legal moves,
     * excluding illegal ones such as moves that leave the king in check.
     *
     * @return a set of {@code PlayerMove} objects representing all valid moves;
     *         returns an empty list if no moves are available.
     */
    public Set<PlayerMove> generateValidMoves() {
        final ChessBoardNavigator navigator = new ChessBoardNavigator(this);
        final List<Field> fields = navigator.allFriendlyFields(figuresTurn);
        final Set<PlayerMove> validMoves = new TreeSet<>();

        for (final Field field : fields) {
            final Coordinate from = field.coordinate;
            final Piece piece = field.piece;

            switch (piece) {
                case Pawn p -> validMovesOfPawn(p, navigator, from, validMoves);
                case Bishop b -> validMovesOfBishop(b, navigator, from, validMoves);
                case Knight n -> validMovesOfKnight(n, navigator, from, validMoves);
                case Rook r -> validMovesOfRook(r, navigator, from, validMoves);
                case Queen q -> validMovesOfQueen(q, navigator, from, validMoves);
                case King k -> validMovesOfKing(k, navigator, from, validMoves);
            }
        }

        return validMoves;
    }

    /**
     * Calculates valid moves for a pawn.
     */
    private Set<PlayerMove> validMovesOfPawn(Pawn pawn, ChessBoardNavigator navigator, Coordinate from, Set<PlayerMove> validMoves) {
        List<Coordinate> potentialMoves = navigator.fieldsForPawnMovement(from, pawn.color());

        for (Coordinate to : potentialMoves) {
            StatusPair<Set<Operations>> isValidMove = pawn.isValidMove(this, from, to);
            if (isValidMove.status()) {
                if (isValidMove.orElseThrow().contains(PROMOTION)) {
                    validMoves.add(new PlayerMove(from, to, new Bishop(pawn.color())));
                    validMoves.add(new PlayerMove(from, to, new Knight(pawn.color())));
                    validMoves.add(new PlayerMove(from, to, new Rook(pawn.color())));
                    validMoves.add(new PlayerMove(from, to, new Queen(pawn.color())));
                    continue;
                }
                validMoves.add(new PlayerMove(from, to, null));
            }
        }
        return validMoves;
    }

    /**
     * Calculates valid moves for a bishop.
     */
    private Set<PlayerMove> validMovesOfBishop(Bishop bishop, ChessBoardNavigator navigator, Coordinate from, Set<PlayerMove> validMoves) {
        List<Field> potentialMoves = navigator.fieldsInDirections(Direction.diagonalDirections(), from);

        for (Field field : potentialMoves) {
            Coordinate to = field.coordinate;
            if (bishop.isValidMove(this, from, to).status()) {
                validMoves.add(new PlayerMove(from, to, null));
            }
        }
        return validMoves;
    }

    /**
     * Calculates valid moves for a knight.
     */
    private Set<PlayerMove> validMovesOfKnight(Knight knight, ChessBoardNavigator navigator, Coordinate from, Set<PlayerMove> validMoves) {
        List<Field> potentialMoves = navigator.knightAttackPositions(from, x -> true);

        for (Field field : potentialMoves) {
            Coordinate to = field.coordinate;
            if (knight.isValidMove(this, from, to).status()) {
                validMoves.add(new PlayerMove(from, to, null));
            }
        }
        return validMoves;
    }

    /**
     * Calculates valid moves for a rook.
     */
    private Set<PlayerMove> validMovesOfRook(Rook rook, ChessBoardNavigator navigator, Coordinate from, Set<PlayerMove> validMoves) {
        List<Field> potentialMoves = navigator.fieldsInDirections(Direction.horizontalVerticalDirections(), from);

        for (Field field : potentialMoves) {
            Coordinate to = field.coordinate;
            if (rook.isValidMove(this, from, to).status()) {
                validMoves.add(new PlayerMove(from, to, null));
            }
        }
        return validMoves;
    }

    /**
     * Calculates valid moves for a queen.
     */
    private Set<PlayerMove> validMovesOfQueen(Queen queen, ChessBoardNavigator navigator, Coordinate from, Set<PlayerMove> validMoves) {
        List<Field> potentialMoves = navigator.fieldsInDirections(Direction.allDirections(), from);

        for (Field field : potentialMoves) {
            Coordinate to = field.coordinate;
            if (queen.isValidMove(this, from, to).status()) {
                validMoves.add(new PlayerMove(from, to, null));
            }
        }
        return validMoves;
    }

    /**
     * Calculates valid moves for a king.
     */
    private Set<PlayerMove> validMovesOfKing(King king, ChessBoardNavigator navigator, Coordinate from,
                                             Set<PlayerMove> validMoves) {
        List<Field> potentialMoves = navigator.fieldsForKingMovement(from, king.color());

        for (Field field : potentialMoves) {
            Coordinate to = field.coordinate;
            if (isCastling(king, from, to)) {
                final boolean isValidCastling = ableToCastling(king.color(), AlgebraicNotation.castle(to)) && king.isValidMove(this, from, to).status();
                if (isValidCastling) {
                    validMoves.add(new PlayerMove(from, to, null));
                }

                continue;
            }

            if (king.isValidMove(this, from, to).status()) {
                validMoves.add(new PlayerMove(from, to, null));
            }
        }
        return validMoves;
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
            throw new IllegalArgumentException(String.format("At the moment, the player for %s must move and not the opponent", figuresTurn));
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
        this.countOfHalfMoves++;
        final Set<Operations> operations = statusPair.orElseThrow();

        startField.removeFigure();

        if (operations.contains(CAPTURE)) {
            inCaseOfCapture(to, piece, endField);
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
        final King opponentKing = theKing(piece.color().equals(WHITE) ? BLACK : WHITE);

        operations.add(
                opponentKing.kingStatus(this, opponentKing.color(), Pair.of(from, to))
        );

        final boolean isStalemate = countOfHalfMoves() + 1 >= 10 && opponentKing.stalemate(this, opponentKing.color(), Pair.of(from, to));
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
        final var inCaseOfPromotionPieceType = inCaseOfPromotion == null ? null : AlgebraicNotation.pieceToType(inCaseOfPromotion);
        listOfAlgebraicNotations.add(AlgebraicNotation.of(AlgebraicNotation.pieceToType(piece), operations, from, to, inCaseOfPromotionPieceType));

        final String currentPositionHash = this.toString();
        fenRepresentationsOfBoard.add(FEN(currentPositionHash));
        hashCodeOfBoard.put(currentPositionHash, hashCodeOfBoard.getOrDefault(currentPositionHash, 0) + 1);

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

        final boolean insufficientMatingMaterial = !isPureChess &&
                ((materialAdvantageOfWhite <= 3 && materialAdvantageOfBlack == 0 ||
                materialAdvantageOfWhite == 0 && materialAdvantageOfBlack <= 3) &&
                !isAtLeastOnePawnOnBoard());

        if (insufficientMatingMaterial) {
            return GameResultMessage.InsufficientMatingMaterial;
        }

        if (!isPureChess && ruleOf50Moves == 100) {
            return GameResultMessage.RuleOf50Moves;
        }

        if (!isPureChess && hashCodeOfBoard.get(currentPositionHash) == 3) {
            return GameResultMessage.RuleOf3EqualsPositions;
        }

        return GameResultMessage.Continue;
    }

    private void inCaseOfCapture(Coordinate to, Piece piece, Field endField) {
        final boolean captureOnPassage = isCaptureOnPassage(piece, to);
        if (captureOnPassage) {
            Field field = fieldMap.get(latestMovement().orElseThrow().getSecond());

            if (piece.color().equals(WHITE)) {
                capturedWhitePieces.add(field.piece);
            }

            if (piece.color().equals(BLACK)) {
                capturedBlackPieces.add(field.piece);
            }

            changeInMaterialAdvantage(field.piece);
            field.removeFigure();
            return;
        }

        if (piece.color().equals(WHITE)) {
            capturedWhitePieces.add(endField.piece);
        }

        if (piece.color().equals(BLACK)) {
            capturedBlackPieces.add(endField.piece);
        }

        changeInMaterialAdvantage(endField.piece);
        endField.removeFigure();
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
        this.countOfHalfMoves++;
        kingStartedField.removeFigure();
        kingEndField.addFigure(king);

        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(AlgebraicNotation.castle(to));
        if (shortCasting) {
            moveRookInShortCastling(to);
        } else {
            moveRookInLongCastling(to);
        }

        /** Check for Checkmate, Stalemate, Check after move executed...*/
        final King opponentKing = theKing(piece.color().equals(WHITE) ? BLACK : WHITE);

        operations.add(
                opponentKing.kingStatus(this, opponentKing.color(), Pair.of(from, to))
        );

        final boolean isStalemate = countOfHalfMoves() + 1 >= 10 && opponentKing.stalemate(this, opponentKing.color(), Pair.of(from, to));
        if (isStalemate) {
            operations.add(STALEMATE);
        }

        /** Monitor opportunities for castling and switch players.*/
        changedKingPosition(king, to);
        changeOfCastlingAbility(from, king);

        switchFiguresTurn();
        ruleOf50MovesAbility(piece, operations);

        /** Recording the move made in algebraic notation.*/
        listOfAlgebraicNotations.add(AlgebraicNotation.of(AlgebraicNotation.pieceToType(piece), operations, from, to, null));

        final String currentPositionHash = toString();
        fenRepresentationsOfBoard.add(FEN(currentPositionHash));
        hashCodeOfBoard.put(currentPositionHash, hashCodeOfBoard.getOrDefault(currentPositionHash, 0) + 1);

        /** Retrieve message about game result.*/
        final Operations opponentKingStatus = AlgebraicNotation.opponentKingStatus(operations);

        if (opponentKingStatus.equals(STALEMATE)) {
            return GameResultMessage.Stalemate;
        }

        if (opponentKingStatus.equals(CHECKMATE)) {
            return GameResultMessage.Checkmate;
        }

        if (!isPureChess && ruleOf50Moves == 100) {
            return GameResultMessage.RuleOf50Moves;
        }

        if (!isPureChess && hashCodeOfBoard.get(currentPositionHash) == 3) {
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

        this.countOfHalfMoves--;

        final String currentPositionHash = fenToHashCodeOfBoard(fenRepresentationsOfBoard.getLast());
        final AlgebraicNotation lastMovement = listOfAlgebraicNotations.getLast();
        final StatusPair<AlgebraicNotation.Castle> isCastling = AlgebraicNotation.isCastling(lastMovement);

        if (isCastling.status()) {
            revertCastling(isCastling.orElseThrow(), currentPositionHash);
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

        if (!isCapture && !(piece instanceof Pawn) && ruleOf50Moves != 0){
            this.ruleOf50Moves--;
        }

        fenRepresentationsOfBoard.removeLast();
        listOfAlgebraicNotations.removeLast();

        final int newValue = hashCodeOfBoard.get(currentPositionHash) - 1;
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
     * @param castle              the castling information
     * @param currentPositionHash represent current position of
     */
    private void revertCastling(final AlgebraicNotation.Castle castle, final String currentPositionHash) {
        final var movementPair = castlingCoordinatesForUndoMove(castle);
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

        this.ruleOf50Moves--;

        listOfAlgebraicNotations.removeLast();
        fenRepresentationsOfBoard.removeLast();

        final int newValue = hashCodeOfBoard.get(currentPositionHash) - 1;
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
        if (figuresTurn.equals(WHITE)) {
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

        if (piece.color().equals(WHITE)) {
            final boolean isValidRows = endRow - passageEnd.getRow() == 1;
            if (!isValidRows) {
                return false;
            }

            fieldMap.get(passageEnd).addFigure(new Pawn(BLACK));
            return true;
        }

        final boolean isValidRows = endRow - passageEnd.getRow() == -1;
        if (!isValidRows) {
            return false;
        }

        fieldMap.get(passageEnd).addFigure(new Pawn(WHITE));
        return true;
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
                Objects.equals(listOfAlgebraicNotations, that.listOfAlgebraicNotations);
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
                listOfAlgebraicNotations
        );
    }

    /**
     * Returns a FEN (Forsyth-Edwards Notation) chessboard presentation in a truncated format,
     * meaning that the FEN excludes the 50-move rule, the number of full moves found at the end of the FEN standard.
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
                    fen.append(countOfEmptyFields).append("/");
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
            } else if (field.isPresent()) {
                if (countOfEmptyFields != 0) {
                    fen.append(countOfEmptyFields).append(convertPieceToHash(field.pieceOptional().orElseThrow()));
                    countOfEmptyFields = 0;
                } else {
                    fen.append(convertPieceToHash(field.pieceOptional().orElseThrow()));
                }
            }
        }

        if (countOfEmptyFields != 0) {
            fen.append(countOfEmptyFields);
        }

        fen.append(" ");
        if (figuresTurn.equals(WHITE)) {
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
        if (validBlackShortCasting) {
            fen.append("k");
        }
        if (validBlackLongCasting) {
            fen.append("q");
        }

        if (!validWhiteShortCasting && !validWhiteLongCasting && !validBlackLongCasting && !validBlackShortCasting) {
            if (fen.charAt(fen.length() - 1) == ' ') {
                fen.append("- ");
            } else {
                fen.append(" - ");
            }
        }

        final var lastMovement = latestMovement();
        if (lastMovement.isEmpty()) {
            if (fen.charAt(fen.length() - 1) == ' ') {
                fen.append("- ");
            } else {
                fen.append(" - ");
            }
        }

        if (latestMovement().isPresent()) {
            final boolean previousMoveWasPassage = new Pawn(WHITE).previousMoveWasPassage(this);
            if (previousMoveWasPassage) {
                final Coordinate to = lastMovement.orElseThrow().getSecond();

                final Coordinate intermediateFieldOfPassage = Coordinate
                        .of(to.getRow() == 4 ? to.getRow() - 1 : to.getRow() + 1, to.columnToInt())
                        .orElseThrow();

                final String result = "" + intermediateFieldOfPassage.getColumn() + intermediateFieldOfPassage.getRow();
                final String whiteSpace = fen.charAt(fen.length() - 1) == ' ' ? "" : " ";
                fen.append(whiteSpace).append(result);
            } else {
                if (fen.charAt(fen.length() - 1) == ' ') {
                    fen.append("- ");
                } else {
                    fen.append(" - ");
                }
            }
        }

        return fen.toString();
    }

    private String convertPieceToHash(final Piece piece) {
        return switch (piece) {
            case King(Color color) -> color.equals(WHITE) ? "K" : "k";
            case Queen(Color color) -> color.equals(WHITE) ? "Q" : "q";
            case Rook(Color color) -> color.equals(WHITE) ? "R" : "r";
            case Bishop(Color color) -> color.equals(WHITE) ? "B" : "b";
            case Knight(Color color) -> color.equals(WHITE) ? "N" : "n";
            case Pawn(Color color) -> color.equals(WHITE) ? "P" : "p";
        };
    }

    private String fenToHashCodeOfBoard(final String fen) {
        return fen.transform(s -> {
            int i = 0;
            int limit = 0;
            for (char c : fen.toCharArray()) {
                i++;
                if (c == ' ') {
                    limit++;

                    if (limit == 4) {
                        break;
                    }
                }
            }
            String result = s.substring(0, i);
            return !Character.isDigit(result.charAt(result.length() - 2)) ? result : result.substring(0, result.length() - 1);
        });
    }

    /**
     * Initializes the standard chess board setup.
     * <p>
     * This method populates the {@code fieldMap} with the initial positions of the chess pieces on the board.
     * The white pieces are placed on the first and second rows, while the black pieces are placed on the seventh and eighth rows.
     * The remaining fields are left empty.
     */
    private void standardInitializer() {
        fieldMap.put(Coordinate.a1, new Field(Coordinate.a1, new Rook(WHITE)));
        fieldMap.put(Coordinate.b1, new Field(Coordinate.b1, new Knight(WHITE)));
        fieldMap.put(Coordinate.c1, new Field(Coordinate.c1, new Bishop(WHITE)));
        fieldMap.put(Coordinate.d1, new Field(Coordinate.d1, new Queen(WHITE)));
        fieldMap.put(Coordinate.e1, new Field(Coordinate.e1, new King(WHITE)));
        fieldMap.put(Coordinate.f1, new Field(Coordinate.f1, new Bishop(WHITE)));
        fieldMap.put(Coordinate.g1, new Field(Coordinate.g1, new Knight(WHITE)));
        fieldMap.put(Coordinate.h1, new Field(Coordinate.h1, new Rook(WHITE)));
        fieldMap.put(Coordinate.a8, new Field(Coordinate.a8, new Rook(BLACK)));
        fieldMap.put(Coordinate.b8, new Field(Coordinate.b8, new Knight(BLACK)));
        fieldMap.put(Coordinate.c8, new Field(Coordinate.c8, new Bishop(BLACK)));
        fieldMap.put(Coordinate.d8, new Field(Coordinate.d8, new Queen(BLACK)));
        fieldMap.put(Coordinate.e8, new Field(Coordinate.e8, new King(BLACK)));
        fieldMap.put(Coordinate.f8, new Field(Coordinate.f8, new Bishop(BLACK)));
        fieldMap.put(Coordinate.g8, new Field(Coordinate.g8, new Knight(BLACK)));
        fieldMap.put(Coordinate.h8, new Field(Coordinate.h8, new Rook(BLACK)));

        for (Coordinate coordinate : Coordinate.values()) {
            if (coordinate.getRow() == 1 || coordinate.getRow() == 8) {
                continue;
            }

            switch (coordinate.getRow()) {
                case 2 -> fieldMap.put(coordinate, new Field(coordinate, new Pawn(WHITE)));
                case 7 -> fieldMap.put(coordinate, new Field(coordinate, new Pawn(BLACK)));
                default -> fieldMap.put(coordinate, new Field(coordinate, null));
            }
        }
    }

    /**
     * Initializes the chess board setup from a given FEN (Forsyth-Edwards Notation) string.
     * <p>
     * This method populates the {@code fieldMap} with the positions of the chess pieces on the board as defined by the FEN notation.
     * The FEN string is expected to be in the standard format, with the following components:
     * <ul>
     *     <li> Piece placement (ranks 1-8)</li>
     *     <li> Active color (w or b)</li>
     *     <li> Castling availability (K, Q, k, q)</li>
     *     <li> En passant target square (e.g. e3)</li>
     *     <li> Halfmove clock (number of half-moves since the last capture or pawn move)</li>
     *     <li> Fullmove number (number of moves made by both players)</li>
     * </ul>
     * <p>
     * Note that this method only initializes the piece positions and does not update the game state or move history.
     *
     * @param fen The FEN notation string representing the chess board setup.
     */
    private void initializerFromFEN(String fen) {
        int pos = 0;
        Coordinate[] coordinates = Coordinate.values();

        for (char c : fen.toCharArray()) {
            if (c == '/') {
                continue;
            }

            if (c == ' ') {
                break;
            }

            Coordinate coordinate = coordinates[pos];

            if (Character.isLetter(c)) {
                Piece piece = AlgebraicNotation.fromSymbol(String.valueOf(c));
                fieldMap.put(coordinate, new Field(coordinate, piece));
                pos++;
                continue;
            }

            fieldMap.put(coordinate, new Field(coordinate, null));
            pos++;

            for (int i = 1; i < Character.getNumericValue(c); i++) {
                Coordinate nextCoordinate = coordinates[pos++];
                fieldMap.put(nextCoordinate, new Field(nextCoordinate, null));
            }
        }
    }
}
