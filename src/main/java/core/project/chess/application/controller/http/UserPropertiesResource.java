package core.project.chess.application.controller.http;

import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/account")
public class UserPropertiesResource {

    private final JsonWebToken jwt;

    private final OutboundUserRepository outboundUserRepository;

    UserPropertiesResource(Instance<JsonWebToken> jwt, OutboundUserRepository outboundUserRepository) {
        this.jwt = jwt.get();
        this.outboundUserRepository = outboundUserRepository;
    }

    @GET
    @Path("/user-properties")
    public Response userProperties() {
        UserProperties userProperties = outboundUserRepository
                .userProperties(jwt.getName())
                .orElseThrow(
                        () -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Can`t find user properties for user.").build())
                );

        return Response.ok(userProperties).build();
    }
}