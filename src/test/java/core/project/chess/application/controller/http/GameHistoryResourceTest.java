package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;

import static io.restassured.RestAssured.given;

@QuarkusTest
class GameHistoryResourceTest {

    @Inject
    AuthUtils authUtils;

    @Test
    void gameHistory() throws JsonProcessingException {
        String token = authUtils.fullLoginProcess().serverResponse().get("token");

        // TODO AinGrace create a function that will fill db with completed games

        given().contentType("application/json")
                .param("pageNumber", 0)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("chessland/account/game-history")
                .then()
                .statusCode(200);
    }
}