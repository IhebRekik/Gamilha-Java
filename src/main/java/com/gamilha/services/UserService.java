package com.gamilha.services;

import com.gamilha.entity.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection conn = DBConnection.getInstance();

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, name, email, profile_image, roles, is_active, ban_until FROM `user`";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, name, email, profile_image, roles, is_active, ban_until FROM `user` WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("profile_image"),
            rs.getString("roles"),
            rs.getBoolean("is_active"),
            rs.getString("ban_until")
        );
    }
}
