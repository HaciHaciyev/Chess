package core.project.chess.application.controller.ws;

import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import io.quarkus.logging.Log;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import testUtils.AuthUtils;
import testUtils.RegistrationForm;
import testUtils.WSClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;

import static org.awaitility.Awaitility.await;
import static testUtils.WSClient.sendMessage;

@QuarkusTest
@Disabled("For technical purpose. Need to fill the database with puzzles.")
class PuzzlesTest {

    private final LinkedBlockingQueue<Message> CHESS_MESSAGES = new LinkedBlockingQueue<>();

    @TestHTTPResource("/chessland/chess-game")
    URI serverURI;

    @Inject
    AuthUtils authUtils;

    @AfterEach
    void purge() {
        CHESS_MESSAGES.clear();
    }

    @BeforeEach
    void printLineBreak() {
        System.out.println();
        System.out.println("---------------------------------------BREAK---------------------------------------");
        System.out.println();
    }

    @Test
    void testPuzzleSolving() throws IOException, DeploymentException {
        RegistrationForm playerForm = authUtils.registerRandom();
        authUtils.enableAccount(playerForm);
        String jwt = authUtils.login(playerForm);

        Session clientSession = ContainerProvider
                .getWebSocketContainer()
                .connectToServer(WSClient.class, authUtils.serverURIWithToken(serverURI, jwt));

        clientSession.addMessageHandler(Message.class, message -> {
            Log.infof("User %s received message: %s", message);
            CHESS_MESSAGES.offer(message);
        });

        sendMessage(clientSession, playerForm.username(), Message.builder(MessageType.PUZZLE).build());
        await().atMost(Duration.ofMillis(125)).until(() -> CHESS_MESSAGES.stream()
                .anyMatch(message -> message.type().equals(MessageType.PUZZLE) && message.PGN() != null && message.FEN() != null && message.gameID() != null));
    }
}
