package core.project.chess.domain.commons.value_objects;

public enum GameResult {
    DRAW,
    WHITE_WIN,
    BLACK_WIN,
    NONE;

    public static GameResult of(int res) {
        return switch (res) {
            case 0 -> DRAW;
            case 1 -> WHITE_WIN;
            case -1 -> BLACK_WIN;
            default -> throw new IllegalArgumentException("Invalid result: " + res);
        };
    }
}