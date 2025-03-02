package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;

import static io.restassured.RestAssured.given;

@QuarkusTest
@Disabled("For technical purpose. Need to fill the database with puzzles.")
class PuzzlesQueryTest {

    @Inject
    AuthUtils authUtils;

    @Test
    void puzzlesTest() throws JsonProcessingException {
        String token = authUtils.fullLoginProcess().serverResponse().get("token");

        String response = given()
                .header("Authorization", "Bearer " + token)
                .param("pageNumber", 0)
                .param("pageSize", 1)
                .when()
                .get("chessland/puzzles/page")
                .then()
                .extract().response().asString();

        Log.infof("Puzzles page: %s", response);
    }
}
