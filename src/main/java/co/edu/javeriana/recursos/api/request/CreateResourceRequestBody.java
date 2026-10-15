package co.edu.javeriana.recursos.api.request;

import co.edu.javeriana.recursos.business.contract.CreateResourceRequestCommand;

/**
 * Cuerpo JSON de {@code POST /requests}.
 *
 * <p>No tiene anotaciones de Bean Validation a propósito: las reglas se
 * validan en el EJB. Los tipos son "abiertos" ({@code Integer}, {@code String})
 * para que los datos faltantes o inválidos lleguen al tier de negocio.</p>
 */
public class CreateResourceRequestBody {

    private String disasterDescription;
    private String resourceType;
    private Integer quantity;
    private String priority;

    public CreateResourceRequestCommand toCommand() {
        return new CreateResourceRequestCommand(disasterDescription, resourceType, quantity, priority);
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
