package com.gamilha.services;

import com.gamilha.entity.AppNotification;
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

public class NotificationService {

    public NotificationService() {
        CoachingFeatureSchemaService.ensureSchema();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    public void notifyVideoAdded(Playlist playlist, CoachingVideo video) {
        if (video == null) {
            return;
        }

        String playlistName = playlist != null && playlist.getTitle() != null
                ? playlist.getTitle()
                : "Sans playlist";
        String message = "La playlist \"" + playlistName + "\" a ajoute la video \"" + video.getTitre() + "\".";

        String sql = """
                INSERT INTO coaching_notification (user_id, playlist_id, video_id, message, is_read)
                VALUES (NULL, ?, ?, ?, 0)
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            if (playlist != null) {
                ps.setInt(1, playlist.getId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setInt(2, video.getId());
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur notifyVideoAdded: " + e.getMessage());
        }
    }

    public List<AppNotification> getRecentNotifications(Integer userId, int limit) {
        List<AppNotification> notifications = new ArrayList<>();
        String sql = """
                SELECT *
                FROM coaching_notification
                WHERE user_id IS NULL OR user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return notifications;
            }
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AppNotification notification = new AppNotification();
                    notification.setId(rs.getInt("id"));
                    int userValue = rs.getInt("user_id");
                    notification.setUserId(rs.wasNull() ? null : userValue);
                    int playlistValue = rs.getInt("playlist_id");
                    notification.setPlaylistId(rs.wasNull() ? null : playlistValue);
                    int videoValue = rs.getInt("video_id");
                    notification.setVideoId(rs.wasNull() ? null : videoValue);
                    notification.setMessage(rs.getString("message"));
                    notification.setRead(rs.getBoolean("is_read"));
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        notification.setCreatedAt(timestamp.toLocalDateTime());
                    }
                    notifications.add(notification);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getRecentNotifications: " + e.getMessage());
        }
        return notifications;
    }

    public int countUnread(Integer userId) {
        String sql = """
                SELECT COUNT(*)
                FROM coaching_notification
                WHERE is_read = 0 AND (user_id IS NULL OR user_id = ?)
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return 0;
            }
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur countUnread: " + e.getMessage());
            return 0;
        }
    }

    public void markAllAsRead(Integer userId) {
        String sql = """
                UPDATE coaching_notification
                SET is_read = 1
                WHERE is_read = 0 AND (user_id IS NULL OR user_id = ?)
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur markAllAsRead: " + e.getMessage());
        }
    }
}
