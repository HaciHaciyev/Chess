package core.project.chess.infrastructure.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.chess.Message;
import io.quarkus.logging.Log;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;

public class MessageDecoder implements Decoder.Text<Message> {

    private static final int MAX_LENGTH = 2048;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Message decode(String json) throws DecodeException {
        if (json == null) {
            throw new DecodeException(json, "Invalid message: null.");
        }

        if (json.length() > MAX_LENGTH) {
            throw new DecodeException(json, "Invalid message: max size %s.".formatted(MAX_LENGTH));
        }

        try {
            return objectMapper.readValue(json, Message.class);
        } catch (Exception e) {
            Log.errorf("can't decode %s", json);
            throw new DecodeException(json, "Unable to decode JSON", e);
        }
    }

    @Override
    public boolean willDecode(String json) {
        return json != null && !json.isBlank() && json.length() <= MAX_LENGTH;
    }
}
