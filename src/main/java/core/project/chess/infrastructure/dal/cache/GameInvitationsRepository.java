package core.project.chess.infrastructure.dal.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import core.project.chess.application.dto.chess.GameParameters;
import core.project.chess.application.util.JSONUtilities;
import core.project.chess.infrastructure.utilities.containers.StatusPair;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hash.HashCommands;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ApplicationScoped
public class GameInvitationsRepository {

    private static final String KEY_FORMAT = "PGI {%s}";

    private final HashCommands<String, String, String> hashCommands;

    GameInvitationsRepository(RedisDataSource redisDataSource) {
        this.hashCommands = redisDataSource.hash(new TypeReference<>(){});
    }

    public void put(String addressee, String addresser, GameParameters data) {
        hashCommands.hset(String.format(KEY_FORMAT, addressee), addresser, JSONUtilities.writeValueAsString(data).orElseThrow());
    }

    public StatusPair<GameParameters> get(String addressee, String addresser) {
        String message = hashCommands.hget(String.format(KEY_FORMAT, addressee), addresser);
        if (Objects.nonNull(message)) {
            return StatusPair.ofTrue(JSONUtilities.gameParameters(message).orElseThrow());
        }

        return StatusPair.ofFalse();
    }

    public Map<String, GameParameters> getAll(String addressee) {
        Map<String, GameParameters> result = new HashMap<>();

        Map<String, String> values = hashCommands.hgetall(String.format(KEY_FORMAT, addressee));
        values.forEach((key, value) -> result.put(key, JSONUtilities.gameParameters(value).orElseThrow()));

        return result;
    }

    public StatusPair<GameParameters> poll(String addressee, String addresser) {
        String message = hashCommands.hget(String.format(KEY_FORMAT, addressee), addresser);
        if (Objects.nonNull(message)) {
            hashCommands.hdel(addressee, addresser);
            return StatusPair.ofTrue(JSONUtilities.gameParameters(message).orElseThrow());
        }

        return StatusPair.ofFalse();
    }

    public Map<String, GameParameters> pollAll(String addressee) {
        Map<String, GameParameters> result = new HashMap<>();

        Map<String, String> values = hashCommands.hgetall(String.format(KEY_FORMAT, addressee));
        values.forEach((key, value) -> result.put(key, JSONUtilities.gameParameters(value).orElseThrow()));

        hashCommands.hdel(String.format(KEY_FORMAT, addressee));
        return result;
    }

    public void delete(String addressee, String addresser) {
        hashCommands.hdel(String.format(KEY_FORMAT, addressee), addresser);
    }
}
