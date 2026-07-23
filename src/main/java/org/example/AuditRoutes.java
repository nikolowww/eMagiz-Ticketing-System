package org.example;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import static org.example.Main.esc;
import static org.example.Main.getConnection;
import static org.example.Main.isSupport;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class AuditRoutes {

    /**
     * Retrieves audit logs.
     * Can search by action, username or details.
     * Returns results.
     * @param userId
     * @param search
     * @return
     */

    @GET
    @Path("audit")
    public Response auditList(@QueryParam("userId") int userId, @QueryParam("search") String search) {
        try (Connection conn = getConnection()) {
            if (!isSupport(conn, userId)) {
                return Response.status(403).entity("[]").build();
            }

            PreparedStatement stmt;
            if (search != null && !search.isBlank()) {
                stmt = conn.prepareStatement(
                        "SELECT * FROM audit_logs WHERE LOWER(action) LIKE ? OR LOWER(username) LIKE ? OR LOWER(details) LIKE ? ORDER BY id DESC"
                );
                String q = "%" + search.toLowerCase() + "%";
                stmt.setString(1, q);
                stmt.setString(2, q);
                stmt.setString(3, q);
            } else {
                stmt = conn.prepareStatement("SELECT * FROM audit_logs ORDER BY id DESC");
            }

            ResultSet rs = stmt.executeQuery();
            StringBuilder json = new StringBuilder("[");

            while (rs.next()) {
                Timestamp created = rs.getTimestamp("created_at");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"user\":\"").append(esc(rs.getString("username"))).append("\",")
                        .append("\"role\":\"").append(esc(rs.getString("role"))).append("\",")
                        .append("\"action\":\"").append(esc(rs.getString("action"))).append("\",")
                        .append("\"target\":\"").append(esc(rs.getString("target"))).append("\",")
                        .append("\"details\":\"").append(esc(rs.getString("details"))).append("\",")
                        .append("\"at\":\"").append(created == null ? "" : created.toLocalDateTime()).append("\"")
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
}
