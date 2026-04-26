package com.gamilha.services;

import com.gamilha.tools.DBconnexion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserLookupService {
    private final Connection cnx = DBconnexion.getInstance().getCnx();

    public Map<Integer, String> findAllUsers() {
        String sql = "SELECT id, name, email FROM user ORDER BY name ASC";
        Map<Integer, String> users = new LinkedHashMap<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                users.put(id, name + " (" + email + ")");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement utilisateurs: " + e.getMessage(), e);
        }

        return users;
    }
}

