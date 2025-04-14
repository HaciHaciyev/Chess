package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.value_objects.Move;
import core.project.chess.infrastructure.utilities.containers.StatusPair;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;
import static core.project.chess.domain.chess.enumerations.Color.BLACK;
import static core.project.chess.domain.chess.enumerations.Color.WHITE;

public final class Pawn implements Piece {
    private final Color color;
    private final int index;

    private static final Pawn WHITE_PAWN = new Pawn(WHITE, 0);
    private static final Pawn BLACK_PAWN = new Pawn(BLACK, 6);
    static final long[] WHITE_PAWN_MOVES_CACHE = new long[64];
    static final long[] BLACK_PAWN_MOVES_CACHE = new long[64];
    static {
        for (int square = 0; square < 64; square++) {
            WHITE_PAWN_MOVES_CACHE[square] = generatePseudoValidPawnMoves(square, Color.WHITE);
            BLACK_PAWN_MOVES_CACHE[square] = generatePseudoValidPawnMoves(square, Color.BLACK);
        }
    }

    public static Pawn of(Color color) {
        return color == WHITE ? WHITE_PAWN : BLACK_PAWN;
    }

    private Pawn(Color color, int index) {
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

    /**
     * Validates whether a move from the 'from' coordinate to the 'to' coordinate is a valid move for a Pawn on the chessboard.
     *
     * <p>
     * This method checks the following conditions:
     * <ul>
     *     <li>The move is not to the same field.</li>
     *     <li>The starting field contains a piece (specifically a Pawn).</li>
     *     <li>The target field is not occupied by a piece of the same color as the Pawn.</li>
     *     <li>The move is valid according to the Pawn's movement rules.</li>
     *     <li>The move does not place the player's king in check.</li>
     * </ul>
     * </p>
     *
     * <p>
     * Note: When calling this implementation for a Pawn, it does not validate for the promotion of the Pawn to another piece.
     * The <code>isValidMove()</code> method can account for the promotion occurring, but it cannot validate the specific piece to promote to.
     * For this, you should additionally call the function <code>isValidPromotion()</code>.
     * </p>
     *
     * @param chessBoard The chessboard on which the move is being validated. This object contains the current state of the board,
     *                   including the positions of all pieces.
     * @param from The starting coordinate of the Pawn's move. This coordinate represents the current position of the Pawn.
     * @param to   The target coordinate to which the Pawn is attempting to move. This coordinate represents the desired position.
     *
     * @return A {@link StatusPair} containing a set of status and a boolean status indicating whether the move is valid.
     *         If the move is valid, the status will be <code>true</code> and the set will contain the status related to the move.
     *         If the move is invalid, the status will be <code>false</code> and the set will be empty.
     *
     * @throws NullPointerException if any of the parameters (<code>chessBoard</code>, <code>from</code>, or <code>to</code>) are <code>null</code>.
     * @throws IllegalStateException if the method is called with a piece that is not a Pawn.
     */
    @Override
    public Set<Operations> isValidMove(final ChessBoard chessBoard, final Coordinate from, final Coordinate to) {
        final Set<Operations> setOfOperations = pawnMove(chessBoard, from, to);
        if (setOfOperations == null) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        return setOfOperations;
    }

    public boolean isValidPromotion(final Pawn pawnForPromotion, final Piece inCaseOfPromotion) {
        if (pawnForPromotion == null || inCaseOfPromotion == null) return false;
        if (inCaseOfPromotion instanceof King || inCaseOfPromotion instanceof Pawn) return false;
        return pawnForPromotion.color() == inCaseOfPromotion.color();
    }

    Set<Operations> pawnMove(ChessBoard chessBoard, Coordinate startField, Coordinate endField) {
        final int startColumn = startField.column();
        final int endColumn = endField.column();
        final int startRow = startField.row();
        final int endRow = endField.row();
        long targetBit = endField.bitMask();
        int fromIndex = startField.index();

        long pseudoMoves = (color == Color.WHITE ? WHITE_PAWN_MOVES_CACHE : BLACK_PAWN_MOVES_CACHE)[fromIndex];
        if ((pseudoMoves & targetBit) == 0) return null;

        long ownPieces = chessBoard.pieces(color);
        if ((targetBit & ownPieces) != 0) return null;

        long opponentPieces = color == WHITE ? chessBoard.blackPieces() : chessBoard.whitePieces();
        long occupied = ownPieces | opponentPieces;

        final boolean straightMove = endColumn == startColumn;
        if (straightMove) {
            if ((occupied & targetBit) != 0) return null;
            int delta = Math.abs(startRow - endRow);
            if (delta == 2) {
                int middleRow = (startRow + endRow) / 2;
                Coordinate middle = Coordinate.of(middleRow, startColumn);
                if (chessBoard.piece(middle) != null) return null;
            }
            Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);
            if (endRow == 8 || endRow == 1) setOfOperations.add(Operations.PROMOTION);
            return setOfOperations;
        }

        final boolean capture = (targetBit & opponentPieces) != 0 || endField == chessBoard.enPassant();
        if (!capture) return null;
        Set<Operations> setOfOperations = EnumSet.of(Operations.CAPTURE);
        if (endRow == 8 || endRow == 1) setOfOperations.add(Operations.PROMOTION);
        return setOfOperations;
    }

