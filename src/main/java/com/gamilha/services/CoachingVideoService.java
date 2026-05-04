package com.gamilha.services;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CoachingVideoService implements ICoachingVideoService {

    public CoachingVideoService() {
        CoachingFeatureSchemaService.ensureSchema();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    @Override
    public void ajouterVideo(CoachingVideo video) {
        String sql = """
                INSERT INTO coaching_video (titre, description, url, niveau, premium, playlist_id, duration)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null
                     ? cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                     : null) {
            if (ps == null) {
                return;
            }

            ps.setString(1, video.getTitre());
            ps.setString(2, video.getDescription());
            ps.setString(3, video.getUrl());
            ps.setString(4, video.getNiveau());
            ps.setBoolean(5, video.isPremium());
            if (video.getPlaylist() != null) {
                ps.setInt(6, video.getPlaylist().getId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setInt(7, video.getDuration());
            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    video.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur ajouterVideo: " + e.getMessage());
        }
    }

    @Override
    public void modifierVideo(CoachingVideo video) {
        String sql = """
                UPDATE coaching_video
                SET titre = ?, description = ?, url = ?, niveau = ?, premium = ?, playlist_id = ?, duration = ?
                WHERE id = ?
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }

            ps.setString(1, video.getTitre());
            ps.setString(2, video.getDescription());
            ps.setString(3, video.getUrl());
            ps.setString(4, video.getNiveau());
            ps.setBoolean(5, video.isPremium());
            if (video.getPlaylist() != null) {
                ps.setInt(6, video.getPlaylist().getId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setInt(7, video.getDuration());
            ps.setInt(8, video.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur modifierVideo: " + e.getMessage());
        }
    }

    @Override
    public void supprimerVideo(int id) {
        String sql = "DELETE FROM coaching_video WHERE id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur supprimerVideo: " + e.getMessage());
        }
    }

    @Override
    public List<CoachingVideo> afficherVideos() {
        List<CoachingVideo> videos = new ArrayList<>();
        String sql = """
                SELECT cv.*,
                       p.id AS p_id, p.title AS p_title, p.description AS p_desc,
                       p.niveau AS p_niveau, p.categorie AS p_categorie,
                       p.image AS p_image, p.created_at AS p_created_at
                FROM coaching_video cv
                LEFT JOIN playlist p ON cv.playlist_id = p.id
                """;
        try (Connection cnx = getConnection();
             Statement st = cnx != null ? cnx.createStatement() : null;
             ResultSet rs = st != null ? st.executeQuery(sql) : null) {
            if (rs == null) {
                return videos;
            }

            while (rs.next()) {
                videos.add(mapVideo(rs, true));
            }
        } catch (SQLException e) {
            System.err.println("Erreur afficherVideos: " + e.getMessage());
        }
        return videos;
    }

    public List<CoachingVideo> afficherVideosByPlaylist(int playlistId) {
        List<CoachingVideo> videos = new ArrayList<>();
        String sql = """
                SELECT cv.*,
                       p.id AS p_id, p.title AS p_title, p.description AS p_desc,
                       p.niveau AS p_niveau, p.categorie AS p_categorie,
                       p.image AS p_image, p.created_at AS p_created_at
                FROM coaching_video cv
                LEFT JOIN playlist p ON cv.playlist_id = p.id
                WHERE cv.playlist_id = ?
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return videos;
            }
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    videos.add(mapVideo(rs, true));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur afficherVideosByPlaylist: " + e.getMessage());
        }
        return videos;
    }

    private CoachingVideo mapVideo(ResultSet rs, boolean withPlaylistDetails) throws SQLException {
        Playlist playlist = new Playlist();
        playlist.setId(rs.getInt("p_id"));
        playlist.setTitle(rs.getString("p_title"));
        if (withPlaylistDetails) {
            playlist.setDescription(rs.getString("p_desc"));
            playlist.setNiveau(rs.getString("p_niveau"));
            playlist.setCategorie(rs.getString("p_categorie"));
            playlist.setImage(rs.getString("p_image"));
            Timestamp ts = rs.getTimestamp("p_created_at");
            if (ts != null) {
                playlist.setCreatedAt(ts.toLocalDateTime());
            }
        }

        CoachingVideo video = new CoachingVideo();
        video.setId(rs.getInt("id"));
        video.setTitre(rs.getString("titre"));
        video.setDescription(rs.getString("description"));
        video.setUrl(rs.getString("url"));
        video.setNiveau(rs.getString("niveau"));
        video.setPremium(rs.getBoolean("premium"));
        video.setDuration(rs.getInt("duration"));
        video.setPlaylist(playlist.getId() > 0 ? playlist : null);
        return video;
    }
}
