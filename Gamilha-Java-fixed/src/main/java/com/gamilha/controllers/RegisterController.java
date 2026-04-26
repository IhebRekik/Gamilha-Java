package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Contrôleur d'inscription.
 * Le mot de passe est haché automatiquement par UserService.add() via BCrypt.
 */
public class RegisterController {

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         avatarPathLabel;

    private File selectedAvatarFile;

    private final UserService userService = new UserService();

    @FXML
    private void chooseAvatar() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        FileChooser fc = ImageStorage.createAvatarFileChooser();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedAvatarFile = f;
            if (avatarPathLabel != null) avatarPathLabel.setText(f.getName());
        }
    }

    @FXML
    private void handleRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Tous les champs sont obligatoires !");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert("Erreur", "Les deux mots de passe ne correspondent pas !");
            return;
        }
        if (password.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères !");
            return;
        }

        try {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            // Le mot de passe en clair — UserService.add() le hachera via BCrypt
            user.setPassword(password);
            user.setActive(true);
            user.setRoles("[\"ROLE_USER\"]");

            if (selectedAvatarFile != null) {
                String storedPath = ImageStorage.saveAvatar(selectedAvatarFile);
                user.setProfileImage(storedPath);
            }

            userService.add(user);
            showAlert("Compte créé", "Votre compte a été créé. Vous pouvez maintenant vous connecter.");
            ((Stage) nameField.getScene().getWindow()).close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de créer le compte : " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
