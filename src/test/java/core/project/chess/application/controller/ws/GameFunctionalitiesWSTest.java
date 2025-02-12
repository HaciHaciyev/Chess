package core.project.chess.application.controller.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import io.quarkus.logging.Log;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;
import testUtils.RegistrationForm;
import testUtils.WSClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static testUtils.WSClient.sendMessage;

@QuarkusTest
@WithTestResource(MessagingTestResource.class)
class GameFunctionalitiesWSTest {

    private final Messages USER_MESSAGES = Messages.newInstance();

    private final Messages CHESS_MESSAGES = Messages.newInstance();

    @TestHTTPResource("/chessland/chess-game")
    URI serverURI;

    URI userSessionURI;

    @Inject
    AuthUtils authUtils;

    private record Messages(LinkedBlockingQueue<Message> user1, LinkedBlockingQueue<Message> user2) {
        public static Messages newInstance() {
            return new Messages(new LinkedBlockingQueue<>(), new LinkedBlockingQueue<>());
        }

        public void clear() {
            user1.clear();
            user2.clear();
        }
    }

    @AfterEach
    void purge() {
        USER_MESSAGES.clear();
        CHESS_MESSAGES.clear();
    }

    @BeforeEach
    void printLineBreak() {
        userSessionURI = URI.create(ConfigProvider.getConfig().getValue("messaging.api.url", String.class) + "/chessland/user-session");

        System.out.println();
        System.out.println("---------------------------------------BREAK---------------------------------------");
        System.out.println();
    }

    @Test
    @DisplayName("Test full game functionalities")
    void testFullGameFunctionalities() throws JsonProcessingException {
        RegistrationForm whiteForm = authUtils.registerRandom();
        authUtils.enableAccount(whiteForm);
        String whiteToken = authUtils.login(whiteForm);

        RegistrationForm blackForm = authUtils.registerRandom();
        authUtils.enableAccount(blackForm);
        String blackToken = authUtils.login(blackForm);

        try (Session wMessagingSession = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, authUtils.serverURIWithToken(userSessionURI, whiteToken));

             Session bMessagingSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, authUtils.serverURIWithToken(userSessionURI, blackToken));

