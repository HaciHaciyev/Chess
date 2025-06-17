package core.project.chess.domain.chess.entities;

import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.enumerations.GameResultMessage;
import core.project.chess.domain.chess.pieces.*;
import core.project.chess.domain.chess.util.ChessNotationsValidator;
import core.project.chess.domain.chess.util.ZobristHashKeys;
import core.project.chess.domain.chess.value_objects.*;
import core.project.chess.domain.commons.annotations.Nullable;
import core.project.chess.domain.commons.containers.StatusPair;
import core.project.chess.domain.commons.tuples.Pair;

import java.util.*;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations.*;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;
import static core.project.chess.domain.chess.value_objects.AlgebraicNotation.pieceToType;
import static java.util.Objects.nonNull;

/**
 * The `ChessBoard` class represents the central entity of the Chess Aggregate. It encapsulates the state and behavior of a chess board,
 * serving as the entry point for all chess-related status within the domain.
 * <p>
 * The `ChessBoard` is responsible for managing the placement and movement of chess pieces, enforcing the rules of the game,
 * and tracking the history of moves made on the board. It provides a well-defined API for interacting with the chess board,
 * ensuring that all status are performed in a consistent and valid manner.
 * <p>
 * The `ChessBoard` is the root entity of the Chess Aggregate, meaning that it owns and is responsible for the lifecycle of all
 * other entities and value objects within the aggregate, such as `Piece`, `Coordinate`, and `AlgebraicNotation`. This ensures
 * that the aggregate remains in a valid and consistent state at all times.
 * <p>
 * The `ChessBoard` class encapsulates the following key responsibilities:
 * <p>
 * 1. **Piece Placement and Movement**: The `doMove()` method allows for the movement of pieces on the board, handling
 *    various status such as capturing, promotion, and castling, while ensuring the validity of each move,
 *    also allow to revert last made move by using 'undoMove()'.
 * <p>
 * 2. **Castling Management**: The `castling()` method handles the specific logic for castling moves, including the movement of the rook
 *    also allow to revert last made move by using 'revertCastling()'.
 * <p>
 * 3. **Move History Tracking**: The `algebraicNotations` property and associated methods allow for the recording and retrieval
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
 * @version 5.0
 */
public class ChessBoard {
    private final UUID chessBoardId;
    private final InitType initType;

    private Color figuresTurn;

    private byte countOfHalfMoves;
    private byte countOfFullMoves;

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

    private Coordinate whiteKingPosition;
    private Coordinate blackKingPosition;

    /** Zobrist Hashing*/
    private final ZobristHashKeys zobrist;

    /**
     * Bitboard representing chess board by using 64 bits integers
     */
    private final long[] bitboard;
    private long whitePieces;
    private long blackPieces;
    private final Piece[] occupation;

    private static final Piece[] defaultOccupation = new Piece[64];
    static {
        defaultOccupation[Coordinate.a1.index()] = Rook.of(WHITE);
        defaultOccupation[Coordinate.b1.index()] = Knight.of(WHITE);
        defaultOccupation[Coordinate.c1.index()] = Bishop.of(WHITE);
        defaultOccupation[Coordinate.d1.index()] = Queen.of(WHITE);
        defaultOccupation[Coordinate.e1.index()] = King.of(WHITE);
        defaultOccupation[Coordinate.f1.index()] = Bishop.of(WHITE);
        defaultOccupation[Coordinate.g1.index()] = Knight.of(WHITE);
        defaultOccupation[Coordinate.h1.index()] = Rook.of(WHITE);

        defaultOccupation[Coordinate.a8.index()] = Rook.of(BLACK);
        defaultOccupation[Coordinate.b8.index()] = Knight.of(BLACK);
        defaultOccupation[Coordinate.c8.index()] = Bishop.of(BLACK);
        defaultOccupation[Coordinate.d8.index()] = Queen.of(BLACK);
        defaultOccupation[Coordinate.e8.index()] = King.of(BLACK);
        defaultOccupation[Coordinate.f8.index()] = Bishop.of(BLACK);
        defaultOccupation[Coordinate.g8.index()] = Knight.of(BLACK);
        defaultOccupation[Coordinate.h8.index()] = Rook.of(BLACK);

        for (Coordinate coordinate : Coordinate.coordinates()) {
            if (coordinate.row() != 2 && coordinate.row() != 7) continue;
            if (coordinate.row() == 2) {
                defaultOccupation[coordinate.index()] = Pawn.of(WHITE);
                continue;
            }
            defaultOccupation[coordinate.index()] = Pawn.of(BLACK);
        }
    }

    /**
     * Hash codes representing unique board positions for repetition detection.
     * Using Zobrist Hashing.
     * Necessary for counting identical positions on the board. Especially for ThreeFold rule.
     * Unnecessary in 'pureChess' mode, where ThreeFold rule is disabled.
     */
    @Nullable
    private final Map<Long, Integer> zobristHash;

    /**
     * Zobrist hash history
     */
    private final Deque<Long> zobristHashKeys = new ArrayDeque<>();

    /**
     * Stack of moves recorded in algebraic notation for game replay and analysis.
     */
    private final Deque<AlgebraicNotation> algebraicNotations = new ArrayDeque<>();

    /**
     * King status
     */
    private final Deque<KingStatus> kingStatuses = new ArrayDeque<>();

    /**
     * History of castling abilities for every position
     */
    private final Deque<CastlingAbility> castlingAbilities = new ArrayDeque<>();

    /**
     * EnPassant History
     */
    private final Deque<Coordinate> enPassantStack = new LinkedList<>();

    /**
     * Half moves clock (for rule of 50 moves)
     */
    private final Deque<Integer> ruleOf50Moves = new ArrayDeque<>();

    /** History of captured pieces for every color*/
    private final Deque<Piece> capturedWhitePieces = new ArrayDeque<>();
    private final Deque<Piece> capturedBlackPieces = new ArrayDeque<>();

