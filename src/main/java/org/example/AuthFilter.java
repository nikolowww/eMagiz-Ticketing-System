package org.example;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;

import static org.example.Main.getConnection;
import static org.example.Main.userIdFromToken;

/**
 * Checks the session token on every request before it reaches a route.
 * Login and the CORS preflight stay public, everything else needs a valid
 * token in the Authorization header, otherwise the request is rejected.
 */

public class AuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext request) {
        String method = request.getMethod();
        String path = request.getUriInfo().getPath();

        // public endpoints: the CORS preflight, the home check and the login call
        if ("OPTIONS".equals(method) || path.isEmpty() || path.equals("login")) {
            return;
        }

        String header = request.getHeaderString("Authorization");
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : "";

        if (token.isBlank()) {
            request.abortWith(Response.status(401).entity("{\"ok\":false,\"message\":\"Not authenticated\"}").build());
            return;
        }

        try (Connection conn = getConnection()) {
            if (userIdFromToken(conn, token) == 0) {
                request.abortWith(Response.status(401).entity("{\"ok\":false,\"message\":\"Invalid session\"}").build());
            }
        } catch (Exception e) {
            e.printStackTrace();
            request.abortWith(Response.serverError().entity("{\"ok\":false,\"message\":\"Auth check failed\"}").build());
        }
    }
}
