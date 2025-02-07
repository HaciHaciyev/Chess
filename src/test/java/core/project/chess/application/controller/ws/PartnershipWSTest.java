package core.project.chess.application.controller.ws;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.infrastructure.security.JwtUtility;
import io.quarkus.logging.Log;
import jakarta.inject.Inject;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.Session;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;
import testUtils.WSClient;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@WithTestResource(MessagingTestResource.class)
@Disabled
@Deprecated
class PartnershipWSTest {
//
//    @Inject
//    AuthUtils authUtils;
//
//    @Inject
//    JwtUtility jwtUtility;
//
//    URI userSessionURI;
//
//    private final Messages userMessages = Messages.newInstance();
//
//    @BeforeEach
//    void printLineBreak() {
//        String messagingURI = ConfigProvider.getConfig().getValue("messaging.api.url", String.class);
//        userSessionURI = URI.create(messagingURI);
//        System.out.println();
//        System.out.println("---------------------------------------BREAK---------------------------------------");
//        System.out.println();
//    }
//
//    @Test
//    void partnershipAcceptedWhenBothConnected() throws Exception {
//        String addresserToken = authUtils.fullLoginProcess().get("token");
//        String addresseeToken = authUtils.fullLoginProcess().get("token");
//
//        try (Session addresserSession = ContainerProvider
//                .getWebSocketContainer()
//                .connectToServer(
//                        WSClient.class, authUtils.serverURIWithToken(userSessionURI, addresserToken)
//                );
//
//             Session addresseeSession = ContainerProvider
//                .getWebSocketContainer()
//                .connectToServer(
//                        ChessWSTest.WSClient.class, authUtils.serverURIWithToken(userSessionURI, addresseeToken)
//                )
//        ) {
//            String addresser = jwtUtility
//                    .extractJWT(addresserSession)
//                    .orElseThrow()
//                    .getName();
//
//            String addressee = jwtUtility
//                    .extractJWT(addresseeSession)
//                    .orElseThrow()
//                    .getName();
//
//            addresserSession.addMessageHandler(Message.class, message -> {
//                Log.infof("Received Message: %s, from user %s.", message, addressee);
//                userMessages.user1().offer(message);
//            });
//
//            addresseeSession.addMessageHandler(Message.class, message -> {
//                Log.infof("Received Message: %s, from user %s.", message, addresser);
//                userMessages.user2().offer(message);
//            });
//
//            Thread.sleep(Duration.ofSeconds(1).toMillis());
//
//            sendMessage(addresserSession, addresser, Message.builder(MessageType.PARTNERSHIP_REQUEST)
//                    .partner(addressee)
//                    .message("Hello! I would be glad to establish contact with you.")
//                    .build());
//
//            await().atMost(Duration.ofSeconds(5)).until(() -> !userMessages.user2().isEmpty());
//
//            sendMessage(addresseeSession, addressee, Message.builder(MessageType.PARTNERSHIP_REQUEST)
//                    .partner(addresser)
//                    .message("Hi")
//                    .build());
//
//            await().atMost(Duration.ofSeconds(5)).until(() -> !userMessages.user1().isEmpty());
//
//            assertThat(userMessages.user1()).anyMatch(message -> message.type().equals(MessageType.USER_INFO) &&
//                    message.message().contains("Partnership") &&
//                    message.message().contains(addressee) &&
//                    message.message().contains("successfully added"));
//
//            assertThat(userMessages.user2()).anyMatch(message -> message.type().equals(MessageType.USER_INFO) &&
//                    message.message().contains("Partnership") &&
//                    message.message().contains(addresser) &&
//                    message.message().contains("successfully added"));
//        }
//    }
//
//    @Test
//    void partnershipDeclinedWhenBothConnected() {
//
//    }
//
//    @Test
//    void partnershipAcceptedOnReconnect() {
//
//    }
//
//    @Test
//    void partnershipDeclinedOnReconnect() {
//
//    }
}
