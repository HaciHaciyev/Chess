package core.project.chess.application.service;

import core.project.chess.application.dto.user.MessageType;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;

@ApplicationScoped
public class UserService {

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    UserService(InboundUserRepository inboundUserRepository, OutboundUserRepository outboundUserRepository) {
        this.inboundUserRepository = inboundUserRepository;
        this.outboundUserRepository = outboundUserRepository;
    }

    public void handlePartnershipRequest(final Username partner, final Pair<Session, UserAccount> sessionAndUserAccount) {
        final Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(partner);
        if (!result.success()) {
            sendMessage(sessionAndUserAccount.getFirst(), "This account is not exists.");
            return;
        }

        final UserAccount firstUser = sessionAndUserAccount.getSecond();
        final UserAccount secondUser = result.value();

        firstUser.addPartner(secondUser);

        if (firstUser.getPartners().contains(secondUser)) {
            sendMessage(sessionAndUserAccount.getFirst(), "Partner successfully added.");

            inboundUserRepository.addPartnership(firstUser, secondUser);
        } else {
            sendMessage(sessionAndUserAccount.getFirst(), "Wait for user answer.");
        }
    }

    private void sendMessage(final Session session, final String message) {
        try {
            session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }
}
