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

public final class Knight implements Piece {
    private final Color color;
    private final int index;

    private static final Knight WHITE_KNIGHT = new Knight(Color.WHITE, 1);
    private static final Knight BLACK_KNIGHT = new Knight(Color.BLACK, 7);
    static final long[] KNIGHT_MOVES_CACHE = new long[64];
    static {
        for (int square = 0; square < 64; square++) KNIGHT_MOVES_CACHE[square] = generatePseudoLegalKnightMoves(square);
    }

    public static Knight of(Color color) {
        return color == Color.WHITE ? WHITE_KNIGHT : BLACK_KNIGHT;
    }

    private Knight(Color color, int index) {
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
        if (!knightMove(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);

        final Color opponentPieceColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField != null && endField.color() == opponentPieceColor;
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    boolean knightMove(final ChessBoard board, final Coordinate from, final Coordinate to) {
        long toMask = to.bitMask();
        final boolean isKnightPattern = (KNIGHT_MOVES_CACHE[from.ordinal()] & toMask) != 0;
        if (!isKnightPattern) return false;

        long ownPieces = board.pieces(color);
        final boolean targetOccupiedByOwn = (ownPieces & toMask) != 0;

        return !targetOccupiedByOwn;
    }

    public boolean isAtLeastOneMove(final ChessBoard chessBoard) {
        long knightBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (knightBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(knightBitboard);
            knightBitboard &= knightBitboard - 1;

            long moves = KNIGHT_MOVES_CACHE[fromIndex] & ~ownPieces;
            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byOrdinal(fromIndex);
                Coordinate to = Coordinate.byOrdinal(toIndex);

                if (chessBoard.safeForKing(from, to)) return true;
            }
        }

        return false;
    }

    public long knightAttacks(int square) {
        return generatePseudoLegalKnightMoves(square);
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard) {
        return allValidMoves(chessBoard, new ArrayList<>());
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard, final List<Move> validMoves) {
        long knightBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (knightBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(knightBitboard);
            knightBitboard &= knightBitboard - 1;

            long moves = KNIGHT_MOVES_CACHE[fromIndex] & ~ownPieces;
            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byOrdinal(fromIndex);
                Coordinate to = Coordinate.byOrdinal(toIndex);

                if (chessBoard.safeForKing(from, to)) validMoves.add(new Move(from, to, null));
            }
        }

        return validMoves;
    }

    private static long generatePseudoLegalKnightMoves(int square) {
        long moves = 0L;

        int row = square / 8;
        int col = square % 8;

        int[] dRows = {-2, -1, 1, 2, 2, 1, -1, -2};
        int[] dCols = {1, 2, 2, 1, -1, -2, -2, -1};

        for (int i = 0; i < 8; i++) {
            int newRow = row + dRows[i];
            int newCol = col + dCols[i];

            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                int newSquare = newRow * 8 + newCol;
                moves |= 1L << newSquare;
            }
        }

        return moves;
    }
}
