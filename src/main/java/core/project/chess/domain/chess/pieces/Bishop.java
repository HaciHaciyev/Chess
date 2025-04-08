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

public final class Bishop implements Piece {
    private final Color color;
    private final int index;

    private static final Bishop WHITE_BISHOP = new Bishop(Color.WHITE, 2);
    private static final Bishop BLACK_BISHOP = new Bishop(Color.BLACK, 8);
    static final long[] BISHOP_MOVES_CACHE = new long[64];
    static {
        for (int square = 0; square < 64; square++) BISHOP_MOVES_CACHE[square] = generatePseudoLegalBishopMoves(square);
    }

    public static Bishop of(Color color) {
        return color == Color.WHITE ? WHITE_BISHOP : BLACK_BISHOP;
    }

    private Bishop(Color color, int index) {
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
        if (!bishopMove(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);

        final Color opponentPieceColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField != null && endField.color() == opponentPieceColor;
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    boolean bishopMove(final ChessBoard chessBoard, final Coordinate startField, final Coordinate endField) {
        int startSquare = startField.ordinal();
        int endSquare = endField.ordinal();
        long validMoves = BISHOP_MOVES_CACHE[startSquare] & ~chessBoard.pieces(color);
        if ((validMoves & (1L << endSquare)) == 0) return false;
        return clearPath(chessBoard, startField, endField);
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard) {
        List<Move> validMoves = new ArrayList<>();

        long bishopBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (bishopBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(bishopBitboard);
            bishopBitboard &= bishopBitboard - 1;

            long moves = BISHOP_MOVES_CACHE[fromIndex] & ~ownPieces;
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

    private static long generatePseudoLegalBishopMoves(int square) {
        long moves = 0L;
        int row = square / 8;
        int col = square % 8;

        int[] rowOffsets = {-1, 1, 1, -1};
        int[] colOffsets = {1, -1, 1, -1};

        for (int i = 0; i < 4; i++) {
            int r = row + rowOffsets[i];
            int c = col + colOffsets[i];
            while (r >= 0 && r < 8 && c >= 0 && c < 8) {
                int newSquare = r * 8 + c;
                moves |= 1L << newSquare;
                r += rowOffsets[i];
                c += colOffsets[i];
            }
        }

        return moves;
    }
}
