package core.project.chess.application.publisher;

import core.project.chess.domain.chess.events.ChessGameResult;
import core.project.chess.domain.chess.events.PuzzleGameResult;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import java.util.Collection;

@ApplicationScoped
public class EventPublisher {

    private final EventBus eventBus;

    EventPublisher(Instance<EventBus> eventBus) {
        this.eventBus = eventBus.get();
    }

    public void publishChessGame(ChessGameResult event) {
        eventBus.request("chess-game.result", event);
    }

    public void publishAllChessGame(Collection<ChessGameResult> events) {
        events.forEach(gameResult -> eventBus.request("chess-game.result", gameResult));
    }

    public void publishPuzzle(PuzzleGameResult event) {
        eventBus.request("puzzle.result", event);
    }

    public void publishAllPuzzle(Collection<PuzzleGameResult> events) {
        events.forEach(event -> eventBus.request("puzzle.result", event));
    }
}