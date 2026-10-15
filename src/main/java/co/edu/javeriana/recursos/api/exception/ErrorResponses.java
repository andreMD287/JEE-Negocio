package co.edu.javeriana.recursos.api.exception;

import co.edu.javeriana.recursos.api.response.ErrorResponse;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Construye respuestas HTTP con el cuerpo uniforme {@link ErrorResponse}.
 */
final class ErrorResponses {

    private ErrorResponses() {
    }

    static Response of(Response.Status status, String code, String message) {
        return of(status.getStatusCode(), code, message);
    }

    static Response of(int status, String code, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorResponse.of(code, message))
                .build();
    }
}
