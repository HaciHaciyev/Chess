package core.project.chess.domain.chess.util;

import core.project.chess.domain.chess.entities.ChessBoard;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.pieces.Piece;

import java.util.Random;

public class ZobristHashKeys {
    private long hash;

    private static final int CASTLING_RIGHTS_COUNT = 16;
    private static final int EN_PASSAUNT_FILES_COUNT = 9;

    private static final long[][] ZOBRIST_TABLE = new long[12][64];
    private static final long[] CASTLING_RIGHTS = new long[CASTLING_RIGHTS_COUNT];
    private static final long[] EN_PASSAUNTS = new long[EN_PASSAUNT_FILES_COUNT];
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

        for (int i = 0; i < EN_PASSAUNT_FILES_COUNT; i++) {
            EN_PASSAUNTS[i] = random.nextLong();
        }

        SIDE_TO_MOVE = random.nextLong();
    }

    public ZobristHashKeys(final ChessBoard chessBoard) {
        long zobristHash = 0L;

        for (Coordinate coordinate : Coordinate.values()) {
            Piece piece = chessBoard.piece(coordinate);
            if (piece != null) {
                // TODO
                //int pieceIndex = piece.index();
            }
        }
    }

    public long hash() {
        return hash;
    }


}
