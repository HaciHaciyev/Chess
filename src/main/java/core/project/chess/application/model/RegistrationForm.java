package core.project.chess.application.model;

public record RegistrationForm(String username, String email,
                               String password, String passwordConfirmation) {
}
