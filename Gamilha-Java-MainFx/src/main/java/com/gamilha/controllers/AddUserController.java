package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.AvatarAiService;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AddUserController {

    @FXML private TextField        nameField;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox         activeCheckBox;
    @FXML private Label            avatarPathLabel;
    @FXML private ImageView        avatarPreview;
    @FXML private Label            errorLabel;
    @FXML private Label            successLabel;
    @FXML private TextField        reportsField;
    @FXML private DatePicker       banDatePicker;

    private File selectedAvatarFile;
    private File generatedAvatarFile;
    private final UserService     userService     = new UserService();
    private final AvatarAiService avatarAiService = new AvatarAiService();

    @FXML
    public void initialize() {
        if (roleCombo != null) {
            roleCombo.getItems().addAll(
                    "[\"ROLE_USER\"]",
                    "[\"ROLE_USER\",\"ROLE_ADMIN\"]",
                    "[\"ROLE_USER\",\"ROLE_MODERATOR\"]",
                    "[\"ROLE_USER\",\"ROLE_COACH\"]");
            roleCombo.setValue("[\"ROLE_USER\"]");
        }
        if (activeCheckBox != null) activeCheckBox.setSelected(true);
        if (errorLabel != null)   errorLabel.setVisible(false);
        if (successLabel != null) successLabel.setVisible(false);
    }

    @FXML
    private void chooseAvatar() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        FileChooser fc = ImageStorage.createAvatarFileChooser();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedAvatarFile = f;
            generatedAvatarFile = null;
            if (avatarPathLabel != null) avatarPathLabel.setText(f.getName());
            showAvatarPreview(f.toURI().toString());
        }
    }

    @FXML
    private void generateAiAvatar() {
        if (selectedAvatarFile == null && avatarPathLabel != null) {
            showError("Choisissez d'abord une photo pour générer l'avatar IA !");
            return;
        }
        if (avatarPathLabel != null) avatarPathLabel.setText("⏳ Génération en cours...");
        if (successLabel != null) { successLabel.setText(""); successLabel.setVisible(false); }

        new Thread(() -> {
            try {
                byte[] result = selectedAvatarFile != null
                        ? avatarAiService.generateAnimeAvatar(selectedAvatarFile)
                        : avatarAiService.generateFromPrompt(null);

                if (result != null && result.length > 0) {
                    // Save to temp file
                    File tmpDir = new File(System.getProperty("user.home"), ".gamilha/avatars");
                    tmpDir.mkdirs();
                    File tmpFile = new File(tmpDir, "ai_avatar_" + System.currentTimeMillis() + ".jpg");
                    Files.write(tmpFile.toPath(), result);
                    generatedAvatarFile = tmpFile;
                    selectedAvatarFile  = tmpFile;

                    Platform.runLater(() -> {
                        if (avatarPathLabel != null) avatarPathLabel.setText("✅ Avatar IA généré !");
                        showAvatarPreview(tmpFile.toURI().toString());
                        if (successLabel != null) {
                            successLabel.setText("✨ Avatar cartoon généré avec succès !");
                            successLabel.setVisible(true);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        if (avatarPathLabel != null) avatarPathLabel.setText("❌ Échec génération IA");
                        showError("Impossible de générer l'avatar. Vérifiez votre token HuggingFace.");
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (avatarPathLabel != null) avatarPathLabel.setText("❌ Erreur");
                    showError("Erreur IA : " + ex.getMessage());
                });
            }
        }).start();
    }

    private void showAvatarPreview(String uri) {
        if (avatarPreview == null) return;
        try {
            Image img = new Image(uri, 80, 80, true, true);
            avatarPreview.setImage(img);
            avatarPreview.setClip(new Circle(40, 40, 40));
        } catch (Exception ignored) {}
    }

    @FXML
    private void saveUser() {
        clearMessages();
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Tous les champs obligatoires doivent être remplis !");
            return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            showError("Adresse email invalide."); return;
        }
        if (password.length() < 8 || !password.matches(".*[A-Z].*") ||
            !password.matches(".*[0-9].*") || !password.matches(".*[^A-Za-z0-9].*")) {
            showError("Mot de passe trop faible : min 8 car., majuscule, chiffre, symbole."); return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setActive(activeCheckBox == null || activeCheckBox.isSelected());
        user.setRoles(roleCombo != null ? roleCombo.getValue() : "[\"ROLE_USER\"]");

        if (reportsField != null && !reportsField.getText().isEmpty()) {
            try { user.setReports(Integer.parseInt(reportsField.getText())); } catch (Exception ignored) {}
        }

        if (banDatePicker != null && banDatePicker.getValue() != null) {
            LocalDate bd = banDatePicker.getValue();
            user.setBanUntil(bd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:59");
        }

        try {
            File avatarToSave = selectedAvatarFile;
            if (avatarToSave != null) {
                user.setProfileImage(ImageStorage.saveAvatar(avatarToSave));
            }
        } catch (IOException e) {
            showError("Upload image impossible: " + e.getMessage()); return;
        }

        userService.add(user);
        showSuccess("✅ Utilisateur ajouté avec succès !");
        new Thread(() -> {
            try { Thread.sleep(1400); } catch (Exception ignored) {}
            Platform.runLater(this::backToList);
        }).start();
    }

    @FXML private void cancel() { backToList(); }

    private void backToList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gamilha/interfaces/Admin/admin_users.fxml"));
            Parent content = loader.load();
            BorderPane contentArea = (BorderPane) nameField.getScene().lookup("#contentArea");
            if (contentArea != null) contentArea.setCenter(content);
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
