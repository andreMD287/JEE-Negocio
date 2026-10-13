package co.edu.javeriana.recursos.business.contract;

import co.edu.javeriana.recursos.domain.model.Priority;
import co.edu.javeriana.recursos.domain.model.RequestStatus;

/**
 * Vista de solo lectura de una solicitud, devuelta por el tier de negocio.
 */
public record ResourceRequestView(
        Long id,
        String disasterDescription,
        String resourceType,
        int quantity,
        Priority priority,
        RequestStatus status) {
}
