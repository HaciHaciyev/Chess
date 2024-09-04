package core.project.chess.application.model;

import java.util.Objects;

public record ChessGameMessage(String FEN, String PGN) {

    public ChessGameMessage {
        Objects.requireNonNull(FEN);
        Objects.requireNonNull(PGN);
    }
}
