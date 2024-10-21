package core.project.chess.infrastructure.broker;

import core.project.chess.domain.aggregates.user.value_objects.Username;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PartnershipRequestsProducer {

    public final void send(final Username username, final String message) {
        /*TODO*/
    }
}
