package com.gamilha.services;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.entity.PlaylistStatistics;
import com.gamilha.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PlaylistStatisticsService {

    private final FavoriteVideoService favoriteVideoService = new FavoriteVideoService();

    public PlaylistStatisticsService() {
        CoachingFeatureSchemaService.ensureSchema();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    public void recordView(CoachingVideo video, Integer userId) {
        if (video == null || video.getId() <= 0) {
            return;
        }

        String sql = """
                INSERT INTO video_view_stat (user_id, playlist_id, video_id)
                VALUES (?, ?, ?)
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
            if (video.getPlaylist() != null) {
                ps.setInt(2, video.getPlaylist().getId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, video.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur recordView: " + e.getMessage());
        }
    }

    public PlaylistStatistics getStatisticsForPlaylist(Playlist playlist) {
        PlaylistStatistics statistics = new PlaylistStatistics();
        if (playlist == null) {
            return statistics;
        }

        statistics.setPlaylistId(playlist.getId());
        statistics.setTotalVideos(fetchTotalVideos(playlist.getId()));
        statistics.setTotalViews(fetchTotalViews(playlist.getId()));
        statistics.setWatchedVideos(fetchWatchedVideos(playlist.getId()));
        statistics.setTotalFavorites(favoriteVideoService.countFavoritesForPlaylist(playlist.getId()));
        statistics.setAverageViewsPerVideo(
                statistics.getTotalVideos() == 0
                        ? 0
                        : (double) statistics.getTotalViews() / statistics.getTotalVideos()
        );

        fetchTopVideo(statistics, playlist.getId());
        statistics.setLastViewedAt(fetchLastViewedAt(playlist.getId()));
        statistics.setTopVideoSummaries(fetchTopVideoSummaries(playlist.getId(), 3));
        return statistics;
    }

    private int fetchTotalVideos(int playlistId) {
        return fetchCount("SELECT COUNT(*) FROM coaching_video WHERE playlist_id = ?", playlistId);
    }

    private int fetchTotalViews(int playlistId) {
        return fetchCount("SELECT COUNT(*) FROM video_view_stat WHERE playlist_id = ?", playlistId);
    }

    private int fetchWatchedVideos(int playlistId) {
        String sql = "SELECT COUNT(DISTINCT video_id) FROM video_view_stat WHERE playlist_id = ?";
        return fetchCount(sql, playlistId);
    }

    private int fetchCount(String sql, int playlistId) {
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
            System.err.println("Erreur fetchCount: " + e.getMessage());
            return 0;
        }
    }

    private void fetchTopVideo(PlaylistStatistics statistics, int playlistId) {
        String sql = """
                SELECT cv.titre, COUNT(*) AS views_count
                FROM video_view_stat vs
                INNER JOIN coaching_video cv ON cv.id = vs.video_id
                WHERE vs.playlist_id = ?
                GROUP BY cv.id, cv.titre
                ORDER BY views_count DESC, cv.titre ASC
                LIMIT 1
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    statistics.setTopVideoTitle(rs.getString("titre"));
                    statistics.setTopVideoViews(rs.getInt("views_count"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur fetchTopVideo: " + e.getMessage());
        }
    }

    private java.time.LocalDateTime fetchLastViewedAt(int playlistId) {
        String sql = "SELECT MAX(viewed_at) FROM video_view_stat WHERE playlist_id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return null;
            }
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    return ts != null ? ts.toLocalDateTime() : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur fetchLastViewedAt: " + e.getMessage());
        }
        return null;
    }

    private List<String> fetchTopVideoSummaries(int playlistId, int limit) {
        List<String> summaries = new ArrayList<>();
        String sql = """
                SELECT cv.titre, COUNT(*) AS views_count
                FROM video_view_stat vs
                INNER JOIN coaching_video cv ON cv.id = vs.video_id
                WHERE vs.playlist_id = ?
                GROUP BY cv.id, cv.titre
                ORDER BY views_count DESC, cv.titre ASC
                LIMIT ?
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return summaries;
            }
            ps.setInt(1, playlistId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    summaries.add(rs.getString("titre") + " - " + rs.getInt("views_count") + " vues");
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur fetchTopVideoSummaries: " + e.getMessage());
        }
        return summaries;
    }
}
