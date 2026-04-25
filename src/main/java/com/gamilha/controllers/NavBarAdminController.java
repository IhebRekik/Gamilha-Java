package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.User;
import com.gamilha.utils.NavigationContext;
import com.gamilha.utils.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * NavBarAdminController — sidebar admin style Gamilha.
 *
 * Sidebar 245px sombre avec sections :
 *   PRINCIPAL    : Dashboard, Utilisateurs
 *   COMPÉTITIONS : Événements, Matchs, Brackets, Équipes
 *   CONTENU      : Coaching, Vidéos, Social (+ sous-items Posts/Commentaires), Streams
 *   FINANCE      : Donations, Abonnements, Inscriptions, Historique
 *   Déconnexion
 */
public class NavBarAdminController implements Initializable {

    private static final String BASE = "/com/gamilha/interfaces/Admin/";

    // Styles CSS (définis dans style.css)
    private static final String BTN_NORMAL   = "sidebar-btn";
    private static final String BTN_ACTIVE   = "sidebar-btn-active";
    private static final String SUB_NORMAL   = "sidebar-sub-btn";
    private static final String SUB_ACTIVE   = "sidebar-sub-btn-active";

    @FXML private BorderPane contentArea;
    @FXML private Label      welcomeLabel;

    // Boutons principaux
    @FXML public  Button btnPlayList;
    @FXML private Button btnDashboard;
    @FXML private Button btnUtilisateurs;
    @FXML private Button btnEvenements;
    @FXML private Button btnMatchs;
    @FXML private Button btnBrackets;
    @FXML private Button btnEquipes;
    @FXML private Button btnCoaching;
    @FXML private Button btnSocial;
    @FXML private Button btnStreams;
    @FXML private Button btnDonations;
    @FXML private Button btnAbonnements;
    @FXML private Button btnInscriptions;
    @FXML private Button btnHistorique;

    // Sous-items Social
    @FXML private HBox   subSocial;
    @FXML private Button btnAdminPosts;
    @FXML private Button btnAdminCommentaires;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionContext.getCurrentUser();
        if (user != null && welcomeLabel != null)
            welcomeLabel.setText(user.getName());

        NavigationContext.setContentArea(contentArea);

        // Masquer sous-menu social au départ
        showSubSocial(false);

