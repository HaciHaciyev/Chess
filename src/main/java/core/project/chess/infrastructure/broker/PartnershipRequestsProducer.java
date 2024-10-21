package core.project.chess.infrastructure.broker;

import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class PartnershipRequestsProducer {

    @Channel("partnership-requests-out")
    private final Emitter<Record<String, String>> sender;

    PartnershipRequestsProducer(Emitter<Record<String, String>> sender) {
        this.sender = sender;
    }

    public final void send(final Record<String, String> message) {
        sender
                .send(message)
                .whenComplete((success, failure) -> {
                    if (Objects.isNull(failure)) {
                        Log.errorf("Failure when trying to send {%s} to Broker.", message.toString());
                        return;
                    }

                    Log.infof("Successfully send message {%s} to Broker.", message.toString());
                });
    }

    /** DO NOT TO USE YET!!!*/
    public final void sendWithRetry(final Record<String, String> message, int retryCount) {
        CompletableFuture.runAsync(() -> send(message))
                .exceptionally(ex -> {
                    if (retryCount > 0) {
                        Log.errorf("Can`t send a message {%s} to broker, try again. Retry count: %d.", message.toString(), retryCount);
                        sendWithRetry(message, retryCount - 1);
                        return null;
                    }

                    Log.infof("Successfully send a message {%s}.", message.toString());
                    return null;
                });
    }
}
