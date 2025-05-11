package core.project.chess.application.controller.ws;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

public class PuzzlerTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PuzzlerTestResource.class);

    private GenericContainer<?> puzzler = new GenericContainer<>(DockerImageName.parse("aingrace/chessland:puzzler"))
            .withExposedPorts(9092)
            .withAccessToHost(true)
            .withStartupAttempts(3);

    @Override
    public Map<String, String> start() {
        Testcontainers.exposeHostPorts(30010, 31001);
        puzzler.start();

        puzzler.followOutput(new Slf4jLogConsumer(LOGGER));
        LOGGER.info("Container logs:\n{}", puzzler.getLogs());

        return Map.of("puzzler.api.url", "http://" + puzzler.getHost() + ":" + puzzler.getMappedPort(9092));
    }

    @Override
    public void stop() {
        if (puzzler != null) {
            puzzler.stop();
            puzzler.close();
            puzzler = null;
        }
    }

    @Override
    public int order() {
        return 2;
    }
}