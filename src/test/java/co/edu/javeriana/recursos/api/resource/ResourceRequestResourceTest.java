package co.edu.javeriana.recursos.api.resource;

import co.edu.javeriana.recursos.api.request.CreateResourceRequestBody;
import co.edu.javeriana.recursos.business.contract.CreateResourceRequestCommand;
import co.edu.javeriana.recursos.business.contract.ResourceRequestService;
import co.edu.javeriana.recursos.business.contract.ResourceRequestView;
import co.edu.javeriana.recursos.business.exception.RequestNotFoundException;
import co.edu.javeriana.recursos.domain.model.Priority;
import co.edu.javeriana.recursos.domain.model.RequestStatus;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba la capa REST con un servicio simulado: demuestra que la API depende
 * solo de la interfaz {@link ResourceRequestService}, no del EJB concreto.
 */
@ExtendWith(MockitoExtension.class)
class ResourceRequestResourceTest {

    @Mock
    private ResourceRequestService service;

    @Mock
    private UriInfo uriInfo;

    private ResourceRequestResource resource;

    private static final ResourceRequestView PENDING = new ResourceRequestView(
            1L, "Inundación en el sector norte", "Agua potable", 200, Priority.ALTA, RequestStatus.PENDIENTE);

    @BeforeEach
    void setUp() {
        resource = new ResourceRequestResource(service);
    }

    @Test
    @DisplayName("POST convierte el cuerpo en comando y responde 201 con Location")
    void registerReturnsCreated() {
        when(uriInfo.getAbsolutePathBuilder())
                .thenReturn(UriBuilder.fromUri("http://localhost:8080/recursos/api/requests"));
        when(service.register(any())).thenReturn(PENDING);

        CreateResourceRequestBody body = new CreateResourceRequestBody();
        body.setDisasterDescription("Inundación en el sector norte");
        body.setResourceType("Agua potable");
        body.setQuantity(200);
        body.setPriority("ALTA");

        Response response = resource.register(body, uriInfo);

        ArgumentCaptor<CreateResourceRequestCommand> captor = ArgumentCaptor.forClass(CreateResourceRequestCommand.class);
        verify(service).register(captor.capture());
        assertEquals(new CreateResourceRequestCommand("Inundación en el sector norte", "Agua potable", 200, "ALTA"),
                captor.getValue());
        assertEquals(201, response.getStatus());
        assertEquals(URI.create("http://localhost:8080/recursos/api/requests/1"), response.getLocation());
        assertSame(PENDING, response.getEntity());
    }

    @Test
    @DisplayName("POST sin cuerpo delega al negocio un comando null (la API no valida)")
    void registerWithoutBodyDelegatesNull() {
        when(service.register(null)).thenThrow(new RuntimeException("validado en el EJB"));

        assertThrows(RuntimeException.class, () -> resource.register(null, uriInfo));

        ArgumentCaptor<CreateResourceRequestCommand> captor = ArgumentCaptor.forClass(CreateResourceRequestCommand.class);
        verify(service).register(captor.capture());
        assertNull(captor.getValue());
    }

    @Test
    @DisplayName("GET /requests devuelve lo que entrega el servicio")
    void findAllDelegates() {
        when(service.findAll()).thenReturn(List.of(PENDING));

        assertEquals(List.of(PENDING), resource.findAll());
    }

    @Test
    @DisplayName("GET /requests/pending devuelve las pendientes del servicio")
    void findPendingDelegates() {
        when(service.findPending()).thenReturn(List.of(PENDING));

        assertEquals(List.of(PENDING), resource.findPending());
    }

    @Test
    @DisplayName("GET /requests/{id} devuelve la solicitud")
    void findByIdDelegates() {
        when(service.findById(1L)).thenReturn(PENDING);

        assertSame(PENDING, resource.findById(1L));
    }

    @Test
    @DisplayName("GET /requests/{id} propaga la excepción de negocio para que la traduzca su mapper")
    void findByIdPropagatesNotFound() {
        when(service.findById(99L)).thenThrow(new RequestNotFoundException(99L));

        assertThrows(RequestNotFoundException.class, () -> resource.findById(99L));
    }

    @Test
    @DisplayName("PUT /requests/{id}/attend devuelve la solicitud atendida")
    void attendDelegates() {
        ResourceRequestView attended = new ResourceRequestView(
                1L, "Inundación en el sector norte", "Agua potable", 200, Priority.ALTA, RequestStatus.ATENDIDA);
        when(service.attend(1L)).thenReturn(attended);

        assertEquals(RequestStatus.ATENDIDA, resource.attend(1L).status());
    }
}