        // Page par défaut : Posts
        setActive(btnDashboard);
        showSubSocial(true);
        setSubActive(btnAdminPosts);
        loadPage("Dashboard.fxml");
    }

    // ── Chargement ────────────────────────────────────────────────────
    void loadPage(String page) {
        NavigationContext.setContentArea(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(BASE + page));
            Parent root = loader.load();
            contentArea.setCenter(root);
        } catch (IOException e) {
            System.err.println("Admin nav error: " + e.getMessage());
        }
    }

    public <T> T chargerPage(String page) {
        NavigationContext.setContentArea(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(BASE + page));
            Parent root = loader.load();
            contentArea.setCenter(root);
            return loader.getController();
        } catch (IOException e) {
            System.err.println("Admin nav error: " + e.getMessage());
            return null;
        }
    }

    // ── Gestion des styles actifs ─────────────────────────────────────
    private void setActive(Button target) {
        Button[] all = {btnDashboard, btnUtilisateurs, btnEvenements, btnMatchs,
                        btnBrackets, btnEquipes, btnCoaching, btnPlayList,
                        btnSocial, btnStreams, btnDonations, btnAbonnements,
                        btnInscriptions, btnHistorique};
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().removeAll(BTN_NORMAL, BTN_ACTIVE);
            b.getStyleClass().add(BTN_NORMAL);
        }
        if (target != null) {
            target.getStyleClass().removeAll(BTN_NORMAL, BTN_ACTIVE);
            target.getStyleClass().add(BTN_ACTIVE);
        }
        // Masquer sous-menu sauf si Social actif
        if (target != btnSocial) showSubSocial(false);
    }

    private void setSubActive(Button target) {
        Button[] subs = {btnAdminPosts, btnAdminCommentaires};
        for (Button b : subs) {
            if (b == null) continue;
            b.getStyleClass().removeAll(SUB_NORMAL, SUB_ACTIVE);
            b.getStyleClass().add(SUB_NORMAL);
        }
        if (target != null) {
            target.getStyleClass().removeAll(SUB_NORMAL, SUB_ACTIVE);
            target.getStyleClass().add(SUB_ACTIVE);
        }
    }

    private void showSubSocial(boolean show) {
        if (subSocial != null) {
            subSocial.setVisible(show);
            subSocial.setManaged(show);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────
    @FXML void goDashboard()    { setActive(btnDashboard);    loadPage("Dashboard.fxml"); }
    @FXML void goUtilisateurs() { setActive(btnUtilisateurs); loadPage("admin_users.fxml"); }
    @FXML void goCoaching()     { setActive(btnCoaching);     loadPage("VideoList.fxml"); }
    @FXML void goPlayList()     { setActive(btnPlayList);     loadPage("PlaylistList.fxml"); }
    @FXML void goStreams()      { setActive(btnStreams);       loadPage("AdminStreamList.fxml"); }
    @FXML void goAbonnement()   { setActive(btnAbonnements);  loadPage("Abonnement.fxml"); }
    @FXML void goHistorique()   { setActive(btnHistorique);   loadPage("HistoriquePaiement.fxml"); }

    @FXML void goDonations()    { setActive(btnDonations);    loadPage("AdminDonationStreams.fxml"); }

    @FXML void goInscriptions(ActionEvent e) {
        setActive(btnInscriptions);
        loadPage("InscriptionAdmin.fxml");
    }

    @FXML void goSocial() {
        setActive(btnSocial);
        showSubSocial(true);
        setSubActive(btnAdminPosts);
        loadPage("AdminPostView.fxml");
    }

    @FXML void goAdminPosts() {
        setActive(btnSocial);
        showSubSocial(true);
        setSubActive(btnAdminPosts);
        loadPage("AdminPostView.fxml");
    }

    @FXML void goAdminCommentaires() {
        setActive(btnSocial);
        showSubSocial(true);
        setSubActive(btnAdminCommentaires);
        loadPage("AdminCommentaireView.fxml");
    }

    @FXML void goEvenements() {
        setActive(btnEvenements);
        openAdminDashboard("evenements_list");
    }

    @FXML void goEquipe() {
        setActive(btnEquipes);
        openAdminDashboard("equipes_list");
    }

    @FXML void goMatchs() {
        setActive(btnMatchs);
        openAdminDashboard("matchs_list");
    }

    @FXML void goBrackets() {
        setActive(btnBrackets);
        openAdminDashboard("brackets_list");
    }

    @FXML void logout(ActionEvent event) {
        NavigationContext.clear();
        SessionContext.clear();
        MainApp.showLogin();
    }

    // ── Dashboard Événements/Équipes ──────────────────────────────────
    private void openAdminDashboard(String pageKey) {
        NavigationContext.setContentArea(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/dashboard-view.fxml"));
            Parent root = loader.load();
            DashboardController dash = loader.getController();

            dash.getSideNav().setVisible(false);
            dash.getSideNav().setManaged(false);

            dash.getTopNav().setVisible(false);
            dash.getTopNav().setManaged(false);

            if (dash.getHeaderBox() != null) {
                dash.getHeaderBox().setVisible(false);
                dash.getHeaderBox().setManaged(false);
            }

            dash.getTitleLabel().setVisible(false);
            dash.getSubtitleLabel().setVisible(false);

            contentArea.setCenter(root);
            dash.navigateTo(pageKey);
        } catch (Exception e) {
            System.err.println("Dashboard error: " + e.getMessage());
        }
    }
}
