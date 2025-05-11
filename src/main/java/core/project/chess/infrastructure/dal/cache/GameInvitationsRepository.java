package core.project.chess.infrastructure.dal.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import core.project.chess.application.util.JSONUtilities;
import core.project.chess.domain.chess.value_objects.GameParameters;
import core.project.chess.domain.commons.containers.StatusPair;
import core.project.chess.domain.user.value_objects.Username;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class GameInvitationsRepository {

    private static final String KEY_FORMAT = "PGI {%s}";

    private final HashCommands<String, String, String> hashCommands;

    GameInvitationsRepository(Instance<RedisDataSource> redisDataSource) {
        this.hashCommands = redisDataSource.get().hash(new TypeReference<>(){});
    }

    public void put(Username addressee, Username addresser, GameParameters data) {
        hashCommands.hset(String.format(KEY_FORMAT, addressee.username()),
                addresser.username(),
                JSONUtilities.writeValueAsString(data).orElseThrow());
    }

    public StatusPair<GameParameters> get(Username addressee, Username addresser) {
        String message = hashCommands.hget(String.format(KEY_FORMAT, addressee.username()), addresser.username());
        if (Objects.nonNull(message)) {
            return StatusPair.ofTrue(JSONUtilities.gameParameters(message).orElseThrow());
        }

        return StatusPair.ofFalse();
    }

    @WithSpan
    public Map<String, GameParameters> getAll(Username addressee) {
        Map<String, GameParameters> result = new HashMap<>();

        Map<String, String> values = hashCommands.hgetall(String.format(KEY_FORMAT, addressee.username()));
        values.forEach((key, value) -> result.put(key, JSONUtilities.gameParameters(value).orElseThrow()));

        return result;
    }

    public void delete(Username addressee, Username addresser) {
        hashCommands.hdel(String.format(KEY_FORMAT, addressee.username()), addresser.username());
    }
}
