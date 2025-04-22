package core.project.chess.application.controller.ws;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Disabled;

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
