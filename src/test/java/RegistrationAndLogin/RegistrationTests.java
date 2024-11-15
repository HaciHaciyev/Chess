package RegistrationAndLogin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import testUtils.UserDBManagement;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;


@QuarkusTest
class RegistrationTests {

    public static final String REGISTRATION = "/chessland/account/registration";
    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserDBManagement dbManagement;

    @AfterEach
    void purge() {
        dbManagement.removeUsers();
    }

    @DisplayName("Registration with valid data")
    @RepeatedTest(10)
    void registration_With_Valid_Data() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .body(containsString("successful"));
    }

    @DisplayName("Registration of existing user")
    @RepeatedTest(10)
    void registration_Of_Existing_User() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .body(containsString("successful"));

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(400)
                .body(containsString("already exists"));
    }

    @DisplayName("Registration of user with existing email")
    @RepeatedTest(10)
    void registration_Of_Existing_Email() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .body(containsString("successful"));

        account = RegistrationForm.randomForm().withEmail(account.email());
        accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .body(containsString("already exists"));
    }

    @ParameterizedTest
    @DisplayName("Registration with invalid username")
    @NullAndEmptySource
    @ValueSource(strings = {"aaaabbbbccccddddeeeeffffgggghhhh", "!@#$%^&hello", "Mike Mike", "   user    "})
    void registration_With_Invalid_Username(String username) throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm().withUsername(username);
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(400)
                .body(containsString("Invalid username"));
    }

    @ParameterizedTest
    @DisplayName("Registration with invalid email")
    @NullAndEmptySource
    @ValueSource(strings = {"something", "something@@gmail.com", "so me thing@gmgm.com", "something@.com"})
    void registration_With_Invalid_Email(String email) throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm().withEmail(email);
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(400)
                .body(containsString("Invalid email"));
    }

    @ParameterizedTest
    @DisplayName("Registration with invalid password")
    @NullAndEmptySource
    @ValueSource(strings = {"tiny"})
    void registration_With_Invalid_Password(String password) throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm().withPassword(password).withPasswordConfirmation(password);
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(400)
                .log().all()
                .body(containsString("Invalid password"));

        account = account.withPasswordConfirmation("something");
        accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(400)
                .body(containsString("don`t match"));
    }
}
