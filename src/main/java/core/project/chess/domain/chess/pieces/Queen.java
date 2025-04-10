package core.project.chess.domain.chess.pieces;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.value_objects.Move;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static core.project.chess.domain.chess.entities.ChessBoard.Operations;

public final class Queen implements Piece {
    private final Color color;
    private final int index;

    private static final Queen WHITE_QUEEN = new Queen(Color.WHITE, 4);
    private static final Queen BLACK_QUEEN = new Queen(Color.BLACK, 10);
    static final long[] QUEEN_MOVES_CACHE = new long[64];
    static {
        for (int square = 0; square < 64; square++) QUEEN_MOVES_CACHE[square] = generatePseudoValidQueenMoves(square);
    }

    public static Queen of(Color color) {
        return color == Color.WHITE ? WHITE_QUEEN : BLACK_QUEEN;
    }

    private Queen(Color color, int index) {
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
        Piece endField = chessBoard.piece(to);
        if (!queenMove(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);

        final Color opponentPieceColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField != null && endField.color().equals(opponentPieceColor);
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    boolean queenMove(final ChessBoard chessBoard, final Coordinate startField, final Coordinate endField) {
        int startSquare = startField.ordinal();
        int endSquare = endField.ordinal();
        long validMoves = QUEEN_MOVES_CACHE[startSquare] & ~chessBoard.pieces(color);
        if ((validMoves & (1L << endSquare)) == 0) return false;
        return clearPath(chessBoard, startField, endField);
    }

    public boolean isAtLeastOneMove(final ChessBoard chessBoard) {
        long queenBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (queenBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(queenBitboard);
            queenBitboard &= queenBitboard - 1;

            long moves = QUEEN_MOVES_CACHE[fromIndex] & ~ownPieces;
            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byOrdinal(fromIndex);
                Coordinate to = Coordinate.byOrdinal(toIndex);
                if (clearPath(chessBoard, from, to) && chessBoard.safeForKing(from, to)) return true;
            }
        }

        return false;
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard) {
        return allValidMoves(chessBoard, new ArrayList<>());
    }

    private List<Move> allValidMoves(final ChessBoard chessBoard, List<Move> validMoves) {
        long queenBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (queenBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(queenBitboard);
            queenBitboard &= queenBitboard - 1;

            long moves = QUEEN_MOVES_CACHE[fromIndex] & ~ownPieces;
            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byOrdinal(fromIndex);
                Coordinate to = Coordinate.byOrdinal(toIndex);

                if (clearPath(chessBoard, from, to) &&
                        chessBoard.safeForKing(from, to)) validMoves.add(new Move(from, to, null));
            }
        }

        return validMoves;
    }

    private static long generatePseudoValidQueenMoves(int square) {
        long moves = 0L;
        int row = square / 8;
        int col = square % 8;

        int[] rowOffsets = {-1, 1, 1, -1};
        int[] colOffsets = {1, -1, 1, -1};

        for (int i = 0; i < 4; i++) {                                     // all diagonals
            int r = row + rowOffsets[i];
            int c = col + colOffsets[i];
            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                int newSquare = r * 8 + c;
                moves |= 1L << newSquare;
                r += rowOffsets[i];
                c += colOffsets[i];
            }
        }

        for (int i = col - 1; i >= 0; i--) moves |= 1L << (row * 8 + i);  // left
        for (int i = col + 1; i < 8; i++) moves |= 1L << (row * 8 + i);   // right

        for (int i = row - 1; i >= 0; i--) moves |= 1L << (i * 8 + col);  // bottom
        for (int i = row + 1; i < 8; i++) moves |= 1L << (i * 8 + col);   // top
        return moves;
    }
}
