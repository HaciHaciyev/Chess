package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;

import static io.restassured.RestAssured.given;

class PartnersResourceTest {

    @Inject
    AuthUtils authUtils;

    @Test
    void partners() throws JsonProcessingException {
        String token = authUtils.fullLoginProcess().get("token");

        // TODO AinGrace write a function for fill db with user partners

        given().contentType("application/json")
                .param("pageNumber", 0)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("chessland/account/partners")
                .then()
                .statusCode(200);
    }
}