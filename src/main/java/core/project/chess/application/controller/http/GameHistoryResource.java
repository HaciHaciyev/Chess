package core.project.chess.application.controller.http;

import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.domain.chess.repositories.OutboundChessRepository;
import core.project.chess.domain.user.value_objects.Username;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Authenticated
@Path("/account")
public class GameHistoryResource {

    private final JsonWebToken jwt;

    private final OutboundChessRepository outboundChessRepository;

    GameHistoryResource(JsonWebToken jwt, OutboundChessRepository outboundChessRepository) {
        this.jwt = jwt;
        this.outboundChessRepository = outboundChessRepository;
    }

    @GET
    @Path("/game-history")
    public Response gameHistory(@QueryParam("pageNumber") int pageNumber) {
        List<ChessGameHistory> listOfGames = outboundChessRepository
                .listOfGames(new Username(jwt.getName()), pageNumber)
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("User does not exist.\uD83D\uDC7B").build())
                );

        return Response.ok(listOfGames).build();
    }
}
