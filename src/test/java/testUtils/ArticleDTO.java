package testUtils;

import java.util.Set;
import java.util.UUID;

public record ArticleDTO(
    UUID id,
    UUID authorId,
    Set<String> tags,
    long views,
    long likes,
    String header,
    String summary,
    String body,
    String status
) {}
