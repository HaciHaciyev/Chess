package core.project.chess.infrastructure.exception_handler;

import core.project.chess.domain.commons.exceptions.DomainValidationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ExceptionHandler implements ExceptionMapper<DomainValidationException> {

  @Override
  public Response toResponse(DomainValidationException e) {
    return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
  }
}