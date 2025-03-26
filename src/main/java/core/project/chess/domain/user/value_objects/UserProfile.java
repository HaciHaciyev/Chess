package core.project.chess.domain.user.value_objects;

/** Password must be hashed*/
public record UserProfile(String firstname, String surname, String username, String email, String password) {

    public UserProfile {
        Firstname.validate(firstname);
        Surname.validate(surname);
        Username.validate(username);
        Email.validate(email);
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
    }
}
