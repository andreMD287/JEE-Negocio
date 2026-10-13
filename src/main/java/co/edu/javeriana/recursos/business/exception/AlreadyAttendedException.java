package co.edu.javeriana.recursos.business.exception;

import jakarta.ejb.ApplicationException;

/**
 * Se intentó atender una solicitud que ya está ATENDIDA (HTTP 409).
 */
@ApplicationException(rollback = true)
public class AlreadyAttendedException extends RuntimeException {

    public AlreadyAttendedException(Long id) {
        super("La solicitud con id " + id + " ya fue atendida");
    }
}
