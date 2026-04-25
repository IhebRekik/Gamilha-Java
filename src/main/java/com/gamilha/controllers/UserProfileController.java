package com.gamilha.controllers;

import com.gamilha.utils.SessionContext;


import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

    // Section "Informations"
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label idLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;

    // Header
    @FXML private Label headerNameLabel;
    @FXML private Label headerEmailLabel;
    @FXML private Label headerIdLabel;
    @FXML private Label headerRoleLabel;
    @FXML private Label headerStatusLabel;

    @FXML private ImageView profileImageView;

    private User currentUser;
    private final UserService userService = new UserService();

    public void setUser(User user) {
        this.currentUser = user;
        if (user == null) return;

        String name = user.getName();
        String email = user.getEmail();
        String id = String.valueOf(user.getId());

        setTextIfPresent(nameLabel, name);
        setTextIfPresent(emailLabel, email);
        setTextIfPresent(idLabel, id);

        setTextIfPresent(headerNameLabel, name);
        setTextIfPresent(headerEmailLabel, email);
        setTextIfPresent(headerIdLabel, id);

        // Affiche rôle lisible
        String roles = user.getRoles();
        String roleText = (roles != null && roles.contains("ROLE_ADMIN")) ? "Administrateur" : "Utilisateur";
        setRole(roleLabel, roleText);
        setRole(headerRoleLabel, roleText);

        // Statut (et couleur)
        applyStatus(statusLabel, user.isActive());
        applyStatus(headerStatusLabel, user.isActive());

        refreshProfileImage(user.getProfileImage());
    }

    private void refreshProfileImage(String storedPath) {
        if (profileImageView == null) return;
        try {
            Image img = null;
            if (storedPath != null && !storedPath.isBlank()) {
                Path p = ImageStorage.resolveToPath(storedPath);
                if (p != null) {
                    img = new Image(p.toUri().toString(), 72, 72, true, true);
                }
            }
            if (img == null) {
                var is = getClass().getResourceAsStream("/com/gamilha/images/logo.jpg");
                if (is != null) img = new Image(is, 72, 72, true, true);
            }
            profileImageView.setImage(img);
            profileImageView.setFitWidth(72);
            profileImageView.setFitHeight(72);
            profileImageView.setPreserveRatio(true);
            profileImageView.setClip(new Circle(36, 36, 36));
        } catch (Exception ignored) {
        }
    }

    private void setTextIfPresent(Label label, String value) {
        if (label != null) {
            label.setText(value != null ? value : "—");
        }
    }

    private void setRole(Label label, String roleText) {
        if (label != null) {
            label.setText(roleText != null ? roleText : "—");
        }
    }

    private void applyStatus(Label label, boolean active) {
        if (label == null) return;

        if (active) {
            label.setText("Actif");
            label.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;");
        } else {
            label.setText("Inactif");
            label.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void editUser() {
        if (currentUser == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/Admin/edit_user.fxml")); // ✅ bon chemin
            Parent form = loader.load();

            EditUserController controller = loader.getController();
            controller.setUser(currentUser);

            // Pour un user normal, la scène n'a pas de #contentArea
            // → on remplace la scène entière
            Stage stage = (Stage) nameLabel.getScene().getWindow();
            Scene scene = new Scene(form, 900, 600);
            URL css = getClass().getResource("/com/gamilha/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void deleteUser() {
        if (currentUser == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer votre compte ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userService.delete(currentUser.getId());
                // ✅ Session : clear si le user supprime son compte
                com.gamilha.utils.SessionContext.clear();
                showAlert("Succès", "Compte supprimé", Alert.AlertType.INFORMATION);
                goToLogin();
            }
        });
    }

    @FXML
    private void backToList() {
        goToLogin(); // Pour un user normal, "retour" = retour au login
    }

    @FXML
    private void logout() {
        goToLogin();
    }

    private void goToLogin() {
        // ✅ Session : clear quand on revient au login (logout)
        com.gamilha.utils.SessionContext.clear();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 700, 520);
            URL css = getClass().getResource("/com/gamilha/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            Stage stage = (Stage) nameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gamilha - Connexion");
            stage.setMaximized(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {}
}