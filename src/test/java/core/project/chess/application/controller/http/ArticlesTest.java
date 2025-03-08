package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.controller.ws.MessagingTestResource;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.datafaker.Faker;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testUtils.ArticleForm;
import testUtils.ArticleStatus;
import testUtils.AuthInfo;
import testUtils.AuthUtils;

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

    static final String messagingURL = ConfigProvider.getConfig().getConfigValue("messaging.api.url").getValue() + "/chessland/articles";

    @Test
    @DisplayName("Test valid article saving")
    void save() throws JsonProcessingException {
        int i = 0;
        while (i < 13) {
            AuthInfo user = authUtils.fullLoginProcess();
            String articleFormJSON = objectMapper.writeValueAsString(articleForm());

            given()
                    .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                    .body(articleFormJSON)
                    .contentType("application/json")
                    .when()
                    .post(messagingURL + "/post")
                    .then()
                    .statusCode(200);

            i++;
        }
    }

    @Test
    @DisplayName("Test invalid article saving: Article can`t be created with status - Archived")
    void invalidArticleSave() throws JsonProcessingException {
        AuthInfo user = authUtils.fullLoginProcess();
        String articleFormJSON = objectMapper.writeValueAsString(articleForm(ArticleStatus.ARCHIVED));

        given()
                .header("Authorization", "Bearer " + user.serverResponse().get("token"))
                .body(articleFormJSON)
                .contentType("application/json")
                .when()
                .post(messagingURL + "/post")
                .then()
                .statusCode(400)
                .body(containsString("Article can`t be created with archived status."));
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
}