package core.project.chess.domain.chess.entities;

class Bitboard {
    private long whiteKing;
    private long blackKing;
    private long whiteQueens;
    private long blackQueens;
    private long whiteBishops;
    private long blackBishops;
    private long whiteKnights;
    private long blackKnights;
    private long whiteRooks;
    private long blackRooks;
    private long whitePawns;
    private long blackPawns;

    Bitboard() {
        initializeBitboard();
    }

    private void initializeBitboard() {

    }
}
