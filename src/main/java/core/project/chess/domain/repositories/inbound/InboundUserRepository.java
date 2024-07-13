package core.project.chess.domain.repositories.inbound;

import core.project.chess.domain.aggregates.user.entities.UserAccount;

public interface InboundUserRepository {

    UserAccount save(UserAccount userAccount);
}
