package core.project.chess.application.dto.gamesession;

public enum MessageType {
    GAME_INIT,
    MOVE,
    RETURN_MOVE,
    AGREEMENT,
    RESIGNATION,
    TREE_FOLD,

    MESSAGE,

    ERROR,
    INVITATION,
    INFO,
    USER_INFO,
    FEN_PGN,
    GAME_START_INFO,
    GAME_ENDED
}
