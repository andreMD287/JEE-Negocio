package co.edu.javeriana.recursos.api.exception;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.NoContentException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cualquier otra excepción → respuesta con el formato uniforme.
 *
 * <ul>
 *   <li>Errores HTTP propios de JAX-RS (ruta inexistente, método no permitido,
 *       tipo de contenido no soportado…) conservan su código.</li>
 *   <li>Cuerpo vacío, JSON mal formado o con tipos incorrectos → HTTP 400.</li>
 *   <li>Todo lo demás → HTTP 500, sin exponer detalles internos.</li>
 * </ul>
 *
 * <p>Las excepciones de negocio no llegan aquí: JAX-RS elige el mapper del tipo
 * más específico.</p>
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        if (hasCause(exception, NoContentException.class)) {
            return ErrorResponses.of(Response.Status.BAD_REQUEST, "EMPTY_BODY",
                    "El cuerpo de la solicitud es obligatorio");
        }
        if (hasCause(exception, JsonbException.class) || hasCause(exception, ProcessingException.class)) {
            return ErrorResponses.of(Response.Status.BAD_REQUEST, "MALFORMED_JSON",
                    "El cuerpo no es un JSON válido o tiene tipos incorrectos");
        }
        if (exception instanceof WebApplicationException web) {
            Response.StatusType status = web.getResponse().getStatusInfo();
            return ErrorResponses.of(status.getStatusCode(), httpCode(status), status.getReasonPhrase());
        }
        LOG.log(Level.SEVERE, "Error no controlado", exception);
        return ErrorResponses.of(Response.Status.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocurrió un error inesperado");
    }

    private static boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
        for (Throwable t = exception; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }

    /** Convierte, p. ej., "Method Not Allowed" en {@code METHOD_NOT_ALLOWED}. */
    private static String httpCode(Response.StatusType status) {
        return status.getReasonPhrase().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }
}
