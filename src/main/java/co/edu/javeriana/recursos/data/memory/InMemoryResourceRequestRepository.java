package co.edu.javeriana.recursos.data.memory;

import co.edu.javeriana.recursos.business.port.ResourceRequestRepository;
import co.edu.javeriana.recursos.domain.model.ResourceRequest;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementación en memoria del puerto {@link ResourceRequestRepository}.
 *
 * <p>Es {@code @ApplicationScoped}: existe una sola instancia compartida por
 * todos los EJB del pool, de modo que todos ven las mismas solicitudes. Usa
 * {@link ConcurrentHashMap} y {@link AtomicLong} para soportar accesos
 * concurrentes, y guarda y devuelve copias para que nadie modifique el
 * almacenamiento sin pasar por {@link #save}.</p>
 */
@ApplicationScoped
public class InMemoryResourceRequestRepository implements ResourceRequestRepository {

    private final ConcurrentMap<Long, ResourceRequest> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public ResourceRequest save(ResourceRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud a guardar no puede ser null");
        }
        ResourceRequest toStore = request.copy();
        if (toStore.getId() == null) {
            toStore.setId(sequence.incrementAndGet());
        }
        storage.put(toStore.getId(), toStore);
        return toStore.copy();
    }

    @Override
    public Optional<ResourceRequest> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id)).map(ResourceRequest::copy);
    }

    @Override
    public List<ResourceRequest> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(ResourceRequest::getId))
                .map(ResourceRequest::copy)
                .toList();
    }
}
