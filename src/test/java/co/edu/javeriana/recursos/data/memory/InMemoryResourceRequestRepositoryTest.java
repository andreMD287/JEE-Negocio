package co.edu.javeriana.recursos.data.memory;

import co.edu.javeriana.recursos.domain.model.Priority;
import co.edu.javeriana.recursos.domain.model.RequestStatus;
import co.edu.javeriana.recursos.domain.model.ResourceRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryResourceRequestRepositoryTest {

    private InMemoryResourceRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryResourceRequestRepository();
    }

    private static ResourceRequest newRequest(String resourceType) {
        return new ResourceRequest(null, "Inundación en el sector norte", resourceType,
                100, Priority.ALTA, RequestStatus.PENDIENTE);
    }

    @Test
    @DisplayName("save asigna un id nuevo a una solicitud sin id")
    void saveAssignsId() {
        ResourceRequest saved = repository.save(newRequest("Agua potable"));

        assertNotNull(saved.getId());
        assertEquals("Agua potable", saved.getResourceType());
        assertEquals(RequestStatus.PENDIENTE, saved.getStatus());
    }

    @Test
    @DisplayName("save genera ids consecutivos y distintos")
    void saveGeneratesSequentialIds() {
        Long first = repository.save(newRequest("Agua")).getId();
        Long second = repository.save(newRequest("Mantas")).getId();

        assertEquals(1L, first);
        assertEquals(2L, second);
    }

    @Test
    @DisplayName("save no modifica el objeto recibido")
    void saveDoesNotMutateArgument() {
        ResourceRequest request = newRequest("Agua");

        repository.save(request);

        assertEquals(null, request.getId());
    }

    @Test
    @DisplayName("save con id existente reemplaza la versión almacenada")
    void saveWithIdUpdates() {
        ResourceRequest saved = repository.save(newRequest("Agua"));
        saved.setStatus(RequestStatus.ATENDIDA);

        repository.save(saved);

        assertEquals(RequestStatus.ATENDIDA, repository.findById(saved.getId()).orElseThrow().getStatus());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("save rechaza null")
    void saveRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> repository.save(null));
    }

    @Test
    @DisplayName("findById devuelve la solicitud existente")
    void findByIdFound() {
        ResourceRequest saved = repository.save(newRequest("Agua"));

        ResourceRequest found = repository.findById(saved.getId()).orElseThrow();

        assertEquals(saved.getId(), found.getId());
        assertEquals("Agua", found.getResourceType());
    }

    @Test
    @DisplayName("findById devuelve vacío para un id inexistente o null")
    void findByIdNotFound() {
        assertTrue(repository.findById(99L).isEmpty());
        assertTrue(repository.findById(null).isEmpty());
    }

    @Test
    @DisplayName("modificar un objeto devuelto no altera el almacenamiento sin save")
    void returnedObjectsAreCopies() {
        ResourceRequest saved = repository.save(newRequest("Agua"));

        ResourceRequest found = repository.findById(saved.getId()).orElseThrow();
        found.setStatus(RequestStatus.ATENDIDA);

        assertNotSame(saved, found);
        assertEquals(RequestStatus.PENDIENTE, repository.findById(saved.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("findAll devuelve todas las solicitudes ordenadas por id")
    void findAllReturnsEverything() {
        repository.save(newRequest("Agua"));
        repository.save(newRequest("Mantas"));
        repository.save(newRequest("Medicamentos"));

        List<ResourceRequest> all = repository.findAll();

        assertEquals(3, all.size());
        assertEquals(List.of(1L, 2L, 3L), all.stream().map(ResourceRequest::getId).toList());
    }

    @Test
    @DisplayName("findAll devuelve lista vacía si no hay solicitudes")
    void findAllEmpty() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("guardados concurrentes no repiten ids ni pierden solicitudes")
    void concurrentSavesAreSafe() throws InterruptedException {
        int threads = 8;
        int savesPerThread = 250;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        Set<Long> ids = ConcurrentHashMap.newKeySet();

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                start.await();
                for (int i = 0; i < savesPerThread; i++) {
                    ids.add(repository.save(newRequest("Agua")).getId());
                }
                return null;
            });
        }
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(threads * savesPerThread, ids.size());
        assertEquals(threads * savesPerThread, repository.findAll().size());
    }
}
