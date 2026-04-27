package com.gamilha.services;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FavoriteVideoService {

    public FavoriteVideoService() {
        CoachingFeatureSchemaService.ensureSchema();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    public boolean isFavorite(Integer userId, int videoId) {
        if (userId == null || videoId <= 0) {
            return false;
        }

        String sql = "SELECT 1 FROM video_favorite WHERE user_id = ? AND video_id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return false;
            }
            ps.setInt(1, userId);
            ps.setInt(2, videoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Erreur isFavorite: " + e.getMessage());
            return false;
        }
    }

    public boolean toggleFavorite(Integer userId, CoachingVideo video) {
        if (userId == null || video == null || video.getId() <= 0) {
            return false;
        }

        if (isFavorite(userId, video.getId())) {
            removeFavorite(userId, video.getId());
            return false;
        }

        addFavorite(userId, video.getId());
        return true;
    }

    public void addFavorite(Integer userId, int videoId) {
        if (userId == null || videoId <= 0) {
            return;
        }

        String sql = "INSERT IGNORE INTO video_favorite (user_id, video_id) VALUES (?, ?)";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            ps.setInt(1, userId);
            ps.setInt(2, videoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur addFavorite: " + e.getMessage());
        }
    }

    public void removeFavorite(Integer userId, int videoId) {
        if (userId == null || videoId <= 0) {
            return;
        }

        String sql = "DELETE FROM video_favorite WHERE user_id = ? AND video_id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            ps.setInt(1, userId);
            ps.setInt(2, videoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur removeFavorite: " + e.getMessage());
        }
    }

    public int countFavorites(Integer userId) {
        if (userId == null) {
            return 0;
        }

        String sql = "SELECT COUNT(*) FROM video_favorite WHERE user_id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return 0;
            }
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur countFavorites: " + e.getMessage());
            return 0;
        }
    }

    public int countFavoritesForPlaylist(int playlistId) {
        String sql = """
                SELECT COUNT(*)
                FROM video_favorite vf
                INNER JOIN coaching_video cv ON cv.id = vf.video_id
                WHERE cv.playlist_id = ?
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return 0;
            }
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur countFavoritesForPlaylist: " + e.getMessage());
            return 0;
        }
    }

    public List<CoachingVideo> getFavoriteVideos(Integer userId) {
        List<CoachingVideo> favorites = new ArrayList<>();
        if (userId == null) {
            return favorites;
        }

        String sql = """
                SELECT cv.*, p.id AS p_id, p.title AS p_title, p.description AS p_desc,
                       p.niveau AS p_niveau, p.categorie AS p_categorie, p.image AS p_image,
                       p.created_at AS p_created_at
                FROM video_favorite vf
                INNER JOIN coaching_video cv ON cv.id = vf.video_id
                LEFT JOIN playlist p ON p.id = cv.playlist_id
                WHERE vf.user_id = ?
                ORDER BY vf.created_at DESC
                """;

        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return favorites;
            }
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    favorites.add(mapVideo(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getFavoriteVideos: " + e.getMessage());
        }

        return favorites;
    }

    private CoachingVideo mapVideo(ResultSet rs) throws SQLException {
        Playlist playlist = new Playlist();
        playlist.setId(rs.getInt("p_id"));
        playlist.setTitle(rs.getString("p_title"));
        playlist.setDescription(rs.getString("p_desc"));
        playlist.setNiveau(rs.getString("p_niveau"));
        playlist.setCategorie(rs.getString("p_categorie"));
        playlist.setImage(rs.getString("p_image"));
        Timestamp playlistTs = rs.getTimestamp("p_created_at");
        if (playlistTs != null) {
            playlist.setCreatedAt(playlistTs.toLocalDateTime());
        }

        CoachingVideo video = new CoachingVideo();
        video.setId(rs.getInt("id"));
        video.setTitre(rs.getString("titre"));
        video.setDescription(rs.getString("description"));
        video.setUrl(rs.getString("url"));
        video.setNiveau(rs.getString("niveau"));
        video.setPremium(rs.getBoolean("premium"));
        video.setDuration(rs.getInt("duration"));
        video.setPlaylist(playlist);
        return video;
    }
}
