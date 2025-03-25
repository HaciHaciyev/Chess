package core.project.chess.infrastructure.clients;

import core.project.chess.application.dto.chess.PuzzleInbound;
import io.quarkus.logging.Log;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class PuzzlerClient {

    @ConfigProperty(name = "puzzler-api")
    String puzzlerApi;

    @ConfigProperty(name = "x-api-key")
    String apiKey;

    private final WebClient client;

    PuzzlerClient(Instance<Vertx> vertx) {
        this.client = WebClient.create(vertx.get());
    }

    public PuzzleInbound sendPGN(String PGN) {
        JsonObject jsonBody = new JsonObject().put("PGN", PGN);

        return client.postAbs(puzzlerApi)
                .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                .putHeader("X-API-KEY", apiKey)
                .sendJson(jsonBody)
                .onFailure(error -> Log.infof("Error sending PGN for puzzler: %s".formatted(error.getMessage())))
                .map(response -> response.bodyAsJson(PuzzleInbound.class))
                .result();
    }
}
