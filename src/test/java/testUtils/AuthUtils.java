package testUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.util.Map;

import static core.project.chess.application.controller.http.LoginTests.LOGIN;
import static core.project.chess.application.controller.http.LoginTests.TOKEN_VERIFICATION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@ApplicationScoped
public class AuthUtils {

    int count = 5;

    public static final String REGISTRATION = "/chessland/account/registration";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserDBManagement dbManagement;

    public AuthInfo fullLoginProcess() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(200);

        String emailConfirmationToken = dbManagement.getToken(account.username());

        given().queryParam("token", emailConfirmationToken)
                .when().patch(TOKEN_VERIFICATION)
                .then()
                .statusCode(200)
                .body(containsString("Now, account is enabled."));

        LoginForm loginForm = LoginForm.from(account);
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        Map<String, String> response = given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(200)
                .body("token", notNullValue(), "refreshToken", notNullValue())
                .extract()
                .body()
                .as(new TypeRef<Map<String, String>>() {
                });

        return new AuthInfo(account.username(), response);
    }

    public AuthInfo fullDistinctLoginProcess() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        Response registrationResponse = given().contentType("application/json")
                .body(accountJSON)
                .when()
                .post(REGISTRATION);

        if (registrationResponse.getStatusCode() != 200) {
            ResponseBody body = registrationResponse.getBody();
            if (!body.asString().equals("Username already exists.") && !body.asString().equals("Email already exists.")) {
                throw new AssertionError("Invalid registration.");
            }

            if (count == 0) {
                throw new AssertionError("Could`t register after 5 attempts.");
            }
            count--;
            return fullDistinctLoginProcess();
        }

        String emailConfirmationToken = dbManagement.getToken(account.username());

        given().queryParam("token", emailConfirmationToken)
                .when().patch(TOKEN_VERIFICATION)
                .then()
                .statusCode(200)
                .body(containsString("Now, account is enabled."));

        LoginForm loginForm = LoginForm.from(account);
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        Map<String, String> response = given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(200)
                .body("token", notNullValue(), "refreshToken", notNullValue())
                .extract()
                .body()
                .as(new TypeRef<Map<String, String>>() {
                });

        return new AuthInfo(account.username(), response);
    }

    public void enableAccount(RegistrationForm account) {
        String emailConfirmationToken = dbManagement.getToken(account.username());

        given().queryParam("token", emailConfirmationToken)
                .when().patch(TOKEN_VERIFICATION)
                .then()
                .statusCode(200)
                .body(containsString("account is enabled"));
    }

    public String login(RegistrationForm account) throws JsonProcessingException {
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
                .jsonPath()
                .get("token")
                .toString();
    }

    public RegistrationForm registerRandom() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(200)
                .body(containsString("successful"));

        return account;
    }

    public URI serverURIWithToken(URI uri, String token) {
        return URI.create(uri + "?token=%s".formatted(token));
    }
}
