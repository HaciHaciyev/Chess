package core.project.chess.application.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.dto.user.Message;
import core.project.chess.application.dto.user.MessageType;
import core.project.chess.application.service.UserSessionService;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import core.project.chess.domain.aggregates.user.value_objects.Username;
import core.project.chess.domain.repositories.outbound.OutboundUserRepository;
import core.project.chess.infrastructure.config.security.JwtUtility;
import core.project.chess.infrastructure.utilities.containers.Pair;
import core.project.chess.infrastructure.utilities.containers.Result;
import io.quarkus.logging.Log;
import jakarta.transaction.Transactional;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/user-session")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class UserSessionHandler {

    private final JwtUtility jwtUtility;

    private final ObjectMapper objectMapper;

    private final UserSessionService userSessionService;

    private final OutboundUserRepository outboundUserRepository;

    private static final ConcurrentHashMap<Username, Pair<Session, UserAccount>> userSessions = new ConcurrentHashMap<>();

    @OnOpen
    public final void onOpen(Session session) {
        final Username username = new Username(jwtUtility.extractJWT(session).getName());
        Result<UserAccount, Throwable> result = outboundUserRepository.findByUsername(username);
        if (!result.success()) {
            sendMessage(session, "This account is do not founded.");
            return;
        }

        userSessions.put(username, Pair.of(session, result.value()));
        CompletableFuture.runAsync(() -> sendReceivedMessagesForUsers(username, session));
    }

    private void sendReceivedMessagesForUsers(final Username username, final Session session) {
        userSessionService.partnershipRequest(session, username);
    }

    @OnMessage
    public final void onMessage(Session session, String message) {
        if (Objects.isNull(message) || message.isBlank()) {
            sendMessage(session, "Message is required.");
            return;
        }

        final Username username = new Username(jwtUtility.extractJWT(session).getName());
        final Pair<Session, UserAccount> sessionAndUserAccount = userSessions.get(username);

        final JsonNode jsonNode = getJsonTree(message);
        final MessageType type = getMessageType(jsonNode);

        CompletableFuture.runAsync(() -> {
            Log.debugf("Handling %s for user {%s}", type, username.username());
            handleWebSocketMessage(jsonNode, type, sessionAndUserAccount);
        });
    }

    @Transactional
    public void handleWebSocketMessage(final JsonNode jsonNode, final MessageType type, final Pair<Session, UserAccount> sessionAndUserAccount) {
        try {
            final String message = Objects.requireNonNull(jsonNode.get("message").asText());
            final Username secondUser = new Username(Objects.requireNonNull(jsonNode.get("username")).asText());

            final boolean partnershipRequest = Objects.requireNonNull(type).equals(MessageType.PARTNERSHIP_REQUEST);
            if (partnershipRequest) {
                if (userSessions.containsKey(secondUser)) {
                    userSessionService.handlePartnershipRequest(userSessions.get(secondUser), new Message(message), sessionAndUserAccount);
                    return;
                }

                userSessionService.handlePartnershipRequest(secondUser, new Message(message), sessionAndUserAccount);
                return;
            }

            sendMessage(sessionAndUserAccount.getFirst(), "Invalid message type.");
        } catch (NullPointerException e) {
            Log.error(e);
            sendMessage(sessionAndUserAccount.getFirst(), "Invalid message.");
        }
    }

    @OnClose
    public final void onClose(Session session) {
        userSessions.remove(new Username(jwtUtility.extractJWT(session).getName()));
        closeSession(session, "Session is closed.");
    }

    private void closeSession(final Session currentSession, final String message) {
        try {
            currentSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, message));
        } catch (Exception e) {
            Log.error(e.getMessage(), e);
        }
    }

    private void sendMessage(final Session session, final String message) {
        try {
            session.getAsyncRemote().sendText(message);
        } catch (Exception e) {
            Log.info(e.getMessage());
        }
    }

    private JsonNode getJsonTree(final String message) {
        try {
            return objectMapper.readTree(message);
        } catch (JsonProcessingException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request.").build());
        }
    }

    private MessageType getMessageType(final JsonNode node) {
        try {
            return MessageType.valueOf(node.get("type").asText());
        } catch (IllegalArgumentException e) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("Invalid JSON request. Invalid Message Type.").build());
        }
    }
}
