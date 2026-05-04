package com.gamilha.services;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * BookmarkService — Sauvegarder / retirer des posts en favoris.
 *
 * Crée automatiquement la table `post_bookmark` si elle n'existe pas.
 * Schéma minimal compatible Symfony :
 *   post_bookmark(id INT PK AUTO_INCREMENT, user_id INT, post_id INT, created_at DATETIME)
 *
 * Aucun changement sur les tables Symfony existantes.
 */
public class BookmarkService {

    private final Connection conn = DBConnection.getInstance();

    // ── Création auto de la table au premier démarrage ────────────────────
    static {
        try {
            Connection c = DBConnection.getInstance();
            try (Statement st = c.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS post_bookmark (" +
                                "  id         INT          NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                                "  user_id    INT          NOT NULL," +
                                "  post_id    INT          NOT NULL," +
                                "  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                                "  UNIQUE KEY uq_user_post (user_id, post_id)" +
                                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"
                );
                System.out.println("✅ Table post_bookmark prête");
            }
        } catch (Exception e) {
            System.err.println("BookmarkService init: " + e.getMessage());
        }
    }

    // ── Toggle : ajouter ou retirer un bookmark ───────────────────────────
    /**
     * Ajoute le post aux favoris si pas encore sauvegardé, sinon le retire.
     * @return true si le post est maintenant sauvegardé, false s'il a été retiré.
     */
    public boolean toggleBookmark(int userId, int postId) throws SQLException {
        if (isBookmarked(userId, postId)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM post_bookmark WHERE user_id=? AND post_id=?")) {
                ps.setInt(1, userId);
                ps.setInt(2, postId);
                ps.executeUpdate();
            }
            return false;
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO post_bookmark (user_id, post_id, created_at) VALUES (?,?,?)")) {
                ps.setInt(1, userId);
                ps.setInt(2, postId);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.executeUpdate();
            }
            return true;
        }
    }

    // ── Vérifier si un post est sauvegardé ───────────────────────────────
    public boolean isBookmarked(int userId, int postId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM post_bookmark WHERE user_id=? AND post_id=?")) {
            ps.setInt(1, userId);
            ps.setInt(2, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── Récupérer tous les posts sauvegardés d'un utilisateur ────────────
    /**
     * Retourne les posts bookmarkés triés par date de sauvegarde (plus récent en premier).
     */
    public List<Post> findBookmarkedPosts(int userId) throws SQLException {
        String sql =
                "SELECT p.id, p.content, p.image, p.created_at, p.mediaurl, p.user_id," +
                        "       IFNULL(p.shared_from_id, 0) AS shared_from_id," +
                        "       u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic," +
                        "       u.roles, u.is_active, u.ban_until," +
                        "       (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) AS likes_count," +
                        "       pb.created_at AS saved_at" +
                        " FROM post_bookmark pb" +
                        " JOIN post p  ON p.id = pb.post_id" +
                        " JOIN `user` u ON u.id = p.user_id" +
                        " WHERE pb.user_id = ?" +
                        " ORDER BY pb.created_at DESC";

        List<Post> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User author = new User(
                            rs.getInt("user_id"),
                            rs.getString("u_name"),
                            rs.getString("u_email"),
                            rs.getString("u_pic"),
                            rs.getString("roles"),
                            rs.getBoolean("is_active"),
                            rs.getString("ban_until")
                    );
                    Post p = new Post(
                            rs.getInt("id"),
                            rs.getString("content"),
                            rs.getString("image"),
                            rs.getString("mediaurl"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            author,
                            rs.getInt("likes_count")
                    );
                    p.setTextStyle(PostService.extractStyle(rs.getString("content")));
                    list.add(p);
                }
            }
        }
        return list;
    }

    // ── Compter les bookmarks d'un post ───────────────────────────────────
    public int countBookmarks(int postId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM post_bookmark WHERE post_id=?")) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
