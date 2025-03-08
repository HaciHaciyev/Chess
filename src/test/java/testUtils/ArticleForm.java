package testUtils;

import java.util.List;

public record ArticleForm(String header, String summary, String body, ArticleStatus status, List<String> tags) {}
