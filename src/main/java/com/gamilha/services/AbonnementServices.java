package com.gamilha.services;

import com.gamilha.utils.DatabaseConnection;
import com.gamilha.entity.Abonnement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AbonnementServices {

    private static DatabaseConnection con;

    public AbonnementServices() {
    }

    public List<Abonnement> getAbonnements() {

        List<Abonnement> abonnements = new ArrayList<>();

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "SELECT * FROM abonnement";
            PreparedStatement ps = conn.prepareStatement(query);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Abonnement a = new Abonnement();

                a.setId(rs.getInt("id"));
                a.setType(rs.getString("type"));
                a.setPrix(rs.getFloat("prix"));
                a.setDuree(rs.getInt("duree"));

                String avantagesJson = rs.getString("avantages");

                List<String> avantages = new ArrayList<>();

                if (avantagesJson != null && !avantagesJson.isEmpty()) {

                    avantagesJson = avantagesJson.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "");

                    String[] arr = avantagesJson.split(",");

                    for (String s : arr) {
                        avantages.add(s.trim());
                    }
                }

                a.setAvantages(avantages);

                String optionsJson = rs.getString("options");

                List<String> options = new ArrayList<>();

                if (optionsJson != null && !optionsJson.isEmpty()) {

                    optionsJson = optionsJson.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "");

                    String[] op = optionsJson.split(",");

                    for (String s : op) {
                        options.add(s.trim());
                    }
                }

                a.setOptions(options);

                abonnements.add(a);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return abonnements;
    }


    public void addAbonnement(Abonnement a) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "INSERT INTO abonnement(type, prix, avantages, duree, options) VALUES (?, ?, ?, ?, ?)";

            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, a.getType());
            ps.setFloat(2, a.getPrix());
            ps.setString(3, listToJson(a.getAvantages()));
            ps.setInt(4, a.getDuree());
            ps.setString(5, listToJson(a.getOptions()));

            ps.executeUpdate();

            System.out.println("Abonnement ajouté");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void updateAbonnement(Abonnement a) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "UPDATE abonnement SET type=?, prix=?, avantages=?, duree=?, options=? WHERE id=?";

            PreparedStatement ps = conn.prepareStatement(query);

            ps.setString(1, a.getType());
            ps.setFloat(2, a.getPrix());
            ps.setString(3, listToJson(a.getAvantages()));
            ps.setInt(4, a.getDuree());
            ps.setString(5, listToJson(a.getOptions()));
            ps.setInt(6, a.getId());

            ps.executeUpdate();

            System.out.println("Abonnement modifié");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void deleteAbonnement(int id) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "DELETE FROM abonnement WHERE id=?";

            PreparedStatement ps = conn.prepareStatement(query);

            ps.setInt(1, id);

            ps.executeUpdate();

            System.out.println("Abonnement supprimé");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String listToJson(List<String> list) {

        if (list == null || list.isEmpty())
            return "[]";

        StringBuilder json = new StringBuilder("[");

        for (int i = 0; i < list.size(); i++) {

            json.append("\"")
                    .append(list.get(i))
                    .append("\"");

            if (i < list.size() - 1)
                json.append(",");
        }

        json.append("]");

        return json.toString();
    }
    public static Abonnement getAbonnementById(int id) {

        Abonnement a = new Abonnement();

        try {
            Connection conn = DatabaseConnection.getConnection();

            String query = "SELECT * FROM abonnement where id=?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, id);


            ResultSet rs = ps.executeQuery();

            while (rs.next()) {



                a.setId(rs.getInt("id"));
                a.setType(rs.getString("type"));
                a.setPrix(rs.getFloat("prix"));
                a.setDuree(rs.getInt("duree"));

                String avantagesJson = rs.getString("avantages");

                List<String> avantages = new ArrayList<>();

                if (avantagesJson != null && !avantagesJson.isEmpty()) {

                    avantagesJson = avantagesJson.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "");

                    String[] arr = avantagesJson.split(",");

                    for (String s : arr) {
                        avantages.add(s.trim());
                    }
                }

                a.setAvantages(avantages);

                String optionsJson = rs.getString("options");

                List<String> options = new ArrayList<>();

                if (optionsJson != null && !optionsJson.isEmpty()) {

                    optionsJson = optionsJson.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "");

                    String[] op = optionsJson.split(",");

                    for (String s : op) {
                        options.add(s.trim());
                    }
                }




            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return a;
    }
}