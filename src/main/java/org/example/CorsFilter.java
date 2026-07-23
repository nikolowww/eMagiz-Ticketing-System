package org.example;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

/**
 * CORS filter in order to handle cross HTTP requests.
 * This allows the browser to communicate with the clients with the backend API.
 */

public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
    }
}
