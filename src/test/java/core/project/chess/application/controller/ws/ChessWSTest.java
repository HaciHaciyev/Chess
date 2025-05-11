package core.project.chess.application.controller.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Color;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.commons.tuples.Pair;
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
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
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
                Log.infof("%s received -> %s", whiteForm.username(), message.toString());
                USER_MESSAGES.user1().offer(message);
            });

            bMessagingSession.addMessageHandler(Message.class, message -> {
                Log.infof("%s received -> %s", blackForm.username(), message.toString());
                USER_MESSAGES.user2().offer(message);
            });

            wChessSession.addMessageHandler(Message.class, message -> {
                Log.infof("%s received -> %s", whiteForm.username(), message.toString());
                CHESS_MESSAGES.user1().offer(message);
            });

            bChessSession.addMessageHandler(Message.class, message -> {
                Log.infof("%s received -> %s", blackForm.username(), message.toString());
                CHESS_MESSAGES.user2().offer(message);
            });

            Thread.sleep(Duration.ofSeconds(3));

            addPartnership(blackForm.username(), wMessagingSession, whiteForm.username(), bMessagingSession);

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
                     .connectToServer(WSClient.class, pathForSecondPlayerSession)
        ) {
            chessGameInitializationWSTestProcess(firstPlayerMessagingSession, secondPlayer, secondPlayerMessagingSession,
                    firstPlayer, firstPlayerSession, secondPlayerSession, secondPlayerToken, firstPlayerToken);
        } catch (DeploymentException | IOException | InterruptedException e) {
            Log.errorf("Error in tests for chess game initialization through web socket sessions: %s", e.getLocalizedMessage());
        }
    }

    @Test
    @DisplayName("Test game initialization via FEN")
    void testGameInitWithFEN() throws JsonProcessingException {
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
                     .connectToServer(WSClient.class, pathForSecondPlayerSession)
        ) {
            addMessageHandlers(firstPlayerMessagingSession, secondPlayer, secondPlayerMessagingSession, firstPlayer, firstPlayerSession, secondPlayerSession);

            Thread.sleep(Duration.ofSeconds(1));

            addPartnership(secondPlayerForm.username(), firstPlayerMessagingSession, firstPlayerForm.username(), secondPlayerMessagingSession);

            Thread.sleep(Duration.ofSeconds(1));

            final String fen = "rnbqkbnr/ppp2ppp/4p3/3p4/3P4/5N2/PPP1PPPP/RNBQKB1R w KQkq - 0 3";
            sendMessage(firstPlayerSession, Message.builder(MessageType.GAME_INIT)
                    .FEN(fen)
                    .partner(secondPlayer)
                    .build()
            );

            await().atMost(Duration.ofSeconds(1)).until(() -> USER_MESSAGES.user2()
                    .stream()
                    .anyMatch(message -> {
                        if (message.type().equals(MessageType.PARTNERSHIP_REQUEST)) {
                            Log.infof("Message: %s", message);
                        }
                        return message.type().equals(MessageType.PARTNERSHIP_REQUEST) &&
                                message.message().contains(firstPlayer) &&
                                message.FEN().equals(fen);
                    })
            );

            Thread.sleep(Duration.ofSeconds(1));

            sendMessage(secondPlayerSession, Message.builder(MessageType.GAME_INIT)
                    .partner(firstPlayer)
                    .respond(Message.Respond.YES)
                    .build()
            );

            await().until(() -> USER_MESSAGES.user1()
                    .stream()
                    .anyMatch(message -> {
                        if (!message.type().equals(MessageType.GAME_START_INFO) && !message.type().equals(MessageType.FEN_PGN)) {
                            return false;
                        }

                        Log.infof("Message: %s", message);
                        if (message.type().equals(MessageType.FEN_PGN)) {
                            return message.FEN().equals(fen);
                        }
                        return true;
                    })
            );
            await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user2()
                    .stream()
                    .anyMatch(message -> {
                        if (!message.type().equals(MessageType.GAME_START_INFO) && !message.type().equals(MessageType.FEN_PGN)) {
                            return false;
                        }

                        Log.infof("Message: %s", message);
                        if (message.type().equals(MessageType.FEN_PGN)) {
                            return message.FEN().equals(fen);
                        }
                        return true;
                    })
            );

            Message gameStartedMessage = USER_MESSAGES.user1.stream()
                    .filter(message -> message.whitePlayerUsername() != null)
                    .findFirst()
                    .orElseThrow();
            String gameID = Objects.requireNonNull(gameStartedMessage.gameID(), "Game ID cannot be null");

            sendMessage(firstPlayerSession, firstPlayer, Message.move(gameID, Coordinate.e2, Coordinate.e3));
            await().atMost(Duration.ofSeconds(1)).until(() -> USER_MESSAGES.user1
                    .stream()
                    .anyMatch(message -> {
                        if (message.type().equals(MessageType.FEN_PGN) && !message.PGN().isBlank() && message.PGN().contains("e2-e3")) {
                            Log.infof("Message after move: %s", message);
                            return true;
                        }
                        return false;
                    })
            );
            sendMessage(secondPlayerSession, secondPlayer, Message.move(gameID, Coordinate.b8, Coordinate.c6));
            await().atMost(Duration.ofSeconds(1)).until(() -> USER_MESSAGES.user2
                    .stream()
                    .anyMatch(message -> {
                        if (message.type().equals(MessageType.FEN_PGN) && message.PGN() != null && message.PGN().contains("b8-c6")) {
                            Log.infof("Message after move: %s", message);
                            return true;
                        }
                        return false;
                    })
            );
        } catch (DeploymentException | IOException | InterruptedException e) {
            Log.errorf("Error in tests for chess game initialization through web socket sessions: %s", e.getLocalizedMessage());
        }
    }

    @Test
    @DisplayName("Test game initialization via PGN")
    void testGameInitWithPGN() throws JsonProcessingException {
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
                     .connectToServer(WSClient.class, pathForSecondPlayerSession)
        ) {
            addMessageHandlers(firstPlayerMessagingSession, secondPlayer, secondPlayerMessagingSession, firstPlayer, firstPlayerSession, secondPlayerSession);

            Thread.sleep(Duration.ofSeconds(1));

            addPartnership(secondPlayerForm.username(), firstPlayerMessagingSession, firstPlayerForm.username(), secondPlayerMessagingSession);

            Thread.sleep(Duration.ofSeconds(1));

            sendMessage(firstPlayerSession, Message.builder(MessageType.GAME_INIT)
                    .PGN("1. e2-e4 e7-e5 ")
                    .partner(secondPlayer)
                    .build()
            );

            await().atMost(Duration.ofSeconds(1)).until(() -> USER_MESSAGES.user2()
                    .stream()
                    .anyMatch(message -> {
                        if (message.type().equals(MessageType.PARTNERSHIP_REQUEST)) {
                            Log.infof("Message: %s", message);
                        }
                        return message.type().equals(MessageType.PARTNERSHIP_REQUEST) &&
                                message.message().contains(firstPlayer) &&
                                message.PGN().equals("1. e2-e4 e7-e5 ");
                    })
            );

            Thread.sleep(Duration.ofSeconds(1));

            sendMessage(secondPlayerSession, Message.builder(MessageType.GAME_INIT)
                    .partner(firstPlayer)
                    .respond(Message.Respond.YES)
                    .build()
            );

            Thread.sleep(Duration.ofSeconds(5));

            await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user1()
                    .stream()
                    .anyMatch(message -> {
                        if (!message.type().equals(MessageType.GAME_START_INFO) && !message.type().equals(MessageType.FEN_PGN)) {
                            return false;
                        }

                        Log.infof("Message: %s", message);
                        return true;
                    })
            );

            await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user2()
                    .stream()
                    .anyMatch(message -> {
                        if (!message.type().equals(MessageType.GAME_START_INFO) && !message.type().equals(MessageType.FEN_PGN)) {
                            return false;
                        }

                        Log.infof("Message: %s", message);
                        return true;
                    })
            );
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
                Log.infof("User %s Received Message: %s.", secondPlayer, message.toString());
                USER_MESSAGES.user2().offer(message);
            });

            testPartnershipDeletionAndReconnection2(firstPlayerMessagingSession, firstPlayer, secondPlayer, reconnectMessagingSPS, secondPlayerToken);
        } catch (DeploymentException e) {
            Log.errorf("Error in tests for chess game initialization process through web socket sessions: %s", e.getLocalizedMessage());
        }

        testRandomGameWithReconnection(
                firstPlayerSession,
                firstPlayer,
                firstPlayerToken,
                secondPlayerSession,
                secondPlayer,
                secondPlayerToken
        );
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
                Log.infof("User %s Received Message: %s.", secondPlayer, message.toString());
                USER_MESSAGES.user2().offer(message);
            });

            await().atMost(Duration.ofSeconds(5)).until(() -> !USER_MESSAGES.user2().isEmpty());

            sendMessage(reconnectedSPS, secondPlayer, Message.builder(MessageType.PARTNERSHIP_REQUEST)
                    .partner(firstPlayer)
                    .message("Hi")
                    .build());

            await().atMost(Duration.ofSeconds(5)).until(() -> !USER_MESSAGES.user1().isEmpty());

            Thread.sleep(2);

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
                Log.infof("User %s Received Message: %s.", secondPlayer, message.toString());
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

    private void testRandomGameWithReconnection(Session firstPlayerSession, String firstPlayer, String firstPlayerToken,
                                                Session secondPlayerSession, String secondPlayer, String secondPlayerToken) throws IOException, InterruptedException {

        Log.info("Test random game with reconnection.");

        sendMessage(firstPlayerSession, firstPlayer, Message.builder(MessageType.GAME_INIT).build());
        sendMessage(secondPlayerSession, secondPlayer, Message.builder(MessageType.GAME_INIT).build());

        Log.info("Await for messages.");
        await().atMost(Duration.ofSeconds(6)).until(() ->
                USER_MESSAGES.user1.stream()
                .anyMatch(message -> {
                    final boolean isHaveGameStartInfoMessage = message.type().equals(MessageType.GAME_START_INFO);
                    if (!isHaveGameStartInfoMessage) {
                        return false;
                    }

                    return message.whitePlayerUsername().equals(firstPlayer) ||
                            message.blackPlayerUsername().equals(firstPlayer);
                }) &&

                USER_MESSAGES.user2.stream()
                .anyMatch(message -> {
                    final boolean isHaveGameStartInfoMessage = message.type().equals(MessageType.GAME_START_INFO);
                    if (!isHaveGameStartInfoMessage) {
                        return false;
                    }

                    return message.whitePlayerUsername().equals(secondPlayer) ||
                            message.blackPlayerUsername().equals(secondPlayer);
                })
        );

        Message gameStartedMessage = USER_MESSAGES.user1.stream().filter(message -> message.whitePlayerUsername() != null).findFirst().orElseThrow();
        String gameId = Objects.requireNonNull(gameStartedMessage.gameID(), "Game ID cannot be null");

        final boolean firstPlayerWhite = gameStartedMessage.whitePlayerUsername().equals(firstPlayer);
        if (firstPlayerWhite) {
            Pair.of(processRandomGameWithReconnection(firstPlayerSession, firstPlayer,
                    secondPlayerSession, secondPlayer, secondPlayerToken, gameId), secondPlayer);
            return;
        }

        Pair.of(processRandomGameWithReconnection(secondPlayerSession, secondPlayer,
                firstPlayerSession, firstPlayer, firstPlayerToken, gameId), firstPlayer);
    }

    private Session processRandomGameWithReconnection(Session whitePlayerSession, String whitePlayer, Session blackPlayerSession,
                                                      String blackPlayer, String blackPlayerToken, final String gameId) throws IOException, InterruptedException {

        sendMessage(whitePlayerSession, whitePlayer, Message.builder(MessageType.MOVE)
                .gameID(gameId)
                .from(Coordinate.e2)
                .to(Coordinate.e4)
                .build());

        Thread.sleep(Duration.ofSeconds(2));

        USER_MESSAGES.user1.forEach(message -> {
            if (message.FEN() != null) {
                Log.infof("User %s. Message FEN: {%s}, PGN: {%s}.", whitePlayer, message.FEN(), message.PGN());
            }
        });

        USER_MESSAGES.user2.forEach(message -> {
            if (message.FEN() != null) {
                Log.infof("User %s. Message FEN: {%s}, PGN: {%s}.", blackPlayer, message.FEN(), message.PGN());
            }
        });

        sendMessage(blackPlayerSession, blackPlayer, Message.builder(MessageType.MOVE)
                .gameID(gameId)
                .from(Coordinate.e7)
                .to(Coordinate.e5)
                .build());

        Thread.sleep(Duration.ofSeconds(2));

        USER_MESSAGES.user1.forEach(message -> {
            if (message.FEN() != null) {
                Log.infof("User %s. Message FEN: {%s}, PGN: {%s}.", whitePlayer, message.FEN(), message.PGN());
            }
        });

        USER_MESSAGES.user2.forEach(message -> {
            if (message.FEN() != null) {
                Log.infof("User %s. Message FEN: {%s}, PGN: {%s}.", blackPlayer, message.FEN(), message.PGN());
            }
        });

        blackPlayerSession.close();

        try (Session reconnectedSPS = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(
                        WSClient.class, authUtils.serverURIWithToken(serverURI, blackPlayerToken)
                )) {

            reconnectedSPS.addMessageHandler(Message.class, message -> {
                Log.infof("User %s Received Message from Chess: %s.", blackPlayer, message.toString());
                USER_MESSAGES.user2().offer(message);
            });

            sendMessage(reconnectedSPS, blackPlayer, Message.builder(MessageType.GAME_INIT)
                    .gameID(gameId)
                    .build());

            await().atMost(Duration.ofSeconds(3)).until(() -> USER_MESSAGES.user2.stream()
                    .anyMatch(message -> {
                        boolean res = message.FEN() != null;
                        if (res) {
                            Log.infof("Message FEN: {%s}, PGN: {%s}.", message.FEN(), message.PGN());
                        }
                        return res;
                    }));

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
            Log.infof("User1 %s Received Message in Messaging Service: %s.", firstPlayer, message.toString());
            USER_MESSAGES.user1().offer(message);
        });

        secondPlayerMessagingSession.addMessageHandler(Message.class, message -> {
            Log.infof("User2 %s Received Message in Messaging Service: %s.", secondPlayer, message.toString());
            USER_MESSAGES.user2().offer(message);
        });

        firstPlayerSession.addMessageHandler(Message.class, message -> {
            Log.infof("User %s Received Message in Chess: %s.", firstPlayer, message.toString());
            USER_MESSAGES.user1().offer(message);
        });

        secondPlayerSession.addMessageHandler(Message.class, message -> {
            Log.infof("User %s Received Message in Chess: %s.", secondPlayer, message.toString());
            USER_MESSAGES.user2().offer(message);
        });

        Thread.sleep(Duration.ofSeconds(1).toMillis());
    }

    private void addPartnership(String blackUsername, Session wMessagingSession,
                                String whiteUsername, Session bMessagingSession) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(1));

        Message wPartnershipRequest = Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .partner(blackUsername)
                .message("br")
                .build();
        sendMessage(wMessagingSession, whiteUsername, wPartnershipRequest);

        await()
                .atMost(1, SECONDS)
                .until(() -> USER_MESSAGES.user2().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(msg ->
                                msg.type() == MessageType.PARTNERSHIP_REQUEST && msg.message().contains("invite you")
                        )
                );
        USER_MESSAGES.user2().clear();

        Message bPartnershipRequest = Message.builder(MessageType.PARTNERSHIP_REQUEST)
                .partner(whiteUsername)
                .message("brrr")
                .build();
        sendMessage(bMessagingSession, blackUsername, bPartnershipRequest);

        await()
                .atMost(1, SECONDS)
                .until(() -> USER_MESSAGES.user1().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(msg ->
                                msg.type() == MessageType.USER_INFO && msg.message().contains("successfully added")
                        )
                );
        USER_MESSAGES.user1().clear();

        await().atMost(1, SECONDS).until(() -> {
            Message msg = USER_MESSAGES.user2().peek();
            return msg != null && msg.type() == MessageType.USER_INFO && msg.message().contains("successfully added");
        });
        USER_MESSAGES.user2().take();
    }

    private void awaitGameStartMessages(String secondPlayer, String firstPlayer) {
        await().atMost(Duration.ofSeconds(5)).until(() ->
                USER_MESSAGES.user1().stream()
                        .anyMatch(message -> {
                            final boolean isHaveGameStartInfoMessage = message.type().equals(MessageType.GAME_START_INFO);
                            if (!isHaveGameStartInfoMessage) {
                                return false;
                            }

                            return message.whitePlayerUsername().equals(firstPlayer) ||
                                    message.blackPlayerUsername().equals(firstPlayer);
                        }) &&
                        USER_MESSAGES.user1().stream().anyMatch(message -> {
                            final boolean isHaveChessNotations = message.type().equals(MessageType.FEN_PGN);
                            if (!isHaveChessNotations) {
                                return false;
                            }

                            Log.infof("Chess Notations found: FEN-{%s}, PGN-{%s}", message.FEN(), message.PGN());
                            return true;
                        }) &&
                        USER_MESSAGES.user2().stream()
                                .anyMatch(message -> {
                                    final boolean isHaveGameStartInfoMessage = message.type().equals(MessageType.GAME_START_INFO);
                                    if (!isHaveGameStartInfoMessage) {
                                        return false;
                                    }

                                    return message.whitePlayerUsername().equals(secondPlayer) ||
                                            message.blackPlayerUsername().equals(secondPlayer);
                                }) &&
                        USER_MESSAGES.user2().stream().anyMatch(message -> {
                            final boolean isHaveChessNotations = message.type().equals(MessageType.FEN_PGN);
                            if (!isHaveChessNotations) {
                                return false;
                            }

                            Log.infof("Chess Notations found: FEN-{%s}, PGN-{%s}", message.FEN(), message.PGN());
                            return true;
                        })
        );
    }
}
