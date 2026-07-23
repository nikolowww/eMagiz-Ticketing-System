package org.example;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * eMagiz ticketing system backend API.
 * The code manages user authentication, ticket lifecycle, and audit logging.
 */

public class Main {

    static final String URL =
            "jdbc:postgresql://bronto.ewi.utwente.nl:5432/dab_di2526-2b_103";

    static final String DB_USER =
            "dab_di2526-2b_103";

    static final String DB_PASSWORD =
            "nw/pPwwWNj/evdHW";

    static final String PEPPER =
            "eMagiz2026SecretPepper";

    // Force-load the PostgreSQL JDBC driver. In a servlet container (Tomcat),
    // DriverManager's auto-registration doesn't always pick up drivers from
    // WEB-INF/library, which shows us as "No suitable driver found".
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hashes a password using Argon2 with secret pepper.
     * Also returns an Argon2 hash string.
     * @param password
     * @return
     */

    static String hashPassword(String password) {

        Argon2 argon2 = Argon2Factory.create();

        return argon2.hash(
                3,
                65536,
                1,
                (password + PEPPER).toCharArray()
        );
    }

    /**
     * Verifies a plain-text password against a stored Argon2 hash.
     * Returns boolean if the password is a match.
     * @param password
     * @param storedHash
     * @return
     */

    static boolean verifyPassword(
            String password,
            String storedHash
    ) {

        Argon2 argon2 = Argon2Factory.create();

        return argon2.verify(
                storedHash,
                (password + PEPPER).toCharArray()
        );
    }

    /**
     * Creates a new random session token for a user.
     * @return
     */

    static String newSessionToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Looks up which user a session token belongs to.
     * Returns the user id, or 0 when the token is not known.
     * @param conn
     * @param token
     * @return
     */