             Session wChessSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, authUtils.serverURIWithToken(serverURI, whiteToken));

             Session bChessSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, authUtils.serverURIWithToken(serverURI, blackToken))
        ) {

            wMessagingSession.addMessageHandler(Message.class, message -> {
                Log.infof("%s received -> %s", whiteForm.username(), message);
                USER_MESSAGES.user1().offer(message);
            });

            bMessagingSession.addMessageHandler(Message.class, message -> {
                Log.infof("%s received -> %s", blackForm.username(), message);
                USER_MESSAGES.user2().offer(message);
            });

            wChessSession.addMessageHandler(Message.class, message -> {
                Log.infof("%s received -> %s", whiteForm.username(), message);
                CHESS_MESSAGES.user1().offer(message);
            });

            bChessSession.addMessageHandler(Message.class, message -> {
                Log.infof("%s received -> %s", blackForm.username(), message);
                CHESS_MESSAGES.user2().offer(message);
            });

            Thread.sleep(Duration.ofSeconds(3));

            addPartnership(blackForm, wMessagingSession, whiteForm, bMessagingSession);

            String wName = whiteForm.username();
            String bName = blackForm.username();

            sendMessage(wChessSession, wName, Message.builder(MessageType.GAME_INIT)
                    .color(Color.WHITE)
                    .partner(bName)
                    .time(ChessGame.Time.RAPID)
                    .build());

            Thread.sleep(Duration.ofSeconds(1));

            sendMessage(bChessSession, bName, Message.builder(MessageType.GAME_INIT)
                    .color(Color.BLACK)
                    .partner(wName)
                    .time(ChessGame.Time.RAPID)
                    .respond(Message.Respond.YES)
                    .build());

            Thread.sleep(Duration.ofSeconds(5));

            String gameID = extractGameID();

            functionalities(wChessSession, wName, gameID, bChessSession, bName);

            wMessagingSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
            bMessagingSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));

            wChessSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
            bChessSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
        } catch (DeploymentException | IOException | InterruptedException e) {
            Log.error("Error when testing game functionalities.", e);
        }
    }

    private void functionalities(Session wChessSession, String wName, String gameID, Session bChessSession, String bName) throws InterruptedException {
        testGameChat(wChessSession, wName, gameID, bChessSession, bName);
        testMoveAndUndo(wChessSession, wName, gameID, bChessSession, bName);
        testAgreement(wChessSession, wName, gameID, bChessSession, bName);
        testThreeFold(wChessSession, wName, bChessSession, bName);
    }

    private void testGameChat(Session wChessSession, String wName, String gameID, Session bChessSession, String bName) {
        String chatMessage1 = "Hello! Have a good game!";
        sendMessage(wChessSession, wName, Message.builder(MessageType.MESSAGE)
                .gameID(gameID)
                .message(chatMessage1)
                .build()
        );
        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user2
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.MESSAGE) && message.message().equals(chatMessage1))
        );

        String chatMessage2 = "Hi";
        sendMessage(bChessSession, bName, Message.builder(MessageType.MESSAGE)
                .gameID(gameID)
                .message(chatMessage2)
                .build()
        );
        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user1
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.MESSAGE) && message.message().equals(chatMessage2))
        );
    }

    private void testMoveAndUndo(Session wSession, String wName, String gameID, Session bSession, String bName) throws InterruptedException {
        //1
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e2, Coordinate.e4));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.e7, Coordinate.e5));
        //2
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f3));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.b8, Coordinate.c6));
        //3
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.b1, Coordinate.c3));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.g8, Coordinate.f6));
        //4
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f1, Coordinate.c4));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.d7, Coordinate.d6));
        //5
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.d2, Coordinate.d3));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.c8, Coordinate.e6));
        //6
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e1, Coordinate.g1));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.e6, Coordinate.c4));
        Log.info("Starting to move undo.");

        // First undo
        sendMessage(bSession, bName, Message.builder(MessageType.RETURN_MOVE).gameID(gameID).build());
        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user1
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.RETURN_MOVE))
        );

        sendMessage(wSession, wName, Message.builder(MessageType.RETURN_MOVE).gameID(gameID).build());
        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user2
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.RETURN_MOVE))
        );

        // Second undo
        Thread.sleep(Duration.ofSeconds(1));
        CHESS_MESSAGES.user1.clear();
        Thread.sleep(Duration.ofSeconds(1));
        CHESS_MESSAGES.user2.clear();
        Thread.sleep(Duration.ofSeconds(1));

        Log.infof("Chess Messages of %s: {%s}", wName, CHESS_MESSAGES.user1.toString());
        Log.infof("Chess Messages of %s: {%s}", bName, CHESS_MESSAGES.user2.toString());
        Thread.sleep(Duration.ofSeconds(1));

        sendMessage(bSession, bName, Message.builder(MessageType.RETURN_MOVE).gameID(gameID).build());
        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user1
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.RETURN_MOVE))
        );

        sendMessage(wSession, wName, Message.builder(MessageType.RETURN_MOVE).gameID(gameID).build());
        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user2
                .stream()
                .anyMatch(message -> {
                    if (message.type().equals(MessageType.FEN_PGN))
                        Log.infof("FEN_PGN message - PGN: %s", message.PGN());
                    return message.type().equals(MessageType.FEN_PGN) &&
                            message.PGN().equals("1. e2-e4 e7-e5 2. Ng1-f3 Nb8-c6 3. Nb1-c3 Ng8-f6 4. Bf1-c4 d7-d6 5. d2-d3 Bc8-e6 ");
                })
        );

        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e1, Coordinate.g1));
    }

    private void testAgreement(Session wChessSession, String wName, String gameID, Session bChessSession, String bName) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(1));

        sendMessage(wChessSession, wName, Message.builder(MessageType.AGREEMENT).gameID(gameID).build());
        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user2
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.AGREEMENT))
        );
        sendMessage(bChessSession, bName, Message.builder(MessageType.AGREEMENT).gameID(gameID).build());

        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user1
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.GAME_ENDED) &&
                        message.gameID().equals(gameID) &&
                        message.message() != null &&
                        (message.message().contains("Game is ended by agreement") || message.message().contains("Game is over by result {DRAW}")))
        );
    }

    private void testThreeFold(Session wSession, String wName, Session bSession, String bName) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(1));

        CHESS_MESSAGES.user1.clear();
        Thread.sleep(Duration.ofSeconds(1));
        CHESS_MESSAGES.user2.clear();
        Thread.sleep(Duration.ofSeconds(1));

        sendMessage(wSession, wName, Message.builder(MessageType.GAME_INIT)
                .color(Color.WHITE)
                .partner(bName)
                .time(ChessGame.Time.RAPID)
                .build());

        Thread.sleep(Duration.ofSeconds(1));

        sendMessage(bSession, bName, Message.builder(MessageType.GAME_INIT)
                .color(Color.BLACK)
                .partner(wName)
                .time(ChessGame.Time.RAPID)
                .respond(Message.Respond.YES)
                .build());

        Thread.sleep(Duration.ofSeconds(5));

        String gameID = extractGameID();

        Thread.sleep(Duration.ofSeconds(1));

        //1
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e2, Coordinate.e4));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.e7, Coordinate.e5));

        //2
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f3));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.b8, Coordinate.c6));

        //3
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.g1));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.c6, Coordinate.b8));

        //4
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f3));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.b8, Coordinate.c6));

        //5
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.g1));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.c6, Coordinate.b8));

        //6
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f3));
        Thread.sleep(Duration.ofSeconds(1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.b8, Coordinate.c6));

        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user1
                .stream()
                .anyMatch(message -> message.isThreeFoldActive() != null && message.isThreeFoldActive())
        );

        sendMessage(bSession, bName, Message.threefold(gameID));

        await().atMost(Duration.ofSeconds(1)).until(() -> CHESS_MESSAGES.user1
                .stream()
                .anyMatch(message -> message.type().equals(MessageType.GAME_ENDED) &&
                        message.gameID().equals(gameID) &&
                        message.message() != null &&
                        (message.message().contains("Game is ended by ThreeFold rule") || message.message().contains("Game is over by result {DRAW}")))
        );
    }

    private void addPartnership(RegistrationForm blackForm, Session wMessagingSession,
                                RegistrationForm whiteForm, Session bMessagingSession) throws InterruptedException {
        Message wPartnershipRequest = Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .partner(blackForm.username())
                .message("br")
                .build();

        sendMessage(wMessagingSession, whiteForm.username(), wPartnershipRequest);

        Thread.sleep(Duration.ofSeconds(3));

        assertThat(USER_MESSAGES.user2().take()).matches(m -> m.type() == MessageType.PARTNERSHIP_REQUEST && m.message().contains("invite you"));

        Message bPartnershipRequest = Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .partner(whiteForm.username())
                .message("brrr")
                .build();

        sendMessage(bMessagingSession, blackForm.username(), bPartnershipRequest);

        Thread.sleep(Duration.ofSeconds(3));

        assertThat(USER_MESSAGES.user1()).anyMatch(m -> m.type() == MessageType.USER_INFO && m.message().contains("successfully added"));
        assertThat(USER_MESSAGES.user2()).anyMatch(m -> m.type() == MessageType.USER_INFO && m.message().contains("successfully added"));
    }

    private String extractGameID() {
        System.out.println("##############################################################");
        Log.info("Looking for game ID");
        for (Message message : CHESS_MESSAGES.user2()) {
            Log.infof("Message -> %s", message);

            if (message.gameID() != null) {
                System.out.println("##############################################################");
                return message.gameID();
            }
        }

        for (Message message : CHESS_MESSAGES.user1()) {
            Log.infof("Message -> %s", message);

            if (message.gameID() != null) {
                System.out.println("##############################################################");
                return message.gameID();
            }
        }

        System.out.println("##############################################################");
        throw new IllegalStateException("No gameID found");
    }
}
