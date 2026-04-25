package com.gamilha.services;

import com.gamilha.utils.ConnectionManager;
import com.gamilha.entity.Abonnement;
import com.gamilha.entity.Inscription;
import com.gamilha.entity.User;
import com.gamilha.utils.SessionContext;
import com.gamilha.utils.StripeAutoHandler;
import com.gamilha.utils.StripeWindow;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.awt.*;
import java.net.URI;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InscriptionServices {

    private Connection con;

    public InscriptionServices() {
        con = ConnectionManager.getConnection();
    }

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
    private static final String STRIPE_KEY =
            "sk_test_51T3ru9I6m3BSEEF9jEJMzZxCAP5P8FHdFKmxx6TQzM5mQRvKpy5nFTY4DdAjNP3pkb5PzJj1a7aAbVDXLKXRn23200bcX15iIw";

    public String createCheckoutSession(int abonnementId) {

        try {

            Stripe.apiKey = STRIPE_KEY;

            User user = SessionContext.getCurrentUser();

            if(user == null){
                throw new Exception("Utilisateur non connecté");
            }

            Abonnement abonnement = getAbonnementById(abonnementId);

            if(abonnement == null){
                throw new Exception("Abonnement non trouvé");
            }

            if(isAlreadySubscribed(user.getId(), abonnementId)){
                throw new Exception("Déjà abonné");
            }

            SessionCreateParams params =
                    SessionCreateParams.builder()

                            .addPaymentMethodType(
                                    SessionCreateParams.PaymentMethodType.CARD
                            )

                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency("eur")
                                                            .setUnitAmount(
                                                                    (long)((abonnement.getPrix()/3.2)*100)
                                                            )
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                            .setName(abonnement.getType())
                                                                            .build()
                                                            )
                                                            .build()
                                            )
                                            .build()
                            )

                            .putMetadata("user_id", String.valueOf(user.getId()))
                            .putMetadata("abonnement_id", String.valueOf(abonnementId))

                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl("https://example.com")
                            .setCancelUrl("https://example.com")

                            .build();

            Session session = Session.create(params);

            // ❌ SUPPRIMER Chrome
            // Desktop.getDesktop().browse(new URI(session.getUrl()));

            // ✅ OUVRIR DANS TON APP
            javafx.application.Platform.runLater(() -> {
                StripeWindow.open(
                        session.getUrl(),
                        session.getId()
                );
            });

            return session.getUrl();

        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    public void paymentSuccess(String sessionId){

        try {

            Stripe.apiKey = STRIPE_KEY;

            Session session = Session.retrieve(sessionId);

            int userId = Integer.parseInt(
                    session.getMetadata().get("user_id")
            );

            int abonnementId = Integer.parseInt(
                    session.getMetadata().get("abonnement_id")
            );

            Abonnement abonnement = getAbonnementById(abonnementId);

            String sqlUA =
                    "INSERT INTO user_abonnement(user_id, abonnement_id, date_debut, date_fin) VALUES (?,?,?,?)";

            PreparedStatement psUA = con.prepareStatement(sqlUA);

            LocalDateTime debut = LocalDateTime.now();

            LocalDateTime fin =
                    debut.plusMonths(abonnement.getDuree());

            psUA.setInt(1, userId);
            psUA.setInt(2, abonnementId);
            psUA.setTimestamp(3, Timestamp.valueOf(debut));
            psUA.setTimestamp(4, Timestamp.valueOf(fin));

            psUA.executeUpdate();


            String sqlPaiement =
                    "INSERT INTO historique_paiement(user_id, abonnement_id, montant, created_at) VALUES (?,?,?,?)";

            PreparedStatement psP = con.prepareStatement(sqlPaiement);

            psP.setInt(1, userId);
            psP.setInt(2, abonnementId);

            psP.setString(
                    3,
                    String.valueOf(abonnement.getPrix()*100)
            );

            psP.setTimestamp(
                    4,
                    Timestamp.valueOf(LocalDateTime.now())
            );

            psP.executeUpdate();

        } catch (Exception e){

            throw new RuntimeException(e.getMessage());

        }

    }

    private boolean isAlreadySubscribed(int userId, int abonnementId) {

        String sql =
                "SELECT id FROM user_abonnement WHERE user_id=? AND abonnement_id=?";

        try {

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, userId);
            ps.setInt(2, abonnementId);

            ResultSet rs = ps.executeQuery();

            return rs.next();

        } catch (SQLException e){
            throw new RuntimeException(e.getMessage());
        }

    }


    private Abonnement getAbonnementById(int id){

        String sql =
                "SELECT * FROM abonnement WHERE id=?";

        try {

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){

                Abonnement a = new Abonnement();

                a.setId(rs.getInt("id"));
                a.setType(rs.getString("type"));
                a.setPrix(rs.getFloat("prix"));
                a.setDuree(rs.getInt("duree"));

                return a;
            }

        } catch (SQLException e){
            throw new RuntimeException(e.getMessage());
        }

        return null;
    }




}