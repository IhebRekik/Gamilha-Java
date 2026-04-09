package com.gamilha.services;

import com.gamilha.entity.User;
import com.gamilha.utils.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private final Connection cnx = DatabaseConnection.getConnection();

    public User login(String email, String plainPassword) {
        String sql = "SELECT id, email, name, password, roles FROM user WHERE email = ? LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String hashedPassword = rs.getString("password");
                if (!isPasswordValid(plainPassword, hashedPassword)) {
                    return null;
                }

                int id = rs.getInt("id");
                String userEmail = rs.getString("email");
                String name = rs.getString("name");
                String rolesJson = rs.getString("roles");

                User u =  new User();
                u.setId(id);
                u.setEmail(email);
                u.setName(name);
                u.setRoles(parseRoles(rolesJson));
                return u;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'authentification: " + e.getMessage(), e);
        }
    }

    private boolean isPasswordValid(String plainPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }

        // Symfony/PHP bcrypt hashes usually start with "$2y$" while jBCrypt
        // expects "$2a$". Normalizing avoids "Invalid salt revision" errors.
        String normalizedHash = hashedPassword.startsWith("$2y$")
                ? "$2a$" + hashedPassword.substring(4)
                : hashedPassword;

        try {
            return BCrypt.checkpw(plainPassword, normalizedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private List<String> parseRoles(String rolesJson) {
        List<String> roles = new ArrayList<>();
        if (rolesJson == null || rolesJson.isBlank()) {
            roles.add("ROLE_USER");
            return roles;
        }

        String normalized = rolesJson
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .trim();

        if (!normalized.isBlank()) {
            for (String role : normalized.split(",")) {
                String cleanRole = role.trim();
                if (!cleanRole.isEmpty()) {
                    roles.add(cleanRole);
                }
            }
        }

        if (!roles.contains("ROLE_USER")) {
            roles.add("ROLE_USER");
        }

        return roles;
    }
}
