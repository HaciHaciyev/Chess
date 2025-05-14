package core.project.chess.application.controller.http;

import core.project.chess.application.service.PuzzlesQueryService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Authenticated
@Path("/puzzles")
public class PuzzleResource {

    private final JsonWebToken jwt;

    private final PuzzlesQueryService puzzlesQueryService;

    PuzzleResource(Instance<JsonWebToken> jwt, PuzzlesQueryService puzzlesQueryService) {
        this.jwt = jwt.get();
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
}
