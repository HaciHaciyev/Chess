package core.project.chess.domain.aggregates.user.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailConfirmationToken(
        UUID tokenId, UUID token,
        LocalDateTime creationDate,
        /**OneToOne*/ UserAccount userAccount
) {}
