package testUtils;

import net.datafaker.Faker;

public record RegistrationForm(String username, String email, String password, String passwordConfirmation) {

    private static final Faker faker = new Faker();

    static RegistrationForm defaultForm() {
        return new RegistrationForm(
                "username",
                "useremail@gmail.com",
                "12345678",
                "12345678"
        );
    }

    public static RegistrationForm randomForm() {
        String password = faker.internet().password();
        return new RegistrationForm(
                faker.name().firstName(),
                faker.internet().emailAddress(),
                password,
                password
        );
    }

    public RegistrationForm withUsername(String username) {
        return new RegistrationForm(
                username,
                email(),
                password(),
                passwordConfirmation()
        );
    }

    public RegistrationForm withEmail(String email) {
        return new RegistrationForm(
                username(),
                email,
                password(),
                passwordConfirmation()
        );
    }

    public RegistrationForm withPassword(String password) {
        return new RegistrationForm(
                username(),
                email(),
                password,
                passwordConfirmation()
        );
    }

    public RegistrationForm withPasswordConfirmation(String passwordConfirmation) {
        return new RegistrationForm(
                username(),
                email(),
                password(),
                passwordConfirmation
        );
    }
}