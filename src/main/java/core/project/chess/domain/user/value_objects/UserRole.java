package core.project.chess.domain.user.value_objects;

public enum UserRole {

    ROLE_USER("User"), NONE("None");

    private final String userRole;

    UserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getUserRole() {
        return userRole;
    }
}
