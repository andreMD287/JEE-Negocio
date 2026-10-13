package co.edu.javeriana.recursos.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Activa JAX-RS. Los recursos quedan bajo {@code /recursos/api}.
 */
@ApplicationPath("api")
public class RestApplication extends Application {
}
