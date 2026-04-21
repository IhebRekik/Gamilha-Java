package com.gamilha.services;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoachingVideoService implements ICoachingVideoService {

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    // AJOUTER
    @Override
    public void ajouterVideo(CoachingVideo video) {

        String sql =
                "INSERT INTO coaching_video " +
                        "(titre, description, url, niveau, premium, playlist_id, duration) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection cnx = getConnection();
                PreparedStatement ps = cnx.prepareStatement(sql)
        ) {

            ps.setString(1, video.getTitre());
            ps.setString(2, video.getDescription());
            ps.setString(3, video.getUrl());
            ps.setString(4, video.getNiveau());
            ps.setBoolean(5, video.isPremium());

            if(video.getPlaylist() != null)
                ps.setInt(6, video.getPlaylist().getId());
            else
                ps.setNull(6, Types.INTEGER);

            ps.setInt(7, video.getDuration());

            ps.executeUpdate();

            System.out.println("Vidéo ajoutée");

        }
        catch (SQLException e){

            System.out.println(e.getMessage());

        }

    }


    // MODIFIER
    @Override
    public void modifierVideo(CoachingVideo video) {

        String sql =
                "UPDATE coaching_video SET " +
                        "titre=?, description=?, url=?, niveau=?, premium=?, playlist_id=?, duration=? " +
                        "WHERE id=?";

        try (
                Connection cnx = getConnection();
                PreparedStatement ps = cnx.prepareStatement(sql)
        ) {

            ps.setString(1, video.getTitre());
            ps.setString(2, video.getDescription());
            ps.setString(3, video.getUrl());
            ps.setString(4, video.getNiveau());
            ps.setBoolean(5, video.isPremium());

            if(video.getPlaylist() != null)
                ps.setInt(6, video.getPlaylist().getId());
            else
                ps.setNull(6, Types.INTEGER);

            ps.setInt(7, video.getDuration());

            ps.setInt(8, video.getId());

            ps.executeUpdate();

            System.out.println("Vidéo modifiée");

        }
        catch (SQLException e){

            System.out.println(e.getMessage());

        }

    }


    // SUPPRIMER
    @Override
    public void supprimerVideo(int id){

        String sql =
                "DELETE FROM coaching_video WHERE id=?";

        try (
                Connection cnx = getConnection();
                PreparedStatement ps = cnx.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            ps.executeUpdate();

            System.out.println("Vidéo supprimée");

        }
        catch (SQLException e){

            System.out.println(e.getMessage());

        }

    }


    // AFFICHER
    @Override
    public List<CoachingVideo> afficherVideos(){

        List<CoachingVideo> liste =
                new ArrayList<>();


        String sql =
                "SELECT cv.*, " +
                        "p.id AS p_id, p.title AS p_title, p.description AS p_desc, " +
                        "p.niveau AS p_niveau, p.categorie AS p_categorie, " +
                        "p.image AS p_image, p.created_at AS p_created_at " +
                        "FROM coaching_video cv " +
                        "LEFT JOIN playlist p ON cv.playlist_id = p.id";


        try (

                Connection cnx = getConnection();

                Statement st = cnx.createStatement();

                ResultSet rs = st.executeQuery(sql)

        ){

            while(rs.next()){

                Playlist playlist = null;

                int pId =
                        rs.getInt("p_id");


                if(!rs.wasNull() && pId > 0){

                    playlist = new Playlist();

                    playlist.setId(pId);

                    playlist.setTitle(
                            rs.getString("p_title")
                    );

                    playlist.setDescription(
                            rs.getString("p_desc")
                    );

                    playlist.setNiveau(
                            rs.getString("p_niveau")
                    );

                    playlist.setCategorie(
                            rs.getString("p_categorie")
                    );

                    playlist.setImage(
                            rs.getString("p_image")
                    );


                    Timestamp ts =
                            rs.getTimestamp("p_created_at");


                    if(ts != null)

                        playlist.setCreatedAt(
                                ts.toLocalDateTime()
                        );

                }


                CoachingVideo v =
                        new CoachingVideo();


                v.setId(
                        rs.getInt("id")
                );

                v.setTitre(
                        rs.getString("titre")
                );

                v.setDescription(
                        rs.getString("description")
                );

                v.setUrl(
                        rs.getString("url")
                );

                v.setNiveau(
                        rs.getString("niveau")
                );

                v.setPremium(
                        rs.getBoolean("premium")
                );

                v.setDuration(
                        rs.getInt("duration")
                );

                v.setPlaylist(
                        playlist
                );


                liste.add(v);

            }

        }
        catch (SQLException e){

            System.out.println(e.getMessage());

        }


        return liste;

    }


    // PAR PLAYLIST
    public List<CoachingVideo> afficherVideosByPlaylist(int playlistId){

        List<CoachingVideo> liste =
                new ArrayList<>();


        String sql =
                "SELECT cv.*, " +
                        "p.id AS p_id, p.title AS p_title " +
                        "FROM coaching_video cv " +
                        "LEFT JOIN playlist p ON cv.playlist_id = p.id " +
                        "WHERE cv.playlist_id=?";


        try (

                Connection cnx = getConnection();

                PreparedStatement ps = cnx.prepareStatement(sql)

        ){

            ps.setInt(1, playlistId);

            ResultSet rs =
                    ps.executeQuery();


            while(rs.next()){

                Playlist playlist = null;

                int pId =
                        rs.getInt("p_id");


                if(!rs.wasNull() && pId > 0){

                    playlist = new Playlist();

                    playlist.setId(pId);

                    playlist.setTitle(
                            rs.getString("p_title")
                    );

                }


                CoachingVideo v =
                        new CoachingVideo();


                v.setId(
                        rs.getInt("id")
                );

                v.setTitre(
                        rs.getString("titre")
                );

                v.setDescription(
                        rs.getString("description")
                );

                v.setUrl(
                        rs.getString("url")
                );

                v.setNiveau(
                        rs.getString("niveau")
                );

                v.setPremium(
                        rs.getBoolean("premium")
                );

                v.setDuration(
                        rs.getInt("duration")
                );

                v.setPlaylist(
                        playlist
                );


                liste.add(v);

            }

        }
        catch (SQLException e){

            System.out.println(e.getMessage());

        }


        return liste;

    }

}