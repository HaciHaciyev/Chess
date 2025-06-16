package core.project.chess.domain.commons.exceptions;

public class DomainValidationException extends IllegalArgumentException {

    public DomainValidationException() {
        super();
    }

    public DomainValidationException(String s) {
        super(s);
    }

    public DomainValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DomainValidationException(Throwable cause) {
        super(cause);
    }
}
