package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.User;
import com.gamilha.services.AbonnementServices;
import com.gamilha.utils.NavigationContext;
import com.gamilha.utils.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * NavBarUserController — identique à base_front.html.twig Symfony.
 *
 * Navbar horizontale :
 *   🎮 Gamilha | Accueil | Événements | Équipes | Coaching | Gamilha AI
 *              | Amis | Réseaux | Streams | Abonnements        👤 user ▾
 *
 * Sous-menu Réseaux : Fil d'actualité | Mes Posts | Mes Commentaires | Mes Amis
 * Dropdown user    : Mon profil / Déconnexion
 */
public class NavBarUserController implements Initializable {

    private static final String BASE_USER  = "/com/gamilha/interfaces/User/";
    private static final String BASE_ADMIN = "/com/gamilha/interfaces/Admin/";

    // Styles CSS classes (définis dans style.css)
    private static final String NORMAL = "nav-link-btn";
    private static final String ACTIVE = "nav-link-btn-active";
    private static final String SUB_NORMAL = "nav-sub-btn";
    private static final String SUB_ACTIVE = "nav-sub-btn-active";

    @FXML private BorderPane contentArea;
    @FXML private ImageView  logoImage;
    @FXML private MenuButton menuUser;

    // Liens principaux — ordre identique Symfony
    @FXML private Button btnAccueil;
    @FXML private Button btnEvenements;
    @FXML private Button btnEquipes;
    @FXML private Button btnCoaching;
    @FXML private Button btnAI;

    @FXML private Button btnReseaux;
    @FXML private Button btnStreams;
    @FXML private Button btnAbonnements;

    // Sous-menu Réseaux
    @FXML private HBox   subMenuSocial;
    @FXML private Button btnFilActualite;
    @FXML private Button btnMesPosts;
    @FXML private Button btnMesCommentaires;
    @FXML private Button btnListAmis;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        NavigationContext.setContentArea(contentArea);

        User user = SessionContext.getCurrentUser();
        if (user != null && menuUser != null) {
            menuUser.setText("👤  " + user.getName());
        }

        // Masquer sous-menu au départ
        if (subMenuSocial != null) {
            subMenuSocial.setVisible(false);
            subMenuSocial.setManaged(false);
        }

        try {
            Image img = new Image(
                    getClass().getResource("/com/gamilha/images/logo.png").toExternalForm());
            logoImage.setImage(img);
        } catch (Exception ignored) {}

        // Page par défaut : Réseaux (identique Symfony qui charge le fil d'actualité)
        setActive(btnAccueil);
        goAccueil();
    }

    // ── Chargement dans contentArea ──────────────────────────────────
    private void load(String fxml) {
        NavigationContext.setContentArea(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            injectUser(loader.getController());
            contentArea.setCenter(root);
        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private <T> T loadWithController(String fxml) {
        NavigationContext.setContentArea(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            injectUser(loader.getController());
            contentArea.setCenter(root);
            return loader.getController();
        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
            return null;
        }
    }

    private void injectUser(Object ctrl) {
        User u = SessionContext.getCurrentUser();
        if (u == null) return;
        if (ctrl instanceof UserPostController c)            c.setCurrentUser(u);
        if (ctrl instanceof UserMesPostsController c)        c.setCurrentUser(u);
        if (ctrl instanceof UserMesCommentairesController c) c.setCurrentUser(u);
        if (ctrl instanceof UserAmisController c)            c.setCurrentUser(u);
    }

    // ── Gestion des styles actifs ─────────────────────────────────────
    /** Active un bouton principal, désactive tous les autres */
    private void setActive(Button target) {
        Button[] all = {btnAccueil, btnEvenements, btnEquipes, btnCoaching,
                        btnAI,  btnReseaux, btnStreams, btnAbonnements};
        for (Button b : all) {
            if (b == null) continue;
            b.getStyleClass().removeAll(NORMAL, ACTIVE);
            b.getStyleClass().add(NORMAL);
        }
        if (target != null) {
            target.getStyleClass().removeAll(NORMAL, ACTIVE);
            target.getStyleClass().add(ACTIVE);
        }
        // Cacher sous-menu sauf si Réseaux actif
        boolean showSub = (target == btnReseaux);
        if (subMenuSocial != null) {
            subMenuSocial.setVisible(showSub);
            subMenuSocial.setManaged(showSub);
        }
    }

    /** Active un bouton du sous-menu Réseaux */
    private void setSubActive(Button target) {
        // Garder Réseaux actif dans la barre principale
        setActive(btnReseaux);
        Button[] subs = {btnFilActualite, btnMesPosts, btnMesCommentaires, btnListAmis};
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
    private boolean hasAccess(String feature) {


        return AbonnementServices.getAbonementActiveUser()
                .stream()
                .anyMatch(a ->
                        a.getOptions().stream()
                                .anyMatch(opt -> opt.contains(feature))
                );
    }
    // ── Navigation principale ─────────────────────────────────────────
    @FXML void goAccueil()   { setActive(btnAccueil);    load(BASE_USER + "UserHomePage.fxml"); }
    @FXML
    void goCoaching() {

        if (!hasAccess("coaching")) {
            goAbonnement();
            return;
        }

        setActive(btnCoaching);
        load(BASE_USER + "PlaylistList.fxml");
    }    @FXML void goAbonnement(){ setActive(btnAbonnements);load(BASE_USER + "Abonnement.fxml"); }


    @FXML
    void goStreams() {



        setActive(btnStreams);
        NavigationContext.setContentArea(contentArea);
        load(BASE_USER + "StreamList.fxml");
    }

    @FXML
    void goAI() {

        if (!hasAccess("ai")) {
            goAbonnement();
            return;
        }

        setActive(btnAI);
        load(BASE_USER + "ChatAiView.fxml");
    }



    @FXML void goReseaux() {
        setSubActive(btnFilActualite);
        User u = SessionContext.getCurrentUser();
        if (u != null && u.isAdmin()) {
            load(BASE_ADMIN + "AdminPostView.fxml");
        } else {
            load(BASE_USER + "UserPostView.fxml");
        }
    }

   @FXML
    void goEvenements() {

        setActive(btnEvenements);
        openDashboard("evenements_list");
    }

    @FXML void goEquipe() {
        setActive(btnEquipes);
        openDashboard("equipes_list");
    }

    // Sous-menu Réseaux
    @FXML void goFilActualite()    { setSubActive(btnFilActualite);    load(BASE_USER + "UserPostView.fxml"); }
    @FXML void goMesPosts()        { setSubActive(btnMesPosts);        load(BASE_USER + "UserMesPostsView.fxml"); }
    @FXML void goMesCommentaires() { setSubActive(btnMesCommentaires); load(BASE_USER + "UserMesCommentairesView.fxml"); }
    @FXML void goListAmis()        { setSubActive(btnListAmis);        load(BASE_USER + "UserAmisView.fxml"); }

    // ── Dropdown user (MenuButton) ────────────────────────────────────
    @FXML void goProfile() {
        NavigationContext.setContentArea(contentArea);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(BASE_USER + "user_profile.fxml"));
            Parent root = loader.load();
            UserProfileController ctrl = loader.getController();
            ctrl.setUser(SessionContext.getCurrentUser());
            contentArea.setCenter(root);
        } catch (Exception e) {
            System.err.println("Profile error: " + e.getMessage());
        }
    }

    @FXML void logout(ActionEvent e) {
        NavigationContext.clear();
        SessionContext.clear();
        MainApp.showLogin();
    }

    // ── Dashboard (Événements/Équipes) ────────────────────────────────
    private void openDashboard(String pageKey) {
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
