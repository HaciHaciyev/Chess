package testUtils;

import io.restassured.common.mapper.TypeRef;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;

import static core.project.chess.application.controller.http.LoginTests.LOGIN;
import static core.project.chess.application.controller.http.LoginTests.TOKEN_VERIFICATION;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@ApplicationScoped
public class AuthUtils {

    public static final String REGISTRATION = "/chessland/account/registration";

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserDBManagement dbManagement;

    public Map<String, String> fullLoginProcess() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
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

    public URI serverURIWithToken(URI uri, String token) {
        return URI.create(uri + "?token=%s".formatted(token));
    }
}
