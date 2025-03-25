package core.project.chess.infrastructure.interceptors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;

@Provider
public class RequestsFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "x-api-key")
    String apiKey;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final boolean isPuzzleCreationEndpoint = requestContext.getUriInfo().getRequestUri().toString().endsWith("/puzzles/save");

        if (isPuzzleCreationEndpoint) {
            String header = requestContext.getHeaderString("X-API-KEY");
            if (!apiKey.equals(header)) {
                requestContext.abortWith(Response
                        .status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid API key")
                        .build());
            }
        }
    }
}
