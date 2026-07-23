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
import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.example.Main.audit;
import static org.example.Main.canUseTicket;
import static org.example.Main.getConnection;
import static org.example.Main.getInt;
import static org.example.Main.getValue;
import static org.example.Main.ticketJson;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class TicketRoutes {

    /**
     * Retrieves tickets based on user role.
     * Support the functions: Sees all, Consultants see assigned, Customers see their own.
     * @param userId
     * @return
     */

    @GET
    @Path("tickets")
    public String tickets(@QueryParam("userId") int userId) {
        try (Connection conn = getConnection()) {
            PreparedStatement userStmt = conn.prepareStatement("SELECT role FROM users WHERE id = ?");
            userStmt.setInt(1, userId);
            ResultSet user = userStmt.executeQuery();

            if (!user.next()) {
                return "[]";
            }

            String role = user.getString("role");
            PreparedStatement stmt;

            if ("Support".equals(role)) {
                stmt = conn.prepareStatement("SELECT * FROM tickets ORDER BY id DESC");
            } else if ("Consultant".equals(role)) {
                stmt = conn.prepareStatement("SELECT * FROM tickets WHERE assignee_id = ? ORDER BY id DESC");
                stmt.setInt(1, userId);
            } else {
                stmt = conn.prepareStatement("SELECT * FROM tickets WHERE requester_user_id = ? ORDER BY id DESC");
                stmt.setInt(1, userId);
            }

            ResultSet rs = stmt.executeQuery();
            StringBuilder json = new StringBuilder("[");

            while (rs.next()) {
                json.append(ticketJson(rs)).append(",");
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
     * Retireves a single ticket by ID if user has permission.
     * It also returns an error code 403, if and only if user cannot access this ticket.
     * And 404 error code if the ticket isn't found.
     * @param id
     * @param userId
     * @return
     */

    @GET
    @Path("tickets/{id}")
    public Response ticket(@PathParam("id") int id, @QueryParam("userId") int userId) {
        try (Connection conn = getConnection()) {
            if (!canUseTicket(conn, userId, id)) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Not allowed\"}").build();
            }

            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tickets WHERE id = ?");
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                return Response.status(404).entity("{\"ok\":false,\"message\":\"Ticket not found\"}").build();
            }

            return Response.ok(ticketJson(rs)).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"ok\":false,\"message\":\"Ticket failed\"}").build();
        }
    }

    /**
     * Creates a new ticket in the system.
     * Return a created ticket object.
     * @param body
     * @return
     */

    @POST
    @Path("tickets")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTicket(String body) {
        int userId = getInt(getValue(body, "currentUserId"));

        try (Connection conn = getConnection()) {
            PreparedStatement userStmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
            userStmt.setInt(1, userId);
            ResultSet user = userStmt.executeQuery();

            if (!user.next()) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Not allowed\"}").build();
            }

            String deadline = getValue(body, "deadline");

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO tickets(title, description, type, source, status, priority, requester, requester_user_id, assignee_id, private_notes, deadline, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, 'Submitted', ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *"
            );
            stmt.setString(1, getValue(body, "title"));
            stmt.setString(2, getValue(body, "description"));
            stmt.setString(3, getValue(body, "type").isBlank() ? "Incident" : getValue(body, "type"));
            stmt.setString(4, getValue(body, "source").isBlank() ? "Customer Portal" : getValue(body, "source"));
            stmt.setString(5, getValue(body, "priority").isBlank() ? "Medium" : getValue(body, "priority"));
            stmt.setString(6, user.getString("role").equals("Customer") ? user.getString("username") : getValue(body, "requester"));
            stmt.setInt(7, userId);
            stmt.setInt(8, getInt(getValue(body, "assigneeId")));
            stmt.setString(9, getValue(body, "privateNotes"));
            stmt.setTimestamp(10, deadline.isBlank() ? null : Timestamp.valueOf(deadline + " 23:59:59"));
            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));

            ResultSet rs = stmt.executeQuery();
            rs.next();

            audit(userId, "Ticket creation", "EM-" + rs.getInt("id"), "Ticket submitted successfully");
            return Response.ok("{\"ok\":true,\"message\":\"Ticket submitted successfully\",\"ticket\":" + ticketJson(rs) + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"ok\":false,\"message\":\"Ticket creation failed\"}").build();
        }
    }

    /**
     * Updates ticket status, assignee, or private notes.
     * Customerds cannot update workflow.
     * Consultants cannot accept/reject or assign.
     * @param id
     * @param body
     * @return
     */

    @PUT
    @Path("tickets/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTicket(@PathParam("id") int id, String body) {
        int userId = getInt(getValue(body, "currentUserId"));
        String status = getValue(body, "status");
        int assigneeId = getInt(getValue(body, "assigneeId"));
        String privateNotes = getValue(body, "privateNotes");
        String deadline = getValue(body, "deadline");

        try (Connection conn = getConnection()) {
            if (!canUseTicket(conn, userId, id)) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Not allowed\"}").build();
            }

            PreparedStatement userStmt = conn.prepareStatement("SELECT role FROM users WHERE id = ?");
            userStmt.setInt(1, userId);
            ResultSet user = userStmt.executeQuery();
            user.next();
            String role = user.getString("role");

            if ("Customer".equals(role)) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Customers cannot update workflow\"}").build();
            }

            if ("Consultant".equals(role) && (status.equals("Accepted") || status.equals("Denied") || assigneeId > 0)) {
                return Response.status(403).entity("{\"ok\":false,\"message\":\"Consultants cannot accept, reject or assign\"}").build();
            }

            PreparedStatement oldStmt = conn.prepareStatement("SELECT assignee_id FROM tickets WHERE id = ?");
            oldStmt.setInt(1, id);
            ResultSet old = oldStmt.executeQuery();
            old.next();
            int oldAssignee = old.getInt("assignee_id");

            PreparedStatement stmt = conn.prepareStatement(
                    "UPDATE tickets SET status = COALESCE(NULLIF(?, ''), status), " +
                            "assignee_id = CASE WHEN ? = 0 THEN assignee_id ELSE ? END, " +
                            "private_notes = COALESCE(NULLIF(?, ''), private_notes), " +
                            "deadline = COALESCE(?, deadline), updated_at = ? WHERE id = ? RETURNING *"
            );
            stmt.setString(1, status);
            stmt.setInt(2, assigneeId);
            stmt.setInt(3, assigneeId);
            stmt.setString(4, privateNotes);
            stmt.setTimestamp(5, deadline.isBlank() ? null : Timestamp.valueOf(deadline + " 23:59:59"));
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(7, id);
            ResultSet rs = stmt.executeQuery();
            rs.next();

            String action = "Ticket update";
            if ("Accepted".equals(status)) {
                action = "Ticket acceptance";
            } else if ("Denied".equals(status)) {
                action = "Ticket rejection";
            } else if ("Closed".equals(status) || "Resolved".equals(status)) {
                action = "Ticket closure";
            } else if (assigneeId > 0 && oldAssignee == 0) {
                action = "Ticket assignment";
            } else if (assigneeId > 0 && oldAssignee != assigneeId) {
                action = "Ticket reassignment";
            }

            audit(userId, action, "EM-" + id, "Ticket changed");
            return Response.ok("{\"ok\":true,\"message\":\"Ticket updated\",\"ticket\":" + ticketJson(rs) + "}").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"ok\":false,\"message\":\"Ticket update failed\"}").build();
        }
    }
}
