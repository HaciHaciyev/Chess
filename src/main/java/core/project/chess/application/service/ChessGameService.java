package core.project.chess.application.service;

import core.project.chess.application.model.InboundGameParameters;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import org.springframework.stereotype.Service;

@Service
public class ChessGameService {

    public boolean isOpponent(
            final UserAccount player, final InboundGameParameters gameParameters,
            final UserAccount opponent, final InboundGameParameters opponentGameParameters
    ) {
        final boolean sameUser = player.getId().equals(opponent.getId());
        if (sameUser) {
            return false;
        }

        final boolean sameTimeControlling = gameParameters.timeControllingTYPE().equals(opponentGameParameters.timeControllingTYPE());
        if (!sameTimeControlling) {
            return false;
        }

        final boolean colorNotSpecified = gameParameters.color() == null || opponentGameParameters.color() == null;
        if (colorNotSpecified) {
            return true;
        }

        final boolean sameColor = gameParameters.color().equals(opponentGameParameters.color());
        return !sameColor;
    }
}
