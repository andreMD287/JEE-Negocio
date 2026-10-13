package co.edu.javeriana.recursos.business.exception;

import jakarta.ejb.ApplicationException;

/**
 * No existe una solicitud con el identificador indicado (HTTP 404).
 */
@ApplicationException(rollback = true)
public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(Long id) {
        super("No existe la solicitud con id " + id);
    }
}
