package com.gamilha.services;

import com.gamilha.entity.Playlist;

import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlaylistService implements IPlaylistService {


    // ─────────────────────────────────────────────────────────────────────────
    // AJOUTER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void ajouterPlaylist(Playlist playlist) {
        String sql = "INSERT INTO playlist (title, description, niveau, categorie, image, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cnx =  ConnectionManager.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, playlist.getTitle());
            ps.setString(2, playlist.getDescription());
            ps.setString(3, playlist.getNiveau());
            ps.setString(4, playlist.getCategorie());
            ps.setString(5, playlist.getImage());
            ps.setTimestamp(6, Timestamp.valueOf(
                    playlist.getCreatedAt() != null ? playlist.getCreatedAt() : LocalDateTime.now()));
            ps.executeUpdate();
            System.out.println("✅ Playlist ajoutée : " + playlist.getTitle());
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajouterPlaylist : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODIFIER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void modifierPlaylist(Playlist playlist) {
        String sql = "UPDATE playlist SET title=?, description=?, niveau=?, categorie=?, image=?, created_at=? "
                   + "WHERE id=?";
        try (Connection cnx =  ConnectionManager.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, playlist.getTitle());
            ps.setString(2, playlist.getDescription());
            ps.setString(3, playlist.getNiveau());
            ps.setString(4, playlist.getCategorie());
            ps.setString(5, playlist.getImage());
            ps.setTimestamp(6, Timestamp.valueOf(
                    playlist.getCreatedAt() != null ? playlist.getCreatedAt() : LocalDateTime.now()));
            ps.setInt(7, playlist.getId());
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✅ Playlist modifiée (id=" + playlist.getId() + ")");
            else
                System.out.println("⚠️ Aucune playlist trouvée avec l'id=" + playlist.getId());
        } catch (SQLException e) {
            System.err.println("❌ Erreur modifierPlaylist : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUPPRIMER
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void supprimerPlaylist(int id) {
        String sql = "DELETE FROM playlist WHERE id=?";
        try (Connection cnx =  ConnectionManager.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("✅ Playlist supprimée (id=" + id + ")");
            else
                System.out.println("⚠️ Aucune playlist trouvée avec l'id=" + id);
        } catch (SQLException e) {
            System.err.println("❌ Erreur supprimerPlaylist : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFICHER TOUTES
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<Playlist> afficherPlaylists() {
        List<Playlist> liste = new ArrayList<>();
        String sql = "SELECT * FROM playlist";
        try (Connection cnx =  ConnectionManager.getConnection();
             Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Playlist p = new Playlist();
                p.setId(rs.getInt("id"));
                p.setTitle(rs.getString("title"));
                p.setDescription(rs.getString("description"));
                p.setNiveau(rs.getString("niveau"));
                p.setCategorie(rs.getString("categorie"));
                p.setImage(rs.getString("image"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
                liste.add(p);
            }
            System.out.println("✅ " + liste.size() + " playlist(s) récupérée(s).");
        } catch (SQLException e) {
            System.err.println("❌ Erreur afficherPlaylists : " + e.getMessage());
        }
        return liste;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFICHER UNE PAR ID
    // ─────────────────────────────────────────────────────────────────────────
    public Playlist getPlaylistById(int id) {
        String sql = "SELECT * FROM playlist WHERE id=?";
        try (Connection cnx =  ConnectionManager.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Playlist p = new Playlist();
                p.setId(rs.getInt("id"));
                p.setTitle(rs.getString("title"));
                p.setDescription(rs.getString("description"));
                p.setNiveau(rs.getString("niveau"));
                p.setCategorie(rs.getString("categorie"));
                p.setImage(rs.getString("image"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) p.setCreatedAt(ts.toLocalDateTime());
                return p;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur getPlaylistById : " + e.getMessage());
        }
        return null;
    }
}
