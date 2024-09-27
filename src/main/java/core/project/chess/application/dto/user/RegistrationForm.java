package core.project.chess.application.dto.user;

public record RegistrationForm(String username, String email,
                               String password, String passwordConfirmation) {
}
