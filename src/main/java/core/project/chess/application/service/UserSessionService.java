package core.project.chess.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import core.project.chess.application.dto.user.Message;
import core.project.chess.application.dto.user.MessageType;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.inbound.InboundUserRepository;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import core.project.chess.infrastructure.utilities.json.JsonUtilities;
import core.project.chess.infrastructure.utilities.web.WSUtilities;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static core.project.chess.application.dto.user.MessageType.PARTNERSHIP_REQUEST;

@ApplicationScoped
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserSessionService {

    private final InboundUserRepository inboundUserRepository;

    private final OutboundUserRepository outboundUserRepository;

    private static final ConcurrentHashMap<Username, Pair<Session, UserAccount>> userSessions = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<Username, Pair<UserAccount, Queue<Message>>> partnershipRequests = new ConcurrentHashMap<>();

    public void handleOnOpen(Session session, Username username) {
        Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(username);
        if (!result.success()) {
            WSUtilities.sendMessage(session, "This account is do not founded.");
            return;
        }

        userSessions.put(username, Pair.of(session, result.value()));
        CompletableFuture.runAsync(() -> messages(session, username));
    }

    public void handleOnMessage(Session session, Username username, String message) {
        final Pair<Session, UserAccount> sessionAndUserAccount = userSessions.get(username);

        final Result<MessageType, Throwable> messageType = JsonUtilities.messageType(message);
        if (!messageType.success()) {
            WSUtilities.sendMessage(session, "Invalid message type.");
            return;
        }

        final Result<JsonNode, Throwable> messageNode = JsonUtilities.jsonTree(message);
        if (!messageNode.success()) {
            WSUtilities.sendMessage(session, "Invalid message.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            Log.debugf("Handling %s for user {%s}", messageType, username.username());
            handleWebSocketMessage(messageNode.value(), messageType.value(), sessionAndUserAccount.getFirst(), sessionAndUserAccount.getSecond());
        });
    }

    public void handleOnClose(Session session, Username username) {
        userSessions.remove(username);
        WSUtilities.closeSession(session, "Session is closed.");
    }

    private void messages(Session session, Username username) {
        partnershipRequests.get(username).getSecond().forEach(message -> WSUtilities.sendMessage(session, message.message()));
    }

    private void handleWebSocketMessage(JsonNode messageNode, MessageType type, Session session, UserAccount user) {
        final Result<String, Throwable> message = JsonUtilities.message(messageNode);
        if (!message.success()) {
            WSUtilities.sendMessage(session, "Message can`t be null.");
            return;
        }

        final Result<Username, Throwable> username = JsonUtilities.usernameOfPartner(messageNode);
        if (!username.success()) {
            WSUtilities.sendMessage(session, "Invalid partner username.");
            return;
        }

        if (type.equals(PARTNERSHIP_REQUEST)) {
            partnershipRequest(session, user, message.value(), username.value());
            return;
        }

        WSUtilities.sendMessage(session, "Invalid message type.");
    }

    private void partnershipRequest(Session session, UserAccount user, String message, Username usernameOfPartner) {
        if (userSessions.containsKey(usernameOfPartner)) {
            processPartnershipRequest(userSessions.get(usernameOfPartner), new Message(message), Pair.of(session, user));
            return;
        }

        processPartnershipRequest(usernameOfPartner, new Message(message), Pair.of(session, user));
    }

    private void processPartnershipRequest(final Username partner, final Message message, final Pair<Session, UserAccount> firstUserPair) {
        final Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(partner);
        if (!result.success()) {
            WSUtilities.sendMessage(firstUserPair.getFirst(), "This account is not exists.");
            return;
        }

        final UserAccount firstUser = firstUserPair.getSecond();
        final UserAccount secondUser = result.value();

        firstUser.addPartner(secondUser);
        final boolean partnershipCreated = firstUser.getPartners().contains(secondUser);
        if (partnershipCreated) {
            WSUtilities.sendMessage(firstUserPair.getFirst(), successfullyAddedPartnershipMessage(firstUser, secondUser));
            partnershipRequests.computeIfAbsent(partner, k -> Pair.of(secondUser, new LinkedList<>())).getSecond().add(message);

            inboundUserRepository.addPartnership(firstUser, secondUser);
            return;
        }

        WSUtilities.sendMessage(firstUserPair.getFirst(), "Wait for user %s answer.".formatted(partner.username()));
        partnershipRequests.computeIfAbsent(partner, k -> Pair.of(secondUser, new LinkedList<>())).getSecond().add(message);
    }

    private void processPartnershipRequest(final Pair<Session, UserAccount> secondUserPair,
                                           final Message message, final Pair<Session, UserAccount> firstUserPair) {

        final UserAccount firstUser = firstUserPair.getSecond();
        final UserAccount secondUser = secondUserPair.getSecond();

        firstUser.addPartner(secondUser);
        if (firstUser.getPartners().contains(secondUser)) {
            WSUtilities.sendMessage(firstUserPair.getFirst(), successfullyAddedPartnershipMessage(firstUser, secondUser));
            WSUtilities.sendMessage(secondUserPair.getFirst(), successfullyAddedPartnershipMessage(secondUser, firstUser));
            inboundUserRepository.addPartnership(firstUser, secondUser);
            return;
        }

        WSUtilities.sendMessage(firstUserPair.getFirst(), "Wait for user answer.");
        WSUtilities.sendMessage(secondUserPair.getFirst(), invitationMessage(message.message(), firstUser));
    }

    private static String invitationMessage(String message, UserAccount firstUser) {
        return "User %s invite you for partnership {%s}.".formatted(firstUser.getUsername().username(), message);
    }

    private static String successfullyAddedPartnershipMessage(UserAccount firstUser, UserAccount secondUser) {
        return "Partnership {%s - %s} successfully added.".formatted(firstUser.getUsername().username(), secondUser.getUsername().username());
    }
}
