package core.project.chess.application.service;

import core.project.chess.application.dto.chess.Puzzle;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.commons.value_objects.Username;
import core.project.chess.domain.user.entities.User;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static core.project.chess.application.service.GameHistoryService.buildLimit;
import static core.project.chess.application.service.GameHistoryService.buildOffSet;
import static core.project.chess.application.util.JSONUtilities.responseException;

@ApplicationScoped
public class PuzzlesQueryService {

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    PuzzlesQueryService(OutboundUserRepository outboundUserRepository, OutboundChessRepository outboundChessRepository) {
        this.outboundUserRepository = outboundUserRepository;
        this.outboundChessRepository = outboundChessRepository;
    }

    public Puzzle puzzle(String id) {
        UUID puzzleId;
        try {
            puzzleId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, "Invalid puzzleID.");
        }

        return outboundChessRepository.puzzle(puzzleId)
                .orElseThrow(() -> responseException(Response.Status.BAD_REQUEST, "Puzzle by this id is do not exists."));
    }

    public List<Puzzle> page(String username, int pageNumber, int pageSize) {
        Username usernameObj;
        try {
            usernameObj = new Username(username);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }

        User user = outboundUserRepository.findByUsername(usernameObj)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "User not found"));

        double minRating = user.puzzlesRating().rating() - core.project.chess.domain.chess.entities.Puzzle.USER_RATING_WINDOW;
        double maxRating = user.puzzlesRating().rating() + core.project.chess.domain.chess.entities.Puzzle.USER_RATING_WINDOW;

        int limit = buildLimit(pageSize);
        int offSet = buildOffSet(limit, pageNumber);
        return outboundChessRepository.listOfPuzzles(minRating, maxRating, limit, offSet)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "Can`t found puzzles."));
    }
}
