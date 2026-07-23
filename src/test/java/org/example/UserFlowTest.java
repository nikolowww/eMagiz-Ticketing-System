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
 * a user test that walks through a normal support workflow from start to end
 * this is basically what a support agent does on a monday morning
 * log in raise a ticket find it in the queue drop a comment on it and then resolve it
 *
 * same as the integration tests this needs the backend and database up
 * and fair warning it leaves a fresh ticket behind every run wich is a little messy
 * we dont have a delete endpoint yet so for now thats just how it is
 */
class UserFlowTest {

    private static final String BASE = "http://localhost:7000";
    private static final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void backendShouldBeUp() {
        boolean up;
        try {
            up = get("/").statusCode() == 200;
        } catch (Exception e) {
            up = false;
        }
        assumeTrue(up, "backend not up so cant run the user flow");
    }

    /**
     * the whole happy path in one go
     * i kept it as a single test on purpose since every step needs the one before it
     * splitting it up would just mean creating a ticket over and over
     */
    @Test
    void supportRaisesAndResolvesATicket() throws Exception {
        // step one the agent logs in
        HttpResponse<String> login = post("/login", "{\"username\":\"support\",\"password\":\"support123\"}");
        assertEquals(200, login.statusCode());
        int userId = readInt(login.body(), "id");
        assertTrue(userId > 0);

        // step two they raise a new incident
        String newTicket = "{\"currentUserId\":" + userId + ",\"title\":\"keyboard not working\","
                + "\"description\":\"the f keys stopped responding this morning\",\"type\":\"Incident\","
                + "\"priority\":\"Medium\",\"requester\":\"eMagiz\"}";
        HttpResponse<String> created = post("/tickets", newTicket);
        assertEquals(200, created.statusCode());
        assertTrue(created.body().contains("\"ok\":true"));
        int ticketId = readInt(created.body(), "id");
        assertTrue(ticketId > 0);

        // step three the new ticket should show up when they pull the queue
        HttpResponse<String> list = get("/tickets?userId=" + userId);
        assertEquals(200, list.statusCode());
        assertTrue(list.body().contains("keyboard not working"));

        // step four they leave a public comment for the customer to see
        String comment = "{\"currentUserId\":" + userId + ",\"text\":\"looking into this now\",\"visibility\":\"public\"}";
        HttpResponse<String> commented = post("/tickets/" + ticketId + "/comments", comment);
        assertEquals(200, commented.statusCode());

        // step five once its sorted they mark it resolved
        String resolve = "{\"currentUserId\":" + userId + ",\"status\":\"Resolved\"}";
        HttpResponse<String> resolved = put("/tickets/" + ticketId, resolve);
        assertEquals(200, resolved.statusCode());
        assertTrue(resolved.body().contains("Resolved"));

        // and the audit trail should have picked all of that up
        HttpResponse<String> audit = get("/audit?userId=" + userId + "&search=");
        assertEquals(200, audit.statusCode());
        assertTrue(audit.body().contains("Ticket creation"));
    }

    /**
     * quick and dirty way to dig an int field out of the json by hand
     * there is already getValue over in Main but its tied to the api package internals
     * so for this user test i just wrote a tiny version right here
     */
    private static int readInt(String json, String key) {
        String find = "\"" + key + "\":";
        int at = json.indexOf(find);
        if (at < 0) {
            return -1;
        }
        at += find.length();
        int end = at;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(at, end));
        } catch (Exception e) {
            return -1;
        }
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(BASE + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> post(String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(BASE + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpResponse<String> put(String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(BASE + path))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
