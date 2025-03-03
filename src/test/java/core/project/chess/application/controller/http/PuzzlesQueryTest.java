package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.chess.Puzzle;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;

import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@Disabled("For technical purpose. Need to fill the database with puzzles.")
class PuzzlesQueryTest {

    @Inject
    AuthUtils authUtils;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void puzzlesTest() throws JsonProcessingException {
        String token = authUtils.fullLoginProcess().serverResponse().get("token");

        List<Puzzle> listOfPuzzles = objectMapper.readValue(given()
                        .header("Authorization", "Bearer " + token)
                        .param("pageNumber", 0)
                        .param("pageSize", 1)
                        .when()
                        .get("chessland/puzzles/page")
                        .then()
                        .statusCode(200)
                        .assertThat()
                        .body(notNullValue())
                        .extract()
                        .response()
                        .asString(), new TypeReference<List<Puzzle>>() {});

        Log.infof("Puzzles page: %s", listOfPuzzles.toString());

        Puzzle puzzle = objectMapper.readValue(given()
                .header("Authorization", "Bearer " + token)
                .pathParam("id", listOfPuzzles.getFirst().puzzleId().toString())
                .when()
                .get("chessland/puzzles/{id}")
                .then()
                .statusCode(200)
                .assertThat()
                .body(notNullValue())
                .extract()
                .response()
                .asString(), new TypeReference<Puzzle>() {
        });

        assertEquals(puzzle, listOfPuzzles.getFirst());
        Log.infof("Puzzle: %s", puzzle.toString());
    }

    @Test
    void puzzleNotFound() throws JsonProcessingException {
        String token = authUtils.fullLoginProcess().serverResponse().get("token");

        given()
                .header("Authorization", "Bearer " + token)
                .pathParam("id", UUID.randomUUID().toString())
                .when()
                .get("chessland/puzzles/{id}")
                .then()
                .statusCode(404)
                .assertThat()
                .body(equalTo("Puzzle by this id is do not exists."));
    }

    @Test
    void invalidPuzzleID() throws JsonProcessingException {
        String token = authUtils.fullLoginProcess().serverResponse().get("token");

        given()
                .header("Authorization", "Bearer " + token)
                .pathParam("id", "invalid puzzle id")
                .when()
                .get("chessland/puzzles/{id}")
                .then()
                .statusCode(400)
                .assertThat()
                .body(equalTo("Invalid puzzle ID"));
    }
}
