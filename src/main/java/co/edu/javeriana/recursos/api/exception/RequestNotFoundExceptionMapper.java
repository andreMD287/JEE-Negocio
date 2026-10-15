package co.edu.javeriana.recursos.api.exception;

import co.edu.javeriana.recursos.business.exception.RequestNotFoundException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Solicitud inexistente → HTTP 404.
 */
@Provider
public class RequestNotFoundExceptionMapper implements ExceptionMapper<RequestNotFoundException> {

    @Override
    public Response toResponse(RequestNotFoundException exception) {
        return ErrorResponses.of(Response.Status.NOT_FOUND, "REQUEST_NOT_FOUND", exception.getMessage());
    }
}
