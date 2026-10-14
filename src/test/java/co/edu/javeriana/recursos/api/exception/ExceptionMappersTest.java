package co.edu.javeriana.recursos.api.exception;

import co.edu.javeriana.recursos.api.response.ErrorResponse;
import co.edu.javeriana.recursos.business.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.business.exception.BusinessValidationException;
import co.edu.javeriana.recursos.business.exception.RequestNotFoundException;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExceptionMappersTest {

    private static ErrorResponse body(Response response) {
        return (ErrorResponse) response.getEntity();
    }

    @Test
    @DisplayName("BusinessValidationException → 400 VALIDATION_ERROR")
    void validation() {
        Response response = new BusinessValidationExceptionMapper()
                .toResponse(new BusinessValidationException("La cantidad debe ser mayor que cero"));

        assertEquals(400, response.getStatus());
        assertEquals("VALIDATION_ERROR", body(response).code());
        assertEquals("La cantidad debe ser mayor que cero", body(response).message());
        assertNotNull(body(response).timestamp());
    }

    @Test
    @DisplayName("RequestNotFoundException → 404 REQUEST_NOT_FOUND")
    void notFound() {
        Response response = new RequestNotFoundExceptionMapper().toResponse(new RequestNotFoundException(10L));

        assertEquals(404, response.getStatus());
        assertEquals("REQUEST_NOT_FOUND", body(response).code());
        assertEquals("No existe la solicitud con id 10", body(response).message());
    }

    @Test
    @DisplayName("AlreadyAttendedException → 409 ALREADY_ATTENDED")
    void alreadyAttended() {
        Response response = new AlreadyAttendedExceptionMapper().toResponse(new AlreadyAttendedException(3L));

        assertEquals(409, response.getStatus());
        assertEquals("ALREADY_ATTENDED", body(response).code());
    }

    @Test
    @DisplayName("Error no controlado → 500 sin exponer el mensaje interno")
    void unexpected() {
        Response response = new UnexpectedExceptionMapper().toResponse(new IllegalStateException("detalle interno"));

        assertEquals(500, response.getStatus());
        assertEquals("INTERNAL_ERROR", body(response).code());
        assertEquals("Ocurrió un error inesperado", body(response).message());
    }

    @Test
    @DisplayName("JSON mal formado → 400 MALFORMED_JSON")
    void malformedJson() {
        Response response = new UnexpectedExceptionMapper()
                .toResponse(new RuntimeException(new JsonbException("Unexpected char")));

        assertEquals(400, response.getStatus());
        assertEquals("MALFORMED_JSON", body(response).code());
    }

    @Test
    @DisplayName("Cuerpo vacío → 400 EMPTY_BODY")
    void emptyBody() {
        Response response = new UnexpectedExceptionMapper()
                .toResponse(new BadRequestException(new NoContentException("vacío")));

        assertEquals(400, response.getStatus());
        assertEquals("EMPTY_BODY", body(response).code());
    }

    @Test
    @DisplayName("Errores HTTP de JAX-RS conservan su código")
    void webApplicationExceptionKeepsStatus() {
        Response notFound = new UnexpectedExceptionMapper().toResponse(new NotFoundException());
        Response notAllowed = new UnexpectedExceptionMapper()
                .toResponse(new NotAllowedException("GET"));

        assertEquals(404, notFound.getStatus());
        assertEquals("NOT_FOUND", body(notFound).code());
        assertEquals(405, notAllowed.getStatus());
        assertEquals("METHOD_NOT_ALLOWED", body(notAllowed).code());
    }
}
