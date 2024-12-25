package core.project.chess.infrastructure.api.engine;

import core.project.chess.domain.api.engine.ChessEngineAPI;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ChessEngineClient implements ChessEngineAPI {

    @Override
    public boolean validateFEN(String FEN) {
        return false;
    }
}
