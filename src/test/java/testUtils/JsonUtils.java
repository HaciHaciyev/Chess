package testUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.model.RegistrationForm;
import io.quarkus.logging.Log;
import org.junit.jupiter.api.Test;

class JsonUtils {

    final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toJson() throws JsonProcessingException {
        final RegistrationForm registrationForm = new RegistrationForm("Username", "usernamek@gmail.com", "password", "password");

        final String json = objectMapper.writeValueAsString(registrationForm);
        Log.info(json);
    }
}