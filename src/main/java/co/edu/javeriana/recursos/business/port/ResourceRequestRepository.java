package co.edu.javeriana.recursos.business.port;

import co.edu.javeriana.recursos.domain.model.ResourceRequest;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de almacenamiento de solicitudes.
 *
 * <p>Lo define el tier de negocio y lo implementa el tier de datos. El EJB solo
 * conoce esta interfaz, por lo que el mecanismo de persistencia (memoria, JPA,
 * archivo, etc.) puede cambiarse sin modificar las reglas de negocio.</p>
 *
 * <p>Contrato para las implementaciones:</p>
 * <ul>
 *   <li>{@link #save} asigna un identificador nuevo si la solicitud no tiene
 *       uno; si ya lo tiene, reemplaza la versión almacenada.</li>
 *   <li>Los objetos devueltos son copias: modificarlos no altera lo almacenado
 *       hasta que se vuelva a llamar a {@link #save}.</li>
 *   <li>No contiene reglas de negocio ni filtra por estado.</li>
 * </ul>
 */
public interface ResourceRequestRepository {

    ResourceRequest save(ResourceRequest request);

    Optional<ResourceRequest> findById(Long id);

    List<ResourceRequest> findAll();
}
