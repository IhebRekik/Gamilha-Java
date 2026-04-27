package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import com.gamilha.utils.PasswordHasher;
import com.gamilha.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class EditUserController {

    // Common fields
    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private ImageView     avatarPreview;
    @FXML private Label         avatarPathLabel;
    @FXML private Label         errorLabel;
    @FXML private Label         successLabel;

    // Password change
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    // Admin-only
    @FXML private CheckBox      activeCheckBox;
    @FXML private CheckBox      adminCheckBox;
    @FXML private CheckBox      moderatorCheckBox;
    @FXML private CheckBox      coachCheckBox;
    @FXML private Label         banUntilLabel;
    @FXML private Label         reportsLabel;
    @FXML private TextField     reportsField;

    // Password strength
    @FXML private Label         strengthLabel;
    @FXML private javafx.scene.shape.Rectangle strengthBar;

    private File selectedAvatarFile;
    private User currentUser;
    private boolean isAdminEditing = false;
    private final UserService userService = new UserService();

    public void setUser(User user) {
        this.currentUser = user;
        User sessionUser = SessionContext.getCurrentUser();
        isAdminEditing = sessionUser != null && sessionUser.isAdmin() && sessionUser.getId() != user.getId();

        if (nameField != null) nameField.setText(user.getName());
        if (emailField != null) {
            emailField.setText(user.getEmail());
            // Users can't change email - only admin
            emailField.setEditable(isAdminEditing);
        }
        if (activeCheckBox != null) activeCheckBox.setSelected(user.isActive());
        if (banUntilLabel != null) banUntilLabel.setText(user.getBanUntil() != null ? user.getBanUntil() : "Aucun");
        if (reportsLabel != null) reportsLabel.setText(String.valueOf(user.getReports()));
        if (reportsField != null) reportsField.setText(String.valueOf(user.getReports()));

        // Roles for admin
        if (adminCheckBox != null) adminCheckBox.setSelected(user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN"));
        if (moderatorCheckBox != null) moderatorCheckBox.setSelected(user.getRoles() != null && user.getRoles().contains("ROLE_MODERATOR"));
        if (coachCheckBox != null) coachCheckBox.setSelected(user.getRoles() != null && user.getRoles().contains("ROLE_COACH"));

        // Hide old password field for admin editing someone else
        if (oldPasswordField != null) {
            oldPasswordField.setVisible(!isAdminEditing);
            oldPasswordField.setManaged(!isAdminEditing);
        }

        refreshAvatarPreview(user.getProfileImage());
        if (errorLabel != null) errorLabel.setVisible(false);
        if (successLabel != null) successLabel.setVisible(false);

        // Password strength listener
        if (passwordField != null && strengthLabel != null) {
            passwordField.textProperty().addListener((obs, o, n) -> updateStrength(n));
        }
    }

    private void updateStrength(String pwd) {
        if (strengthLabel == null) return;
        int score = 0;
        if (pwd.length() >= 8) score++;
        if (pwd.matches(".*[A-Z].*")) score++;
        if (pwd.matches(".*[a-z].*")) score++;
        if (pwd.matches(".*[0-9].*")) score++;
        if (pwd.matches(".*[^A-Za-z0-9].*")) score++;
        strengthLabel.setVisible(!pwd.isEmpty());
        switch (score) {
            case 0, 1, 2 -> strengthLabel.setText("⚡ Faible");
            case 3       -> strengthLabel.setText("⚡ Moyen");
            case 4       -> strengthLabel.setText("✅ Fort");
            case 5       -> strengthLabel.setText("🔒 Très fort");
        }
    }

    @FXML
    private void chooseAvatar() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        FileChooser fc = ImageStorage.createAvatarFileChooser();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedAvatarFile = f;
            if (avatarPathLabel != null) avatarPathLabel.setText(f.getName());
            refreshAvatarPreview(f.toURI().toString());
        }
    }

    private void refreshAvatarPreview(String storedOrUri) {
        if (avatarPreview == null) return;
        try {
            Image img = null;
            if (storedOrUri == null || storedOrUri.isBlank()) {
                var is = getClass().getResourceAsStream("/com/gamilha/images/logo.jpg");
                if (is != null) img = new Image(is, 96, 96, true, true);
            } else if (storedOrUri.startsWith("file:") || storedOrUri.startsWith("http")) {
                img = new Image(storedOrUri, 96, 96, true, true);
            } else {
                Path p = ImageStorage.resolveToPath(storedOrUri);
                if (p != null) img = new Image(p.toUri().toString(), 96, 96, true, true);
            }
            if (img != null) {
                avatarPreview.setImage(img);
                avatarPreview.setClip(new Circle(48, 48, 48));
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void updateUser() {
        if (currentUser == null) return;
        clearMessages();

        String name = nameField.getText().trim();
        if (name.isEmpty()) { showError("Le nom est obligatoire !"); return; }

        // Password change logic
        String newPwd  = passwordField != null ? passwordField.getText() : "";
        String confirm = confirmPasswordField != null ? confirmPasswordField.getText() : "";

        if (!newPwd.isEmpty()) {
            // If regular user changing own password, require old password
            if (!isAdminEditing) {
                String oldPwd = oldPasswordField != null ? oldPasswordField.getText() : "";
                if (oldPwd.isEmpty()) { showError("Veuillez entrer votre ancien mot de passe."); return; }
                if (!PasswordHasher.check(oldPwd, currentUser.getPassword())) {
                    showError("Ancien mot de passe incorrect !"); return;
                }
            }
            // Validate new password strength
            if (newPwd.length() < 8 || !newPwd.matches(".*[A-Z].*") ||
                !newPwd.matches(".*[0-9].*") || !newPwd.matches(".*[^A-Za-z0-9].*")) {
                showError("Nouveau mot de passe trop faible (8 car., majuscule, chiffre, symbole requis).");
                return;
            }
            if (!newPwd.equals(confirm)) { showError("Les mots de passe ne correspondent pas !"); return; }
            currentUser.setPassword(newPwd);
        } else {
            currentUser.setPassword(null); // don't change
        }

        currentUser.setName(name);

        if (isAdminEditing) {
            if (emailField != null) currentUser.setEmail(emailField.getText().trim());
            if (activeCheckBox != null) currentUser.setActive(activeCheckBox.isSelected());
            // Build roles
            StringBuilder roles = new StringBuilder("[\"ROLE_USER\"");
            if (adminCheckBox != null && adminCheckBox.isSelected()) roles.append(",\"ROLE_ADMIN\"");
            if (moderatorCheckBox != null && moderatorCheckBox.isSelected()) roles.append(",\"ROLE_MODERATOR\"");
            if (coachCheckBox != null && coachCheckBox.isSelected()) roles.append(",\"ROLE_COACH\"");
            roles.append("]");
            currentUser.setRoles(roles.toString());
            if (reportsField != null) {
                try { currentUser.setReports(Integer.parseInt(reportsField.getText())); } catch (Exception ignored) {}
            }
        }

        try {
            if (selectedAvatarFile != null) {
                currentUser.setProfileImage(ImageStorage.saveAvatar(selectedAvatarFile));
            }
        } catch (IOException e) {
            showError("Upload image impossible: " + e.getMessage()); return;
        }

        userService.update(currentUser);

        User sessionUser = SessionContext.getCurrentUser();
        if (sessionUser != null && sessionUser.getId() == currentUser.getId()) {
            SessionContext.setCurrentUser(currentUser);
        }

        showSuccess("✅ Utilisateur modifié avec succès !");
        new Thread(() -> {
            try { Thread.sleep(1500); } catch (Exception ignored) {}
            javafx.application.Platform.runLater(this::backToList);
        }).start();
    }

    @FXML private void cancel() { backToList(); }

    @FXML
    private void backToList() {
        try {
            BorderPane contentArea = (BorderPane) nameField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gamilha/interfaces/Admin/admin_users.fxml"));
                contentArea.setCenter(loader.load());
                return;
            }
        } catch (Exception ignored) {}
        try {
            User userToShow = currentUser != null ? currentUser : SessionContext.getCurrentUser();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gamilha/interfaces/User/user_profile.fxml"));
            Parent root = loader.load();
            if (userToShow != null) ((UserProfileController) loader.getController()).setUser(userToShow);
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        if (errorLabel != null) { errorLabel.setText("⚠ " + msg); errorLabel.setVisible(true); }
        else { Alert a = new Alert(Alert.AlertType.ERROR); a.setContentText(msg); a.showAndWait(); }
    }

    private void showSuccess(String msg) {
        if (successLabel != null) { successLabel.setText(msg); successLabel.setVisible(true); }
    }

    private void clearMessages() {
        if (errorLabel != null) errorLabel.setVisible(false);
        if (successLabel != null) successLabel.setVisible(false);
    }
}
