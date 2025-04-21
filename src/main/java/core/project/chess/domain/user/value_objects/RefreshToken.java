package core.project.chess.domain.user.value_objects;

import java.util.UUID;

public record RefreshToken(UUID userID, String refreshToken) {

    public RefreshToken {
        if (userID == null) throw new IllegalArgumentException("User id is null");
        if (refreshToken == null) throw new IllegalArgumentException("Refresh token is null");
    }
}