package core.project.chess.WS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import testUtils.LoginForm;
import testUtils.RegistrationForm;
import core.project.chess.application.dto.gamesession.Message;
import core.project.chess.domain.aggregates.chess.entities.ChessGame;
import core.project.chess.domain.aggregates.chess.enumerations.Coordinate;
import io.quarkus.logging.Log;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import testUtils.UserDBManagement;

import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@Slf4j
@QuarkusTest
class ChessWSTest {

    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    public static final String REGISTRATION = "/chessland/account/registration";

    public static final String LOGIN = "/chessland/account/login/";

    public static final String TOKEN_VERIFICATION = "/chessland/account/token/verification";

    @TestHTTPResource("/chessland/chess-game")
    URI serverURI;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    UserDBManagement userDBManagement;

    @AfterEach
    void purge() {
        MESSAGES.clear();
    }

    @BeforeEach
    void printLineBreak() {
        System.out.println();
        System.out.println("---------------------------------------BREAK---------------------------------------");
        System.out.println();
    }

    @Test
    @DisplayName("Successful connection")
    void successful_Connection() throws Exception {
        RegistrationForm account = registerRandom();
        enableAccount(account);
        String token = login(account);

        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(WSClient.class, serverURIWithToken(token))) {
            sendMessage(session, account.username(), "Hello, world");
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
        }
    }

    @Test
    @DisplayName("Initialize game")
    void initialize_Game() throws Exception {
        RegistrationForm account = registerRandom();
        enableAccount(account);
        String token = login(account);

        try (Session session = ContainerProvider.getWebSocketContainer().connectToServer(WSClient.class, serverURIWithToken(token))) {
            sendMessage(session, account.username(), Message.gameInit("WHITE", ChessGame.TimeControllingTYPE.RAPID).write().orElseThrow());
            session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
        }
    }

    @Test
    @DisplayName("Custom game matchmaking")
    @Disabled
    void custom_Game_Matchmaking() throws Exception {
        RegistrationForm whiteForm = registerRandom();
        enableAccount(whiteForm);
        String whiteToken = login(whiteForm);

        RegistrationForm blackForm = registerRandom();
        enableAccount(blackForm);
        String blackToken = login(blackForm);

        try (Session wSession = ContainerProvider.getWebSocketContainer().connectToServer(WSClient.class, serverURIWithToken(whiteToken));
             Session bSession = ContainerProvider.getWebSocketContainer().connectToServer(WSClient.class, serverURIWithToken(blackToken))) {

            String wName = whiteForm.username();
            sendMessage(wSession, wName, Message.gameInit("WHITE", ChessGame.TimeControllingTYPE.RAPID).write().orElseThrow());

            String bName = blackForm.username();
            sendMessage(bSession, bName, Message.gameInit("BLACK", ChessGame.TimeControllingTYPE.RAPID).write().orElseThrow());

            String gameID = extractGameID();
            //1
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.e2, Coordinate.e4).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.e7, Coordinate.e5).write().orElseThrow());
            //2
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.b8, Coordinate.c6).write().orElseThrow());
            //3
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.b1, Coordinate.c3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.g8, Coordinate.f6).write().orElseThrow());
            //4
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f1, Coordinate.c4).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.d7, Coordinate.d6).write().orElseThrow());
            //5
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.d2, Coordinate.d3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.c8, Coordinate.e6).write().orElseThrow());
            //6
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.e1, Coordinate.g1).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.e6, Coordinate.c4).write().orElseThrow());
            //7
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.d3, Coordinate.c4).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.f8, Coordinate.e7).write().orElseThrow());
            //8
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.d1, Coordinate.e2).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.d8, Coordinate.d7).write().orElseThrow());
            //9
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.c1, Coordinate.e3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.e8, Coordinate.c8).write().orElseThrow());
            //10
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.a1, Coordinate.d1).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.f6, Coordinate.g4).write().orElseThrow());
            //11
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.c3, Coordinate.d5).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.h7, Coordinate.h6).write().orElseThrow());
            //12
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.b2, Coordinate.b3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.e7, Coordinate.g5).write().orElseThrow());
            //13
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.h2, Coordinate.h3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.g4, Coordinate.e3).write().orElseThrow());
            //14
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f2, Coordinate.e3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.a7, Coordinate.a6).write().orElseThrow());
            //15
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.h3, Coordinate.h4).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.g5, Coordinate.f6).write().orElseThrow());
            //16
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.h2).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.f6, Coordinate.h4).write().orElseThrow());
            //17
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f1, Coordinate.f3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.h8, Coordinate.f8).write().orElseThrow());
            //18
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.f5).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.g7, Coordinate.g6).write().orElseThrow());
            //19
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f5, Coordinate.f3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.f7, Coordinate.f5).write().orElseThrow());
            //20
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f3, Coordinate.h3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.g6, Coordinate.g5).write().orElseThrow());
            //21
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.e2, Coordinate.h5).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.f5, Coordinate.e4).write().orElseThrow());
            //22
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.h5, Coordinate.h6).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.f8, Coordinate.f2).write().orElseThrow());
            //23
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.g2, Coordinate.g3).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.d7, Coordinate.h3).write().orElseThrow());
            //24
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.d5, Coordinate.e7).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.c6, Coordinate.e7).write().orElseThrow());
            //25
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.g1, Coordinate.f2).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.h3, Coordinate.h2).write().orElseThrow());
            //26
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.f2, Coordinate.f1).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.h2, Coordinate.g3).write().orElseThrow());
            //27
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.h6, Coordinate.e6).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.d8, Coordinate.d7).write().orElseThrow());
            //28
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.e6, Coordinate.g8).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.d7, Coordinate.d8).write().orElseThrow());
            //29
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.g8, Coordinate.e6).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.c8, Coordinate.b8).write().orElseThrow());
            //30
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.e6, Coordinate.e7).write().orElseThrow());
            sendMessage(bSession, bName, Message.move(gameID, Coordinate.g3, Coordinate.f2).write().orElseThrow());

            //31
            sendMessage(wSession, wName, Message.move(gameID, Coordinate.d1, Coordinate.d2).write().orElseThrow());

            wSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
            bSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
        }
    }

    private String extractGameID() {
        for (String message : MESSAGES) {
            try {
                Message msg = objectMapper.readValue(message, Message.class);

                if (msg.gameID() != null) {
                    return msg.gameID();
                }

            } catch (JsonProcessingException e) {
                Log.info(e);
            }
        }

        return "";
    }


    private void sendMessage(Session session, String username, String message) {
        Log.infof("%s sending -> %s", username, message);
        session.getAsyncRemote().sendText(message);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Log.info(e);
        }
    }

    private URI serverURIWithToken(String token) {
        return URI.create(serverURI + "?token=%s".formatted(token));
    }

    private String login(RegistrationForm account) throws JsonProcessingException {
        LoginForm loginForm = LoginForm.from(account);
        String loginJSON = objectMapper.writer().writeValueAsString(loginForm);

        return given().contentType("application/json")
                .body(loginJSON)
                .when().post(LOGIN)
                .then()
                .statusCode(200)
                .body("token", notNullValue(), "refresh-token", notNullValue())
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

    @ClientEndpoint
    static class WSClient {

        @Inject
        JWTParser jwtParser;

        @OnOpen
        public void onOpen(Session session) {
            String username = extractToken(session);
            Log.infof("User %s connected to the server", username);
        }

        @OnMessage
        public void onMessage(Session session, String message) {
            String username = extractToken(session);
            MESSAGES.add(message);
            Log.infof("%s received -> %s", username, message);
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
