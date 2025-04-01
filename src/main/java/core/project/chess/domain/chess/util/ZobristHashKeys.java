package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;

import java.util.Random;

public class ZobristHashKeys {
    private long hash;

    private static final int CASTLING_RIGHTS_COUNT = 16;
    private static final int EN_PASSANT_FILES_COUNT = 9;

    private static final long[][] ZOBRIST_TABLE = new long[12][64];
    private static final long[] CASTLING_RIGHTS = new long[CASTLING_RIGHTS_COUNT];
    private static final long[] EN_PASSANTS = new long[EN_PASSANT_FILES_COUNT];
    private static final long SIDE_TO_MOVE;

    static {
        Random random = new Random(69);

        for (int piece = 0; piece < 12; piece++) {
            for (int square = 0; square < 64; square++) {
                ZOBRIST_TABLE[piece][square] = random.nextLong();
            }
        }

        for (int i = 0; i < CASTLING_RIGHTS_COUNT; i++) {
            CASTLING_RIGHTS[i] = random.nextLong();
        }

        for (int i = 0; i < EN_PASSANT_FILES_COUNT; i++) {
            EN_PASSANTS[i] = random.nextLong();
        }

        SIDE_TO_MOVE = random.nextLong();
    }

    public ZobristHashKeys(final ChessBoard chessBoard) {
        long zobristHash = 0L;

        int square = 0;
        for (Coordinate coordinate : Coordinate.values()) {
            Piece piece = chessBoard.piece(coordinate);
            if (piece != null) {
                int pieceIndex = piece.index();
                zobristHash ^= ZOBRIST_TABLE[pieceIndex][square];
            }
            square++;
        }

        zobristHash ^= CASTLING_RIGHTS[chessBoard.castlingRights()];

        int enPassantFile = chessBoard.enPassantFile();
        if (enPassantFile >= 0) {
            zobristHash ^= EN_PASSANTS[enPassantFile];
        }

        if (chessBoard.turn() == Color.BLACK) {
            zobristHash ^= SIDE_TO_MOVE;
        }

        this.hash = zobristHash;
    }

    public long hash() {
        return hash;
    }

    public long updateHash(final Piece startedPiece,
                           final Coordinate from,
                           final Piece endedPiece,
                           final Coordinate to,
                           final int castlingRights,
                           final int enPassantFile) {
        long newHash = this.hash;

        newHash ^= ZOBRIST_TABLE[startedPiece.index()][from.ordinal()];

        newHash ^= ZOBRIST_TABLE[endedPiece.index()][to.ordinal()];

        newHash ^= CASTLING_RIGHTS[castlingRights];

        if (enPassantFile >= 0) {
            newHash ^= EN_PASSANTS[enPassantFile];
        }

        newHash ^= SIDE_TO_MOVE;

        this.hash = newHash;
        return newHash;
    }

    public long updateHash(final Piece startedPiece,
                           final Coordinate from,
                           final Piece endedPiece,
                           final Coordinate to,
                           final Piece capturedPiece,
                           final Coordinate capturedAt,
                           final int castlingRights,
                           final int enPassantFile) {

        long newHash = updateHash(startedPiece, from, endedPiece, to, castlingRights, enPassantFile);

        newHash ^= ZOBRIST_TABLE[capturedPiece.index()][capturedAt.ordinal()];

        this.hash = newHash;
        return newHash;
    }

