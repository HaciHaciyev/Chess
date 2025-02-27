package core.project.chess.application.controller.http;

import core.project.chess.application.service.GameHistoryService;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Authenticated
@Path("/account")
public class GameHistoryResource {

    private final JsonWebToken jwt;

    private final GameHistoryService gameHistoryService;

    GameHistoryResource(Instance<JsonWebToken> jwt, GameHistoryService gameHistoryService) {
        this.jwt = jwt.get();
        this.gameHistoryService = gameHistoryService;
    }

    @GET
    @Path("/game-history")
    public Response gameHistory(@QueryParam("pageNumber") int pageNumber, @QueryParam("pageSize") int pageSize) {
        return Response.ok(gameHistoryService.listOfGames(jwt.getName(), pageNumber, pageSize)).build();
    }

    @GET
    @Path("/game")
    public Response getGameByID(@QueryParam("gameID") String gameID) {
        return Response.ok(gameHistoryService.getGameByID(gameID)).build();
    }
}