    public boolean isAtLeastOneMove(final ChessBoard chessBoard) {
        long pawnBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (pawnBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(pawnBitboard);
            pawnBitboard &= pawnBitboard - 1;

            long moves = color == WHITE ?
                    WHITE_PAWN_MOVES_CACHE[fromIndex] & ~ownPieces :
                    BLACK_PAWN_MOVES_CACHE[fromIndex] & ~ownPieces;

            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byIndex(fromIndex);
                Coordinate to = Coordinate.byIndex(toIndex);

                final int startColumn = from.column();
                final int endColumn = to.column();
                final int startRow = from.row();
                final int endRow = to.row();
                long targetBit = to.bitMask();

                if ((targetBit & ownPieces) != 0) continue;
                long opponentPieces = color == WHITE ? chessBoard.blackPieces() : chessBoard.whitePieces();
                long occupied = ownPieces | opponentPieces;

                final boolean straightMove = endColumn == startColumn;
                if (straightMove) {
                    if ((occupied & targetBit) != 0) continue;
                    int delta = Math.abs(startRow - endRow);
                    if (delta == 2) {
                        int middleRow = (startRow + endRow) / 2;
                        Coordinate middle = Coordinate.of(middleRow, startColumn);
                        if (chessBoard.piece(middle) != null) continue;
                    }
                    return chessBoard.safeForKing(from, to);
                }

                final boolean capture = (targetBit & opponentPieces) != 0 || to == chessBoard.enPassant();
                if (!capture) continue;
                return chessBoard.safeForKing(from, to);
            }
        }

        return false;
    }

    public long pawnsAttacks(int square) {
        long attacks = 0L;
        int row = square / 8;
        int col = square % 8;

        if (color == WHITE) {
            if (row < 7) {
                if (col > 0) attacks |= 1L << ((row + 1) * 8 + (col - 1));
                if (col < 7) attacks |= 1L << ((row + 1) * 8 + (col + 1));
            }
            return attacks;
        }

        if (row > 0) {
            if (col > 0) attacks |= 1L << ((row - 1) * 8 + (col - 1));
            if (col < 7) attacks |= 1L << ((row - 1) * 8 + (col + 1));
        }
        return attacks;
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard) {
        return allValidMoves(chessBoard, new ArrayList<>());
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard, final List<Move> validMoves) {
        long pawnBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (pawnBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(pawnBitboard);
            pawnBitboard &= pawnBitboard - 1;

            long moves = color == WHITE ?
                    WHITE_PAWN_MOVES_CACHE[fromIndex] & ~ownPieces :
                    BLACK_PAWN_MOVES_CACHE[fromIndex] & ~ownPieces;

            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byIndex(fromIndex);
                Coordinate to = Coordinate.byIndex(toIndex);

                final int startColumn = from.column();
                final int endColumn = to.column();
                final int startRow = from.row();
                final int endRow = to.row();
                long targetBit = to.bitMask();

                if ((targetBit & ownPieces) != 0) continue;
                long opponentPieces = color == WHITE ? chessBoard.blackPieces() : chessBoard.whitePieces();
                long occupied = ownPieces | opponentPieces;

                final boolean straightMove = endColumn == startColumn;
                if (straightMove) {
                    if ((occupied & targetBit) != 0) continue;
                    int delta = Math.abs(startRow - endRow);
                    if (delta == 2) {
                        int middleRow = (startRow + endRow) / 2;
                        Coordinate middle = Coordinate.of(middleRow, startColumn);
                        if (chessBoard.piece(middle) != null) continue;
                    }
                    addMove(chessBoard, from, to, validMoves);
                }

                final boolean capture = (targetBit & opponentPieces) != 0 || to == chessBoard.enPassant();
                if (!capture) continue;
                addMove(chessBoard, from, to, validMoves);
            }
        }

        return validMoves;
    }

    private void addMove(ChessBoard chessBoard, Coordinate from, Coordinate to, List<Move> validMoves) {
        if (chessBoard.safeForKing(from, to)) {
            if (to.row() == 1 || to.row() == 8) {
                validMoves.add(new Move(from, to, Queen.of(color)));
                validMoves.add(new Move(from, to, Rook.of(color)));
                validMoves.add(new Move(from, to, Bishop.of(color)));
                validMoves.add(new Move(from, to, Knight.of(color)));
            }
            else validMoves.add(new Move(from, to, null));
        }
    }

    private static long generatePseudoValidPawnMoves(int square, Color color) {
        long moves = 0L;
        int direction = (color == Color.WHITE) ? -1 : 1;
        int row = square / 8;
        int col = square % 8;
        if (isValidSquare(row + direction, col)) moves |= (1L << (square + direction * 8));
        if (color == Color.WHITE && row == 6 && isValidSquare(row + direction * 2, col)) moves |= (1L << (square + direction * 16));
        if (color == Color.BLACK && row == 1 && isValidSquare(row + direction * 2, col)) moves |= (1L << (square + direction * 16));
        if (isValidSquare(row + direction, col - 1)) moves |= (1L << (square + direction * 8 - 1));
        if (isValidSquare(row + direction, col + 1)) moves |= (1L << (square + direction * 8 + 1));
        return moves;
    }

    private static boolean isValidSquare(int row, int col) {
        return row >= 0 && row < 8 && col >= 0 && col < 8;
    }
}
