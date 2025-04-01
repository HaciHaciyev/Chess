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
                           final int enPassantFile,
                           final Color turn) {
        return 0L; // TODO
    }

    public long updateHash(final Piece startedPiece,
                           final Coordinate from,
                           final Piece endedPiece,
                           final Coordinate to,
                           final Piece capturedPiece,
                           final Coordinate capturedAt,
                           final int castlingRights,
                           final int enPassantFile,
                           final Color turn) {
        return 0L; // TODO
    }

    public long updateHashForCastling(final Color color, final AlgebraicNotation.Castle castle, int castlingRights) {
        return 0L; // TODO
    }

    public long updateHashOnRevertMove(final Piece startedPiece,
                                       final Coordinate from,
                                       final Piece endedPiece,
                                       final Coordinate to,
                                       final int castlingRights,
                                       final int enPassantFile,
                                       final Color turn) {
        return 0L; // TODO
    }

    public long updateHashOnRevertMove(final Piece startedPiece,
                                       final Coordinate from,
                                       final Piece endedPiece,
                                       final Coordinate to,
                                       final Piece capturedPiece,
                                       final Coordinate capturedAt,
                                       final int castlingRights,
                                       final int enPassantFile,
                                       final Color turn) {
        return 0L; // TODO
    }

    public long updateHashForCastlingRevert(final Color color, final AlgebraicNotation.Castle castle, int castlingRights) {
        return 0L; // TODO
    }
}
