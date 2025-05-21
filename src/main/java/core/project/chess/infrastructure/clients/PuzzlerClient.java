package core.project.chess.infrastructure.clients;

import core.project.chess.application.dto.chess.PuzzleInbound;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.logging.Log;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
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

    @WithSpan("Sending PGN to puzzler")
    public void sendPGN(String PGN, Handler<HttpResponse<PuzzleInbound>> handler) {
        Log.info("Sending PGN to puzzler: " + PGN);
        JsonObject jsonBody = new JsonObject().put("PGN", PGN);

        client.postAbs(puzzlerApi)
        .as(BodyCodec.json(PuzzleInbound.class))
        .putHeader("Content-Type", MediaType.APPLICATION_JSON)
        .putHeader("X-API-KEY", apiKey)
        .sendJson(jsonBody)
        .onFailure(error -> {
            Log.errorf("Error sending PGN for puzzler: %s".formatted(error.getMessage()));
            error.printStackTrace();
        })
        .onSuccess(handler);
    }
}
