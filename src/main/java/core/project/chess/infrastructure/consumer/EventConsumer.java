package core.project.chess.infrastructure.consumer;

import core.project.chess.domain.chess.events.ChessGameResult;
import core.project.chess.domain.chess.events.PuzzleGameResult;
import core.project.chess.domain.commons.value_objects.Rating;
import core.project.chess.domain.commons.value_objects.RatingType;
import core.project.chess.domain.commons.value_objects.RatingUpdate;
import core.project.chess.domain.commons.value_objects.RatingUpdateOnPuzzle;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.InboundUserRepository;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class EventConsumer {

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    EventConsumer(InboundUserRepository inboundUserRepository,
                  OutboundUserRepository outboundUserRepository) {
        this.inboundUserRepository = inboundUserRepository;
        this.outboundUserRepository = outboundUserRepository;
    }

    @ConsumeEvent("chess-game.result")
    public void consumeChessGameResult(ChessGameResult event) {
        User playerForWhites = findUser(event.whitePlayer());
        User playerForBlacks = findUser(event.blackPlayer());

        Rating whiteRating = getPlayerRating(playerForWhites, event.ratingType());
        Rating blackRating = getPlayerRating(playerForBlacks, event.ratingType());
        RatingUpdate ratingUpdate = new RatingUpdate(event.gameID(), playerForWhites.id(), whiteRating,
                playerForBlacks.id(), blackRating, event.gameResult(), event.ratingType());

        playerForWhites.changeRating(ratingUpdate);
        playerForBlacks.changeRating(ratingUpdate);

        switch (ratingUpdate.ratingType()) {
            case CLASSIC -> {
                inboundUserRepository.updateOfRating(playerForWhites);
                inboundUserRepository.updateOfRating(playerForBlacks);
            }
            case RAPID -> {
                inboundUserRepository.updateOfRapidRating(playerForWhites);
                inboundUserRepository.updateOfRapidRating(playerForBlacks);
            }
            case BLITZ -> {
                inboundUserRepository.updateOfBlitzRating(playerForWhites);
                inboundUserRepository.updateOfBlitzRating(playerForBlacks);
            }
            case BULLET -> {
                inboundUserRepository.updateOfBulletRating(playerForWhites);
                inboundUserRepository.updateOfBulletRating(playerForBlacks);
            }
        }
    }

    @ConsumeEvent("puzzle.result")
    public void consumePuzzleResult(PuzzleGameResult event) {
        User player = findUser(event.playerID());
        Rating playerRating = player.puzzlesRating();

        var ratingUpdate = new RatingUpdateOnPuzzle(event.puzzleID(), player.id(),
                event.puzzleRating(), playerRating, event.gameResult());
        player.changeRating(ratingUpdate);

        inboundUserRepository.updateOfPuzzleRating(player);
    }

    private User findUser(UUID userID) {
        return outboundUserRepository.findById(userID)
                .orElseThrow(() -> new IllegalStateException("User not found."));
    }

    private Rating getPlayerRating(User user, RatingType ratingType) {
        return switch (ratingType) {
            case CLASSIC -> user.rating();
            case RAPID -> user.rapidRating();
            case BLITZ -> user.blitzRating();
            case BULLET -> user.bulletRating();
        };
    }
}
