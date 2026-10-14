package co.edu.javeriana.recursos.api.exception;

import co.edu.javeriana.recursos.business.exception.AlreadyAttendedException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Solicitud ya atendida → HTTP 409.
 */
@Provider
public class AlreadyAttendedExceptionMapper implements ExceptionMapper<AlreadyAttendedException> {

    @Override
    public Response toResponse(AlreadyAttendedException exception) {
        return ErrorResponses.of(Response.Status.CONFLICT, "ALREADY_ATTENDED", exception.getMessage());
    }
}
