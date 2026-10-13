package co.edu.javeriana.recursos.business.contract;

import jakarta.ejb.Local;

import java.util.List;

/**
 * Contrato del tier de negocio para la gestión de solicitudes de recursos.
 *
 * <p>Lo implementa un EJB de sesión sin estado. Los clientes (por ejemplo, la
 * API REST) dependen solo de esta interfaz.</p>
 */
@Local
public interface ResourceRequestService {

    ResourceRequestView register(CreateResourceRequestCommand command);

    List<ResourceRequestView> findAll();

    ResourceRequestView findById(Long id);

    ResourceRequestView attend(Long id);

    List<ResourceRequestView> findPending();
}
