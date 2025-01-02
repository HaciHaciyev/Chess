package core.project.chess.infrastructure.dal.util.exceptions;

public class RepositoryDataException extends RuntimeException {

    public RepositoryDataException() {
    }

    public RepositoryDataException(String message) {
        super(message);
    }

    public RepositoryDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepositoryDataException(Throwable cause) {
        super(cause);
    }

    public RepositoryDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
