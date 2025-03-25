package core.project.chess.application.controller.http;

import core.project.chess.application.dto.chess.PuzzleInbound;
import core.project.chess.application.service.PuzzlesQueryService;
import core.project.chess.domain.chess.services.PuzzleService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import static core.project.chess.application.util.JSONUtilities.responseException;

@Authenticated
@Path("/puzzles")
public class PuzzleResource {

    private final JsonWebToken jwt;

    private final PuzzleService puzzleService;

    private final PuzzlesQueryService puzzlesQueryService;

    PuzzleResource(Instance<JsonWebToken> jwt, PuzzleService puzzleService, PuzzlesQueryService puzzlesQueryService) {
        this.jwt = jwt.get();
        this.puzzleService = puzzleService;
        this.puzzlesQueryService = puzzlesQueryService;
    }

    @GET
    @Path("/{id}")
    public Response getPuzzle(@PathParam("id") String id) {
        return Response.ok(puzzlesQueryService.puzzle(id)).build();
    }

    @GET
    @Path("/page")
    public Response page(@QueryParam("pageNumber") int pageNumber, @QueryParam("pageSize") int pageSize) {
        return Response.ok(puzzlesQueryService.page(jwt.getName(), pageNumber, pageSize)).build();
    }

    @POST
    @Path("/save")
    public Response save(PuzzleInbound puzzleInbound) {
        try {
            puzzleService.save(puzzleInbound.PGN(), puzzleInbound.startPositionOfPuzzle());
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }
        return Response.ok().build();
    }
}
