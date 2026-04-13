package com.gamilha.service;

import com.gamilha.model.Post;
import com.gamilha.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostService {

    private final Connection conn = DBConnection.getInstance();

    // ── SQL de base (jointure User + count likes) ────────────────────────
    private static final String BASE_SQL =
        "SELECT p.id, p.content, p.image, p.created_at, p.mediaurl, p.user_id, " +
        "       u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic, " +
        "       u.roles, u.is_active, u.ban_until, " +
        "       (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) AS likes_count " +
        "FROM post p " +
        "JOIN `user` u ON u.id = p.user_id ";

    // ── CREATE ──────────────────────────────────────────────────────────
    public void create(Post post) throws SQLException {
        String sql = "INSERT INTO post (content, image, created_at, mediaurl, user_id) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getContent());
            ps.setString(2, post.getImage());
            ps.setTimestamp(3, Timestamp.valueOf(
                post.getCreatedAt() != null ? post.getCreatedAt() : LocalDateTime.now()));
            ps.setString(4, post.getMediaurl());
            ps.setInt(5, post.getUser().getId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) post.setId(rs.getInt(1));
            }
        }
    }

    // ── READ ALL ─────────────────────────────────────────────────────────
    public List<Post> findAll() throws SQLException {
        List<Post> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(BASE_SQL + "ORDER BY p.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ── READ BY ID ────────────────────────────────────────────────────────
    public Post findById(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(BASE_SQL + "WHERE p.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ── SEARCH par contenu ou nom auteur ──────────────────────────────────
    public List<Post> search(String keyword) throws SQLException {
        List<Post> list = new ArrayList<>();
        String sql = BASE_SQL + "WHERE p.content LIKE ? OR u.name LIKE ? ORDER BY p.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    public void update(Post post) throws SQLException {
        String sql = "UPDATE post SET content=?, image=?, mediaurl=?, user_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getContent());
            ps.setString(2, post.getImage());
            ps.setString(3, post.getMediaurl());
            ps.setInt(4, post.getUser().getId());
            ps.setInt(5, post.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    public void delete(int id) throws SQLException {
        // Les commentaires sont supprimés en CASCADE (onDelete="CASCADE" dans Symfony)
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM post WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── mapper ResultSet → Post ───────────────────────────────────────────
    private Post map(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getInt("user_id"),
            rs.getString("u_name"),
            rs.getString("u_email"),
            rs.getString("u_pic"),
            rs.getString("roles"),
            rs.getBoolean("is_active"),
            rs.getString("ban_until")
        );
        return new Post(
            rs.getInt("id"),
            rs.getString("content"),
            rs.getString("image"),
            rs.getString("mediaurl"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            user,
            rs.getInt("likes_count")
        );
    }
}
