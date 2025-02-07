package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import testUtils.AuthUtils;
import testUtils.LoginForm;
import testUtils.RegistrationForm;
import testUtils.UserDBManagement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
public class LoginTests {

    public static final String LOGIN = "/chessland/account/login/";
    public static final String TOKEN_VERIFICATION = "/chessland/account/token/verification";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserDBManagement dbManagement;

    @Inject
    AuthUtils authUtils;

    @AfterEach
    void purge() {
        dbManagement.removeUsers();
    }

    @RepeatedTest(3)
    @DisplayName("Login existing user")
    void login_Existing_User() throws JsonProcessingException {
        authUtils.fullLoginProcess();
    }

    @Test
    @DisplayName("Refresh-Token test")
    void refreshToken() throws JsonProcessingException {
        String refreshToken = authUtils.fullLoginProcess().serverResponse().get("refreshToken");

        given().contentType("application/json")
                .header("Refresh-Token", refreshToken)
                .when()
                .patch("chessland/account/refresh-token")
                .then()
                .statusCode(200);
    }

    @RepeatedTest(3)
    @DisplayName("Login none existent user")
    void login_None_Existent_User() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        LoginForm loginForm = LoginForm.from(account);
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(400)
                .body(containsString("%s not found".formatted(loginForm.username())));
    }

    @RepeatedTest(3)
    @DisplayName("Login non enabled user")
    void login_Non_Enabled_User() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(RegistrationTests.REGISTRATION)
                .then()
                .statusCode(200)
                .body(containsString("successful"));

        LoginForm loginForm = LoginForm.from(account);
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(400)
                .body(containsString("account is not enabled"));
    }

    @RepeatedTest(3)
    @DisplayName("Login wrong password")
    void login_Wrong_Password() throws JsonProcessingException {
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
                .body(containsString("account is enabled"));

        LoginForm loginForm = LoginForm.from(account.withPassword("somethingElse"));
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(400)
                .body(containsString("Invalid password"));
    }

    @RepeatedTest(3)
    @DisplayName("Login wrong username")
    void login_Wrong_Username() throws JsonProcessingException {
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
                .body(containsString("account is enabled"));

        LoginForm loginForm = LoginForm.from(account.withUsername("somethingElse"));
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(400)
                .body(containsString("%s not found".formatted(loginForm.username())));
    }
}