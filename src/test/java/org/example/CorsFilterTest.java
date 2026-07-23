package org.example;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * makes sure the cors filter puts the right headers on every response
 * without these the vue frontend running on a different port just gets blocked by the browser
 * we fake the jersey request and response with mockito so there is no real server involved
 */
class CorsFilterTest {

    /**
     * run the filter once and check the three access control headers come out
     */
    @Test
    void addsTheCorsHeaders() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        ContainerResponseContext response = mock(ContainerResponseContext.class);

        MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        when(response.getHeaders()).thenReturn(headers);

        new CorsFilter().filter(request, response);

        assertEquals("*", headers.getFirst("Access-Control-Allow-Origin"));
        assertEquals("Content-Type", headers.getFirst("Access-Control-Allow-Headers"));
        // the methods header should at least mention POST since we lean on it everywhere
        assertTrue(headers.getFirst("Access-Control-Allow-Methods").toString().contains("POST"));
    }
}
