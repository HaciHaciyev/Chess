package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import testUtils.LoginForm;
import testUtils.RegistrationForm;
import testUtils.UserDBManagement;

import java.util.Map;

import static core.project.chess.application.controller.http.LoginTests.LOGIN;
import static core.project.chess.application.controller.http.LoginTests.TOKEN_VERIFICATION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class UserPropertiesResourceTest {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserDBManagement dbManagement;

    @RepeatedTest(10)
    @DisplayName("User properties endpoint test.")
    void userPropertiesTest() throws JsonProcessingException {
        String token = fullLoginProcess().get("token");
        given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .get("chessland/account/user-properties")
                .then()
                .body(containsString("username"), containsString("email"), containsString("rating"));
    }

    Map<String, String> fullLoginProcess() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(RegistrationTests.REGISTRATION)
                .then()
                .statusCode(200)
                .body(containsString("successful"));

        String emailConfirmationToken = dbManagement.getToken(account.username());

        given().queryParam("token", emailConfirmationToken)
                .when().patch(TOKEN_VERIFICATION)
                .then()
                .statusCode(200)
                .body(containsString("Now, account is enabled."));

        LoginForm loginForm = LoginForm.from(account);
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        return given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(200)
                .body("token", notNullValue(), "refreshToken", notNullValue())
                .extract()
                .body()
                .as(new TypeRef<Map<String, String>>() {});
    }
}