    /**
     * Constructs a new `ChessBoard` instance with the given parameters.
     *
     * @param chessBoardId        The unique identifier of the chess board.
     * @param maybeFEN The data for initialization of chess board from FEN.
     * @param isPureChess         The data which disables the following chess rules:
     *      - insufficient mating material;
     *      - three-fold rule;
     *      - fifty moves rule.
     */
    private ChessBoard(final UUID chessBoardId, @Nullable final FromFEN maybeFEN,
                       final boolean isPureChess, @Nullable final List<AlgebraicNotation> algebraicNotations) {

        this.chessBoardId = chessBoardId;
        this.isPureChess = isPureChess;
        this.ruleOf50Moves.add(0);
        this.countOfHalfMoves = 0;
        this.countOfFullMoves = 1;

        // if FEN is null initialize chess board with default position
        if (maybeFEN == null) {
            this.figuresTurn = WHITE;
            this.whiteKingPosition = Coordinate.e1;
            this.blackKingPosition = Coordinate.e8;

            this.materialAdvantageOfWhite = 39;
            this.materialAdvantageOfBlack = 39;

            this.validWhiteShortCasting = true;
            this.validWhiteLongCasting = true;
            this.validBlackShortCasting = true;
            this.validBlackLongCasting = true;
            addCastlingAbility();

            this.bitboard = new long[12];
            this.occupation = defaultOccupation.clone();
            initialize();
            this.zobrist = new ZobristHashKeys();
            long key = zobrist.computeZobristHash(this);
            this.zobristHashKeys.add(key);
            if (!isPureChess) {
                this.zobristHash = new HashMap<>();
                this.zobristHash.put(key, 1);
            } else {
                this.zobristHash = null;
            }

            if (nonNull(algebraicNotations)) {
                validateNotations(algebraicNotations);
                this.initType = InitType.PGN;
            } else {
                this.initType = InitType.DEFAULT;
            }

            return;
        }

        String FEN = maybeFEN.fen();

        this.initType = InitType.FEN;
        this.enPassantStack.addLast(getEnPassant(maybeFEN));

        this.figuresTurn = maybeFEN.figuresTurn();
        this.whiteKingPosition = maybeFEN.whiteKing();
        this.blackKingPosition = maybeFEN.blackKing();

        this.materialAdvantageOfWhite = maybeFEN.materialAdvantageOfWhite();
        this.materialAdvantageOfBlack = maybeFEN.materialAdvantageOfBlack();

        this.validWhiteShortCasting = maybeFEN.validWhiteShortCasting();
        this.validWhiteLongCasting = maybeFEN.validWhiteLongCasting();
        this.validBlackShortCasting = maybeFEN.validBlackShortCasting();
        this.validBlackLongCasting = maybeFEN.validBlackLongCasting();
        
        addCastlingAbility();

        this.bitboard = new long[12];
        this.occupation = new Piece[64];
        initializeFromFEN(FEN);
        validateStalemateAndCheckmate(maybeFEN);

        this.zobrist = new ZobristHashKeys();
        long key = zobrist.computeZobristHash(this);
        this.zobristHashKeys.add(key);
        if (!isPureChess) {
            this.zobristHash = new HashMap<>();
            this.zobristHash.put(key, 1);
        }
        else this.zobristHash = null;
    }

    /**
    * helper method which is called when constructing Board from FEN
    */
    private Coordinate getEnPassant(FromFEN maybeFEN) {
        Optional<Pair<Coordinate, Coordinate>> lastMovement = maybeFEN.isLastMovementWasPassage();
        if (lastMovement.isEmpty()) {
            return null;
        }

        Coordinate endCoordinate = lastMovement.get().getSecond();
        int enPassauntRow = endCoordinate.row() == 4 ? 3 : 6;
        return Coordinate.of(enPassauntRow, endCoordinate.column());
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
            throw new IllegalArgumentException("Invalid FEN: " + fen);
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
            throw new IllegalArgumentException("Invalid FEN: " + fen);
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
            throw new IllegalArgumentException("PGN is empty");
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
            throw new IllegalArgumentException("PGN is empty");
        }

