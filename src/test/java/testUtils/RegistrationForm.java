package testUtils;

import core.project.chess.domain.user.value_objects.Firstname;
import core.project.chess.domain.user.value_objects.Surname;
import core.project.chess.infrastructure.utilities.containers.Result;
import net.datafaker.Faker;
import net.datafaker.providers.base.Name;

public record RegistrationForm(String firstname, String surname, String username,
                               String email, String password, String passwordConfirmation) {

    private static final Faker faker = new Faker();

    public static RegistrationForm randomForm() {
        String password = faker.internet().password();

        if (password.length() >= 64) {
            password = password.substring(0, 63);
        }

        if (password.length() < 8) {
            password = password + password;
        }

        Name name = faker.name();
        return new RegistrationForm(
                generateFirstname(),
                generateSurname(),
                name.firstName(),
                faker.internet().emailAddress(),
                password,
                password
        );
    }

    public RegistrationForm withUsername(String username) {
        Name name = faker.name();
        return new RegistrationForm(
                generateFirstname(),
                generateSurname(),
                username,
                email(),
                password(),
                passwordConfirmation()
        );
    }

    public RegistrationForm withEmail(String email) {
        Name name = faker.name();
        return new RegistrationForm(
                generateFirstname(),
                generateSurname(),
                username(),
                email,
                password(),
                passwordConfirmation()
        );
    }

    public RegistrationForm withPassword(String password) {
        Name name = faker.name();
        return new RegistrationForm(
                generateFirstname(),
                generateSurname(),
                username(),
                email(),
                password,
                passwordConfirmation()
        );
    }

    public RegistrationForm withPasswordConfirmation(String passwordConfirmation) {
        Name name = faker.name();
        return new RegistrationForm(
                generateFirstname(),
                generateSurname(),
                username(),
                email(),
                password(),
                passwordConfirmation
        );
    }

    private static String generateSurname() {
        while (true) {
            Result<Surname, Throwable> surnameResult = Result.ofThrowable(() -> new Surname(faker.name().firstName()));
            if (!surnameResult.success()) {
                continue;
            }

            return surnameResult.value().surname();
        }
    }

    private static String generateFirstname() {
        while (true) {
            Result<Firstname, Throwable> firstnameResult = Result.ofThrowable(() -> new Firstname(faker.name().lastName()));
            if (!firstnameResult.success()) {
                continue;
            }

            return firstnameResult.value().firstname();
        }
    }
}