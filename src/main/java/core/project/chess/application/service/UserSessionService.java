package core.project.chess.application.service;

import core.project.chess.application.dto.user.Message;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserSessionService {

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private static final ConcurrentHashMap<Username, Pair<UserAccount, Queue<Message>>> partnershipRequests = new ConcurrentHashMap<>();

    public void partnershipRequest(final Session session, final Username username) {
        partnershipRequests.get(username).getSecond().forEach(message -> sendMessage(session, message.message()));
    }

    public void handlePartnershipRequest(final Username partner, final Message message,
                                         final Pair<Session, UserAccount> firstUserPair) {

        final Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(partner);
        if (!result.success()) {
            sendMessage(firstUserPair.getFirst(), "This account is not exists.");
            return;
        }

        final UserAccount firstUser = firstUserPair.getSecond();
        final UserAccount secondUser = result.value();

        firstUser.addPartner(secondUser);
        final boolean partnershipCreated = firstUser.getPartners().contains(secondUser);
        if (partnershipCreated) {
            sendMessage(firstUserPair.getFirst(), successfullyAddedPartnershipMessage(firstUser, secondUser));
            partnershipRequests.computeIfAbsent(partner, k -> Pair.of(secondUser, new LinkedList<>())).getSecond().add(message);

            inboundUserRepository.addPartnership(firstUser, secondUser);
            return;
        }

        sendMessage(firstUserPair.getFirst(), "Wait for user %s answer.".formatted(partner.username()));
        partnershipRequests.computeIfAbsent(partner, k -> Pair.of(secondUser, new LinkedList<>())).getSecond().add(message);
    }

    public void handlePartnershipRequest(final Pair<Session, UserAccount> secondUserPair,
                                         final Message message, final Pair<Session, UserAccount> firstUserPair) {

        final UserAccount firstUser = firstUserPair.getSecond();
        final UserAccount secondUser = secondUserPair.getSecond();

        firstUser.addPartner(secondUser);
        if (firstUser.getPartners().contains(secondUser)) {
            sendMessage(firstUserPair.getFirst(), successfullyAddedPartnershipMessage(firstUser, secondUser));
            sendMessage(secondUserPair.getFirst(), successfullyAddedPartnershipMessage(secondUser, firstUser));
            inboundUserRepository.addPartnership(firstUser, secondUser);
            return;
        }

        sendMessage(firstUserPair.getFirst(), "Wait for user answer.");
        sendMessage(secondUserPair.getFirst(), invitationMessage(message.message(), firstUser));
    }

    private static String invitationMessage(String message, UserAccount firstUser) {
        return "User %s invite you for partnership {%s}.".formatted(firstUser.getUsername().username(), message);
    }

    private static String successfullyAddedPartnershipMessage(UserAccount firstUser, UserAccount secondUser) {
        return "Partnership {%s - %s} successfully added.".formatted(firstUser.getUsername().username(), secondUser.getUsername().username());
    }

    private void sendMessage(final Session session, final String message) {
        try {
            session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }
}
