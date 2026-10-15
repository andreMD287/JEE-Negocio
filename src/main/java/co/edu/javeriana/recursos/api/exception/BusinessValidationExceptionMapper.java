package co.edu.javeriana.recursos.api.exception;

import co.edu.javeriana.recursos.business.exception.BusinessValidationException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Datos que violan una regla de negocio → HTTP 400.
 */
@Provider
public class BusinessValidationExceptionMapper implements ExceptionMapper<BusinessValidationException> {

    @Override
    public Response toResponse(BusinessValidationException exception) {
        return ErrorResponses.of(Response.Status.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }
}