        return new ChessBoard(UUID.randomUUID(), null, true, listOfAlgebraicNotations);
    }

    public UUID ID() {
        return chessBoardId;
    }

    public int countOfFullMoves() {
        return countOfFullMoves;
    }

    public boolean isPureChess() {
        return isPureChess;
    }

    public Coordinate enPassant() {
        return enPassantStack.peekLast();
    }

    public Color turn() {
        return figuresTurn;
    }

    public long whitePieces() {
        return whitePieces;
    }

    public long blackPieces() {
        return blackPieces;
    }

    public long allPiecesOf(final Color color) {
        return color == WHITE ? whitePieces : blackPieces;
    }

    public long bitboard(final Piece piece) {
        return bitboard[piece.index()];
    }

    public int ruleOf50MovesCount() {
        return this.ruleOf50Moves.peekLast();
    }

    public int castlingRights() {
        int rights = 0;
        if (validWhiteShortCasting) rights |= 1;
        if (validWhiteLongCasting) rights |= (1 << 1);
        if (validBlackShortCasting) rights |= (1 << 2);
        if (validBlackLongCasting) rights |= (1 << 3);
        return rights;
    }

    public int enPassantFile() {
        Coordinate coordinate = enPassantStack.peekLast();
        if (coordinate != null) {
            return coordinate.column() - 1;
        }
        return -1;
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
    public Coordinate whiteKingPosition() {
        return whiteKingPosition;
    }

    /**
     * Returns the current position of the black king on the chess board.
     *
     * @return a new Coordinate object representing the current position of the white king
     */
    public Coordinate blackKingPosition() {
        return blackKingPosition;
    }

    public Coordinate kingCoordinate(Color color) {
        return color == WHITE ? whiteKingPosition : blackKingPosition;
    }

    /**
     * Retrieves a list of algebraic notations representing the moves made on the chess board.
     *
     * @return algebraic notations in type of AlgebraicNotation.
     */
    public List<String> listOfAlgebraicNotations() {
        return algebraicNotations.stream().map(AlgebraicNotation::algebraicNotation).toList();
    }

    public AlgebraicNotation[] arrayOfAlgebraicNotations() {
        return this.algebraicNotations.toArray(new AlgebraicNotation[0]);
    }

    /**
     * Retrieves the last algebraic notation representing the moves made on the chess board.
     *
     * @return An algebraic notation in type of String.
     */
    public Optional<AlgebraicNotation> lastAlgebraicNotation() {
        if (algebraicNotations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(algebraicNotations.peekLast());
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

    public long zobristHash() {
        return zobristHashKeys.peekLast();
    }

    @Nullable
    public KingStatus kingStatus() {
        return kingStatuses.peekLast();
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
        return !isPureChess && zobristHash.get(zobristHash()) == 3;
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
        final AlgebraicNotation[] arrayOfAlgebraicNotations = algebraicNotations.toArray(new AlgebraicNotation[0]);

        int number = 1;
        for (int i = 0; i < arrayOfAlgebraicNotations.length; i += 2) {
            final String notation = arrayOfAlgebraicNotations[i].algebraicNotation();

            final String secondNotation;
            if (i + 1 <= arrayOfAlgebraicNotations.length - 1) {
                secondNotation = arrayOfAlgebraicNotations[i + 1].algebraicNotation();
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
     * Checks if the move from the specified 'from' coordinate to the 'to' coordinate is safe for the king.
     *
     * @param from  The coordinate the piece is moving from.
     * @param to    The coordinate the piece is moving to.
     * @return True if the move is safe for the king, false otherwise.
     */
    public boolean safeForKing(final Coordinate from, final Coordinate to) {
        Piece piece = piece(from);
        Color kingColor = piece.color();
        King king = theKing(kingColor);
        return king.safeForKing(this, from, to);
    }

    /**
     * Validates the stalemate and checkmate conditions.
     *
     * @param fromFEN The FEN representation of the chess position.
     */
    private void validateStalemateAndCheckmate(FromFEN fromFEN) {
        final Color activeColor = fromFEN.figuresTurn();
        final King whiteKing = theKing(WHITE);
        final King blackKing = theKing(BLACK);

        final KingStatus checkOrMateForWhite = whiteKing
                .kingStatus(this, null, null);
        if (checkOrMateForWhite.status().equals(CHECKMATE) ||
                !activeColor.equals(WHITE) && checkOrMateForWhite.status().equals(CHECK)) {
            throw new IllegalArgumentException("Invalid FEN. Checkmate position: " + fromFEN.fen());
        }

        final KingStatus checkOrMateForBlack = blackKing
                .kingStatus(this, null, null);
        if (checkOrMateForBlack.status().equals(CHECKMATE) ||
                !activeColor.equals(BLACK) && checkOrMateForBlack.status().equals(CHECK)) {
            throw new IllegalArgumentException("Invalid FEN. Checkmate position: " + fromFEN.fen());
        }

        final boolean stalemateForWhite = activeColor.equals(WHITE) &&
                whiteKing.stalemate(this, null, null);
        if (stalemateForWhite) {
            throw new IllegalArgumentException("Invalid FEN. Stalemate position: " + fromFEN.fen());
        }

        final boolean stalemateForBlack = activeColor.equals(BLACK) &&
                blackKing.stalemate(this, null, null);
        if (stalemateForBlack) {
            throw new IllegalArgumentException("Invalid FEN. Stalemate position: " + fromFEN.fen());
        }
    }

    /** 
    * helper method for validating PGN when Board is initialized from FEN
    *<p>
    * called in constructor
    */
    private void validateNotations(final List<AlgebraicNotation> listOfAlgebraicNotations) {
        for (final AlgebraicNotation algebraicNotation : listOfAlgebraicNotations) {
            final Pair<Coordinate, Coordinate> coordinates = extractCoordinates(algebraicNotation);

            final Coordinate from = coordinates.getFirst();
            final Coordinate to = coordinates.getSecond();
            final Piece inCaseOfPromotion = getInCaseOfPromotion(algebraicNotation);

            GameResultMessage message;
            try {
                message = doMove(from, to, inCaseOfPromotion);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(String.format("Invalid PGN: %s", e.getMessage()));
            }

            final boolean isGameOver = message.equals(GameResultMessage.Checkmate) ||
                    message.equals(GameResultMessage.Stalemate) ||
                    message.equals(GameResultMessage.RuleOf50Moves) ||
                    message.equals(GameResultMessage.InsufficientMatingMaterial);

            if (isGameOver) {
                throw new IllegalArgumentException("Invalid PGN, You can`t start a game from ended position.");
            }
        }
    }

    Pair<Coordinate, Coordinate> extractCoordinates(AlgebraicNotation algebraicNotation) {
        final AlgebraicNotation.Castle isCastling = AlgebraicNotation.isCastling(algebraicNotation);
        if (isCastling != null) return algebraicNotation.castlingCoordinates(isCastling, figuresTurn);
        return algebraicNotation.coordinates();
    }

    Piece getInCaseOfPromotion(AlgebraicNotation algebraicNotation) {
        AlgebraicNotation.PieceTYPE promotion = algebraicNotation.promotionType();
        Piece inCaseOfPromotion = null;
        if (promotion != null) inCaseOfPromotion = AlgebraicNotation.fromSymbol(promotion, figuresTurn);
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
        if (kingColor == WHITE) return (King) piece(whiteKingPosition);
        return (King) piece(blackKingPosition);
    }

    private void switchFiguresTurn() {
        if (figuresTurn == WHITE) figuresTurn = BLACK;
        else figuresTurn = WHITE;
    }

    /**
    * This is not a complete validation of which player should play at this point.
    * This validation rather checks what color pieces should be moved.
    * Finally, validation of the question of who should walk can only be carried out in the controller.
    */
    private boolean validateFiguresTurnAndPieceExistence(final Coordinate coordinate, final Coordinate to) {
        Piece piece = piece(coordinate);
        return piece.color() == figuresTurn;
    }

    /**
     * Updates the position of the king on the chess board.
     *
     * @param king        The king piece that has moved.
     * @param coordinate  The new coordinate of the king.
     */
    private void changeKingPosition(final King king, final Coordinate coordinate) {
        if (king.color() == WHITE) this.whiteKingPosition = coordinate;
        else this.blackKingPosition = coordinate;
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

        if (removedPiece.color() == WHITE) materialAdvantageOfWhite -= price;
        else materialAdvantageOfBlack -= price;
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
            case King k -> throw new UnsupportedOperationException("Can't perform such operation on a King");
        };
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
    private boolean changeOfCastlingAbility(final Coordinate from, final Coordinate to, final Piece piece) {
        if (piece instanceof King) {
            if (piece.color() == WHITE) {
                validWhiteShortCasting = false;
                validWhiteLongCasting = false;
            } else {
                validBlackShortCasting = false;
                validBlackLongCasting = false;
            }
        }

        if (piece instanceof Rook) {
            if (piece.color() == WHITE) {
                if (from == Coordinate.a1) validWhiteLongCasting = false;
                if (from == Coordinate.h1) validWhiteShortCasting = false;
            } else {
                if (from == Coordinate.a8) validBlackLongCasting = false;
                if (from == Coordinate.h8) validBlackShortCasting = false;
            }
        }

        switch (to) {
            case a1 -> validWhiteLongCasting = false;
            case h1 -> validWhiteShortCasting = false;
            case a8 -> validBlackLongCasting = false;
            case h8 -> validBlackShortCasting = false;
        }

        CastlingAbility previous = castlingAbilities.peekLast();
        addCastlingAbility();
        return previous != castlingAbilities.peekLast();
    }

    /**
     * This method is responsible for adding a new castling ability to the stack.
     */
    private void addCastlingAbility() {
        castlingAbilities.addLast(new CastlingAbility(
                validWhiteShortCasting,
                validWhiteLongCasting,
                validBlackShortCasting,
                validBlackLongCasting
        ));
    }

    /**
     * This method is responsible for updating the castling ability of a piece during a revert move.
     *
     * @param piece  The piece being moved.
     * @throws IllegalStateException if the provided piece is not a King or Rook.
     */
    private void changeOfCastlingAbilityInRevertMove(final Piece piece) {
        castlingAbilities.pollLast();
        CastlingAbility castlingAbility = castlingAbilities.peekLast();
        this.validWhiteShortCasting = castlingAbility.whiteShortCastling();
        this.validWhiteLongCasting = castlingAbility.whiteLongCastling();
        this.validBlackShortCasting = castlingAbility.blackShortCastling();
        this.validBlackLongCasting = castlingAbility.blackLongCastling();
    }

    /**
    * update enPassaunt stack
    */
    private void changeOfEnPassaunt(final Coordinate from, final Coordinate to, final Piece piece) {
        if (piece instanceof Pawn && from.column() == to.column() && Math.abs(from.row() - to.row()) == 2) {
            int enPassauntRow = to.row() == 4 ? 3 : 6;
            this.enPassantStack.addLast(Coordinate.of(enPassauntRow, to.column()));
        } else {
            this.enPassantStack.addLast(null);
        }
    }

    private void updateZobristHash(final Piece piece, Coordinate from, final Coordinate to, final Piece inCaseOfPromotion,
                                   final Pair<Piece, Coordinate> capturedAt, final boolean isCastlingAbilityChanged) {
        Piece endedPiece = inCaseOfPromotion == null ? piece : inCaseOfPromotion;
        int castlingRights = isCastlingAbilityChanged ? -1 : castlingRights();

        if (nonNull(capturedAt)) {
            long newZobristHash = this.zobrist.updateHash(
                zobristHash(),
                piece, from, endedPiece, to,
                capturedAt.getFirst(), capturedAt.getSecond(),
                castlingRights, enPassantFile()
            );
            zobristHashKeys.addLast(newZobristHash);
            if (isPureChess) return;
            zobristHash.put(newZobristHash, zobristHash.getOrDefault(newZobristHash, 0) + 1);
            return;
        }

        long newZobristHash = this.zobrist.updateHash(
            zobristHash(),
            piece, from, endedPiece, to,
            castlingRights(), enPassantFile()
        );
        zobristHashKeys.addLast(newZobristHash);
        if (isPureChess) return;
        zobristHash.put(newZobristHash, zobristHash.getOrDefault(newZobristHash, 0) + 1);
    }

    private void updateZobristHashForCastling(final AlgebraicNotation.Castle castle, final Color color) {
        long newZobristHash = this.zobrist.updateHashForCastling(zobristHash(), color, castle, castlingRights());
        zobristHashKeys.addLast(newZobristHash);
        if (!isPureChess) zobristHash.put(newZobristHash, zobristHash.getOrDefault(newZobristHash, 0) + 1);
    }

    /**
     * Updates the count of moves for the 50-move rule based on the current piece and status performed.
     * <p>
     * The 50-move rule states that a player can claim a draw if no pawn has been moved and no capture has been made
     * in the last 50 full moves. This method checks the current piece and the status performed during the turn to
     * determine whether to increment the move counters.
     *
     * @param piece The piece that is currently being moved. This is used to check if the piece is a pawn.
     * @param operations A set of status that were performed during the turn. This is used to check if a capture
     *                   occurred.
     * </p>
     */
    private void ruleOf50MovesAbility(final Piece piece, final Set<Operations> operations) {
        if (!operations.contains(CAPTURE) && !(piece instanceof Pawn)) {
            Integer lastRuleOf50Moves = this.ruleOf50Moves.peekLast();
            ruleOf50Moves.add(lastRuleOf50Moves + 1);
        }
        else this.ruleOf50Moves.add(0);
    }

    public boolean isInsufficientMatingMaterial() {
        return !isPureChess && ((materialAdvantageOfWhite <= 3 && materialAdvantageOfBlack == 0 ||
                materialAdvantageOfWhite == 0 && materialAdvantageOfBlack <= 3) &&
                !isAtLeastOnePawnOnBoard());
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
            if (figuresTurn.equals(WHITE)) return Pair.of(Coordinate.e8, Coordinate.g8);
            else return Pair.of(Coordinate.e1, Coordinate.g1);
        }

        if (figuresTurn.equals(WHITE)) return Pair.of(Coordinate.e8, Coordinate.c8);
        else return Pair.of(Coordinate.e1, Coordinate.c1);
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
        final boolean king = piece instanceof King;
        if (!king) return false;

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
    private boolean isAtLeastOnePawnOnBoard() {
        long whitePawnsBitboard = bitboard(Pawn.of(WHITE));
        if (whitePawnsBitboard != 0) return true;

        long blackPawnsBitboard = bitboard(Pawn.of(BLACK));
        return blackPawnsBitboard != 0;
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
        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(castle);
        if (shortCasting) {
            if (color.equals(WHITE)) return validWhiteShortCasting;
            if (color.equals(BLACK)) return validBlackShortCasting;
        }

        if (color.equals(WHITE)) return validWhiteLongCasting;
        return validBlackLongCasting;
    }

    /**
     * Initializes the chess board with the starting positions of all pieces.
     */
    private void initialize() {
        whitePieces = 65535L;
        bitboard[Pawn.of(WHITE).index()] = 65280L;
        bitboard[Knight.of(WHITE).index()] = 66L;
        bitboard[Bishop.of(WHITE).index()] = 36L;
        bitboard[Rook.of(WHITE).index()] = 129L;
        bitboard[Queen.of(WHITE).index()] = 8L;
        bitboard[King.of(WHITE).index()] = 16L;

        blackPieces = -281474976710656L;
        bitboard[Pawn.of(BLACK).index()] = 71776119061217280L;
        bitboard[Knight.of(BLACK).index()] = 4755801206503243776L;
        bitboard[Bishop.of(BLACK).index()] = 2594073385365405696L;
        bitboard[Rook.of(BLACK).index()] = -9151314442816847872L;
        bitboard[Queen.of(BLACK).index()] = 576460752303423488L;
        bitboard[King.of(BLACK).index()] = 1152921504606846976L;
    }

    /**
     * Initializes the chess board from a Forsyth-Edwards Notation (FEN) string.
     */
    private void initializeFromFEN(String fen) {
        int pos = 0;
        Coordinate[] coordinates = Coordinate.values();

        for (char c : fen.toCharArray()) {
            if (c == '/') continue;
            if (c == ' ') break;

            Coordinate coordinate = coordinates[pos];

            if (Character.isLetter(c)) {
                Piece piece = AlgebraicNotation.fromSymbol(String.valueOf(c));
                
                bitboard[piece.index()] |= coordinate.bitMask(); // add piece to bitboard
                occupation[coordinate.index()] = piece; // add piece to occupation array
                
                if (piece.color() == WHITE) whitePieces |= coordinate.bitMask();
                if (piece.color() == BLACK) blackPieces |= coordinate.bitMask();
                
                pos++;
                continue;
            }

            pos++;
            for (int i = 1; i < Character.getNumericValue(c); i++) pos++;
        }
    }

    /**
     * Retrieves the `Piece` object at the specified coordinate on the chess board.
     *
     * @param coordinate The coordinate of the field where piece is placed to retrieve.
     * @return A `Piece` object or null if piece not exists on field.
     */
    @Nullable
    public Piece piece(Coordinate coordinate) {
        return occupation[coordinate.index()];
    }

    private void addFigure(Coordinate coordinate, Piece piece) {
        long squareMask = coordinate.bitMask();
        bitboard[piece.index()] |= squareMask;
        occupation[coordinate.index()] = piece;
        if (piece.color() == WHITE) whitePieces |= squareMask;
        else blackPieces |= squareMask;
    }

    private Piece removeFigure(Coordinate coordinate, Piece piece) {
        long squareMask = coordinate.bitMask();
        bitboard[piece.index()] &= ~squareMask;
        Piece removedPiece = occupation[coordinate.index()];
        occupation[coordinate.index()] = null;
        if (piece.color() == WHITE) whitePieces &= ~squareMask;
        else blackPieces &= ~squareMask;
        return removedPiece;
    }

    public List<Move> generateAllValidMoves() {
        List<Move> allValidMoves = new ArrayList<>(40);
        Pawn.of(figuresTurn).allValidMoves(this, allValidMoves);
        Knight.of(figuresTurn).allValidMoves(this, allValidMoves);
        Bishop.of(figuresTurn).allValidMoves(this, allValidMoves);
        Rook.of(figuresTurn).allValidMoves(this, allValidMoves);
        Queen.of(figuresTurn).allValidMoves(this, allValidMoves);
        King.of(figuresTurn).allValidMoves(this, allValidMoves);
        return allValidMoves;
    }

    public GameResultMessage doMove(final Coordinate from, final Coordinate to) {
        return doMove(from, to, null);
    }

    /**
     * Processes a piece movement on the chess board.
     *
     * @param from                  The coordinate the piece is moving from.
     * @param to                    The coordinate the piece is moving to.
     * @param inCaseOfPromotion     The piece to promote to in case of a pawn promotion, or null if no promotion.
     * @return The status performed during the repositioning.
     * @throws IllegalArgumentException If the move is invalid.
     */
    public final GameResultMessage doMove(final Coordinate from, final Coordinate to, final @Nullable Piece inCaseOfPromotion) {
        /** Preparation of necessary data and validation.*/
        if (!validateFiguresTurnAndPieceExistence(from, to)) {
            throw new IllegalArgumentException(String
                    .format("At the moment, the player for %s must move, not the opponent", figuresTurn));
        }

        if (from.equals(to)) {
            throw new IllegalArgumentException("Invalid move. The source and destination coordinates cannot be the same");
        }

        final Piece startField = piece(from);
        final Piece endField = piece(to);

        /** Delegate the status to another method if necessary.*/
        if (isCastling(startField, from, to)) return castling(from, to);

        /** Validation.*/
        final Set<Operations> operations = startField.isValidMove(this, from, to);
        if (operations == null) {
            throw new IllegalArgumentException(String
                    .format("Invalid move. From:%s. To:%s. Failed validation for %s movement.", from, to, startField));
        }

        if (operations.contains(PROMOTION)) {
            Pawn pawn = (Pawn) startField;
            final boolean isValidPieceForPromotion = pawn.isValidPromotion(pawn, inCaseOfPromotion);
            if (!isValidPieceForPromotion) {
                throw new IllegalArgumentException("Mismatch in color of figures for pawn promotion. Failed validation.");
            }
        }

        /** Process status from StatusPair. All validation need to be processed before that.*/
        this.countOfHalfMoves++;
        removeFigure(from, startField);
        Pair<Piece, Coordinate> capturedAt = null;
        if (operations.contains(CAPTURE)) capturedAt = inCaseOfCapture(startField, to, endField);
        if (operations.contains(PROMOTION)) {
            changeInMaterialAdvantageInCaseOfPromotion(inCaseOfPromotion);
            addFigure(to, inCaseOfPromotion);
        } else {
            addFigure(to, startField);
        }

        /** Check for Checkmate, Stalemate, Check after move executed...*/
        final King opponentKing = theKing(startField.color() == WHITE ? BLACK : WHITE);
        Move lastMove = new Move(from, to, null);

        final boolean isCheckPossible = countOfHalfMoves() >= 3 || initType == InitType.FEN;

        KingStatus opponentKingStatusAndEnemies = isCheckPossible ?
                opponentKing.kingStatus(this, lastMove, null) :
                new KingStatus(CONTINUE, Collections.emptyList());

        kingStatuses.addLast(opponentKingStatusAndEnemies);
        Operations opponentKingStatus = opponentKingStatusAndEnemies.status();
        operations.add(opponentKingStatus);

        final boolean isRequiredTOCheckStalemate = countOfHalfMoves() >= 19 || initType == InitType.FEN;

        final boolean isStalemate = isRequiredTOCheckStalemate &&
                opponentKingStatus == CONTINUE &&
                opponentKing.stalemate(this, lastMove, null);

        if (isStalemate) operations.add(STALEMATE);

        /** Monitor opportunities for castling, switch players.*/
        if (startField instanceof King king) changeKingPosition(king, to);
        final boolean isCastlingChanged = changeOfCastlingAbility(from, to, startField);
        if (figuresTurn == BLACK) countOfFullMoves++;
        switchFiguresTurn();
        ruleOf50MovesAbility(startField, operations);
        changeOfEnPassaunt(from, to, startField);

        /** Recording the move made in algebraic notation and Zobrist hashing.*/
        final var inCaseOfPromotionPT = inCaseOfPromotion == null ? null : pieceToType(inCaseOfPromotion);
        algebraicNotations.add(AlgebraicNotation.of(pieceToType(startField), operations, from, to, inCaseOfPromotionPT));
        updateZobristHash(startField, from, to, inCaseOfPromotion, capturedAt, isCastlingChanged);

        /** Retrieve message about game result.*/
        if (isStalemate) return GameResultMessage.Stalemate;
        if (opponentKingStatus.equals(CHECKMATE)) return GameResultMessage.Checkmate;
        if (opponentKingStatus.equals(CHECK)) return GameResultMessage.Continue;
        if (isInsufficientMatingMaterial()) return GameResultMessage.InsufficientMatingMaterial;
        if (!isPureChess && ruleOf50Moves.peekLast() == 100) return GameResultMessage.RuleOf50Moves;
        if (isThreeFoldActive()) return GameResultMessage.RuleOf3EqualsPositions;
        return GameResultMessage.Continue;
    }

    private Pair<Piece, Coordinate> inCaseOfCapture(Piece startField, Coordinate to, Piece endField) {
        final boolean captureOnPassage = enPassantStack.peekLast() != null &&
                to == enPassantStack.peekLast() &&
                startField instanceof Pawn;

        if (captureOnPassage) {
            int row = to.row() == 6 ? 5 : 4;
            Coordinate fieldCoordinate = Coordinate.of(row, to.column());
            Piece field = piece(fieldCoordinate);

            final boolean isCapturedPieceWhite = field.color() == WHITE;
            if (isCapturedPieceWhite) capturedWhitePieces.add(field);

            final boolean isCapturedPieceBlack = field.color() == BLACK;
            if (isCapturedPieceBlack) capturedBlackPieces.add(field);

            changeInMaterialAdvantage(field);
            Piece capturedPiece = removeFigure(fieldCoordinate, field);
            return Pair.of(capturedPiece, fieldCoordinate);
        }

        final boolean isCapturedPieceWhite = endField.color() == WHITE;
        if (isCapturedPieceWhite) capturedWhitePieces.add(endField);

        final boolean isCapturedPieceBlack = endField.color() == BLACK;
        if (isCapturedPieceBlack) capturedBlackPieces.add(endField);

        changeInMaterialAdvantage(endField);
        Piece capturedPiece = removeFigure(to, endField);
        return Pair.of(capturedPiece, to);
    }

    /**
     * Processes a castling move on the chess board.
     *
     * @param from The coordinate the king is moving from.
     * @param to   The coordinate the king is moving to.
     * @return The status performed during the castling.
     * @throws IllegalArgumentException If the castling move is invalid.
     * @throws IllegalStateException    If the method is used incorrectly.
     */
    private GameResultMessage castling(final Coordinate from, final Coordinate to) {
        /** Preparation of necessary data and validation.*/
        final King king = (King) piece(from);

        final Color color = king.color();
        AlgebraicNotation.Castle castle = AlgebraicNotation.castle(to);
        if (!ableToCastling(color, castle)) {
            throw new IllegalArgumentException(
                    "Invalid move. One or both of the pieces to be castled have made moves, castling is not possible."
            );
        }

        final Set<Operations> operations = king.isValidMove(this, from, to);
        if (operations == null) {
            throw new IllegalArgumentException("Invalid move. Failed validation.");
        }

        /**Process status from StatusPair. All validation need to be processed before that.*/
        this.countOfHalfMoves++;
        removeFigure(from, king);
        addFigure(to, king);
        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING == castle;
        if (shortCasting) moveRookInShortCastling(to);
        else moveRookInLongCastling(to);

        /** Check for Checkmate, Stalemate, Check after move executed...*/
        final King opponentKing = theKing(king.color() == WHITE ? BLACK : WHITE);
        Move lastMove = new Move(from, to, null);

        final boolean isCheckPossible = countOfHalfMoves() + 1 >= 3 || initType == InitType.FEN;

        KingStatus opponentKingStatusAndEnemies = isCheckPossible ?
                opponentKing.kingStatus(this, lastMove, castle) :
                new KingStatus(CONTINUE, Collections.emptyList());

        kingStatuses.addLast(opponentKingStatusAndEnemies);
        Operations opponentKingStatus = opponentKingStatusAndEnemies.status();
        operations.add(opponentKingStatus);

        final boolean isRequiredTOCheckStalemate = countOfHalfMoves() >= 19 || initType == InitType.FEN;

        final boolean isStalemate = isRequiredTOCheckStalemate &&
                opponentKingStatus == CONTINUE &&
                opponentKing.stalemate(this, lastMove, castle);

        if (isStalemate) operations.add(STALEMATE);

        /** Monitor opportunities for castling, enPassaunt, king position, fifty rules ability, and switch players.*/
        changeKingPosition(king, to);
        changeOfCastlingAbility(from, to, king);
        enPassantStack.addLast(null);
        if (figuresTurn == BLACK) countOfFullMoves++;
        switchFiguresTurn();
        Integer lastRuleOf50Moves = this.ruleOf50Moves.peekLast();
        ruleOf50Moves.add(lastRuleOf50Moves + 1);

        /** Recording the move made in algebraic notation and Zobrist hashing.*/
        algebraicNotations.add(AlgebraicNotation.castlingOf(castle, operations));
        updateZobristHashForCastling(castle, king.color());

        /** Retrieve message about move result.*/
        if (isStalemate) return GameResultMessage.Stalemate;
        if (opponentKingStatus.equals(CHECKMATE)) return GameResultMessage.Checkmate;
        if (!isPureChess && ruleOf50Moves.peekLast() == 100) return GameResultMessage.RuleOf50Moves;
        if (isThreeFoldActive()) return GameResultMessage.RuleOf3EqualsPositions;
        return GameResultMessage.Continue;
    }

    /**
     * Processes the movement of the rook during a short castling.
     *
     * @param to The coordinate the king is moving to during the castling.
     */
    private void moveRookInShortCastling(final Coordinate to) {
        final boolean isWhiteCastling = to.row() == 1;

        if (isWhiteCastling) {
            final Piece rook = piece(Coordinate.WHITE_ROOK_SHORT_CASTLE_START);
            removeFigure(Coordinate.WHITE_ROOK_SHORT_CASTLE_START, rook);
            addFigure(Coordinate.WHITE_ROOK_SHORT_CASTLE_END, rook);
            return;
        }

        final Piece rook = piece(Coordinate.BLACK_ROOK_SHORT_CASTLE_START);
        removeFigure(Coordinate.BLACK_ROOK_SHORT_CASTLE_START, rook);
        addFigure(Coordinate.BLACK_ROOK_SHORT_CASTLE_END, rook);
    }

    /**
     * Processes the movement of the rook during a long castling.
     *
     * @param to The coordinate the king is moving to during the castling.
     */
    private void moveRookInLongCastling(final Coordinate to) {
        final boolean isWhiteCastling = to.row() == 1;

        if (isWhiteCastling) {
            final Piece rook = piece(Coordinate.WHITE_ROOK_LONG_CASTLE_START);
            removeFigure(Coordinate.WHITE_ROOK_LONG_CASTLE_START, rook);
            addFigure(Coordinate.WHITE_ROOK_LONG_CASTLE_END, rook);
            return;
        }

        final Piece rook = piece(Coordinate.BLACK_ROOK_LONG_CASTLE_START);
        removeFigure(Coordinate.BLACK_ROOK_LONG_CASTLE_START, rook);
        addFigure(Coordinate.BLACK_ROOK_LONG_CASTLE_END, rook);
    }

    /**
     * Reverts the last move made in the game.
     *
     * @return `true` if the last move was successfully reverted, `false` otherwise.
     */
    public final boolean undoMove() {
        if (algebraicNotations.isEmpty()) return false;

        final AlgebraicNotation lastMovement = algebraicNotations.removeLast();
        final AlgebraicNotation.Castle isCastling = AlgebraicNotation.isCastling(lastMovement);

        if (isCastling != null) {
            revertCastling(isCastling);
            return true;
        }

        final Pair<Coordinate, Coordinate> movementPair = lastMovement.coordinates();
        final Coordinate from = movementPair.getFirst();
        final Coordinate to = movementPair.getSecond();

        final Piece endField = piece(to);
        final Piece pieceForUndo = getPieceForUndo(endField, movementPair, lastMovement);

        removeFigure(to, endField);
        addFigure(from, pieceForUndo);

        final boolean isCapture = lastMovement.isCapture();
        if (isCapture) revertCapture(to, pieceForUndo);

        this.countOfHalfMoves--;
        this.ruleOf50Moves.pollLast();
        if (pieceForUndo instanceof King king) changeKingPosition(king, from);
        changeOfCastlingAbilityInRevertMove(pieceForUndo);
        if (countOfFullMoves != 1 && figuresTurn == WHITE) countOfFullMoves--;
        enPassantStack.pollLast();
        zobristHashKeys.removeLast();
        kingStatuses.removeLast();
        switchFiguresTurn();
        return true;
    }

    private static Piece getPieceForUndo(Piece endedField, Pair<Coordinate, Coordinate> lastMove, AlgebraicNotation notation) {
        if (notation.isPromotion()) {
            boolean wasBlackPromotion = lastMove.getSecond().row() == 1;
            Color color = wasBlackPromotion ? BLACK : WHITE;
            return Pawn.of(color);
        }

        return endedField;
    }

    /**
     * Reverts a castling move.
     *
     * @param castle              the castling information
     */
    private void revertCastling(final AlgebraicNotation.Castle castle) {
        final var movementPair = castlingCoordinatesForUndoMove(castle);
        final Coordinate from = movementPair.getFirst();
        final Coordinate to = movementPair.getSecond();

        final Piece kingEndedField = piece(to);

        removeFigure(to, kingEndedField);
        addFigure(from, kingEndedField);

        final boolean shortCasting = AlgebraicNotation.Castle.SHORT_CASTLING.equals(castle);
        if (shortCasting) revertRookInShortCastling(to);
        else revertRookInLongCastling(to);

        this.ruleOf50Moves.pollLast();
        this.countOfHalfMoves--;
        enPassantStack.removeLast();
        changeKingPosition((King) kingEndedField, from);
        changeOfCastlingAbilityInRevertMove(kingEndedField);
        if (countOfFullMoves != 1 && figuresTurn == WHITE) countOfFullMoves--;
        zobristHashKeys.removeLast();
        kingStatuses.removeLast();
        switchFiguresTurn();
    }

    /**
     * Reverts the rook's move in a short castling.
     *
     * @param to the coordinate where the rook ended up after the castling
     */
    private void revertRookInShortCastling(Coordinate to) {
        final boolean isWhiteCastling = to.row() == 1;

        if (isWhiteCastling) {
            final Piece endField = piece(Coordinate.WHITE_ROOK_SHORT_CASTLE_END);
            removeFigure(Coordinate.WHITE_ROOK_SHORT_CASTLE_END, endField);
            addFigure(Coordinate.WHITE_ROOK_SHORT_CASTLE_START, endField);
            return;
        }

        final Piece endField = piece(Coordinate.BLACK_ROOK_SHORT_CASTLE_END);
        removeFigure(Coordinate.BLACK_ROOK_SHORT_CASTLE_END, endField);
        addFigure(Coordinate.BLACK_ROOK_SHORT_CASTLE_START, endField);
    }

    /**
     * Reverts the rook's move in a long castling.
     *
     * @param to the coordinate where the rook ended up after the castling
     */
    private void revertRookInLongCastling(Coordinate to) {
        final boolean isWhiteCastling = to.row() == 1;

        if (isWhiteCastling) {
            final Piece endField = piece(Coordinate.WHITE_ROOK_LONG_CASTLE_END);
            removeFigure(Coordinate.WHITE_ROOK_LONG_CASTLE_END, endField);
            addFigure(Coordinate.WHITE_ROOK_LONG_CASTLE_START, endField);
            return;
        }

        final Piece endField = piece(Coordinate.BLACK_ROOK_LONG_CASTLE_END);
        removeFigure(Coordinate.BLACK_ROOK_LONG_CASTLE_END, endField);
        addFigure(Coordinate.BLACK_ROOK_LONG_CASTLE_START, endField);
    }

    private void revertCapture(final Coordinate endFieldCoordinate, final Piece piece) {
        if (revertPotentialCaptureOnPassage(endFieldCoordinate, piece)) return;

        if (figuresTurn.equals(BLACK)) {
            Piece capturedPiece = capturedBlackPieces.removeLast();
            addFigure(endFieldCoordinate, capturedPiece);
            materialAdvantageOfBlack += materialAdvantageOfFigure(capturedPiece);
            return;
        }

        Piece capturedPiece = capturedWhitePieces.removeLast();
        addFigure(endFieldCoordinate, capturedPiece);
        materialAdvantageOfWhite += materialAdvantageOfFigure(capturedPiece);
    }

    private boolean revertPotentialCaptureOnPassage(final Coordinate endFieldCoordinate, final Piece piece) {
        if (!(piece instanceof Pawn)) return false;
        if (countOfHalfMoves < 5 && initType != InitType.FEN) return false;
        if (enPassantStack.size() < 2) return false;

        Coordinate tempEnPassaunt_currentState = enPassantStack.removeLast();
        Coordinate penultimateEnPassaunt = enPassantStack.peekLast();

        if (penultimateEnPassaunt == null) {
            enPassantStack.addLast(tempEnPassaunt_currentState);
            return false;
        }
        if (tempEnPassaunt_currentState != null) {
            enPassantStack.addLast(tempEnPassaunt_currentState);
            return false;
        }
        if (penultimateEnPassaunt != endFieldCoordinate) {
            enPassantStack.addLast(tempEnPassaunt_currentState);
            return false;
        }
        enPassantStack.addLast(tempEnPassaunt_currentState);

        int requiredRow = penultimateEnPassaunt.row() == 6 ? 5 : 4;
        Coordinate required = Coordinate.of(requiredRow, penultimateEnPassaunt.column());

        if (piece.color() == WHITE) {
            Piece capturedPawn = capturedBlackPieces.removeLast();
            addFigure(required, capturedPawn);
            materialAdvantageOfBlack++;
            return true;
        }

        Piece capturedPawn = capturedWhitePieces.removeLast();
        addFigure(required, capturedPawn);
        materialAdvantageOfWhite++;
        return true;
    }

    /**
     * Represents the different status that can be performed during a chess move,
     * such as capture, promotion, check, checkmate, and stalemate or empty if status not exists.
     */
    public enum Operations {
        PROMOTION("=", new byte[]{61}),     // '='
        CAPTURE("x", new byte[]{120}),      // 'x'
        CHECK("+", new byte[]{43}),         // '+'
        STALEMATE(".", new byte[]{46}),     // '.'
        CHECKMATE("#", new byte[]{35}),     // '#'
        CONTINUE("", new byte[]{});        // no operation

        private final String algebraicNotation;

        private final byte[] bytes;

        Operations(String algebraicNotation, byte[] bytes) {
            this.algebraicNotation = algebraicNotation;
            this.bytes = bytes;
        }

        public String getAlgebraicNotation() {
            return algebraicNotation;
        }

        public byte bytes() {
            return bytes[0];
        }
    }

    public enum InitType {
        DEFAULT,
        FEN,
        PGN
    }

    /**
     * Returns a FEN (Forsyth-Edwards Notation) chessboard presentation.
     */
    @Override
    public final String toString() {
        var fen = new StringBuilder();

        int row = 8;
        int countOfEmptyFields = 0;
        for (final Coordinate coordinate : Coordinate.values()) {
            if (coordinate.row() == row - 1) {
                row -= 1;
                if (countOfEmptyFields == 0) fen.append("/");
                else {
                    fen.append(countOfEmptyFields).append("/");
                    countOfEmptyFields = 0;
                }
            }

            final Piece field = piece(coordinate);
            if (field == null) countOfEmptyFields++;

            if (countOfEmptyFields == 8) {
                fen.append(countOfEmptyFields);
                countOfEmptyFields = 0;
            } else if (field != null) {
                if (countOfEmptyFields != 0) {
                    fen.append(countOfEmptyFields).append(convertPieceToChar(field));
                    countOfEmptyFields = 0;
                } else fen.append(convertPieceToChar(field));
            }
        }

        if (countOfEmptyFields != 0) fen.append(countOfEmptyFields);
        fen.append(" ");
        if (figuresTurn.equals(WHITE)) fen.append("w");
        else fen.append("b");
        fen.append(" ");
        if (validWhiteShortCasting) fen.append("K");
        if (validWhiteLongCasting) fen.append("Q");
        if (validBlackShortCasting) fen.append("k");
        if (validBlackLongCasting) fen.append("q");
        if (!validWhiteShortCasting && !validWhiteLongCasting && !validBlackLongCasting && !validBlackShortCasting) {
            if (fen.charAt(fen.length() - 1) == ' ') fen.append("- ");
            else fen.append(" - ");
        }
        if (fen.charAt(fen.length() - 1) != ' ') fen.append(" ");
        if (this.enPassantStack.peekLast() != null) fen.append(enPassantStack.getLast());
        else fen.append("- ");
        if (fen.charAt(fen.length() - 1) != ' ') fen.append(' ')
                .append(this.ruleOf50Moves.peekLast())
                .append(' ')
                .append(this.countOfFullMoves());
        else fen.append(this.ruleOf50Moves.peekLast()).append(' ').append(this.countOfFullMoves());
        return fen.toString();
    }

    private String convertPieceToChar(final Piece piece) {
        return switch (piece) {
            case King king -> king.color() == WHITE ? "K" : "k";
            case Queen queen -> queen.color() == WHITE ? "Q" : "q";
            case Rook rook -> rook.color() == WHITE ? "R" : "r";
            case Bishop bishop -> bishop.color() == WHITE ? "B" : "b";
            case Knight knight -> knight.color() == WHITE ? "N" : "n";
            case Pawn pawn -> pawn.color() == WHITE ? "P" : "p";
        };
    }
}
