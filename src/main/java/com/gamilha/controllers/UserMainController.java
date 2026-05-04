package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Conteneur principal de l'interface utilisateur.
 * Injecter l'utilisateur connecté via setCurrentUser().
 * FXML : UserMainView.fxml
 */
public class UserMainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button    btnPosts;
    @FXML private Button    btnMesPosts;
    @FXML private Button    btnMesCommentaires;
    @FXML private Label     userNameLabel;
    @FXML private Label     userEmailLabel;
    @FXML private ImageView userAvatar;

    private User currentUser;

    private static final String ACTIVE =
        "-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:8;" +
        "-fx-alignment:CENTER_LEFT;-fx-padding:10 14;-fx-font-size:13;-fx-cursor:hand;";
    private static final String INACTIVE =
        "-fx-background-color:transparent;-fx-text-fill:#c9d1d9;-fx-background-radius:8;" +
        "-fx-alignment:CENTER_LEFT;-fx-padding:10 14;-fx-font-size:13;-fx-cursor:hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /** Appeler après loadFXML pour injecter l'utilisateur connecté */
    public void setCurrentUser(User user) {
        this.currentUser = user;

        if (user != null) {
            userNameLabel.setText(user.getName());
            userEmailLabel.setText(user.getEmail());

            // Avatar
            if (user.getProfileImage() != null) {
                File f = new File(AdminPostController.UPLOADS + user.getProfileImage());
                if (f.exists()) {
                    userAvatar.setImage(new Image(f.toURI().toString()));
                }
            }
            userAvatar.setClip(new Circle(22, 22, 22));
        }

        // Charger la vue par défaut
        showFeed();
    }

    // ── Navigation ────────────────────────────────────────────────────────

    /** Fil principal de tous les posts */
    @FXML public void showFeed() {
        loadUserView("/com/gamilha/UserPostView.fxml");
        setActive(btnPosts);
    }

    /** Mes posts uniquement */
    @FXML public void showMesPosts() {
        loadUserView("/com/gamilha/UserMesPostsView.fxml");
        setActive(btnMesPosts);
    }

    /** Mes commentaires uniquement */
    @FXML public void showMesCommentaires() {
        loadUserView("/com/gamilha/UserMesCommentairesView.fxml");
        setActive(btnMesCommentaires);
    }

    // ── Chargement d'une vue avec injection de l'utilisateur ──────────────
    private void loadUserView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);

            // Injection de l'utilisateur dans le contrôleur enfant
            Object ctrl = loader.getController();
            if (ctrl instanceof UserPostController upc) {
                upc.setCurrentUser(currentUser);
            } else if (ctrl instanceof UserMesPostsController umpc) {
                umpc.setCurrentUser(currentUser);
            } else if (ctrl instanceof UserMesCommentairesController umcc) {
                umcc.setCurrentUser(currentUser);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnPosts, btnMesPosts, btnMesCommentaires}) {
            b.setStyle(b == active ? ACTIVE : INACTIVE);
        }
    }
}
