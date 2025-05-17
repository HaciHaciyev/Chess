package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.controller.ws.MessagingTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import net.datafaker.Faker;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import testUtils.ArticleForm;
import testUtils.ArticleStatus;
import testUtils.AuthInfo;
import testUtils.AuthUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@WithTestResource(MessagingTestResource.class)
class ArticlesTest {

    @Inject
    AuthUtils authUtils;

    static final Faker faker = new Faker();

    static final ObjectMapper objectMapper = new ObjectMapper();

    static final ThreadLocalRandom random = ThreadLocalRandom.current();

    static final String messagingURL = ConfigProvider.getConfig()
            .getConfigValue("messaging.api.url").getValue() + "/chessland/articles";

    @RepeatedTest(3)
    @DisplayName("Test valid article creation")
    void save() throws JsonProcessingException {
        AuthInfo user = authUtils.fullLoginProcess();
        String articleFormJSON = objectMapper.writeValueAsString(articleForm(ArticleStatus.DRAFT));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(articleFormJSON)
                .contentType(ContentType.JSON)
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(200);
    }

    @RepeatedTest(3)
    @DisplayName("Test invalid article creation: Article can`t be created with status - Archived")
    void invalidArticleSave_StatusArchived() throws JsonProcessingException {
        AuthInfo user = authUtils.fullLoginProcess();
        String articleFormJSON = objectMapper.writeValueAsString(articleForm(ArticleStatus.ARCHIVED));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(articleFormJSON)
                .contentType(ContentType.JSON)
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(400)
                .body(containsString("Article can`t be created with archived status."));
    }

    @RepeatedTest(3)
    @DisplayName("Test invalid article creation: Invalid article header.")
    void invalidArticleSave_InvalidText() throws JsonProcessingException {
        AuthInfo user = authUtils.fullLoginProcess();

        String invalidHeaderArticle = objectMapper.writeValueAsString(new ArticleForm(
                "", // invalid header
                faker.lorem().sentence(),
                faker.lorem().paragraph(10),
                ArticleStatus.DRAFT,
                List.of("test", "header", "summary", "body")
        ));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(invalidHeaderArticle)
                .contentType(ContentType.JSON)
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(400)
                .body(containsString("Header cannot be blank."));
    }

    @RepeatedTest(3)
    @DisplayName("Test invalid article creation: Invalid article summary.")
    void invalidArticleSave_InvalidSummary() throws JsonProcessingException {
        AuthInfo user = authUtils.fullLoginProcess();
        String invalidSummaryArticle = objectMapper.writeValueAsString(new ArticleForm(
                faker.book().title(),
                "",
                faker.lorem().paragraph(10),
                ArticleStatus.DRAFT,
                List.of("test", "header", "summary", "body")
        ));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(invalidSummaryArticle)
                .contentType(ContentType.JSON)
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(400)
                .body(containsString("Summary must not be blank."));
    }

    @RepeatedTest(3)
    @DisplayName("Test invalid article creation: Invalid article body.")
    void invalidArticleSave_InvalidBody() throws JsonProcessingException {
        AuthInfo user = authUtils.fullLoginProcess();

        String invalidBodyArticle = objectMapper.writeValueAsString(new ArticleForm(
                faker.book().title(),
                faker.lorem().sentence(),
                "",
                ArticleStatus.DRAFT,
                List.of("test", "header", "summary", "body")
        ));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(invalidBodyArticle)
                .contentType(ContentType.JSON)
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(400)
                .body(containsString("Content is blank."));
    }

    @RepeatedTest(3)
    @DisplayName("Test invalid article creation: Invalid article tags.")
    void invalidArticleSave_InvalidTags() throws JsonProcessingException {
        AuthInfo user = authUtils.fullLoginProcess();

        String invalidTagsArticleTooFewTags = objectMapper.writeValueAsString(articleForm(List.of("tag"), new ArrayList<>()));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(invalidTagsArticleTooFewTags)
                .contentType(ContentType.JSON)
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(400)
                .body(containsString("You need at least create 3 tags for Article and no more than 8."));

        String invalidTagsArticlesTooManyTags = objectMapper.writeValueAsString(articleForm(List
                .of("tag", "another", "third", "idk", "java", "maven", "quarkus", "Immanuel Kant", "Megadeath"), new ArrayList<>()));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(invalidTagsArticlesTooManyTags)
                .contentType(ContentType.JSON)
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(400)
                .body(containsString("You need at least create 3 tags for Article and no more than 8."));
    }

    static ArticleForm articleForm() {
        String header = faker.book().title();
        String summary = faker.lorem().sentence();
        String body = faker.lorem().paragraph(10);
        ArticleStatus status = ArticleStatus.values()[random.nextInt(2)];
        List<String> tags = List.of(faker.lorem().word(), faker.lorem().word(), faker.lorem().word());

        return new ArticleForm(header, summary, body, status, tags);
    }

    static ArticleForm articleForm(ArticleStatus status) {
        String header = faker.book().title();
        String summary = faker.lorem().sentence();
        String body = faker.lorem().paragraph(10);
        List<String> tags = List.of(faker.lorem().word(), faker.lorem().word(), faker.lorem().word());

        return new ArticleForm(header, summary, body, status, tags);
    }

    static ArticleForm articleForm(List<String> keywords) {
        String header = insertKeywords(faker.book().title(), keywords);
        String summary = insertKeywords(faker.lorem().sentence(), keywords);
        String body = insertKeywords(faker.lorem().paragraph(10), keywords);
        ArticleStatus status = ArticleStatus.values()[random.nextInt(2)];
        List<String> tags = List.of(faker.lorem().word(), faker.lorem().word(), faker.lorem().word());

        return new ArticleForm(header, summary, body, status, tags);
    }

    static ArticleForm articleForm(List<String> customTags, List<String> keywords) {
        String header = insertKeywords(faker.book().title(), keywords);
        String summary = insertKeywords(faker.lorem().sentence(), keywords);
        String body = insertKeywords(faker.lorem().paragraph(10), keywords);
        ArticleStatus status = ArticleStatus.values()[random.nextInt(2)];

        return new ArticleForm(header, summary, body, status, customTags);
    }

    static ArticleForm articleForm(ArticleStatus status, List<String> customTags, List<String> keywords) {
        String header = insertKeywords(faker.book().title(), keywords);
        String summary = insertKeywords(faker.lorem().sentence(), keywords);
        String body = insertKeywords(faker.lorem().paragraph(10), keywords);

        return new ArticleForm(header, summary, body, status, customTags);
    }

    private static String insertKeywords(String text, List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) return text;
        var sb = new StringBuilder(text);
        for (String keyword : keywords) sb.append(" ").append(keyword);
        return sb.toString();
    }
}