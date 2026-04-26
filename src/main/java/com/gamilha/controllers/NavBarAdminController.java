package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.User;
import com.gamilha.utils.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class NavBarAdminController implements Initializable {

    private static final String BASE = "/com/gamilha/interfaces/Admin/";

    @FXML private BorderPane contentArea;
    @FXML private Label      welcomeLabel;

    @FXML private Button btnDashboard, btnUtilisateurs;
    @FXML private Button btnEvenements, btnMatchs, btnBrackets, btnEquipes;
    @FXML private Button btnPlayList, btnCoaching, btnSocial;
    @FXML private Button btnAdminPosts, btnAdminCommentaires;
    @FXML private Button btnStreams;
    @FXML private Button btnDonations, btnAbonnements, btnInscriptions, btnHistorique;
    @FXML private Button btnPrediction;
    @FXML private Button btnAnalytics;

    // Styles
    private static final String N = "-fx-text-fill:#9ca3af;-fx-background-color:transparent;-fx-font-size:13px;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;-fx-padding:8 14;-fx-background-radius:8;";
    private static final String A = "-fx-text-fill:#f9fafb;-fx-background-color:#1f2937;-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;-fx-padding:8 14;-fx-background-radius:8;-fx-border-color:#374151;-fx-border-radius:8;";
    private static final String P = "-fx-text-fill:#c4b5fd;-fx-background-color:#2e1065;-fx-font-size:13px;-fx-font-weight:bold;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;-fx-padding:8 14;-fx-background-radius:8;";
    private static final String SN = "-fx-text-fill:#6b7280;-fx-background-color:transparent;-fx-font-size:12px;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;-fx-padding:5 10;-fx-background-radius:6;";
    private static final String SA = "-fx-text-fill:#c84cff;-fx-background-color:#1a0a2e;-fx-font-size:12px;-fx-font-weight:bold;-fx-cursor:hand;-fx-alignment:CENTER_LEFT;-fx-padding:5 10;-fx-background-radius:6;";

    private static final Button[] ALL_BUTTONS = null; // initialized lazily

    @FXML private BorderPane navBarRoot; // fx:id="navBarRoot" on the root BorderPane

    public void initialize(URL url, ResourceBundle rb) {
        // Stocker le contrôleur dans userData du noeud root
        // → AdminDonationListController peut le retrouver via scene.getRoot().getUserData()
        javafx.application.Platform.runLater(() -> {
            if (navBarRoot != null) navBarRoot.setUserData(this);
        });
        User user = SessionContext.getCurrentUser();
        if (user != null && welcomeLabel != null)
            welcomeLabel.setText(user.getName());
        // Page par défaut
        setActive(btnSocial);
        setSub(btnAdminPosts);
        loadPage("AdminPostView.fxml");
    }

    public void loadPage(String page) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(BASE + page));
            Parent root = loader.load();
            contentArea.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Impossible de charger : " + BASE + page);
        }
    }

    public <T> T chargerPage(String page) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(BASE + page));
            Parent root = loader.load();
            contentArea.setCenter(root);
            return loader.getController();
        } catch (IOException e) { e.printStackTrace(); return null; }
    }

    private void setActive(Button btn) {
        Button[] all = {btnDashboard, btnUtilisateurs, btnEvenements, btnMatchs,
                btnBrackets, btnEquipes, btnPlayList, btnCoaching,
                btnSocial, btnStreams, btnDonations, btnAbonnements,
                btnInscriptions, btnHistorique};
        for (Button b : all) if (b != null) b.setStyle(N);
        if (btnPrediction != null) btnPrediction.setStyle(P.replace("#c4b5fd", "#9ca3af").replace("#2e1065", "transparent"));
        if (btn != null) btn.setStyle(btn == btnPrediction ? P : A);
        resetSub();
    }

    private void resetSub() {
        if (btnAdminPosts != null)        btnAdminPosts.setStyle(SN);
        if (btnAdminCommentaires != null) btnAdminCommentaires.setStyle(SN);
    }
    private void setSub(Button btn) { resetSub(); if (btn != null) btn.setStyle(SA); }

    @FXML void goDashboard()    { setActive(btnDashboard);    loadPage("AdminPostView.fxml"); }
    @FXML void goUtilisateurs() { setActive(btnUtilisateurs); loadPage("admin_users.fxml"); }
    @FXML void goPlayList()     { setActive(btnPlayList);     loadPage("PlaylistList.fxml"); }
    @FXML void goCoaching()     { setActive(btnCoaching);     loadPage("VideoList.fxml"); }
    @FXML void goStreams()      { setActive(btnStreams);       loadPage("AdminStreamList.fxml"); }
    @FXML void goDonations()    { setActive(btnDonations);    loadPage("AdminDonationList.fxml"); }
    @FXML void goAbonnement()   { setActive(btnAbonnements);  loadPage("Abonnement.fxml"); }
    @FXML void goHistorique()   { setActive(btnHistorique);   loadPage("AdminPostView.fxml"); }
    @FXML void goSocial()       { setActive(btnSocial); setSub(btnAdminPosts); loadPage("AdminPostView.fxml"); }
    @FXML void goAdminPosts()   { setActive(btnSocial); setSub(btnAdminPosts); loadPage("AdminPostView.fxml"); }
    @FXML void goAdminCommentaires() { setActive(btnSocial); setSub(btnAdminCommentaires); loadPage("AdminCommentaireView.fxml"); }

    @FXML void goAnalytics() {
        if (btnAnalytics != null) btnAnalytics.setStyle(P);
        Button[] all = {btnDashboard, btnUtilisateurs, btnEvenements, btnMatchs,
                btnBrackets, btnEquipes, btnPlayList, btnCoaching,
                btnSocial, btnStreams, btnDonations, btnAbonnements,
                btnInscriptions, btnHistorique};
        for (Button b : all) if (b != null) b.setStyle(N);
        if (btnPrediction != null) btnPrediction.setStyle(N);
        resetSub();
        loadPage("AdminAnalytics.fxml");
    }

    @FXML void goPrediction() {
        if (btnPrediction != null) btnPrediction.setStyle(P);
        Button[] all = {btnDashboard, btnUtilisateurs, btnEvenements, btnMatchs,
                btnBrackets, btnEquipes, btnPlayList, btnCoaching,
                btnSocial, btnStreams, btnDonations, btnAbonnements,
                btnInscriptions, btnHistorique};
        for (Button b : all) if (b != null) b.setStyle(N);
        resetSub();
        loadPage("AdminStreamPrediction.fxml");
    }

    @FXML void goInscriptions(ActionEvent e) { setActive(btnInscriptions); loadPage("InscriptionAdmin.fxml"); }

    @FXML void goEvenements() { setActive(btnEvenements); openDash("evenements_list"); }
    @FXML void goEquipe()     { setActive(btnEquipes);    openDash("equipes_list"); }
    @FXML void goMatchs()     { setActive(btnMatchs);     openDash("matchs_list"); }
    @FXML void goBrackets()   { setActive(btnBrackets);   openDash("brackets_list"); }

    private void openDash(String key) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gamilha/dashboard-view.fxml"));
            Parent root = loader.load();
            DashboardController d = loader.getController();
            d.getSideNav().setVisible(false); d.getSideNav().setManaged(false);
            d.getTopNav().setVisible(false);  d.getTopNav().setManaged(false);
            d.getTitleLabel().setVisible(false); d.getSubtitleLabel().setVisible(false);
            contentArea.setCenter(root);
            d.navigateTo(key);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void logout(ActionEvent event) { SessionContext.clear(); MainApp.showLogin(); }
}
