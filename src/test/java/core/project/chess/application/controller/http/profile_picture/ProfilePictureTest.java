package core.project.chess.application.controller.http.profile_picture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import testUtils.ByteArrayToImageConsole;
import testUtils.UserDBManagement;

import java.io.File;
import java.io.IOException;

import static core.project.chess.application.controller.http.RegistrationAndLogin.LoginTests.TOKEN_VERIFICATION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@Transactional
class ProfilePictureTest {

    static final String PUT_PROFILE_PICTURE = "/chessland/account/put-profile-picture";

    static final String GET_PROFILE_PICTURE = "/chessland/account/profile-picture";

    static final String DELETE_PROFILE_PICTURE = "/chessland/account/delete-profile-picture";

    @Inject
    UserDBManagement userDBManagement;

    @Test
    void profilePicture() throws IOException {
        registrationLoad();

        String emailConfirmationToken = userDBManagement.getToken("HHadzhy");
        Log.infof("Email confirmation token: %s.", emailConfirmationToken);

        given().queryParam("token", emailConfirmationToken)
                .when().patch(TOKEN_VERIFICATION)
                .then()
                .statusCode(200)
                .body(containsString("Now, account is enabled."));

        final String token = extractToken(loginLoad());

        // GET
        getUserProfilePicture(token);

        // PUT
        final File tacoImage = new File("src/main/resources/static/profile/photos/others/TacoCloud.jpg");
        putUserProfilePicture(token, tacoImage);

        // GET
        getUserProfilePicture(token);

        // PUT
        final File picture = new File("src/main/resources/static/profile/photos/others/democracy.png");
        putUserProfilePicture(token, picture);

        // GET
        getUserProfilePicture(token);

        // DELETE
        deleteUserProfilePicture(token);
    }

    private static String loginLoad() {
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
        return token;
    }

    private static void registrationLoad() {
        given()
                .contentType("application/json")
                .body("""
                      {
                        "username":"HHadzhy",
                        "email":"hhadzhy@email.com",
                        "password":"hhadzhy72",
                        "passwordConfirmation":"hhadzhy72"
                      }
                      """)
                .when().post("/chessland/account/registration")
                .then()
                .statusCode(200);

        Log.infof("Registered account.");
    }

    private static void deleteUserProfilePicture(String token) {
        Log.info("Profile picture test: DELETE");
        given()
                .header("Authorization", "Bearer " + token)
                .delete(DELETE_PROFILE_PICTURE)
                .then()
                .statusCode(202);
    }

    private static void putUserProfilePicture(String token, File picture) {
        Log.info("Profile picture test: PUT");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(picture)
                .when()
                .put(PUT_PROFILE_PICTURE)
                .then()
                .statusCode(202);
        Log.info("Successfully upload image.");
    }

    private static void getUserProfilePicture(String token) throws IOException {
        Log.info("Profile picture test: GET");
        String response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(GET_PROFILE_PICTURE)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(response);

        byte[] bytes = node.get("profilePicture").binaryValue();
        ByteArrayToImageConsole.renderImage(bytes);
    }

    static String extractToken(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(json);
            return rootNode.get("token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract token from JSON: " + e.getMessage(), e);
        }
    }
}