package core.project.chess.application.controller.http;

import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
import core.project.chess.domain.user.value_objects.Username;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import static core.project.chess.application.util.JSONUtilities.responseException;

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
        if (!Username.isValid(username)) {
            throw responseException(Response.Status.BAD_REQUEST, "Invalid username");
        }

        UserProperties userProperties = outboundUserRepository.userProperties(username)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "Can`t find user properties."));

        return Response.ok(userProperties).build();
    }
}