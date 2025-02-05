package core.project.chess.application.controller.ws.chess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.project.chess.application.controller.ws.messaging.MessagingTestResource;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import core.project.chess.domain.chess.entities.ChessGame;
import core.project.chess.domain.chess.enumerations.Coordinate;
import core.project.chess.infrastructure.ws.MessageDecoder;
import core.project.chess.infrastructure.ws.MessageEncoder;
import io.quarkus.logging.Log;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.*;
import testUtils.LoginForm;
import testUtils.RegistrationForm;
import testUtils.UserDBManagement;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@WithTestResource(MessagingTestResource.class)
class ChessWSTest {

    private final Messages USER_MESSAGES = Messages.newInstance();

    private final Messages CHESS_MESSAGES = Messages.newInstance();

    public static final String LOGIN = "/chessland/account/login/";

    public static final String REGISTRATION = "/chessland/account/registration";

    public static final String TOKEN_VERIFICATION = "/chessland/account/token/verification";

    @TestHTTPResource("/chessland/chess-game")
    URI serverURI;

    URI userSessionURI;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserDBManagement userDBManagement;

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
        String messagingURI = ConfigProvider.getConfig().getValue("messaging.api.url", String.class);
        userSessionURI = URI.create(messagingURI);
        System.out.println();
        System.out.println("---------------------------------------BREAK---------------------------------------");
        System.out.println();
    }

    @Disabled("Temporary shutdown due to technical reasons.")
    @Test
    @DisplayName("Custom game matchmaking")
    void customGameMatchmaking() throws Exception {
        RegistrationForm whiteForm = registerRandom();
        enableAccount(whiteForm);
        String whiteToken = login(whiteForm);

        RegistrationForm blackForm = registerRandom();
        enableAccount(blackForm);
        String blackToken = login(blackForm);

        try (Session wSession = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, serverURIWithToken(serverURI, whiteToken));

             Session bSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, serverURIWithToken(serverURI, blackToken))
        ) {

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
        RegistrationForm whiteForm = registerRandom();
        enableAccount(whiteForm);
        String whiteToken = login(whiteForm);

        RegistrationForm blackForm = registerRandom();
        enableAccount(blackForm);
        String blackToken = login(blackForm);

        try (Session wMessagingSession = ContainerProvider
                      .getWebSocketContainer()
                      .connectToServer(WSClient.class, serverURIWithToken(userSessionURI, whiteToken));

             Session bMessagingSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, serverURIWithToken(userSessionURI, blackToken));

             Session wChessSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, serverURIWithToken(serverURI, whiteToken));

             Session bChessSession = ContainerProvider
                     .getWebSocketContainer()
                     .connectToServer(WSClient.class, serverURIWithToken(serverURI, blackToken))
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

            assertThat(USER_MESSAGES.user2().take()).matches(m -> m.type() == MessageType.USER_INFO && m.message().contains("invite you"));

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

    private void sendMessage(Session session, String username, Message message) {
        Log.infof("%s sending -> %s", username, message);
        session.getAsyncRemote().sendObject(message);
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Log.errorf("Error: %s.", e);
        }
    }

    private URI serverURIWithToken(URI uri, String token) {
        return URI.create(uri + "?token=%s".formatted(token));
    }

    private String login(RegistrationForm account) throws JsonProcessingException {
        LoginForm loginForm = LoginForm.from(account);
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        return given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(200)
                .body("token", notNullValue(), "refreshToken", notNullValue())
                .extract()
                .body()
                .jsonPath()
                .get("token")
                .toString();
    }

    private void enableAccount(RegistrationForm account) {
        String emailConfirmationToken = userDBManagement.getToken(account.username());

        given().queryParam("token", emailConfirmationToken)
                .when().patch(TOKEN_VERIFICATION)
                .then()
                .statusCode(200)
                .body(containsString("account is enabled"));
    }

    private RegistrationForm registerRandom() throws JsonProcessingException {
        RegistrationForm account = RegistrationForm.randomForm();
        String accountJSON = objectMapper.writer().writeValueAsString(account);

        given().contentType("application/json")
                .body(accountJSON)
                .when().post(REGISTRATION)
                .then()
                .statusCode(200)
                .body(containsString("successful"));

        return account;
    }

    @ClientEndpoint(decoders = MessageDecoder.class, encoders = MessageEncoder.class)
    static class WSClient {

        @Inject
        JWTParser jwtParser;

        @OnOpen
        public void onOpen(Session session) {
            String username = extractToken(session);
            Log.infof("User %s connected to the server -> %s", username, session.getRequestURI().getPath());
        }

        @OnClose
        public void onClose(Session session, CloseReason closeReason) {
            String username = extractToken(session);
            Log.infof("%s's session closed. Reason -> %s", username, closeReason.getReasonPhrase());
        }

        private String extractToken(Session session) {
            String query = session.getQueryString();
            String token = query.substring(query.indexOf("=") + 1);

            try {
                return jwtParser.parse(token).getName();
            } catch (ParseException e) {
                Log.info(e);
            }

            return "";
        }
    }
}
