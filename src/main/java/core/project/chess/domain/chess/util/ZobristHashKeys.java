package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;

import java.util.Random;

import static core.project.chess.domain.chess.enumerations.Coordinate.*;

/**
 * Generates Zobrist hash keys for chess board positions.
 */
public class ZobristHashKeys {
    private static final int CASTLING_RIGHTS_COUNT = 16;
    private static final int EN_PASSANT_FILES_COUNT = 9;

    private static final long[][] ZOBRIST_TABLE = new long[12][64];
    private static final long[] CASTLING_RIGHTS = new long[CASTLING_RIGHTS_COUNT];
    private static final long[] EN_PASSANTS = new long[EN_PASSANT_FILES_COUNT];
    private static final long SIDE_TO_MOVE;

    static {
        Random random = new Random(69);

        for (int piece = 0; piece < 12; piece++) {
            for (int square = 0; square < 64; square++) ZOBRIST_TABLE[piece][square] = random.nextLong();
        }

        for (int i = 0; i < CASTLING_RIGHTS_COUNT; i++) CASTLING_RIGHTS[i] = random.nextLong();

        for (int i = 0; i < EN_PASSANT_FILES_COUNT; i++) EN_PASSANTS[i] = random.nextLong();

        SIDE_TO_MOVE = random.nextLong();
    }

    /**
     * Computes the Zobrist hash for the given chess board.
     *
     * @param chessBoard the chess board
     * @return the Zobrist hash
     */
    public long computeZobristHash(final ChessBoard chessBoard) {
        long zobristHash = 0L;

        int square = 0;
        for (Coordinate coordinate : Coordinate.coordinates()) {
            Piece piece = chessBoard.piece(coordinate);
            if (piece != null) {
                int pieceIndex = piece.index();
                zobristHash ^= ZOBRIST_TABLE[pieceIndex][square];
            }
            square++;
        }

        int castlingRights = chessBoard.castlingRights();
        if (castlingRights >= 0) zobristHash ^= CASTLING_RIGHTS[castlingRights];

        int enPassantFile = chessBoard.enPassantFile();
        if (enPassantFile >= 0) zobristHash ^= EN_PASSANTS[enPassantFile];

        if (chessBoard.turn() == Color.BLACK) zobristHash ^= SIDE_TO_MOVE;

        return zobristHash;
    }

    public long updateHash(final long hash,
                           final Piece startedPiece,
                           final Coordinate from,
                           final Piece endedPiece,
                           final Coordinate to,
                           final int castlingRights,
                           final int enPassantFile) {
        long newHash = hash;

        newHash ^= ZOBRIST_TABLE[startedPiece.index()][from.index()];

        newHash ^= ZOBRIST_TABLE[endedPiece.index()][to.index()];

        if (castlingRights >= 0) newHash ^= CASTLING_RIGHTS[castlingRights];

        if (enPassantFile >= 0) newHash ^= EN_PASSANTS[enPassantFile];

        newHash ^= SIDE_TO_MOVE;

        return newHash;
    }

    public long updateHash(final long hash,
                           final Piece startedPiece,
                           final Coordinate from,
                           final Piece endedPiece,
                           final Coordinate to,
                           final Piece capturedPiece,
                           final Coordinate capturedAt,
                           final int castlingRights,
                           final int enPassantFile) {
        long newHash = updateHash(hash, startedPiece, from, endedPiece, to, castlingRights, enPassantFile);

        newHash ^= ZOBRIST_TABLE[capturedPiece.index()][capturedAt.index()];

        return newHash;
    }

    public long updateHashForCastling(final long hash,
                                      final Color color,
                                      final AlgebraicNotation.Castle castle,
                                      final int castlingRights) {
        long newHash = hash;

        int kingIndex = color == Color.WHITE ? 5 : 11;
        int rookIndex = color == Color.WHITE ? 3 : 9;

        if (color == Color.WHITE) {
            if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                newHash ^= ZOBRIST_TABLE[kingIndex][WHITE_KING_START.index()];
                newHash ^= ZOBRIST_TABLE[kingIndex][WHITE_KING_SHORT_CASTLE.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][WHITE_ROOK_SHORT_CASTLE_START.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][WHITE_ROOK_SHORT_CASTLE_END.index()];
            } else {
                newHash ^= ZOBRIST_TABLE[kingIndex][WHITE_KING_START.index()];
                newHash ^= ZOBRIST_TABLE[kingIndex][WHITE_KING_LONG_CASTLE.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][WHITE_ROOK_LONG_CASTLE_START.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][WHITE_ROOK_LONG_CASTLE_END.index()];
            }
        } else {
            if (castle == AlgebraicNotation.Castle.SHORT_CASTLING) {
                newHash ^= ZOBRIST_TABLE[kingIndex][BLACK_KING_START.index()];
                newHash ^= ZOBRIST_TABLE[kingIndex][BLACK_KING_SHORT_CASTLE.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][BLACK_ROOK_SHORT_CASTLE_START.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][BLACK_ROOK_SHORT_CASTLE_END.index()];
            } else {
                newHash ^= ZOBRIST_TABLE[kingIndex][BLACK_KING_START.index()];
                newHash ^= ZOBRIST_TABLE[kingIndex][BLACK_KING_LONG_CASTLE.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][BLACK_ROOK_LONG_CASTLE_START.index()];
                newHash ^= ZOBRIST_TABLE[rookIndex][BLACK_ROOK_LONG_CASTLE_END.index()];
            }
        }

        newHash ^= CASTLING_RIGHTS[castlingRights];
        newHash ^= SIDE_TO_MOVE;

        return newHash;
    }
}
