package co.edu.javeriana.recursos.business.ejb;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.javeriana.recursos.business.contract.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.business.contract.ResourceRequestView;
import co.edu.javeriana.recursos.business.exception.AlreadyAttendedException;
import co.edu.javeriana.recursos.business.exception.BusinessValidationException;
import co.edu.javeriana.recursos.business.exception.RequestNotFoundException;
import co.edu.javeriana.recursos.business.port.ResourceRequestRepository;
import co.edu.javeriana.recursos.domain.model.Priority;
import co.edu.javeriana.recursos.domain.model.RequestStatus;
import co.edu.javeriana.recursos.domain.model.ResourceRequest;

@ExtendWith(MockitoExtension.class)
class ResourceRequestServiceBeanTest {

    @Mock
    private ResourceRequestRepository repository;

    private ResourceRequestServiceBean service;

    @BeforeEach
    void setUp() {
        service = new ResourceRequestServiceBean();
        service.repository = repository;
    }

    @Test
    void registerCreatesPendingRequest() {
        CreateResourceRequestCommand command = new CreateResourceRequestCommand(
                "  Inundación en el norte  ",
                "  Agua potable  ",
                100,
                " alta "
        );

        when(repository.save(any(ResourceRequest.class)))
                .thenAnswer(invocation -> {
                    ResourceRequest saved =
                            invocation.<ResourceRequest>getArgument(0).copy();
                    saved.setId(1L);
                    return saved;
                });

        ResourceRequestView result = service.register(command);

        ArgumentCaptor<ResourceRequest> captor =
                ArgumentCaptor.forClass(ResourceRequest.class);
        verify(repository).save(captor.capture());

        ResourceRequest stored = captor.getValue();

        assertEquals(null, stored.getId());
        assertEquals("Inundación en el norte",
                stored.getDisasterDescription());
        assertEquals("Agua potable", stored.getResourceType());
        assertEquals(100, stored.getQuantity());
        assertEquals(Priority.ALTA, stored.getPriority());
        assertEquals(RequestStatus.PENDIENTE, stored.getStatus());

        assertEquals(1L, result.id());
        assertEquals(Priority.ALTA, result.priority());
        assertEquals(RequestStatus.PENDIENTE, result.status());
    }

    @Test
    void registerRejectsNullCommand() {
        assertThrows(
                BusinessValidationException.class,
                () -> service.register(null)
        );

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void registerRejectsInvalidDescription(String description) {
        CreateResourceRequestCommand command =
                validCommand(description, "Agua", 10, "ALTA");

        assertThrows(
                BusinessValidationException.class,
                () -> service.register(command)
        );

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void registerRejectsInvalidResourceType(String resourceType) {
        CreateResourceRequestCommand command =
                validCommand("Inundación", resourceType, 10, "ALTA");

        assertThrows(
                BusinessValidationException.class,
                () -> service.register(command)
        );

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void registerRejectsInvalidQuantity(Integer quantity) {
        CreateResourceRequestCommand command =
                validCommand("Inundación", "Agua", quantity, "ALTA");

        assertThrows(
                BusinessValidationException.class,
                () -> service.register(command)
        );

        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "URGENTE"})
    void registerRejectsInvalidPriority(String priority) {
        CreateResourceRequestCommand command =
                validCommand("Inundación", "Agua", 10, priority);

        assertThrows(
                BusinessValidationException.class,
                () -> service.register(command)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void findAllReturnsViews() {
        when(repository.findAll()).thenReturn(List.of(
                request(1L, RequestStatus.PENDIENTE),
                request(2L, RequestStatus.ATENDIDA)
        ));

        List<ResourceRequestView> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(RequestStatus.PENDIENTE, result.get(0).status());
        assertEquals(2L, result.get(1).id());
        assertEquals(RequestStatus.ATENDIDA, result.get(1).status());
    }

    @Test
    void findByIdReturnsExistingRequest() {
        when(repository.findById(1L)).thenReturn(
                Optional.of(request(1L, RequestStatus.PENDIENTE))
        );

        ResourceRequestView result = service.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("Inundación", result.disasterDescription());
        assertEquals("Agua", result.resourceType());
        assertEquals(10, result.quantity());
        assertEquals(Priority.ALTA, result.priority());
        assertEquals(RequestStatus.PENDIENTE, result.status());
    }

    @Test
    void findByIdRejectsUnknownId() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                RequestNotFoundException.class,
                () -> service.findById(99L)
        );
    }

    @Test
    void attendChangesStatusAndSavesRequest() {
        when(repository.findById(1L)).thenReturn(
                Optional.of(request(1L, RequestStatus.PENDIENTE))
        );

        when(repository.save(any(ResourceRequest.class)))
                .thenAnswer(invocation ->
                        invocation.<ResourceRequest>getArgument(0).copy()
                );

        ResourceRequestView result = service.attend(1L);

        ArgumentCaptor<ResourceRequest> captor =
                ArgumentCaptor.forClass(ResourceRequest.class);
        verify(repository).save(captor.capture());

        assertEquals(
                RequestStatus.ATENDIDA,
                captor.getValue().getStatus()
        );
        assertEquals(RequestStatus.ATENDIDA, result.status());
    }

    @Test
    void attendRejectsAlreadyAttendedRequest() {
        when(repository.findById(1L)).thenReturn(
                Optional.of(request(1L, RequestStatus.ATENDIDA))
        );

        assertThrows(
                AlreadyAttendedException.class,
                () -> service.attend(1L)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void findPendingFiltersInBusinessTier() {
        when(repository.findAll()).thenReturn(List.of(
                request(1L, RequestStatus.PENDIENTE),
                request(2L, RequestStatus.ATENDIDA),
                request(3L, RequestStatus.PENDIENTE)
        ));

        List<ResourceRequestView> result = service.findPending();

        assertEquals(2, result.size());
        assertEquals(List.of(1L, 3L),
                result.stream().map(ResourceRequestView::id).toList());
        assertTrue(result.stream().allMatch(
                view -> view.status() == RequestStatus.PENDIENTE
        ));
    }

    private static CreateResourceRequestCommand validCommand(
            String description,
            String resourceType,
            Integer quantity,
            String priority) {
        return new CreateResourceRequestCommand(
                description,
                resourceType,
                quantity,
                priority
        );
    }

    private static ResourceRequest request(
            Long id,
            RequestStatus status) {
        return new ResourceRequest(
                id,
                "Inundación",
                "Agua",
                10,
                Priority.ALTA,
                status
        );
    }
}