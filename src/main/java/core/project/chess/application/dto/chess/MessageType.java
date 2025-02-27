package core.project.chess.application.dto.chess;

public enum MessageType {
    GAME_INIT,
    MOVE,
    RETURN_MOVE,
    AGREEMENT,
    RESIGNATION,
    TREE_FOLD,

    PUZZLE,
    PUZZLE_MOVE,

    MESSAGE,

    ERROR,
    INVITATION,
    INFO,
    USER_INFO,
    FEN_PGN,
    GAME_START_INFO,
    GAME_ENDED,

    PARTNERSHIP_REQUEST,
}
