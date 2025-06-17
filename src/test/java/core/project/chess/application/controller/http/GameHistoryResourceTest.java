package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.controller.ws.MessagingTestResource;
import core.project.chess.application.dto.chess.ChessGameHistory;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.domain.chess.value_objects.AlgebraicNotation;
import core.project.chess.domain.chess.value_objects.ChessMove;
import core.project.chess.domain.chess.value_objects.Move;
import core.project.chess.domain.commons.enumerations.Color;
import io.quarkus.logging.Log;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;
import testUtils.SimplePGNReader;
import testUtils.WSClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;
import static testUtils.WSClient.sendMessage;

@QuarkusTest
@WithTestResource(MessagingTestResource.class)
class GameHistoryResourceTest {

    private final Messages USER_MESSAGES = Messages.newInstance();

    @TestHTTPResource("/chessland/chess-game")
    URI serverURI;

    URI userSessionURI;

    @Inject
    AuthUtils authUtils;

    @Inject
    JWTParser jwtParser;

    @Inject
    ObjectMapper objectMapper;

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
    }

    @BeforeEach
    void printLineBreak() {
        userSessionURI = URI.create(ConfigProvider.getConfig().getValue("messaging.api.url", String.class) + "/chessland/user-session");

        System.out.println();
        System.out.println("---------------------------------------BREAK---------------------------------------");
        System.out.println();
    }

    @Test
    void gameHistory() throws JsonProcessingException, ParseException {
        String token = authUtils.fullLoginProcess().serverResponse().get("token");
        fillTheDatabase(token);

        String result = given().contentType("application/json")
                .param("pageNumber", 0)
                .param("pageSize", 10)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("chessland/account/game-history")
                .then()
                .statusCode(200)
                .assertThat()
                .body(notNullValue())
                .extract()
                .body()
                .asString();

        List<ChessGameHistory> historyList = objectMapper.readValue(result, new TypeReference<>() {});
        Log.infof("Game history resource, page - 1, size: %s, content: %s", historyList.size(), historyList);

        String result2 = given().contentType("application/json")
                .param("pageNumber", 2)
                .param("pageSize", 10)
                .header("Authorization", "Bearer " + token)
                .when()
                .get("chessland/account/game-history")
                .then()
                .statusCode(200)
                .assertThat()
                .body(notNullValue())
                .extract()
                .body()
                .asString();

        List<ChessGameHistory> historyList2 = objectMapper.readValue(result2, new TypeReference<>() {});
        Log.infof("Game history resource, page - 2, size: %s, content: %s", historyList2.size(), historyList2);

        String lastGame = given()
                .contentType("application/json")
                .param("gameID", historyList.getFirst().chessHistoryId().toString())
                .header("Authorization", "Bearer " + token)
                .when()
                .get("chessland/account/game")
                .then()
                .statusCode(200)
                .assertThat()
                .body(notNullValue())
                .extract()
                .body()
                .asString();

        Log.infof("Last game: %s.", objectMapper.readValue(lastGame, new TypeReference<ChessGameHistory>(){}));
    }

    private void fillTheDatabase(String token) throws ParseException, JsonProcessingException {
        JsonWebToken jwt = jwtParser.parse(token);
        String username = jwt.getName();
        String opponentToken = authUtils.fullLoginProcess().serverResponse().get("token");
        String opponentUsername = jwtParser.parse(opponentToken).getName();
        fillDBWithChessGames(username, token, opponentUsername, opponentToken);
    }

    private void fillDBWithChessGames(String username, String token, String opponentUsername, String opponentToken) {
        try (Session playerMessaging = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, authUtils.serverURIWithToken(userSessionURI, token));
             
             Session opponentMessaging = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, authUtils.serverURIWithToken(userSessionURI, opponentToken));

             Session playerChess = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, authUtils.serverURIWithToken(serverURI, token));

             Session opponentChess = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, authUtils.serverURIWithToken(serverURI, opponentToken))) {

            addMessageHandlers(playerMessaging, opponentUsername, opponentMessaging, username, playerChess, opponentChess);
            awaitSuccessfulConnection();
            addPartnership(opponentUsername, playerMessaging, username, opponentMessaging);
            loadGames(username, playerChess, opponentUsername, opponentChess);
        } catch (DeploymentException | IOException | InterruptedException e) {
            Log.errorf("Error: %s.", e.getMessage());
        }
    }

    private void addMessageHandlers(Session firstPlayerMessagingSession, String secondPlayer, Session secondPlayerMessagingSession,
                                    String firstPlayer, Session firstPlayerSession, Session secondPlayerSession)
            throws InterruptedException {

        firstPlayerMessagingSession.addMessageHandler(Message.class, message -> {
            if (message.type() != MessageType.MOVE && message.type() != MessageType.FEN_PGN)
                Log.infof("User1 %s Received Message in Messaging Service: %s.", firstPlayer, message.toString());
            USER_MESSAGES.user1().offer(message);
        });

        secondPlayerMessagingSession.addMessageHandler(Message.class, message -> {
            if (message.type() != MessageType.MOVE && message.type() != MessageType.FEN_PGN)
                Log.infof("User2 %s Received Message in Messaging Service: %s.", secondPlayer, message.toString());
            USER_MESSAGES.user2().offer(message);
        });

        firstPlayerSession.addMessageHandler(Message.class, message -> {
            if (message.type() != MessageType.MOVE && message.type() != MessageType.FEN_PGN)
                Log.infof("User %s Received Message in Chess: %s.", firstPlayer, message.toString());
            USER_MESSAGES.user1().offer(message);
        });

        secondPlayerSession.addMessageHandler(Message.class, message -> {
            if (message.type() != MessageType.MOVE && message.type() != MessageType.FEN_PGN)
                Log.infof("User %s Received Message in Chess: %s.", secondPlayer, message.toString());
            USER_MESSAGES.user2().offer(message);
        });
    }

    private void awaitSuccessfulConnection() {
        await()
                .atMost(1, SECONDS)
                .until(() -> USER_MESSAGES.user1.stream()
                        .anyMatch(message -> message.message().contains("Successful connection to chessland"))
                );

        await()
                .atMost(1, SECONDS)
                .until(() -> USER_MESSAGES.user2.stream()
                        .anyMatch(message -> message.message().contains("Successful connection to chessland"))
                );

        await()
                .atMost(1, SECONDS)
                .until(() -> USER_MESSAGES.user1.stream()
                        .anyMatch(message -> message.message().contains("Successful connection to messaging"))
                );

        await()
                .atMost(1, SECONDS)
                .until(() -> USER_MESSAGES.user2.stream()
                        .anyMatch(message -> message.message().contains("Successful connection to messaging"))
                );
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

    private void loadGames(String username, Session playerChess, String opponentUsername, Session opponentChess) throws InterruptedException {
        Thread.sleep(Duration.ofSeconds(1));
        final String path = "src/main/resources/pgn/game-history-resource.pgn";
        for (String PGN : SimplePGNReader.extractFromPGN(path)) {
            printLineBreak();
            var pgnReader = new SimplePGNReader(PGN);
            executeGame(username, playerChess, opponentUsername, opponentChess, pgnReader.readAll());
        }
    }

    private void executeGame(String username, Session playerChess, String opponentUsername,
                             Session opponentChess, List<ChessMove> listOfMoves) throws InterruptedException {
        final String gameID = startNewPartnershipGame(username, playerChess, opponentUsername, opponentChess);
        for (ChessMove move : listOfMoves) {
            var whiteMoves = move.white();
            var blackMoves = move.black();

            proceedFullMove(username, playerChess, opponentUsername, opponentChess, gameID, whiteMoves, blackMoves);
        }
    }

    private String startNewPartnershipGame(String username, Session playerChess,
                                           String opponentUsername, Session opponentChess) throws InterruptedException {
        USER_MESSAGES.user1.clear();
        USER_MESSAGES.user2.clear();
        Thread.sleep(Duration.ofSeconds(1));

        sendMessage(playerChess, username, Message.builder(MessageType.GAME_INIT)
                .color(Color.WHITE)
                .partner(opponentUsername)
                .build());

        await().atMost(2, SECONDS).until(() -> USER_MESSAGES.user2.stream()
                .anyMatch(message -> {
                    if (message.type().equals(MessageType.PARTNERSHIP_REQUEST)) {
                        Log.infof("Message: %s", message);
                        return message.color().equals(Color.BLACK) && message.message().contains(username);
                    }
                    return false;
                }));

        sendMessage(opponentChess, opponentUsername, Message.builder(MessageType.GAME_INIT)
                .partner(username)
                .respond(Message.Respond.YES)
                .build());

        await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user1()
                .stream()
                .anyMatch(message -> {
                    if (!message.type().equals(MessageType.GAME_START_INFO) &&
                            !message.type().equals(MessageType.FEN_PGN)) return false;
                    Log.infof("Message: %s", message);
                    return true;
                }));

        await().atMost(Duration.ofSeconds(2)).until(() -> USER_MESSAGES.user2()
                .stream()
                .anyMatch(message -> {
                    if (!message.type().equals(MessageType.GAME_START_INFO) &&
                            !message.type().equals(MessageType.FEN_PGN)) return false;
                    Log.infof("Message: %s", message);
                    return true;
                }));

        return USER_MESSAGES.user1.stream()
                .filter(message -> message.whitePlayerUsername() != null &&
                        message.whitePlayerUsername().equals(username))
                .findFirst()
                .orElseThrow()
                .gameID();
    }

    private void proceedFullMove(String username, Session playerChess,
                                 String opponentUsername, Session opponentChess,
                                 String gameID, Move whiteMoves, Move blackMoves) {
        String inCaseOfPromotion = whiteMoves.promotion() == null ? null : AlgebraicNotation.pieceToType(whiteMoves.promotion()).getPieceType();
        sendMessage(playerChess, username, Message.builder(MessageType.MOVE)
                .gameID(gameID)
                .from(whiteMoves.from())
                .to(whiteMoves.to())
                .inCaseOfPromotion(inCaseOfPromotion)
                .build());

        await().until(() -> {
            final boolean firstUserReceivedMoveMessage = USER_MESSAGES.user1.stream().anyMatch(message -> validateMove(whiteMoves, message));
            final boolean secondUserReceivedMoveMessage = USER_MESSAGES.user2.stream().anyMatch(message -> validateMove(whiteMoves, message));
            return firstUserReceivedMoveMessage && secondUserReceivedMoveMessage;
        });

        if (Objects.isNull(blackMoves)) {
            return;
        }

        String inCaseOfPromotionForBlack = blackMoves.promotion() == null ? null : AlgebraicNotation.pieceToType(blackMoves.promotion()).getPieceType().toLowerCase();

        sendMessage(opponentChess, opponentUsername, Message.builder(MessageType.MOVE)
                .gameID(gameID)
                .from(blackMoves.from())
                .to(blackMoves.to())
                .inCaseOfPromotion(inCaseOfPromotionForBlack)
                .build());

        await().atMost(Duration.ofSeconds(1)).until(() -> {
            final boolean firstUserReceivedMoveMessage = USER_MESSAGES.user1.stream().anyMatch(message -> validateMove(blackMoves, message));
            final boolean secondUserReceivedMoveMessage = USER_MESSAGES.user2.stream().anyMatch(message -> validateMove(whiteMoves, message));
            return firstUserReceivedMoveMessage && secondUserReceivedMoveMessage;
        });
    }

    private static boolean validateMove(Move move, Message message) {
        if (message.type().equals(MessageType.GAME_ENDED)) {
            return true;
        }

        Coordinate from = move.from();
        Coordinate to = move.to();

        if (Objects.isNull(message.PGN()) || message.PGN().isBlank()) {
            return false;
        }

        String[] arrayOfMoves = message.PGN().split(" ");
        final String lastMove = arrayOfMoves[arrayOfMoves.length - 1].equals("...") ?
                arrayOfMoves[arrayOfMoves.length - 2] :
                arrayOfMoves[arrayOfMoves.length - 1];

        if (lastMove.contains("O-O")) {
            return true;
        }

        return lastMove.contains(from + "-" + to) || lastMove.contains(from + "x" + to);
    }
}
