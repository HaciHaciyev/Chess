package core.project.chess.infrastructure.clients;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
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

    public void sendPGN(String PGN) {
        JsonObject jsonBody = new JsonObject().put("PGN", PGN);

        client.postAbs(puzzlerApi)
                .putHeader("X-API-KEY", apiKey)
                .sendJson(jsonBody);
    }
}
