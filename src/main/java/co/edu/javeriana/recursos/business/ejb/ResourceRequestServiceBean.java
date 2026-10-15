package co.edu.javeriana.recursos.business.ejb;

import co.edu.javeriana.recursos.business.contract.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.business.contract.ResourceRequestService;
import co.edu.javeriana.recursos.business.contract.ResourceRequestView;
import co.edu.javeriana.recursos.business.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.business.exception.BusinessValidationException;
import co.edu.javeriana.recursos.business.exception.RequestNotFoundException;
import co.edu.javeriana.recursos.business.port.ResourceRequestRepository;
import co.edu.javeriana.recursos.domain.model.Priority;
import co.edu.javeriana.recursos.domain.model.RequestStatus;
import co.edu.javeriana.recursos.domain.model.ResourceRequest;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Locale;

/**
 * Implementación EJB de las reglas de negocio de solicitudes.
 */
@Stateless
public class ResourceRequestServiceBean implements ResourceRequestService {

    @Inject
    ResourceRequestRepository repository;

    @Override
    public ResourceRequestView register(CreateResourceRequestCommand command) {
        validateCommand(command);

        ResourceRequest request = new ResourceRequest(
                null,
                command.disasterDescription().trim(),
                command.resourceType().trim(),
                command.quantity(),
                parsePriority(command.priority()),
                RequestStatus.PENDIENTE
        );

        return toView(repository.save(request));
    }

    @Override
    public List<ResourceRequestView> findAll() {
        return repository.findAll()
                .stream()
                .map(ResourceRequestServiceBean::toView)
                .toList();
    }

    @Override
    public ResourceRequestView findById(Long id) {
        return toView(findExisting(id));
    }

    @Override
    public ResourceRequestView attend(Long id) {
        ResourceRequest request = findExisting(id);

        if (request.getStatus() == RequestStatus.ATENDIDA) {
            throw new AlreadyAttendedException(id);
        }

        request.setStatus(RequestStatus.ATENDIDA);
        return toView(repository.save(request));
    }

    @Override
    public List<ResourceRequestView> findPending() {
        return repository.findAll()
                .stream()
                .filter(request -> request.getStatus() == RequestStatus.PENDIENTE)
                .map(ResourceRequestServiceBean::toView)
                .toList();
    }

    private ResourceRequest findExisting(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RequestNotFoundException(id));
    }

    private static void validateCommand(CreateResourceRequestCommand command) {
        if (command == null) {
            throw new BusinessValidationException(
                    "El cuerpo de la solicitud es obligatorio"
            );
        }

        if (command.disasterDescription() == null
                || command.disasterDescription().isBlank()) {
            throw new BusinessValidationException(
                    "La descripción del desastre es obligatoria"
            );
        }

        if (command.resourceType() == null
                || command.resourceType().isBlank()) {
            throw new BusinessValidationException(
                    "El tipo de recurso es obligatorio"
            );
        }

        if (command.quantity() == null) {
            throw new BusinessValidationException(
                    "La cantidad es obligatoria"
            );
        }

        if (command.quantity() <= 0) {
            throw new BusinessValidationException(
                    "La cantidad debe ser mayor que cero"
            );
        }

        if (command.priority() == null || command.priority().isBlank()) {
            throw new BusinessValidationException(
                    "La prioridad es obligatoria"
            );
        }
    }

    private static Priority parsePriority(String value) {
        try {
            return Priority.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessValidationException(
                    "La prioridad debe ser BAJA, MEDIA o ALTA"
            );
        }
    }

    private static ResourceRequestView toView(ResourceRequest request) {
        return new ResourceRequestView(
                request.getId(),
                request.getDisasterDescription(),
                request.getResourceType(),
                request.getQuantity(),
                request.getPriority(),
                request.getStatus()
        );
    }
}