package testUtils;

public record LoginForm(String username, String password) {
        public static LoginForm from(RegistrationForm registrationForm) {
            return new LoginForm(
                    registrationForm.username(),
                    registrationForm.password()
            );
        }
    }
