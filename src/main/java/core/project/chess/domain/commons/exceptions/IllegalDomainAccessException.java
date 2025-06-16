package core.project.chess.domain.commons.exceptions;

public class IllegalDomainAccessException extends DomainValidationException {
    public IllegalDomainAccessException(String message) {
        super(message);
    }
}
