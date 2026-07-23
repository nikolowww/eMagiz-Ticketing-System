package org.example;

import jakarta.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * JAX-RS entry point for running inside a servlet container (Tomcat).
 *
 * Registers the same resources as the embedded server in Main.main(), so the
 * API is served at the web app's root context. The database schema is set up on
 * startup, mirroring what Main.main() does when run locally.
 */
@ApplicationPath("/api")
public class RestApplication extends ResourceConfig {

    public RestApplication() {
        Main.setupDatabase();
        register(AuthRoutes.class);
        register(UserRoutes.class);
        register(TicketRoutes.class);
        register(CommentRoutes.class);
        register(AuditRoutes.class);
        register(CorsFilter.class);//
        register(AuthFilter.class);
    }
}
