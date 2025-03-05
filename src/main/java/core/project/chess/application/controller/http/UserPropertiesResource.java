package core.project.chess.application.controller.http;

import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.Username;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Authenticated
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
        String username = jwt.getName();
        if (!Username.validate(username)) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid username.").build());
        }

        UserProperties userProperties = outboundUserRepository
                .userProperties(username)
                .orElseThrow(() -> new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                        .entity("Can`t find user properties for %s.".formatted(username))
                        .build()));

        return Response.ok(userProperties).build();
    }
}