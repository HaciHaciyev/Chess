package core.project.chess.application.controller.http;

import core.project.chess.application.dto.user.UserProperties;
import core.project.chess.domain.commons.value_objects.Username;
import core.project.chess.domain.user.repositories.OutboundUserRepository;
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
        String upn = jwt.getName();
        Username username;
        try {
            username = new Username(upn);
        } catch (IllegalArgumentException e) {
            throw responseException(Response.Status.BAD_REQUEST, e.getMessage());
        }

        UserProperties userProperties = outboundUserRepository.userProperties(username)
                .orElseThrow(() -> responseException(Response.Status.NOT_FOUND, "Can`t find the user properties for " + username));

        return Response.ok(userProperties).build();
    }
}