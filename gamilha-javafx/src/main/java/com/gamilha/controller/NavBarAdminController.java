package com.gamilha.controller;

import com.gamilha.model.User;
import com.gamilha.service.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * NavBar sidebar Admin.
 * Social → Posts en cards (tous les posts) ou Commentaires en cards.
 * Base path : /com/gamilha/interfaces/Admin/
 */
public class NavBarAdminController implements Initializable {

    private static final String BASE = "/com/gamilha/interfaces/Admin/";

    @FXML private BorderPane contentArea;
    @FXML private Label      welcomeLabel;

    @FXML private Button btnDashboard;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnEvenements;
    @FXML private Button btnMatchs;
    @FXML private Button btnBrackets;
    @FXML private Button btnEquipes;
    @FXML private Button btnCoaching;
    @FXML private Button btnSocial;
    @FXML private Button btnAdminPosts;
    @FXML private Button btnAdminCommentaires;
    @FXML private Button btnStreams;
    @FXML private Button btnDonations;
    @FXML private Button btnAbonnements;
    @FXML private Button btnInscriptions;
    @FXML private Button btnHistorique;

    private Button activeBtn;

    private static final String NORMAL =
        "-fx-text-fill:white;-fx-background-color:transparent;" +
        "-fx-font-size:12;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;-fx-padding:7 12;";
    private static final String ACTIVE =
        "-fx-text-fill:#c84cff;-fx-background-color:#1a0a2e;" +
        "-fx-font-size:12;-fx-font-weight:bold;-fx-cursor:hand;" +
        "-fx-alignment:CENTER_LEFT;-fx-padding:7 12;-fx-background-radius:8;";
    private static final String SUB_NORMAL =
        "-fx-text-fill:#9ca3af;-fx-background-color:transparent;" +
        "-fx-font-size:11;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;-fx-padding:5 10;";
    private static final String SUB_ACTIVE =
        "-fx-text-fill:#c84cff;-fx-background-color:#12082a;" +
        "-fx-font-size:11;-fx-font-weight:bold;-fx-cursor:hand;" +
        "-fx-alignment:CENTER_LEFT;-fx-padding:5 10;-fx-background-radius:6;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionContext.getCurrentUser();
        if (user != null && welcomeLabel != null)
            welcomeLabel.setText("Bonjour " + user.getName());

        // Page par défaut : Posts en cards
        setActive(btnSocial);
        setSub(btnAdminPosts);
        loadPage("AdminPostView.fxml");
    }

    // ── Chargement ────────────────────────────────────────────────────────
    void loadPage(String page) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(BASE + page));
            Parent root = loader.load();
            contentArea.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Impossible de charger : " + BASE + page);
        }
    }

    private void setActive(Button btn) {
        Button[] all = {btnDashboard, btnUtilisateurs, btnEvenements,
                        btnMatchs, btnBrackets, btnEquipes, btnCoaching,
                        btnSocial, btnStreams, btnDonations, btnAbonnements,
                        btnInscriptions, btnHistorique};
        for (Button b : all) if (b != null) b.setStyle(NORMAL);
        // Garder Social violet quand un sous-item est actif
        if (btn != null) { btn.setStyle(ACTIVE); activeBtn = btn; }
        btnSocial.setStyle("-fx-text-fill:#c84cff;-fx-background-color:#1a0a2e;" +
                           "-fx-font-size:12;-fx-font-weight:bold;-fx-cursor:hand;" +
                           "-fx-alignment:CENTER_LEFT;-fx-padding:7 12;-fx-background-radius:8;");
        resetSub();
    }

    private void resetSub() {
        if (btnAdminPosts != null)         btnAdminPosts.setStyle(SUB_NORMAL);
        if (btnAdminCommentaires != null)  btnAdminCommentaires.setStyle(SUB_NORMAL);
    }

    private void setSub(Button btn) {
        resetSub();
        if (btn != null) btn.setStyle(SUB_ACTIVE);
    }

    // ── Handlers ──────────────────────────────────────────────────────────
    @FXML void goDashboard()    { setActive(btnDashboard);    loadPage("Dashboard.fxml"); }
    @FXML void goUtilisateurs() { setActive(btnUtilisateurs); loadPage("UserAdmin.fxml"); }
    @FXML void goEvenements()   { setActive(btnEvenements);   loadPage("EvenementAdmin.fxml"); }
    @FXML void goMatchs()       { setActive(btnMatchs);       loadPage("GameMatch.fxml"); }
    @FXML void goBrackets()     { setActive(btnBrackets);     loadPage("Bracket.fxml"); }
    @FXML void goEquipes()      { setActive(btnEquipes);      loadPage("EquipeAdmin.fxml"); }
    @FXML void goCoaching()     { setActive(btnCoaching);     loadPage("CoachingAdmin.fxml"); }
    @FXML void goStreams()      { setActive(btnStreams);       loadPage("StreamAdmin.fxml"); }
    @FXML void goDonations()    { setActive(btnDonations);    loadPage("DonationAdmin.fxml"); }
    @FXML void goAbonnement()   { setActive(btnAbonnements);  loadPage("Abonnement.fxml"); }
    @FXML void goHistorique()   { setActive(btnHistorique);   loadPage("HistoriquePaiement.fxml"); }

    @FXML void goInscriptions(ActionEvent e) {
        setActive(btnInscriptions);
        loadPage("InscriptionAdmin.fxml");
    }

    /** 🌐 Social → charge directement les Posts */
    @FXML void goSocial() {
        setActive(btnSocial);
        setSub(btnAdminPosts);
        loadPage("AdminPostView.fxml");
    }

    /** 📝 Posts (tous les posts — cards avec image + media) */
    @FXML void goAdminPosts() {
        setActive(btnSocial);
        setSub(btnAdminPosts);
        loadPage("AdminPostView.fxml");
    }

    /** 💬 Commentaires (tous les commentaires — cards) */
    @FXML void goAdminCommentaires() {
        setActive(btnSocial);
        setSub(btnAdminCommentaires);
        loadPage("AdminCommentaireView.fxml");
    }

    // ── Déconnexion ───────────────────────────────────────────────────────
    @FXML public void logout(ActionEvent event) {
        try {
            SessionContext.clear();
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/interfaces/login-view.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Connexion");
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
