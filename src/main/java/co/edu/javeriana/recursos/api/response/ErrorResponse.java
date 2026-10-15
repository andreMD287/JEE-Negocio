package co.edu.javeriana.recursos.api.response;

import java.time.Instant;

/**
 * Respuesta uniforme de error de la API.
 *
 * @param code      código estable para el cliente, p. ej. {@code REQUEST_NOT_FOUND}
 * @param message   descripción legible del error
 * @param timestamp instante del error en ISO-8601 (UTC)
 */
public record ErrorResponse(String code, String message, String timestamp) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, Instant.now().toString());
    }
}
