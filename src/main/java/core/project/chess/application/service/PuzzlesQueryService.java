package core.project.chess.application.service;

import core.project.chess.application.dto.chess.Puzzle;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.Username;
import core.project.chess.infrastructure.utilities.containers.Result;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

import static core.project.chess.application.service.GameHistoryService.buildLimit;
import static core.project.chess.application.service.GameHistoryService.buildOffSet;

@ApplicationScoped
public class PuzzlesQueryService {

    private final OutboundUserRepository outboundUserRepository;

    private final OutboundChessRepository outboundChessRepository;

    PuzzlesQueryService(OutboundUserRepository outboundUserRepository, OutboundChessRepository outboundChessRepository) {
        this.outboundUserRepository = outboundUserRepository;
        this.outboundChessRepository = outboundChessRepository;
    }

    public Puzzle puzzle(String id) {
        UUID puzzleId = Result.ofThrowable(() -> UUID.fromString(id))
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid puzzle ID").build()));

        return outboundChessRepository.puzzle(puzzleId)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("Puzzle by this id is do not exists.").build())
                );
    }

    public List<Puzzle> page(String username, int pageNumber, int pageSize) {
        if (!Username.validate(username)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username").build());
        }

        double rating = outboundUserRepository.userProperties(username).orElseThrow().rating();
        int limit = buildLimit(pageSize);
        int offSet = buildOffSet(limit, pageNumber);

        return outboundChessRepository.listOfPuzzles(rating, limit, offSet)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.NOT_FOUND).entity("No puzzles found").build())
                );
    }
}
