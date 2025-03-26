package core.project.chess.domain.user.value_objects;

/** Password must be hashed*/
public record PersonalData(String firstname, String surname, String username, String email, String password) {

    public PersonalData {
        Firstname.validate(firstname);
        Surname.validate(surname);
        Username.validate(username);
        Email.validate(email);
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }
    }
}
