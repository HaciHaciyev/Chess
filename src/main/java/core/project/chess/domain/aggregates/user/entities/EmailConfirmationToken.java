package core.project.chess.domain.aggregates.user.entities;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmailConfirmationToken(
        @NotNull UUID tokenId,
        @NotNull UUID token,
        @NotNull LocalDateTime creationDate,
        @NotNull /**OneToOne*/ UserAccount userAccount
) {}
