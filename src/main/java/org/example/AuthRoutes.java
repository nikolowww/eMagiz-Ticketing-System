package org.example;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.example.Main.audit;
import static org.example.Main.getConnection;
import static org.example.Main.getInt;
import static org.example.Main.getValue;
import static org.example.Main.newSessionToken;
import static org.example.Main.userJson;
import static org.example.Main.verifyPassword;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class AuthRoutes {

    @GET
    public String home() {
        return "{\"message\":\"Jersey backend running\"}";
    }

    @OPTIONS
    @Path("{path:.*}")
    public Response options() {
        return Response.ok().build();
    }

    /**
     * Authenticates a user with username and password.
     * Returns user details if credentials are valid, optherwise it returns the error with number 401.
     * @param body
     * @return
     */
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(String body) {
        String username = getValue(body, "username");
        String password = getValue(body, "password");

        try (Connection conn = getConnection()) {

            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ?"
            );

            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {

                String storedPassword =
                        rs.getString("password");

                boolean validLogin = false;

                if (storedPassword.startsWith("$argon2")) {

                    validLogin = verifyPassword(
                            password,
                            storedPassword
                    );

                } else {

                    validLogin =
                            storedPassword.equals(password);
                }

                if (validLogin) {

                    int userId = rs.getInt("id");
                    String userJsonString = userJson(rs);

                    String token = newSessionToken();
                    PreparedStatement tokenStmt = conn.prepareStatement(
                            "UPDATE users SET session_token = ? WHERE id = ?"
                    );
                    tokenStmt.setString(1, token);
                    tokenStmt.setInt(2, userId);
                    tokenStmt.executeUpdate();

                    audit(
                            userId,
                            "Login",
                            "User " + userId,
                            "User logged in"
                    );

                    return Response.ok(
                            "{\"ok\":true,\"message\":\"Login successful\",\"token\":\""
                                    + token
                                    + "\",\"user\":"
                                    + userJsonString
                                    + "}"
                    ).build();
                }
            }

            return Response.status(401)
                    .entity("{\"ok\":false,\"message\":\"Invalid credentials\"}")
                    .build();

        } catch (Exception e) {
            e.printStackTrace();

            return Response.serverError()
                    .entity("{\"ok\":false,\"message\":\"Login failed\"}")
                    .build();
        }
    }

    /**
     * Logs out the user and put records in the audit.
     * Returns success confirmation.
     * @param body
     * @return
     */

    @POST
    @Path("logout")
    @Consumes(MediaType.APPLICATION_JSON)
    public String logout(String body) {
        int userId = getInt(getValue(body, "userId"));

        try (Connection conn = getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("UPDATE users SET session_token = NULL WHERE id = ?");
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        audit(userId, "Logout", "User " + userId, "User logged out");
        return "{\"ok\":true,\"message\":\"Logout successful\"}";
    }
}
