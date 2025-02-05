package core.project.chess.application.controller.ws.messaging;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class MessagingTestResource implements QuarkusTestResourceLifecycleManager {

    private GenericContainer<?> messaging = new GenericContainer<>(DockerImageName.parse("aingrace/chessland:messaging"))
            .withExposedPorts(9091)
            .withAccessToHost(true)
            .withEnv("QUARKUS_PROFILE", "testcontainer")
            .withStartupAttempts(3);

    @Override
    public Map<String, String> start() {
        Testcontainers.exposeHostPorts(30010, 31001);
        messaging.start();
        return Map.of("messaging.api.url", "http://" + messaging.getHost() + ":" + messaging.getMappedPort(9091) + "/user-session");
    }

    @Override
    public void stop() {
        if (messaging != null) {
            messaging.stop();
            messaging.close();
            messaging = null;
        }
    }

    @Override
    public int order() {
        return 2;
    }
}
