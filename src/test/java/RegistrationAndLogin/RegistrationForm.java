package RegistrationAndLogin;

import net.datafaker.Faker;

record RegistrationForm(String username, String email, String password, String passwordConfirmation) {

    private static final Faker faker = new Faker();

    static RegistrationForm defaultForm() {
        return new RegistrationForm(
                "username",
                "useremail@gmail.com",
                "12345678",
                "12345678"
        );
    }

    static RegistrationForm randomForm() {
        String password = faker.internet().password();
        return new RegistrationForm(
                faker.name().firstName(),
                faker.internet().emailAddress(),
                password,
                password
        );
    }

    RegistrationForm withUsername(String username) {
        return new RegistrationForm(
                username,
                email(),
                password(),
                passwordConfirmation()
        );
    }

    RegistrationForm withEmail(String email) {
        return new RegistrationForm(
                username(),
                email,
                password(),
                passwordConfirmation()
        );
    }

    RegistrationForm withPassword(String password) {
        return new RegistrationForm(
                username(),
                email(),
                password,
                passwordConfirmation()
        );
    }

    RegistrationForm withPasswordConfirmation(String passwordConfirmation) {
        return new RegistrationForm(
                username(),
                email(),
                password(),
                passwordConfirmation
        );
    }
}