package core.project.chess.application.controller.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.security.JwtUtility;
import core.project.chess.infrastructure.utilities.containers.Pair;
import io.quarkus.logging.Log;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.*;
import testUtils.AuthUtils;
import testUtils.RegistrationForm;
import testUtils.WSClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static testUtils.WSClient.sendMessage;

@QuarkusTest
@WithTestResource(MessagingTestResource.class)
class ChessWSTest {

    private final Messages USER_MESSAGES = Messages.newInstance();

    private final Messages CHESS_MESSAGES = Messages.newInstance();

    @TestHTTPResource("/chessland/chess-game")
    URI serverURI;

    URI userSessionURI;

    @Inject
    AuthUtils authUtils;

    @Inject
    JwtUtility jwtUtility;

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

    @Disabled("Temporary shutdown due to technical reasons.")
    @Test
    @DisplayName("Custom game matchmaking")
    void customGameMatchmaking() throws Exception {
        RegistrationForm whiteForm = authUtils.registerRandom();
        authUtils.enableAccount(whiteForm);
        String whiteToken = authUtils.login(whiteForm);

        RegistrationForm blackForm = authUtils.registerRandom();
        authUtils.enableAccount(blackForm);
        String blackToken = authUtils.login(blackForm);

        try (Session wSession = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, authUtils.serverURIWithToken(serverURI, whiteToken));

             Session bSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, authUtils.serverURIWithToken(serverURI, blackToken))
        ) {
            // TODO ??? Where is message handlers
            String wName = whiteForm.username();
            sendMessage(wSession, wName, Message.gameInit("WHITE", ChessGame.Time.RAPID));

            String bName = blackForm.username();
            sendMessage(bSession, bName, Message.gameInit("BLACK", ChessGame.Time.RAPID));

            String gameID = extractGameID();
            simulateGame(wSession, wName, gameID, bSession, bName);

            wSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
            bSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
        }
    }

    @Test
    @DisplayName("Custom partnership game")
    void customPartnershipGame() throws Exception {
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

            String wName = whiteForm.username();
            String bName = blackForm.username();

            sendMessage(wChessSession, wName, Message.partnershipGame("WHITE", bName, null, ChessGame.Time.RAPID));
            sendMessage(bChessSession, bName, Message.partnershipGame("BLACK", wName, null, ChessGame.Time.RAPID));

            Thread.sleep(Duration.ofSeconds(5));

            String gameID = extractGameID();

            simulateGame(wChessSession, wName, gameID, bChessSession, bName);

            wMessagingSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
            bMessagingSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));

            wChessSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
            bChessSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
        }
    }

    @Test
    @DisplayName("Chess game WS initialization test.")
    void chessGameInitializationWSTest() throws JsonProcessingException {
        RegistrationForm firstPlayerForm = authUtils.registerRandom();
        authUtils.enableAccount(firstPlayerForm);
        String firstPlayerToken = authUtils.login(firstPlayerForm);

        RegistrationForm secondPlayerForm = authUtils.registerRandom();
        authUtils.enableAccount(secondPlayerForm);
        String secondPlayerToken = authUtils.login(secondPlayerForm);

        String firstPlayer = firstPlayerForm.username();
        String secondPlayer = secondPlayerForm.username();

        URI pathForFirstPlayerMessagingSession = authUtils.serverURIWithToken(userSessionURI, firstPlayerToken);
        URI pathForSecondPlayerMessagingSession = authUtils.serverURIWithToken(userSessionURI, secondPlayerToken);
        URI pathForFirstPlayerSession = authUtils.serverURIWithToken(serverURI, firstPlayerToken);
        URI pathForSecondPlayerSession = authUtils.serverURIWithToken(serverURI, secondPlayerToken);
        addLoggingForWSPaths(pathForFirstPlayerMessagingSession, pathForSecondPlayerMessagingSession,
                pathForFirstPlayerSession, pathForSecondPlayerSession);

        try (Session firstPlayerMessagingSession = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, pathForFirstPlayerMessagingSession);

             Session secondPlayerMessagingSession = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, pathForSecondPlayerMessagingSession);

             Session firstPlayerSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, pathForFirstPlayerSession);

             Session secondPlayerSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, pathForSecondPlayerSession);
        ) {
            chessGameInitializationWSTestProcess(firstPlayerMessagingSession, secondPlayer, secondPlayerMessagingSession,
                    firstPlayer, firstPlayerSession, secondPlayerSession, secondPlayerToken, firstPlayerToken);
        } catch (DeploymentException | IOException | InterruptedException e) {
            Log.errorf("Error in tests for chess game initialization through web socket sessions: %s", e.getLocalizedMessage());
        }
    }

    private void chessGameInitializationWSTestProcess(Session firstPlayerMessagingSession, String secondPlayer, Session secondPlayerMessagingSession,
                                                      String firstPlayer, Session firstPlayerSession, Session secondPlayerSession,
                                                      String secondPlayerToken, String firstPlayerToken) throws InterruptedException, IOException {

        addMessageHandlers(firstPlayerMessagingSession, secondPlayer, secondPlayerMessagingSession, firstPlayer, firstPlayerSession, secondPlayerSession);

        testPartnershipWithReconnect(firstPlayerMessagingSession, firstPlayer, secondPlayer, secondPlayerMessagingSession, secondPlayerToken);

        URI pathForSecondPlayerMessagingSession = authUtils.serverURIWithToken(userSessionURI, secondPlayerToken);
        try (Session reconnectMessagingSPS = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, pathForSecondPlayerMessagingSession)) {

            reconnectMessagingSPS.addMessageHandler(Message.class, message -> {
                Log.infof("Received Message: %s, from user %s.", message, firstPlayer);
                USER_MESSAGES.user2().offer(message);
            });

            testPartnershipDeletionAndReconnection2(firstPlayerMessagingSession, firstPlayer, secondPlayer, reconnectMessagingSPS, secondPlayerToken);
        } catch (DeploymentException e) {
            Log.errorf("Error in tests for chess game initialization process through web socket sessions: %s", e.getLocalizedMessage());
        }

        /*Pair<Session, String> sessionAndUsername = testRandomGameWithReconnection(
                Objects.requireNonNull(firstPlayerSession, "First player session is null."),
                firstPlayer,
                Objects.requireNonNull(firstPlayerToken, "First player token is null."),
                Objects.requireNonNull(secondPlayerSession, "Second player session is null."),
                secondPlayer,
                Objects.requireNonNull(secondPlayerToken, "Second player token is null.")
        );*/
    }

    private void testPartnershipWithReconnect(Session firstPlayerSession, String firstPlayer, String secondPlayer,
                                              Session secondPlayerSession, String secondPlayerToken) throws IOException, InterruptedException {
        secondPlayerSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Closed for tests."));

        Thread.sleep(Duration.ofSeconds(2));

        sendMessage(firstPlayerSession, firstPlayer, Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .partner(secondPlayer)
                .message("Hello! I would be glad to establish contact with you.")
                .build());

        Thread.sleep(Duration.ofSeconds(2));

        try (Session reconnectedSPS = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, authUtils.serverURIWithToken(userSessionURI, secondPlayerToken))
        ) {
            reconnectedSPS.addMessageHandler(Message.class, message -> {
                Log.infof("Received Message: %s, from user %s.", message, firstPlayer);
                USER_MESSAGES.user2().offer(message);
            });

            await().atMost(Duration.ofSeconds(5)).until(() -> !USER_MESSAGES.user2().isEmpty());

            sendMessage(reconnectedSPS, secondPlayer, Message.builder(MessageType.PARTNERSHIP_REQUEST)
                    .partner(firstPlayer)
                    .message("Hi")
                    .build());

            await().atMost(Duration.ofSeconds(5)).until(() -> !USER_MESSAGES.user1().isEmpty());

            assertThat(USER_MESSAGES.user1()).anyMatch(message -> message.type().equals(MessageType.USER_INFO) &&
                    message.message().contains("Partnership") &&
                    message.message().contains(secondPlayer) &&
                    message.message().contains("successfully added"));

            assertThat(USER_MESSAGES.user2()).anyMatch(message -> message.type().equals(MessageType.USER_INFO) &&
                    message.message().contains("Partnership") &&
                    message.message().contains(firstPlayer) &&
                    message.message().contains("successfully added"));

        } catch (DeploymentException e) {
            String errorMessage = "Can`t reconnect second player in partnership reconnection test: %s".formatted(e.getLocalizedMessage());
            Log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    private void testPartnershipDeletionAndReconnection2(Session firstPlayerSession, String firstPlayer, String secondPlayer,
                                                         Session secondPlayerSession, String secondPlayerToken) throws InterruptedException, IOException {

        Thread.sleep(Duration.ofSeconds(2));

        sendMessage(firstPlayerSession, firstPlayer, Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .partner(secondPlayer)
                .message("Hello! I would be glad to establish contact with you AGAIN.")
                .build());

        await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user1()
                .stream()
                .anyMatch(message -> message.message() != null &&
                        message.message().contains("You can`t invite someone who has partnership with you already.")));

        String messagingURL = ConfigProvider.getConfig().getConfigValue("messaging.api.url").getValue() + "/chessland/account/remove-partner";

        given().header("Authorization", "Bearer " + secondPlayerToken)
                .param("partner", firstPlayer)
                .when()
                .delete(messagingURL)
                .then()
                .statusCode(204);

        sendMessage(firstPlayerSession, firstPlayer, Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .partner(secondPlayer)
                .message("Hello! I would be glad to establish contact with you AGAIN.")
                .build());

        await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user2()
                .stream()
                .anyMatch(message -> message.message() != null && message.message().contains("AGAIN")));

        Thread.sleep(Duration.ofSeconds(2));

        USER_MESSAGES.user2().clear();

        Thread.sleep(Duration.ofSeconds(2));

        secondPlayerSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Closed for tests."));

        Thread.sleep(Duration.ofSeconds(2));

        try (Session reconnectedSPS = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, authUtils.serverURIWithToken(userSessionURI, secondPlayerToken))
        ) {
            reconnectedSPS.addMessageHandler(Message.class, message -> {
                Log.infof("Received Message: %s, from user %s.", message, firstPlayer);
                USER_MESSAGES.user2().offer(message);
            });

            await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user2()
                    .stream()
                    .anyMatch(message -> message.message() != null && message.message().contains("AGAIN")));

            sendMessage(reconnectedSPS, secondPlayer, Message.builder(MessageType.PARTNERSHIP_REQUEST)
                    .partner(firstPlayer)
                    .message("Hi AGAIN")
                    .build());

            Thread.sleep(Duration.ofSeconds(1));

            assertThat(USER_MESSAGES.user1()).anyMatch(message -> message.type().equals(MessageType.USER_INFO) &&
                    message.message().contains("Partnership") &&
                    message.message().contains(secondPlayer) &&
                    message.message().contains("successfully added"));

            assertThat(USER_MESSAGES.user2()).anyMatch(message -> message.type().equals(MessageType.USER_INFO) &&
                    message.message().contains("Partnership") &&
                    message.message().contains(firstPlayer) &&
                    message.message().contains("successfully added"));
        } catch (DeploymentException | IOException e) {
            String errorMessage = "Can`t reconnect second player in partnership reconnection test: %s".formatted(e.getLocalizedMessage());
            Log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    private Pair<Session, String> testRandomGameWithReconnection(Session firstPlayerSession, String firstPlayer, String firstPlayerToken,
                                                                 Session secondPlayerSession, String secondPlayer, String secondPlayerToken) throws IOException {

        Log.info("Test random game with reconnection.");

        sendMessage(firstPlayerSession, firstPlayer, Message.builder(MessageType.GAME_INIT).build());
        sendMessage(secondPlayerSession, secondPlayer, Message.builder(MessageType.GAME_INIT).build());

        Log.info("Await for messages.");
        await().atMost(Duration.ofSeconds(3)).until(() ->
                USER_MESSAGES.user1.stream()
                .anyMatch(message -> {
                    final boolean isHaveGameStartInfoMessage = message.type().equals(MessageType.GAME_START_INFO);
                    if (!isHaveGameStartInfoMessage) {
                        return false;
                    }

                    return message.whitePlayerUsername().username().equals(firstPlayer) ||
                            message.blackPlayerUsername().username().equals(firstPlayer);
                }) &&

                USER_MESSAGES.user2.stream()
                .anyMatch(message -> {
                    final boolean isHaveGameStartInfoMessage = message.type().equals(MessageType.GAME_START_INFO);
                    if (!isHaveGameStartInfoMessage) {
                        return false;
                    }

                    return message.whitePlayerUsername().username().equals(secondPlayer) ||
                            message.blackPlayerUsername().username().equals(secondPlayer);
                })
        );

        Message gameStartedMessage = USER_MESSAGES.user1.stream().filter(message -> message.whitePlayerUsername() != null).findFirst().orElseThrow();
        String gameId = gameStartedMessage.gameID();

        final boolean firstPlayerWhite = gameStartedMessage.whitePlayerUsername().username().equals(firstPlayer);
        if (firstPlayerWhite) {
            return Pair.of(processRandomGameWithReconnection(firstPlayerSession, firstPlayer,
                    secondPlayerSession, secondPlayer, secondPlayerToken, gameId), secondPlayer);
        }

        return Pair.of(processRandomGameWithReconnection(secondPlayerSession, secondPlayer,
                firstPlayerSession, firstPlayer, firstPlayerToken, gameId), firstPlayer);
    }

    private Session processRandomGameWithReconnection(Session whitePlayerSession, String whitePlayer, Session blackPlayerSession,
                                                      String blackPlayer, String blackPlayerToken, String gameId) throws IOException {

        sendMessage(whitePlayerSession, whitePlayer, Message.builder(MessageType.MOVE)
                .gameID(gameId)
                .from(Coordinate.e2)
                .to(Coordinate.e4)
                .build());

        await().atMost(Duration.ofSeconds(3)).until(() -> USER_MESSAGES.user2.stream()
                .anyMatch(message -> message.FEN() != null && message.FEN().equals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1")));

        sendMessage(blackPlayerSession, blackPlayer, Message.builder(MessageType.MOVE)
                .gameID(gameId)
                .from(Coordinate.e7)
                .to(Coordinate.e5)
                .build());

        await().atMost(Duration.ofSeconds(3)).until(() -> USER_MESSAGES.user1.stream()
                .anyMatch(message -> {
                    Log.infof("Message FEN: %s", message.FEN());
                    return message.FEN() != null && message.FEN().equals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");
                }));

        blackPlayerSession.close();

        try (Session reconnectedSPS = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(
                        WSClient.class, authUtils.serverURIWithToken(userSessionURI, blackPlayerToken)
                )) {

            reconnectedSPS.addMessageHandler(Message.class, message -> {
                Log.infof("Received Message from Chess: %s.", message);
                USER_MESSAGES.user2().offer(message);
            });

            sendMessage(reconnectedSPS, blackPlayer, Message.builder(MessageType.GAME_INIT)
                    .gameID(gameId)
                    .build());

            await().atMost(Duration.ofSeconds(3)).until(() -> USER_MESSAGES.user2.stream()
                    .anyMatch(message -> message.FEN() != null && message.FEN().equals("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2")));

            sendMessage(reconnectedSPS, blackPlayer, Message.builder(MessageType.RESIGNATION).gameID(gameId).build());

            await().atMost(Duration.ofSeconds(5)).until(() -> USER_MESSAGES.user1.stream()
                    .anyMatch(message -> message.type().equals(MessageType.GAME_ENDED) && message.gameID().equals(gameId)));

            return reconnectedSPS;
        } catch (DeploymentException e) {
            String errorMessage = "Can`t reconnect second player in random game reconnection test: %s".formatted(e.getLocalizedMessage());
            Log.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
    }

    private void simulateGame(Session wSession, String wName, String gameID, Session bSession, String bName) {
        //1
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e2, Coordinate.e4));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.e7, Coordinate.e5));
        //2
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.b8, Coordinate.c6));
        //3
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.b1, Coordinate.c3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.g8, Coordinate.f6));
        //4
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f1, Coordinate.c4));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.d7, Coordinate.d6));
        //5
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.d2, Coordinate.d3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.c8, Coordinate.e6));
        //6
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e1, Coordinate.g1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.e6, Coordinate.c4));
        //7
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.d3, Coordinate.c4));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.f8, Coordinate.e7));
        //8
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.d1, Coordinate.e2));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.d8, Coordinate.d7));
        //9
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.c1, Coordinate.e3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.e8, Coordinate.c8));
        //10
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.a1, Coordinate.d1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.f6, Coordinate.g4));
        //11
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.c3, Coordinate.d5));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.h7, Coordinate.h6));
        //12
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.b2, Coordinate.b3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.e7, Coordinate.g5));
        //13
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.h2, Coordinate.h3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.g4, Coordinate.e3));
        //14
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f2, Coordinate.e3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.a7, Coordinate.a6));
        //15
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.h3, Coordinate.h4));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.g5, Coordinate.f6));
        //16
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.h2));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.f6, Coordinate.h4));
        //17
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f1, Coordinate.f3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.h8, Coordinate.f8));
        //18
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.f5));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.g7, Coordinate.g6));
        //19
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f5, Coordinate.f3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.f7, Coordinate.f5));
        //20
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.h3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.g6, Coordinate.g5));
        //21
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e2, Coordinate.h5));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.f5, Coordinate.e4));
        //22
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.h5, Coordinate.h6));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.f8, Coordinate.f2));
        //23
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g2, Coordinate.g3));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.d7, Coordinate.h3));
        //24
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.d5, Coordinate.e7));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.c6, Coordinate.e7));
        //25
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f2));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.h3, Coordinate.h2));
        //26
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.f2, Coordinate.f1));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.h2, Coordinate.g3));
        //27
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.h6, Coordinate.e6));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.d8, Coordinate.d7));
        //28
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e6, Coordinate.g8));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.d7, Coordinate.d8));
        //29
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.g8, Coordinate.e6));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.c8, Coordinate.b8));
        //30
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.e6, Coordinate.e7));
        sendMessage(bSession, bName, Message.move(gameID, Coordinate.g3, Coordinate.f2));

        //31
        sendMessage(wSession, wName, Message.move(gameID, Coordinate.d1, Coordinate.d2));
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

    private static void addLoggingForWSPaths(URI pathForFirstPlayerMessagingSession, URI pathForSecondPlayerMessagingSession,
                                             URI pathForFirstPlayerSession, URI pathForSecondPlayerSession) {

        Log.infof("First player session path for MessagingService: %s", pathForFirstPlayerMessagingSession);
        Log.infof("Second player session path for MessagingService: %s", pathForSecondPlayerMessagingSession);
        Log.infof("First player session path for Chess: %s", pathForFirstPlayerSession);
        Log.infof("Second player session path for Chess: %s", pathForSecondPlayerSession);
    }

    private void addMessageHandlers(Session firstPlayerMessagingSession, String secondPlayer, Session secondPlayerMessagingSession,
                                    String firstPlayer, Session firstPlayerSession, Session secondPlayerSession) throws InterruptedException {

        firstPlayerMessagingSession.addMessageHandler(Message.class, message -> {
            Log.infof("Received Message in Messaging Service: %s, from user %s.", message, secondPlayer);
            USER_MESSAGES.user1().offer(message);
        });

        secondPlayerMessagingSession.addMessageHandler(Message.class, message -> {
            Log.infof("Received Message in Messaging Service: %s, from user %s.", message, firstPlayer);
            USER_MESSAGES.user2().offer(message);
        });

        firstPlayerSession.addMessageHandler(Message.class, message -> {
            Log.infof("Received Message in Chess: %s.", message);
            USER_MESSAGES.user1().offer(message);
        });

        secondPlayerSession.addMessageHandler(Message.class, message -> {
            Log.infof("Received Message in Chess: %s.", message);
            USER_MESSAGES.user2().offer(message);
        });

        Thread.sleep(Duration.ofSeconds(1).toMillis());
    }
}
