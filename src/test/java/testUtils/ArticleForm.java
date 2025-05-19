package testUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public record ArticleForm(String header, String summary, String body, ArticleStatus status, List<String> tags) {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public String asJSON() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