    public long updateHashForCastling(final Color color, final AlgebraicNotation.Castle castle, int castlingRights) {
        long newHash = this.hash;

        if (color == Color.WHITE) {
            if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                newHash ^= ZOBRIST_TABLE[5][Coordinate.e1.ordinal()];
                newHash ^= ZOBRIST_TABLE[5][Coordinate.g1.ordinal()];
                newHash ^= ZOBRIST_TABLE[3][Coordinate.h1.ordinal()];
                newHash ^= ZOBRIST_TABLE[3][Coordinate.f1.ordinal()];
            } else {
                newHash ^= ZOBRIST_TABLE[11][Coordinate.e1.ordinal()];
                newHash ^= ZOBRIST_TABLE[11][Coordinate.c1.ordinal()];
                newHash ^= ZOBRIST_TABLE[9][Coordinate.a1.ordinal()];
                newHash ^= ZOBRIST_TABLE[9][Coordinate.d1.ordinal()];
            }
        } else {
            if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                newHash ^= ZOBRIST_TABLE[5][Coordinate.e8.ordinal()];
                newHash ^= ZOBRIST_TABLE[5][Coordinate.g8.ordinal()];
                newHash ^= ZOBRIST_TABLE[3][Coordinate.h8.ordinal()];
                newHash ^= ZOBRIST_TABLE[3][Coordinate.f8.ordinal()];
            } else {
                newHash ^= ZOBRIST_TABLE[11][Coordinate.e8.ordinal()];
                newHash ^= ZOBRIST_TABLE[11][Coordinate.c8.ordinal()];
                newHash ^= ZOBRIST_TABLE[9][Coordinate.a8.ordinal()];
                newHash ^= ZOBRIST_TABLE[9][Coordinate.d8.ordinal()];
            }
        }

        newHash ^= CASTLING_RIGHTS[castlingRights];

        newHash ^= SIDE_TO_MOVE;

        this.hash = newHash;
        return newHash;
    }

    public long updateHashOnRevertMove(final Piece startedPiece,
                                       final Coordinate from,
                                       final Piece endedPiece,
                                       final Coordinate to,
                                       final int castlingRights,
                                       final int enPassantFile) {
        long newHash = this.hash;

        newHash ^= ZOBRIST_TABLE[endedPiece.index()][to.ordinal()];

        newHash ^= ZOBRIST_TABLE[startedPiece.index()][from.ordinal()];

        newHash ^= CASTLING_RIGHTS[castlingRights];

        if (enPassantFile >= 0) {
            newHash ^= EN_PASSANTS[enPassantFile];
        }

        newHash ^= SIDE_TO_MOVE;

        this.hash = newHash;
        return newHash;
    }

    public long updateHashOnRevertMove(final Piece startedPiece,
                                       final Coordinate from,
                                       final Piece endedPiece,
                                       final Coordinate to,
                                       final Piece capturedPiece,
                                       final Coordinate capturedAt,
                                       final int castlingRights,
                                       final int enPassantFile) {
        long newHash = updateHashOnRevertMove(startedPiece, from, endedPiece, to, castlingRights, enPassantFile);

        newHash ^= ZOBRIST_TABLE[capturedPiece.index()][capturedAt.ordinal()];

        this.hash = newHash;
        return newHash;
    }

    public long updateHashForCastlingRevert(final Color color, final AlgebraicNotation.Castle castle, int castlingRights) {
        long newHash = this.hash;

        Coordinate kingFrom;
        Coordinate kingTo;
        Coordinate rookFrom;
        Coordinate rookTo;

        if (color == Color.WHITE) {
            if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                kingFrom = Coordinate.e1; kingTo = Coordinate.g1;
                rookFrom = Coordinate.h1; rookTo = Coordinate.f1;
            } else {
                kingFrom = Coordinate.e1; kingTo = Coordinate.c1;
                rookFrom = Coordinate.a1; rookTo = Coordinate.d1;
            }
        } else {
            if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                kingFrom = Coordinate.e8; kingTo = Coordinate.g8;
                rookFrom = Coordinate.h8; rookTo = Coordinate.f8;
            } else {
                kingFrom = Coordinate.e8; kingTo = Coordinate.c8;
                rookFrom = Coordinate.a8; rookTo = Coordinate.d8;
            }
        }

        int kingIndex = color == Color.WHITE ? 5 : 11;
        int rookIndex = color == Color.WHITE ? 3 : 9;

        newHash ^= ZOBRIST_TABLE[kingIndex][kingTo.ordinal()];
        newHash ^= ZOBRIST_TABLE[kingIndex][kingFrom.ordinal()];

        newHash ^= ZOBRIST_TABLE[rookIndex][rookTo.ordinal()];
        newHash ^= ZOBRIST_TABLE[rookIndex][rookFrom.ordinal()];

        newHash ^= CASTLING_RIGHTS[castlingRights];

        newHash ^= SIDE_TO_MOVE;

        this.hash = newHash;
        return newHash;
    }
}
