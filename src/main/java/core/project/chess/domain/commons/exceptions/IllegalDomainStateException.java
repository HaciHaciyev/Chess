package core.project.chess.domain.commons.exceptions;

public class IllegalDomainStateException extends DomainValidationException {
    public IllegalDomainStateException(String message) {
        super(message);
    }
}
