package co.edu.javeriana.recursos.api.resource;

import co.edu.javeriana.recursos.api.request.CreateResourceRequestBody;
import co.edu.javeriana.recursos.business.contract.ResourceRequestService;
import co.edu.javeriana.recursos.business.contract.ResourceRequestView;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;

/**
 * API REST de solicitudes de recursos.
 *
 * <p>Solo traduce HTTP a llamadas a {@link ResourceRequestService}; no contiene
 * reglas de negocio. Los errores los traducen los {@code ExceptionMapper} de
 * {@code api.exception}.</p>
 */
@Path("requests")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ResourceRequestResource {

    private ResourceRequestService service;

    public ResourceRequestResource() {
    }

    @Inject
    public ResourceRequestResource(ResourceRequestService service) {
        this.service = service;
    }

    @POST
    public Response register(CreateResourceRequestBody body, @Context UriInfo uriInfo) {
        // Un cuerpo vacío llega como comando null: el EJB decide que es inválido.
        ResourceRequestView created = service.register(body == null ? null : body.toCommand());
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.id())).build();
        return Response.created(location).entity(created).build();
    }

    @GET
    public List<ResourceRequestView> findAll() {
        return service.findAll();
    }

    @GET
    @Path("pending")
    public List<ResourceRequestView> findPending() {
        return service.findPending();
    }

    @GET
    @Path("{id: \\d+}")
    public ResourceRequestView findById(@PathParam("id") Long id) {
        return service.findById(id);
    }

    @PUT
    @Path("{id: \\d+}/attend")
    public ResourceRequestView attend(@PathParam("id") Long id) {
        return service.attend(id);
    }
}
