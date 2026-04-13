package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.image.ImageView;
/**
 * NavBar horizontale commune User + Admin.
 *
 * Bouton "🌐 Réseaux" :
 *   ROLE_ADMIN → AdminPostView  (cards, gestion de tout)
 *   ROLE_USER  → UserPostView   (fil social + sidebar amis)
 *
 * Sous-menu Social (sous la topbar) — visible seulement ROLE_USER :
 *   🏠 Fil d'actualité  |  📝 Mes Posts  |  💬 Mes Commentaires  |  👤 Amis
 */
public class NavBarUserController implements Initializable {

    private static final String BASE_USER  = "/com/gamilha/interfaces/User/";
    private static final String BASE_ADMIN = "/com/gamilha/interfaces/Admin/";

    // ── FXML ────────────────────────────────────────────────────────────
    @FXML private BorderPane contentArea;
    @FXML private Label      welcomeLabel;
    @FXML private HBox       subMenuSocial;   // barre sous la topbar

    // Boutons menu principal
    @FXML private Button btnAccueil;
    @FXML private Button btnEvenements;
    @FXML private Button btnEquipes;
    @FXML private Button btnCoaching;
    @FXML private Button btnAI;
    @FXML private Button btnReseaux;
    @FXML private Button btnStreams;
    @FXML private Button btnAbonnements;

    // Boutons sous-menu Social
    @FXML private Button btnFilActualite;
    @FXML private Button btnMesPosts;
    @FXML private Button btnMesCommentaires;
    @FXML private Button btnAmis;
    @FXML
    private ImageView logoImage;
    // ── Styles ───────────────────────────────────────────────────────────
    private static final String MAIN_NORMAL =
        "-fx-background-color:transparent;-fx-text-fill:white;" +
        "-fx-font-size:13;-fx-cursor:hand;-fx-padding:6 10;";
    private static final String MAIN_ACTIVE =
        "-fx-background-color:transparent;-fx-text-fill:#c84cff;" +
        "-fx-font-size:13;-fx-font-weight:bold;-fx-cursor:hand;" +
        "-fx-border-color:#c84cff;-fx-border-width:0 0 2 0;-fx-padding:6 10;";
    private static final String SUB_NORMAL =
        "-fx-background-color:transparent;-fx-text-fill:#9ca3af;" +
        "-fx-font-size:12;-fx-cursor:hand;-fx-padding:4 12;";
    private static final String SUB_ACTIVE =
        "-fx-background-color:#1f1f3a;-fx-text-fill:#c84cff;" +
        "-fx-font-size:12;-fx-font-weight:bold;-fx-cursor:hand;" +
        "-fx-padding:4 12;-fx-background-radius:6;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User user = SessionContext.getCurrentUser();

        // Nom dans la topbar
        if (user != null && welcomeLabel != null)
            welcomeLabel.setText("Bonjour, " + user.getName());

        // Masquer le sous-menu Social pour les admins (ils voient tout via cards)
        if (user != null && user.isAdmin() && subMenuSocial != null) {
            subMenuSocial.setVisible(false);
            subMenuSocial.setManaged(false);
        }