    static int userIdFromToken(Connection conn, String token) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE session_token = ?");
        stmt.setString(1, token);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("id");
        }

        return 0;
    }

    public static void main(String[] args) throws Exception {

        setupDatabase();

        // Same JAX-RS resources as the deployed WAR (see RestApplication).
        ResourceConfig config = new ResourceConfig();
        config.register(AuthRoutes.class);
        config.register(UserRoutes.class);
        config.register(TicketRoutes.class);
        config.register(CommentRoutes.class);
        config.register(AuditRoutes.class);
        config.register(CorsFilter.class);
        config.register(AuthFilter.class);

        // Run an embedded Tomcat locally so the dev server is the same kind of
        // server as production (Previder also serves the app on Tomcat). Jersey
        // is mapped at the root, so the frontend keeps calling
        // http://localhost:7000/... exactly as before.
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(7000);
        tomcat.getConnector();

        Context context = tomcat.addContext("", new File(".").getAbsolutePath());
        Tomcat.addServlet(context, "jersey", new ServletContainer(config));
        context.addServletMappingDecoded("/*", "jersey");

        tomcat.start();
        System.out.println("Jersey backend running on http://localhost:7000 (embedded Tomcat)");
        tomcat.getServer().await();
    }

    /**
     * Creates a new database connection using configures credentials.
     * Returns a connection to the database.
     * @return
     * @throws Exception
     */



    static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Initializes database tables and default users on startup.
     * Also it creates users, tickets, comments, and audit logs tablse, only if they don't exist.
     */

    static void setupDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY, " +
                    "username VARCHAR(100) UNIQUE, " +
                    "password VARCHAR(100))");

            stmt.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(150)");
            stmt.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(150)");
            stmt.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(30)");
            stmt.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS organization VARCHAR(150)");
            stmt.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS session_token VARCHAR(100)");

            stmt.executeUpdate("UPDATE users SET role = 'Support' WHERE role IS NOT NULL AND LOWER(role) NOT IN ('support', 'consultant', 'customer')");
            stmt.executeUpdate("UPDATE users SET role = 'Support' WHERE role IS NULL AND username = 'support'");
            stmt.executeUpdate("UPDATE users SET role = 'Customer' WHERE role IS NULL");
            stmt.executeUpdate("UPDATE users SET name = username WHERE name IS NULL");
            stmt.executeUpdate("UPDATE users SET organization = name WHERE organization IS NULL");
            stmt.executeUpdate("UPDATE users SET session_token = md5(random()::text || id::text) WHERE session_token IS NULL");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tickets (" +
                    "id SERIAL PRIMARY KEY, " +
                    "title TEXT, " +
                    "description TEXT)");

            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS type VARCHAR(30)");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS source VARCHAR(80)");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS status VARCHAR(40)");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS priority VARCHAR(40)");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS requester VARCHAR(150)");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS requester_user_id INTEGER");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS assignee_id INTEGER");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS private_notes TEXT");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS created_at TIMESTAMP");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP");
            stmt.executeUpdate("ALTER TABLE tickets ADD COLUMN IF NOT EXISTS deadline TIMESTAMP");

            stmt.executeUpdate("UPDATE tickets SET type = 'Incident' WHERE type IS NULL");
            stmt.executeUpdate("UPDATE tickets SET source = 'Customer Portal' WHERE source IS NULL");
            stmt.executeUpdate("UPDATE tickets SET status = 'Submitted' WHERE status IS NULL");
            stmt.executeUpdate("UPDATE tickets SET priority = 'Medium' WHERE priority IS NULL");
            stmt.executeUpdate("UPDATE tickets SET requester = 'Unknown' WHERE requester IS NULL");
            stmt.executeUpdate("UPDATE tickets SET created_at = NOW() WHERE created_at IS NULL");
            stmt.executeUpdate("UPDATE tickets SET updated_at = created_at WHERE updated_at IS NULL");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS comments (" +
                    "id SERIAL PRIMARY KEY, " +
                    "ticket_id INTEGER, " +
                    "user_id INTEGER, " +
                    "author VARCHAR(150), " +
                    "visibility VARCHAR(30), " +
                    "text TEXT, " +
                    "created_at TIMESTAMP)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id SERIAL PRIMARY KEY, " +
                    "user_id INTEGER, " +
                    "username VARCHAR(150), " +
                    "role VARCHAR(30), " +
                    "action VARCHAR(120), " +
                    "target VARCHAR(120), " +
                    "details TEXT, " +
                    "created_at TIMESTAMP)");

            addUserIfMissing(conn, "support", "support123", "Support User", "support@example.com", "Support", "eMagiz");
            addUserIfMissing(conn, "consultant", "consultant123", "Consultant User", "consultant@example.com", "Consultant", "eMagiz");
            addUserIfMissing(conn, "customer", "customer123", "Customer User", "customer@example.com", "Customer", "Northwind Logistics");

        } catch (Exception e) {
            System.out.println("Database setup failed");
            e.printStackTrace();
        }
    }

    static void addUserIfMissing(Connection conn, String username, String password, String name, String email, String role, String org) throws Exception {
        PreparedStatement check = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
        check.setString(1, username);
        ResultSet rs = check.executeQuery();

        if (!rs.next()) {
            PreparedStatement insert = conn.prepareStatement(
                    "INSERT INTO users(username, password, name, email, role, organization, session_token) VALUES (?, ?, ?, ?, ?, ?, ?)"
            );
            insert.setString(1, username);
            insert.setString(
                    2,
                    hashPassword(password)
            );
            insert.setString(3, name);
            insert.setString(4, email);
            insert.setString(5, role);
            insert.setString(6, org);
            insert.setString(7, newSessionToken());
            insert.executeUpdate();
        }
    }

    static String getValue(String body, String key) {
        String find = "\"" + key + "\":";
        int start = body.indexOf(find);

        if (start < 0) {
            return "";
        }

        start = start + find.length();

        while (start < body.length() && body.charAt(start) == ' ') {
            start++;
        }

        if (start < body.length() && body.charAt(start) == '"') {
            start++;
            StringBuilder out = new StringBuilder();
            boolean escape = false;

            for (int i = start; i < body.length(); i++) {
                char c = body.charAt(i);

                if (escape) {
                    out.append(c);
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    break;
                } else {
                    out.append(c);
                }
            }

            return out.toString();
        }

        int end = body.indexOf(",", start);
        if (end < 0) {
            end = body.indexOf("}", start);
        }

        if (end < 0) {
            return "";
        }

        return body.substring(start, end).trim();
    }

    static int getInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    static String esc(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
    }

    static String userJson(ResultSet rs) throws Exception {
        return "{" +
                "\"id\":" + rs.getInt("id") + "," +
                "\"username\":\"" + esc(rs.getString("username")) + "\"," +
                "\"name\":\"" + esc(rs.getString("name")) + "\"," +
                "\"email\":\"" + esc(rs.getString("email")) + "\"," +
                "\"role\":\"" + esc(rs.getString("role")) + "\"," +
                "\"organization\":\"" + esc(rs.getString("organization")) + "\"" +
                "}";
    }

    static String ticketJson(ResultSet rs) throws Exception {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        Timestamp deadline = rs.getTimestamp("deadline");

        return "{" +
                "\"id\":" + rs.getInt("id") + "," +
                "\"title\":\"" + esc(rs.getString("title")) + "\"," +
                "\"description\":\"" + esc(rs.getString("description")) + "\"," +
                "\"type\":\"" + esc(rs.getString("type")) + "\"," +
                "\"source\":\"" + esc(rs.getString("source")) + "\"," +
                "\"status\":\"" + esc(rs.getString("status")) + "\"," +
                "\"priority\":\"" + esc(rs.getString("priority")) + "\"," +
                "\"requester\":\"" + esc(rs.getString("requester")) + "\"," +
                "\"requesterUserId\":" + rs.getInt("requester_user_id") + "," +
                "\"assigneeId\":" + rs.getInt("assignee_id") + "," +
                "\"privateNotes\":\"" + esc(rs.getString("private_notes")) + "\"," +
                "\"createdAt\":\"" + (created == null ? "" : created.toLocalDateTime()) + "\"," +
                "\"updatedAt\":\"" + (updated == null ? "" : updated.toLocalDateTime()) + "\"," +
                "\"deadline\":\"" + (deadline == null ? "" : deadline.toLocalDateTime()) + "\"" +
                "}";
    }

    static void audit(int userId, String action, String target, String details) {
        try (Connection conn = getConnection()) {
            PreparedStatement userStmt = conn.prepareStatement("SELECT username, role FROM users WHERE id = ?");
            userStmt.setInt(1, userId);
            ResultSet user = userStmt.executeQuery();

            String username = "Unknown";
            String role = "Unknown";

            if (user.next()) {
                username = user.getString("username");
                role = user.getString("role");
            }

            PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO audit_logs(user_id, username, role, action, target, details, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
            );
            stmt.setInt(1, userId);
            stmt.setString(2, username);
            stmt.setString(3, role);
            stmt.setString(4, action);
            stmt.setString(5, target);
            stmt.setString(6, details);
            stmt.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean isSupport(Connection conn, int userId) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("SELECT role FROM users WHERE id = ?");
        stmt.setInt(1, userId);
        ResultSet rs = stmt.executeQuery();
        return rs.next() && "Support".equals(rs.getString("role"));
    }

    static boolean canUseTicket(Connection conn, int userId, int ticketId) throws Exception {
        PreparedStatement userStmt = conn.prepareStatement("SELECT role FROM users WHERE id = ?");
        userStmt.setInt(1, userId);
        ResultSet user = userStmt.executeQuery();

        if (!user.next()) {
            return false;
        }

        String role = user.getString("role");

        PreparedStatement ticketStmt = conn.prepareStatement("SELECT requester_user_id, assignee_id FROM tickets WHERE id = ?");
        ticketStmt.setInt(1, ticketId);
        ResultSet ticket = ticketStmt.executeQuery();

        if (!ticket.next()) {
            return false;
        }

        if ("Support".equals(role)) {
            return true;
        }

        if ("Consultant".equals(role)) {
            return ticket.getInt("assignee_id") == userId;
        }

        return ticket.getInt("requester_user_id") == userId;
    }

}
