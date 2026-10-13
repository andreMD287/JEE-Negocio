package co.edu.javeriana.recursos.domain.model;

import java.util.Objects;

/**
 * Solicitud de recursos registrada durante una emergencia.
 *
 * <p>Es un objeto de dominio puro: no depende de JAX-RS, EJB ni del mecanismo
 * de almacenamiento. Las reglas de negocio se validan en el EJB, no aquí.</p>
 */
public class ResourceRequest {

    private Long id;
    private String disasterDescription;
    private String resourceType;
    private int quantity;
    private Priority priority;
    private RequestStatus status;

    public ResourceRequest() {
    }

    public ResourceRequest(Long id, String disasterDescription, String resourceType,
                           int quantity, Priority priority, RequestStatus status) {
        this.id = id;
        this.disasterDescription = disasterDescription;
        this.resourceType = resourceType;
        this.quantity = quantity;
        this.priority = priority;
        this.status = status;
    }

    /**
     * Copia independiente de esta solicitud. El repositorio la usa para que
     * los cambios hechos fuera de él solo se persistan llamando a {@code save}.
     */
    public ResourceRequest copy() {
        return new ResourceRequest(id, disasterDescription, resourceType, quantity, priority, status);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisasterDescription() {
        return disasterDescription;
    }

    public void setDisasterDescription(String disasterDescription) {
        this.disasterDescription = disasterDescription;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResourceRequest other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ResourceRequest{id=" + id
                + ", disasterDescription='" + disasterDescription + '\''
                + ", resourceType='" + resourceType + '\''
                + ", quantity=" + quantity
                + ", priority=" + priority
                + ", status=" + status + '}';
    }
}
