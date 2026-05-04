package com.gamilha.services;

import com.gamilha.entity.User;
import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
            "WHERE f.user_id = ? ";
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


    public Map<Integer, Boolean> getOnlineStatus(List<User> users) {
        Map<Integer, Boolean> status = new HashMap<>();
        if (users == null || users.isEmpty()) return status;

        // Essai 1 : table user_activity avec last_seen
        try {
            String ids = users.stream()
                .map(u -> String.valueOf(u.getId()))
                .reduce((a, b) -> a + "," + b).orElse("0");
            String sql = "SELECT user_id, " +
                "CASE WHEN last_seen >= DATE_SUB(NOW(), INTERVAL 15 MINUTE) " +
                "     THEN 1 ELSE 0 END AS online " +
                "FROM user_activity WHERE user_id IN (" + ids + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) status.put(rs.getInt("user_id"), rs.getInt("online") == 1);
                for (User u : users) status.putIfAbsent(u.getId(), false);
                return status;
            }
        } catch (SQLException ignored) {}

        // Fallback : is_active=true + pas de ban = "en ligne"
        for (User u : users) {
            boolean online = u.isActive() && (u.getBanUntil() == null || u.getBanUntil().isBlank());
            status.put(u.getId(), online);
        }
        return status;
    }

    /** Suggestions : utilisateurs pas encore amis (dans aucun sens) */


    public void removeFriend(int userId, int friendId) throws SQLException {
        String sql1 = "DELETE FROM friend WHERE user_id=? AND friend_id=?";
        String sql2 = "DELETE FROM friend WHERE user_id=? AND friend_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setInt(1, userId); ps.setInt(2, friendId); ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setInt(1, friendId); ps.setInt(2, userId); ps.executeUpdate();
        }
        System.out.println("✅ Ami retiré : " + userId + " ↔ " + friendId);
    }

    /**
     * Statut en ligne :
     * 1. Cherche user_activity.last_seen < 15 min
     * 2. Fallback : is_active + pas de ban
     */
}
