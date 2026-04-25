package com.gamilha.services;

import com.gamilha.entity.Commentaire;
import com.gamilha.entity.Post;
import com.gamilha.entity.User;

import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentaireService {

    private final Connection conn = ConnectionManager.getInstance().getConnection();

    // ── CREATE ────────────────────────────────────────────────────────────
    public void create(Commentaire c) throws SQLException {
        String sql = "INSERT INTO commentaire (text, created_at, post_id, user_id) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getText());
            ps.setTimestamp(2, Timestamp.valueOf(
                c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.now()));
            ps.setInt(3, c.getPost().getId());
            ps.setInt(4, c.getUser().getId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) c.setId(rs.getInt(1));
            }
        }
    }

    // ── READ — tous les commentaires d'un post (avec jointure User) ───────
    public List<Commentaire> findByPost(int postId) throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql =
            "SELECT c.id, c.text, c.created_at, c.post_id, c.user_id, " +
            "       u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic, " +
            "       u.roles, u.is_active, u.ban_until " +
            "FROM commentaire c " +
            "JOIN `user` u ON u.id = c.user_id " +
            "WHERE c.post_id = ? " +
            "ORDER BY c.created_at ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLight(rs));
            }
        }
        return list;
    }

    // ── READ ALL — admin (avec jointure User + infos post) ────────────────
    public List<Commentaire> findAll() throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql =
            "SELECT c.id, c.text, c.created_at, c.post_id, c.user_id, " +
            "       u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic, " +
            "       u.roles, u.is_active, u.ban_until, " +
            "       p.content AS p_content, p.image AS p_image, p.created_at AS p_date, p.mediaurl " +
            "FROM commentaire c " +
            "JOIN `user` u ON u.id = c.user_id " +
            "JOIN post   p ON p.id = c.post_id " +
            "ORDER BY c.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapFull(rs));
        }
        return list;
    }

    // ── SEARCH par texte ou auteur ─────────────────────────────────────────
    public List<Commentaire> search(String keyword) throws SQLException {
        List<Commentaire> list = new ArrayList<>();
        String sql =
            "SELECT c.id, c.text, c.created_at, c.post_id, c.user_id, " +
            "       u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic, " +
            "       u.roles, u.is_active, u.ban_until " +
            "FROM commentaire c " +
            "JOIN `user` u ON u.id = c.user_id " +
            "WHERE c.text LIKE ? OR u.name LIKE ? " +
            "ORDER BY c.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLight(rs));
            }
        }
        return list;
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────
    public void update(Commentaire c) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE commentaire SET text=? WHERE id=?")) {
            ps.setString(1, c.getText());
            ps.setInt(2, c.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────────────
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM commentaire WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── mappers ─────────────────────────────────────────────────────────────
    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("user_id"),
            rs.getString("u_name"),
            rs.getString("u_email"),
            rs.getString("u_pic"),
            rs.getString("roles"),
            rs.getBoolean("is_active"),
            rs.getString("ban_until")
        );
    }

    /** Sans infos complètes du post (juste l'id) */
    private Commentaire mapLight(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("post_id"));
        return new Commentaire(
            rs.getInt("id"),
            rs.getString("text"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            post,
            mapUser(rs)
        );
    }

    /** Avec infos du post (pour vue admin liste) */
    private Commentaire mapFull(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("post_id"));
        post.setContent(rs.getString("p_content"));
        post.setImage(rs.getString("p_image"));
        post.setMediaurl(rs.getString("mediaurl"));
        post.setCreatedAt(rs.getTimestamp("p_date").toLocalDateTime());
        return new Commentaire(
            rs.getInt("id"),
            rs.getString("text"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            post,
            mapUser(rs)
        );
    }
}
