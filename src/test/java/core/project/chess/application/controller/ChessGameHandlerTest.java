package core.project.chess.application.controller;

import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class ChessGameHandlerTest {

    @Test
    void startGame() {
        gameLoad();
    }

    String gameLoad() {
        given()
                .when()
                .header("Authorization", "Bearer %s".formatted(jwt()))
                .post("chessland/chess-game/start-game")
                .then()
                .statusCode(200)
                .body(containsString("Try to find opponent for you."));

        final String gameStartedMessage = given()
                .when()
                .header("Authorization", "Bearer %s".formatted(opponentJwt()))
                .post("chessland/chess-game/start-game")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        Log.info(gameStartedMessage);

        return gameStartedMessage.split("\\{")[1].split("}")[0];
    }

    String jwt() {
        return given()
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
    }

    String opponentJwt() {
        return given()
                .contentType("application/json")
                .body("""
                        {
                          "username": "AinGrace",
                          "password": "aingrace72"
                        }
                        """)
                .when().post("/chessland/account/login")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();
    }
}