        // Vue par défaut : Réseaux (selon rôle)
        setMainActive(btnReseaux);
        goReseaux();
        try {
            Image img = new Image(
                    getClass().getResource("/com/gamilha/images/logo.png").toExternalForm()
            );
            logoImage.setImage(img);
        } catch (Exception e) {
            System.out.println("Logo non trouvé");

        }

        }


    // ════════════════════════════════════════════════════════════════════
    //  CHARGEMENT DES PAGES
    // ════════════════════════════════════════════════════════════════════

    private void load(String fullPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fullPath));
            Parent root = loader.load();
            injectUser(loader.getController());
            contentArea.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("❌ Impossible de charger : " + fullPath);
        }
    }

    /** Injecte l'utilisateur dans les contrôleurs qui le nécessitent */
    private void injectUser(Object ctrl) {
        User u = SessionContext.getCurrentUser();
        if      (ctrl instanceof UserPostController            c) c.setCurrentUser(u);
        else if (ctrl instanceof UserMesPostsController        c) c.setCurrentUser(u);
        else if (ctrl instanceof UserMesCommentairesController c) c.setCurrentUser(u);
        else if (ctrl instanceof UserAmisController            c) c.setCurrentUser(u);
    }

    // ════════════════════════════════════════════════════════════════════
    //  GESTION DE L'ÉTAT ACTIF (menu principal + sous-menu)
    // ════════════════════════════════════════════════════════════════════

    private void setMainActive(Button btn) {
        for (Button b : new Button[]{
            btnAccueil, btnEvenements, btnEquipes, btnCoaching,
            btnAI, btnReseaux, btnStreams, btnAbonnements})
            if (b != null) b.setStyle(MAIN_NORMAL);
        if (btn != null) btn.setStyle(MAIN_ACTIVE);
        resetSub();
    }

    /** Active Réseaux + un sous-item */
    private void setSubActive(Button btn) {
        // Garder Réseaux actif dans la topbar
        setMainActive(btnReseaux);
        // Reset + activer le sous-bouton
        resetSub();
        if (btn != null) btn.setStyle(SUB_ACTIVE);
    }

    private void resetSub() {
        for (Button b : new Button[]{
            btnFilActualite, btnMesPosts, btnMesCommentaires, btnAmis})
            if (b != null) b.setStyle(SUB_NORMAL);
    }

    // ════════════════════════════════════════════════════════════════════
    //  HANDLERS
    // ════════════════════════════════════════════════════════════════════

    @FXML void goAccueil()    { setMainActive(btnAccueil);     load(BASE_USER + "Accueil.fxml"); }
    @FXML void goEvenements() { setMainActive(btnEvenements);  load(BASE_USER + "Evenement.fxml"); }
    @FXML void goEquipe()     { setMainActive(btnEquipes);     load(BASE_USER + "Equipe.fxml"); }
    @FXML void goCoaching()   { setMainActive(btnCoaching);    load(BASE_USER + "Coaching.fxml"); }
    @FXML void goAI()         { setMainActive(btnAI);          load(BASE_USER + "GamilhaAI.fxml"); }
    @FXML void goStreams()    { setMainActive(btnStreams);      load(BASE_USER + "Stream.fxml"); }
    @FXML void goAbonnement() { setMainActive(btnAbonnements); load(BASE_USER + "Abonnement.fxml"); }

    /**
     * 🌐 Réseaux — détecte le rôle :
     *   Admin  → cards AdminPostView (gestion complète)
     *   User   → fil social UserPostView
     */
    @FXML
    public void goReseaux() {
        User u = SessionContext.getCurrentUser();
        if (u != null && u.isAdmin()) {
            setMainActive(btnReseaux);
            load(BASE_ADMIN + "AdminPostView.fxml");
        } else {
            setSubActive(btnFilActualite);
            load(BASE_USER + "UserPostView.fxml");
        }
    }

    /** 🏠 Fil d'actualité (sous-menu) */
    @FXML
    public void goFilActualite() {
        setSubActive(btnFilActualite);
        load(BASE_USER + "UserPostView.fxml");
    }

    /** 📝 Mes Posts (sous-menu) */
    @FXML
    public void goMesPosts() {
        setSubActive(btnMesPosts);
        load(BASE_USER + "UserMesPostsView.fxml");
    }

    /** 💬 Mes Commentaires (sous-menu) */
    @FXML
    public void goMesCommentaires() {
        setSubActive(btnMesCommentaires);
        load(BASE_USER + "UserMesCommentairesView.fxml");
    }

    /** 👤 Amis (sous-menu) */
    @FXML
    public void goAmis() {
        setSubActive(btnAmis);
        load(BASE_USER + "UserAmisView.fxml");
    }

    // ── Déconnexion ───────────────────────────────────────────────────────
    @FXML
    public void logout(ActionEvent event) {
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
