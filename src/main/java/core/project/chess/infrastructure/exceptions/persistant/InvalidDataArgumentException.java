package core.project.chess.infrastructure.exceptions.persistant;

public class InvalidDataArgumentException extends RepositoryDataException {

    public InvalidDataArgumentException() {
    }

    public InvalidDataArgumentException(String message) {
        super(message);
    }

    public InvalidDataArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidDataArgumentException(Throwable cause) {
        super(cause);
    }

    public InvalidDataArgumentException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
