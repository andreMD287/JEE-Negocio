package co.edu.javeriana.recursos.business.contract;

/**
 * Datos de entrada para registrar una solicitud.
 *
 * <p>No lleva id ni estado: los asigna el sistema. La prioridad llega como
 * texto y el EJB la valida y la convierte a {@code Priority}, para que un valor
 * inválido se reporte como error de negocio. La cantidad es {@code Integer}
 * para que el EJB también pueda detectar cuando no se envía.</p>
 */
public record CreateResourceRequestCommand(
        String disasterDescription,
        String resourceType,
        Integer quantity,
        String priority) {
}
