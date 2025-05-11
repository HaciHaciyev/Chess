package core.project.chess.infrastructure.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper;

    PuzzlerClient(Instance<Vertx> vertx, ObjectMapper objectMapper) {
        this.client = WebClient.create(vertx.get());
        this.objectMapper = objectMapper;
    }

    public PuzzleInbound sendPGN(String PGN) {
        JsonObject jsonBody = new JsonObject().put("PGN", PGN);

        return client.postAbs(puzzlerApi)
                .putHeader("Content-Type", MediaType.APPLICATION_JSON)
                .putHeader("X-API-KEY", apiKey)
                .sendJson(jsonBody)
                .onFailure(error -> {
                    Log.errorf("Error sending PGN for puzzler: %s".formatted(error.getMessage()));
                    error.printStackTrace();
                })
                .map(response -> {
                    try {
                        return objectMapper.readValue(response.bodyAsString(), new TypeReference<PuzzleInbound>() {});
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Can`t parse a puzzle", e);
                    }
                })
                .result();
    }
}
