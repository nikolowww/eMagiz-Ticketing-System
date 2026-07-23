package org.example;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * integration tests that actually talk to the running backend on port 7000
 * the server has to be up and pointed at the database before you run these
 *
 * heads up these read from the live database and the login one writes an audit row
 * so dont be surprised if you see a bit of extra noise in there afterwards
 */
class ApiIntegrationTest {

    private static final String BASE = "http://localhost:7000";
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * if the backend isnt running theres no point pretending these pass
     * so we ping the home endpoint first and just skip everything when its not there
     */
    @BeforeAll
    static void backendShouldBeUp() {
        boolean up;
        try {
            up = get("/").statusCode() == 200;
        } catch (Exception e) {
            up = false;
        }
        assumeTrue(up, "backend not reachable on 7000 so skipping the integration tests");
    }

    /**
     * the seeded support account should log in fine and come back as a support user
     */
    @Test
    void supportCanLogIn() throws Exception {
        HttpResponse<String> r = post("/login", "{\"username\":\"support\",\"password\":\"support123\"}");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("\"ok\":true"));
        assertTrue(r.body().contains("\"role\":\"Support\""));
    }

    /**
     * a rubbish password has to be turned away with a 401 and no user object
     */
    @Test
    void badPasswordIsRejected() throws Exception {
        HttpResponse<String> r = post("/login", "{\"username\":\"support\",\"password\":\"definitely wrong\"}");
        assertEquals(401, r.statusCode());
        assertTrue(r.body().contains("Invalid credentials"));
    }

    /**
     * the consultants list is open to everyone and should always be a json array
     * even with no consultants yet it should at least be the empty brackets
     */
    @Test
    void consultantsComesBackAsAList() throws Exception {
        HttpResponse<String> r = get("/consultants");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().trim().startsWith("["));
    }

    /**
     * audit is support only so asking as some random user id should be forbidden
     */
    @Test
    void auditIsBlockedForNonSupport() throws Exception {
        // 999999 is almost certainly not a real support user
        HttpResponse<String> r = get("/audit?userId=999999&search=");
        assertEquals(403, r.statusCode());
    }

    // small helpers so the tests above dont get cluttered with builder noise

    private static HttpResponse<String> get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
