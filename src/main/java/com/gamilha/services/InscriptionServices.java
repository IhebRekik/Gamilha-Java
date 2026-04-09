package com.gamilha.services;

import com.gamilha.utils.DatabaseConnection;
import com.gamilha.entity.Abonnement;
import com.gamilha.entity.Inscription;
import com.gamilha.entity.User;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InscriptionServices {

    private Connection con;

    public InscriptionServices() {
        con = DatabaseConnection.getConnection();
    }

    // CREATE
    public void ajouter(Inscription inscription) {
        String sql = "INSERT INTO user_abonnement (date_debut, date_fin, user_id,abonnement_id) VALUES (?, ?, ?,?)";
        String sqlAbonnement = "SELECT * FROM abonnement WHERE id=?";
        Abonnement abonnement = new Abonnement();
        try {

            PreparedStatement ps = con.prepareStatement(sqlAbonnement);

            ps.setInt(1, inscription.getAbonnements().getId()); // id abonnement recherché

            ResultSet rs = ps.executeQuery();

            if(rs.next()){



                abonnement.setId(rs.getInt("id"));
                abonnement.setType(rs.getString("type"));
                abonnement.setPrix(rs.getFloat("prix"));
                abonnement.setDuree(rs.getInt("duree"));


            }

        } catch (Exception e){

            System.out.println(e.getMessage());
        }

        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setDate(2, Date.valueOf(LocalDate.now().plusMonths(abonnement.getDuree())));
            ps.setInt(3, inscription.getUser().getId());
            ps.setInt(4,inscription.getAbonnements().getId());

            int rs = ps.executeUpdate();

            if (rs>0) {
                System.out.println("Inscription ajoutée");
            }else{
                System.out.println("Faild d'ajoutée inscription");

            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }



    // READ ALL
    public List<Inscription> afficher() {

        List<Inscription> list = new ArrayList<>();

        String sql = "SELECT * FROM user_abonnement";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {

                Inscription i = new Inscription();

                i.setId(rs.getInt("id"));
                i.setDateDebut(rs.getDate("date_debut").toLocalDate());
                i.setDateFin(rs.getDate("date_fin").toLocalDate());

                // user simple
                PreparedStatement ps =  con.prepareStatement("SELECT * FROM user where id=?");
                ps.setInt(1, rs.getInt("user_id"));
                ResultSet rs2 = ps.executeQuery();
                User user = new User();
                if (rs2.next()) {
                    user.setEmail(rs2.getString("email"));
                    user.setId(rs2.getInt("id"));
                }else{
                    throw new Exception("L'utilisateur n'existe pas");
                }
                i.setUser(user);

                // abonnements
                Abonnement abonnement = AbonnementServices.getAbonnementById(rs.getInt("abonnement_id"));

                i.setAbonnements(abonnement);

                list.add(i);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return list;
    }


    // UPDATE
    public void modifier(Inscription inscription) {

        String sql = "UPDATE user_abonnement SET date_debut=?, date_fin=?, user_id=? , abonnement_id=? WHERE id=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(inscription.getDateDebut()));
            ps.setDate(2, Date.valueOf(inscription.getDateFin()));
            ps.setInt(3, inscription.getUser().getId());
            ps.setInt(4, inscription.getAbonnements().getId());
            ps.setInt(5, inscription.getId());

            int res = ps.executeUpdate();

            if(res > 0) {
                System.out.println("Inscription modifiée");
            }else{
                System.out.println("Faild de modifiée inscription");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // DELETE
    public void supprimer(int id) {

        try {

            String sql = "DELETE FROM user_abonnement WHERE id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, id);

            ps.executeUpdate();

            System.out.println("Inscription supprimée");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public List<Integer> getUserAbonnementsActifs(User user) {

        List<Integer> list = new ArrayList<>();

        String sql =
                "SELECT abonnement_id FROM  user_abonnement  WHERE user_id = ? AND date_fin > CURRENT_DATE";

        try {

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, user.getId());

            ResultSet rs = ps.executeQuery();

            while(rs.next()){

                int a = rs.getInt("abonnement_id");


                list.add(a);
            }

        } catch (Exception e) {

            System.out.println(e.getMessage());
        }

        return list;
    }




}