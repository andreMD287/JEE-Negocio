package co.edu.javeriana.recursos.business.exception;

import jakarta.ejb.ApplicationException;

/**
 * Los datos de una solicitud no cumplen las reglas de negocio (HTTP 400).
 *
 * <p>{@code @ApplicationException} evita que el contenedor la envuelva en
 * {@code EJBException}, de modo que llega tal cual al cliente del EJB.</p>
 */
@ApplicationException(rollback = true)
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
