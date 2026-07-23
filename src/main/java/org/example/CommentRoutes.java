package org.example;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.example.Main.audit;
import static org.example.Main.canUseTicket;
import static org.example.Main.esc;
import static org.example.Main.getConnection;
import static org.example.Main.getInt;
import static org.example.Main.getValue;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class CommentRoutes {

    /**
     * Retrieves comments for a ticket, based on the role.
     * Customers see public comments only.
     * Support and Consultants see it all.
     * @param id
     * @param userId
     * @return
     */

    @GET
    @Path("tickets/{id}/comments")
    public Response comments(@PathParam("id") int id, @QueryParam("userId") int userId) {
        try (Connection conn = getConnection()) {
            if (!canUseTicket(conn, userId, id)) {
                return Response.status(403).entity("[]").build();
            }

            PreparedStatement userStmt = conn.prepareStatement("SELECT role FROM users WHERE id = ?");
            userStmt.setInt(1, userId);
            ResultSet user = userStmt.executeQuery();
            user.next();
            String role = user.getString("role");

            PreparedStatement stmt;
            if ("Customer".equals(role)) {
                stmt = conn.prepareStatement("SELECT * FROM comments WHERE ticket_id = ? AND visibility = 'public' ORDER BY id");
            } else {
                stmt = conn.prepareStatement("SELECT * FROM comments WHERE ticket_id = ? ORDER BY id");
            }
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            StringBuilder json = new StringBuilder("[");
            while (rs.next()) {
                Timestamp created = rs.getTimestamp("created_at");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"author\":\"").append(esc(rs.getString("author"))).append("\",")
                        .append("\"visibility\":\"").append(esc(rs.getString("visibility"))).append("\",")
                        .append("\"createdAt\":\"").append(created == null ? "" : created.toLocalDateTime()).append("\",")
                        .append("\"text\":\"").append(esc(rs.getString("text"))).append("\"")
                        .append("},");
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
     * Adds a comment to a ticket.
     * Customers can create publics comments.
     * Support can create private notes only.
     * @param id
     * @param body
     * @return
     */

    @POST
    @Path("tickets/{id}/comments")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addComment(@PathParam("id") int id, String body) {
        int userId = getInt(getValue(body, "currentUserId"));

        try (Connection conn = getConnection()) {
            if (!canUseTicket(conn, userId, id)) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Not allowed\"}").build();
            }

            PreparedStatement userStmt = conn.prepareStatement("SELECT name, role FROM users WHERE id = ?");
            userStmt.setInt(1, userId);
            ResultSet user = userStmt.executeQuery();
            user.next();

            String visibility = getValue(body, "visibility");
            if ("Customer".equals(user.getString("role"))) {
                visibility = "public";
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO comments(ticket_id, user_id, author, visibility, text, created_at) VALUES (?, ?, ?, ?, ?, ?)"
            );
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            stmt.setString(3, user.getString("name"));
            stmt.setString(4, visibility.isBlank() ? "public" : visibility);
            stmt.setString(5, getValue(body, "text"));
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            audit(userId, "Ticket update", "EM-" + id, "Comment added");
            return Response.ok("{\"ok\":true,\"message\":\"Comment added\"}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"ok\":false,\"message\":\"Comment failed\"}").build();
        }
    }
}
