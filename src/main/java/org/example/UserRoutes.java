package org.example;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.example.Main.audit;
import static org.example.Main.getConnection;
import static org.example.Main.getInt;
import static org.example.Main.getValue;
import static org.example.Main.hashPassword;
import static org.example.Main.isSupport;
import static org.example.Main.newSessionToken;
import static org.example.Main.userJson;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class UserRoutes {

    /**
     * Retrieves all customer users, so it supports only roles.
     * Returns a JSON array of the customer objects.
     * @param userId
     * @return
     */

    @GET
    @Path("users")
    public Response users(@QueryParam("userId") int userId) {
        try (Connection conn = getConnection()) {
            if (!isSupport(conn, userId)) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Not allowed\"}").build();
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE role = 'Customer' ORDER BY id"
            );
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            while (rs.next()) {
                json.append(userJson(rs)).append(",");
            }
            if (json.charAt(json.length() - 1) == ',') {
                json.deleteCharAt(json.length() - 1);
            }
            json.append("]");

            return Response.ok(json.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("[]").build();
        }
    }

    /**
     * Retrieves all consultant users in the system.
     * Returns a JSON array of consultant objects.
     * @return
     */

    @GET
    @Path("consultants")
    public String consultants() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE role = 'Consultant' ORDER BY name")) {

            ResultSet rs = stmt.executeQuery();
            StringBuilder json = new StringBuilder("[");

            while (rs.next()) {
                json.append(userJson(rs)).append(",");
            }
            if (json.charAt(json.length() - 1) == ',') {
                json.deleteCharAt(json.length() - 1);
            }
            json.append("]");
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    /**
     * It creates a new customer user, so it supports only roles.
     * Returns the new created customer objects.
     * @param body
     * @return
     */

    @POST
    @Path("users")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUser(String body) {
        int currentUserId = getInt(getValue(body, "currentUserId"));

        try (Connection conn = getConnection()) {
            if (!isSupport(conn, currentUserId)) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Not allowed\"}").build();
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO users(username, password, name, email, role, organization, session_token) VALUES (?, ?, ?, ?, 'Customer', ?, ?) RETURNING *"
            );
            stmt.setString(1, getValue(body, "username"));
            String hashedPassword =
                    hashPassword(
                            getValue(body, "password")
                    );

            stmt.setString(
                    2,
                    hashedPassword
            );
            stmt.setString(3, getValue(body, "name"));
            stmt.setString(4, getValue(body, "email"));
            stmt.setString(5, getValue(body, "name"));
            stmt.setString(6, newSessionToken());
            ResultSet rs = stmt.executeQuery();
            rs.next();

            audit(currentUserId, "Customer creation", "User " + rs.getInt("id"), "Customer account created");
            return Response.ok("{\"ok\":true,\"message\":\"Customer created\",\"user\":" + userJson(rs) + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"ok\":false,\"message\":\"Customer creation failed\"}").build();
        }
    }

    /**
     * Updates a customer's profile information.
     * Can update name, email, username and if the user wants password.
     * @param id
     * @param body
     * @return
     */

    @PUT
    @Path("users/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateUser(@PathParam("id") int id, String body) {
        int currentUserId = getInt(getValue(body, "currentUserId"));

        try (Connection conn = getConnection()) {

            if (!isSupport(conn, currentUserId) && currentUserId != id) {
                return Response.status(403)
                        .entity("{\"ok\":false,\"message\":\"Not allowed\"}")
                        .build();
            }

            String password = getValue(body, "password");

            PreparedStatement stmt;

            if (password != null && !password.isBlank()) {

                stmt = conn.prepareStatement(
                        "UPDATE users SET name = ?, email = ?, username = ?, password = ? WHERE id = ? AND role = 'Customer' RETURNING *"
                );

                stmt.setString(1, getValue(body, "name"));
                stmt.setString(2, getValue(body, "email"));
                stmt.setString(3, getValue(body, "username"));
                stmt.setString(4, hashPassword(password));
                stmt.setInt(5, id);

            } else {

                stmt = conn.prepareStatement(
                        "UPDATE users SET name = ?, email = ?, username = ? WHERE id = ? AND role = 'Customer' RETURNING *"
                );

                stmt.setString(1, getValue(body, "name"));
                stmt.setString(2, getValue(body, "email"));
                stmt.setString(3, getValue(body, "username"));
                stmt.setInt(4, id);
            }

            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return Response.status(404)
                        .entity("{\"ok\":false,\"message\":\"Customer not found\"}")
                        .build();
            }

            audit(
                    currentUserId,
                    "Customer update",
                    "User " + id,
                    "Customer account updated"
            );

            return Response.ok(
                    "{\"ok\":true,\"message\":\"Customer updated\",\"user\":"
                            + userJson(rs)
                            + "}"
            ).build();

        } catch (Exception e) {
            e.printStackTrace();

            return Response.serverError()
                    .entity("{\"ok\":false,\"message\":\"Customer update failed\"}")
                    .build();
        }
    }
}
