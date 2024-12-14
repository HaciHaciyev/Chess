package core.project.chess.domain.subdomains.user.value_objects;

import lombok.Getter;

@Getter
public enum UserRole {

    ROLE_USER("User"), NONE("None");

    private final String userRole;

    UserRole(String userRole) {
        this.userRole = userRole;
    }
}
