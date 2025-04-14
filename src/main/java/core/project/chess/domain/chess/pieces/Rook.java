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

public final class Rook implements Piece {
    private final Color color;
    private final int index;

    private static final Rook WHITE_ROOK = new Rook(Color.WHITE, 3);
    private static final Rook BLACK_ROOK = new Rook(Color.BLACK, 9);
    static final long[] ROOK_MOVES_CACHE = new long[64];
    static {
        for (int square = 0; square < 64; square++) ROOK_MOVES_CACHE[square] = generatePseudoValidRookMoves(square);
    }

    public static Rook of(Color color) {
        return color == Color.WHITE ? WHITE_ROOK : BLACK_ROOK;
    }

    private Rook(Color color, int index) {
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
        if (!rookMove(chessBoard, from, to)) return null;
        if (!chessBoard.safeForKing(from, to)) return null;

        Set<Operations> setOfOperations = EnumSet.noneOf(Operations.class);

        final Color opponentPieceColor = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        final boolean opponentPieceInEndField = endField != null && endField.color().equals(opponentPieceColor);
        if (opponentPieceInEndField) setOfOperations.add(Operations.CAPTURE);

        return setOfOperations;
    }

    boolean rookMove(final ChessBoard chessBoard, final Coordinate startField, final Coordinate endField) {
        int startSquare = startField.index();
        int endSquare = endField.index();
        long validMoves = ROOK_MOVES_CACHE[startSquare] & ~chessBoard.pieces(color);
        if ((validMoves & (1L << endSquare)) == 0) return false;
        return clearPath(chessBoard, startField, endField);
    }

    public boolean isAtLeastOneMove(final ChessBoard chessBoard) {
        long rookBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (rookBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(rookBitboard);
            rookBitboard &= rookBitboard - 1;

            long moves = ROOK_MOVES_CACHE[fromIndex] & ~ownPieces;
            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byIndex(fromIndex);
                Coordinate to = Coordinate.byIndex(toIndex);
                if (clearPath(chessBoard, from, to) && chessBoard.safeForKing(from, to)) return true;
            }
        }

        return false;
    }

    public long rookAttacks(int square) {
        return generatePseudoValidRookMoves(square);
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard) {
        return allValidMoves(chessBoard, new ArrayList<>());
    }

    public List<Move> allValidMoves(final ChessBoard chessBoard, final List<Move> validMoves) {
        long rookBitboard = chessBoard.bitboard(this);
        long ownPieces = chessBoard.pieces(color);

        while (rookBitboard != 0) {
            int fromIndex = Long.numberOfTrailingZeros(rookBitboard);
            rookBitboard &= rookBitboard - 1;

            long moves = ROOK_MOVES_CACHE[fromIndex] & ~ownPieces;
            while (moves != 0) {
                int toIndex = Long.numberOfTrailingZeros(moves);
                moves &= moves - 1;

                Coordinate from = Coordinate.byIndex(fromIndex);
                Coordinate to = Coordinate.byIndex(toIndex);

                if (clearPath(chessBoard, from, to) &&
                        chessBoard.safeForKing(from, to)) validMoves.add(new Move(from, to, null));
            }
        }

        return validMoves;
    }

    private static long generatePseudoValidRookMoves(int square) {
        long moves = 0L;
        int row = square / 8;
        int col = square % 8;

        for (int i = col - 1; i >= 0; i--) moves |= 1L << (row * 8 + i);
        for (int i = col + 1; i < 8; i++) moves |= 1L << (row * 8 + i);

        for (int i = row - 1; i >= 0; i--) moves |= 1L << (i * 8 + col);
        for (int i = row + 1; i < 8; i++) moves |= 1L << (i * 8 + col);
        return moves;
    }
}
