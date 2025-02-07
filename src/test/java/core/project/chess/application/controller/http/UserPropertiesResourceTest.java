package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import testUtils.AuthUtils;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class UserPropertiesResourceTest {

    @Inject
    AuthUtils authUtils;

    @RepeatedTest(3)
    @DisplayName("User properties endpoint test.")
    void userPropertiesTest() throws JsonProcessingException {
        String token = authUtils.fullLoginProcess().serverResponse().get("token");
        given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .get("chessland/account/user-properties")
                .then()
                .body(containsString("username"), containsString("email"), containsString("rating"));
    }
}
