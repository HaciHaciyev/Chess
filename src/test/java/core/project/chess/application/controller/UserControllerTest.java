package core.project.chess.application.controller;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@Transactional
class UserControllerTest {

    @Test
    void login() {
        final String token = given()
                .contentType("application/json")
                .body("""
                        {
                          "username": "HHadzhy",
                          "password": "hhadzhy72"
                        }
                        """)
                .when().post("/chessland/account/login")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Log.info(token);
    }

    @Test
    void registration() {
        final String token = given()
                .contentType("application/json")
                .body("""
                        {
                            "username":"Test",
                            "email":"test@email.com",
                            "password":"password",
                            "passwordConfirmation":"password"
                        }
                        """)
                .when().post("/chessland/account/registration")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Log.info(token);
    }
}