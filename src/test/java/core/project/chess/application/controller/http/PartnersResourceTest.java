package core.project.chess.application.controller.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import core.project.chess.application.controller.ws.MessagingTestResource;
import core.project.chess.application.dto.chess.Message;
import core.project.chess.application.dto.chess.MessageType;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Session;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;
import testUtils.AuthInfo;
import testUtils.AuthUtils;
import testUtils.WSClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.*;

@QuarkusTest
@WithTestResource(MessagingTestResource.class)
class PartnersResourceTest {

    @Inject
    AuthUtils authUtils;

    @Test
    void partners() throws JsonProcessingException {
        AuthInfo info = authUtils.fullLoginProcess();
        String hostToken = info.serverResponse().get("token");
        String[] generatedPartners = addPartnersFor(info.username(), hostToken, 5);

        String[] response = given().contentType("application/json")
                .param("pageNumber", 0)
                .header("Authorization", "Bearer " + hostToken)
                .when()
                .get("chessland/account/partners")
                .peek()
                .then()
                .statusCode(200)
                .extract().body().as(String[].class);

        assertThat(generatedPartners).contains(response);
    }

    private String[] addPartnersFor(String host, String userToken, int numOfPartners) throws JsonProcessingException {
        String template = ConfigProvider.getConfig().getValue("messaging.api.url", String.class);
        URI hostURI = URI.create(template + "?token=%s".formatted(userToken));

        String[] partners = new String[numOfPartners];
        for (int i = 0; i < numOfPartners; i++) {
            AuthInfo partnerInfo = authUtils.fullLoginProcess();
            URI partnerURI = URI.create(template + "?token=%s".formatted(partnerInfo.serverResponse().get("token")));

            try (Session hostSession = ContainerProvider
                    .getWebSocketContainer().connectToServer(WSClient.class, hostURI);

                 Session partnerSession = ContainerProvider
                         .getWebSocketContainer().connectToServer(WSClient.class, partnerURI)) {

                Thread.sleep(Duration.ofMillis(500));

                Message hostRequest = Message.builder(MessageType.PARTNERSHIP_REQUEST)
                        .partner(partnerInfo.username())
                        .message("")
                        .build();

                WSClient.sendMessage(hostSession, host, hostRequest);

                Thread.sleep(Duration.ofMillis(100));

                Message partnerRequest = Message.builder(MessageType.PARTNERSHIP_REQUEST)
                        .partner(host)
                        .message("")
                        .build();

                WSClient.sendMessage(partnerSession, partnerInfo.username(), partnerRequest);

                Thread.sleep(Duration.ofMillis(100));

                hostSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));
                partnerSession.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "reached end of context"));

                partners[i] = partnerInfo.username();
            } catch (DeploymentException | IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return partners;
    }
}