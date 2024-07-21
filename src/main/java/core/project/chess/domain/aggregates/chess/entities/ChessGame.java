package core.project.chess.domain.aggregates.chess.entities;

import core.project.chess.domain.aggregates.chess.value_objects.Color;
import core.project.chess.domain.aggregates.chess.value_objects.TimeControllingTYPE;
import core.project.chess.domain.aggregates.user.entities.UserAccount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChessGame {
    private final ChessBoard chessBoard;
    private final UserAccount playerForWhite;
    private final UserAccount playerForBlack;
    private final Color activePlayer;
    private final TimeControllingTYPE timeControllingTYPE;

    // TODO
    private class TimeManagement {}
}
