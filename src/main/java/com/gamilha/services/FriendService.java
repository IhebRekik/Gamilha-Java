package com.gamilha.services;

import com.gamilha.entity.User;
import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour les amis (table friend dans Symfony).
 */
public class FriendService {

    private final Connection conn = ConnectionManager.getConnection();

    /** Récupère les amis de l'utilisateur */
    public List<User> findFriends(int userId) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql =
            "SELECT u.id, u.name, u.email, u.profile_image, u.roles, u.is_active, u.ban_until " +
            "FROM friend f " +
            "JOIN `user` u ON u.id = f.friend_id " +
            "WHERE f.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapUser(rs));
            }
        }
        return list;
    }

    /** Suggestions : utilisateurs qui ne sont pas encore amis */
    public List<User> findSuggestions(int userId, int limit) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql =
            "SELECT u.id, u.name, u.email, u.profile_image, u.roles, u.is_active, u.ban_until " +
            "FROM `user` u " +
            "WHERE u.id != ? " +
            "  AND u.id NOT IN (" +
            "      SELECT f.friend_id FROM friend f WHERE f.user_id = ?" +
            "  ) " +
            "LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapUser(rs));
            }
        }
        return list;
    }

    /** Ajouter un ami */
    public void addFriend(int userId, int friendId) throws SQLException {
        String sql = "INSERT IGNORE INTO friend (user_id, friend_id) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, friendId);
            ps.executeUpdate();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
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
