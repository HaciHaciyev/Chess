package core.project.chess.infrastructure.broker;

import core.project.chess.domain.aggregates.user.value_objects.Username;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PartnershipRequestsConsumer {

    public List<String> getMessagesForUser(final Username username) {
        /*TODO*/
        return new ArrayList<>();
    }
}