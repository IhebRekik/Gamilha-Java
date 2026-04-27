package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.AvatarAiService;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;

public class RegisterController {

    @FXML private TextField     nameField;
    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private Button        eyeButton;
    @FXML private Label         avatarPathLabel;
    @FXML private Label         errorLabel;
    @FXML private VBox          errorBox;
    @FXML private Label         successLabel;
    @FXML private Label         strengthLabel;
    @FXML private Label         aiStatusLabel;
    @FXML private ImageView     avatarImageView;
    @FXML private Label         avatarPlaceholder;

    private File    selectedAvatarFile;
    private boolean passwordShown = false;

    private final UserService     userService     = new UserService();
    private final AvatarAiService avatarAiService = new AvatarAiService();

    @FXML
    public void initialize() {
        hide(errorBox);
        hide(successLabel);
        hide(strengthLabel);
        hide(aiStatusLabel);

        if (passwordVisible != null && passwordField != null) {
            hide(passwordVisible);
            passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
        }
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, o, n) -> updateStrength(n));
        }
    }

    // ── Eye ──────────────────────────────────────────────────────────────
    @FXML
    private void toggleEye() {
        passwordShown = !passwordShown;
        if (passwordField != null && passwordVisible != null) {
            setVisible(passwordField,  !passwordShown);
            setVisible(passwordVisible, passwordShown);
        }
        if (eyeButton != null) eyeButton.setText(passwordShown ? "🙈" : "👁");
    }

    // ── Strength ──────────────────────────────────────────────────────────
    private void updateStrength(String pwd) {
        if (strengthLabel == null || pwd == null) return;
        int s = 0;
        if (pwd.length() >= 8)               s++;
        if (pwd.matches(".*[A-Z].*"))         s++;
        if (pwd.matches(".*[a-z].*"))         s++;
        if (pwd.matches(".*[0-9].*"))         s++;
        if (pwd.matches(".*[^A-Za-z0-9].*")) s++;
        setVisible(strengthLabel, !pwd.isEmpty());
        switch (s) {
            case 0,1 -> { strengthLabel.setText("⚡ Très faible"); strengthLabel.setTextFill(Color.web("#ef4444")); }
            case 2   -> { strengthLabel.setText("⚡ Faible");       strengthLabel.setTextFill(Color.web("#f97316")); }
            case 3   -> { strengthLabel.setText("⚡ Moyen");        strengthLabel.setTextFill(Color.web("#eab308")); }
            case 4   -> { strengthLabel.setText("✅ Fort");         strengthLabel.setTextFill(Color.web("#22c55e")); }
            case 5   -> { strengthLabel.setText("🔒 Très fort");   strengthLabel.setTextFill(Color.web("#8b5cf6")); }
        }
    }

    // ── Avatar ────────────────────────────────────────────────────────────
    @FXML
    private void chooseAvatar() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        FileChooser fc = ImageStorage.createAvatarFileChooser();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedAvatarFile = f;
            if (avatarPathLabel != null) avatarPathLabel.setText(f.getName());
            showAvatarPreview(f.toURI().toString());
            hide(aiStatusLabel);
        }
    }

    private void showAvatarPreview(String uri) {
        try {
            if (avatarImageView != null) {
                Image img = new Image(uri, 110, 110, true, true);
                avatarImageView.setImage(img);
                Circle clip = new Circle(55, 55, 55);
                avatarImageView.setClip(clip);
                setVisible(avatarImageView, true);
            }
            hide(avatarPlaceholder);
        } catch (Exception ignored) {}
    }

    // ── AI Avatar ─────────────────────────────────────────────────────────
    @FXML
    private void generateAiAvatar() {
        if (selectedAvatarFile == null) { showError("Choisissez d'abord une photo !"); return; }
        if (aiStatusLabel != null) {
            aiStatusLabel.setText("⏳ Génération en cours...");
            aiStatusLabel.setStyle("-fx-text-fill:#fbbf24;-fx-font-size:12px;");
            setVisible(aiStatusLabel, true);
        }
        new Thread(() -> {
            try {
                byte[] result = avatarAiService.generateAnimeAvatar(selectedAvatarFile);
                if (result != null && result.length > 0) {
                    File tmp = new File(System.getProperty("user.home"), ".gamilha/avatars/ai_" + System.currentTimeMillis() + ".jpg");
                    tmp.getParentFile().mkdirs();
                    Files.write(tmp.toPath(), result);
                    selectedAvatarFile = tmp;
                    Platform.runLater(() -> {
                        showAvatarPreview(tmp.toURI().toString());
                        if (aiStatusLabel != null) {
                            aiStatusLabel.setText("✅ Avatar IA prêt — créez votre compte !");
                            aiStatusLabel.setStyle("-fx-background-color:#166534;-fx-text-fill:#86efac;" +
                                    "-fx-background-radius:20;-fx-padding:6 16;-fx-font-size:13px;-fx-font-weight:bold;");
                        }
                        if (avatarPathLabel != null) avatarPathLabel.setText("ℹ La photo sera transformée en avatar anime");
                    });
                } else {
                    Platform.runLater(() -> {
                        if (aiStatusLabel != null) {
                            aiStatusLabel.setText("❌ Échec. Vérifiez le token HuggingFace.");
                            aiStatusLabel.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12px;");
                        }
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (aiStatusLabel != null) {
                        aiStatusLabel.setText("❌ Erreur: " + ex.getMessage());
                        aiStatusLabel.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12px;");
                    }
                });
            }
        }).start();
    }

    // ── Register ──────────────────────────────────────────────────────────
    @FXML
    private void handleRegister() {
        String name  = nameField    != null ? nameField.getText().trim()  : "";
        String email = emailField   != null ? emailField.getText().trim() : "";
        String pwd   = passwordField != null ? passwordField.getText()    : "";

        clearMessages();

        if (name.isEmpty() || email.isEmpty() || pwd.isEmpty()) {
            showError("Tous les champs sont obligatoires !"); return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            showError("Adresse email invalide."); return;
        }
        String pwdErr = validatePassword(pwd);
        if (pwdErr != null) { showError(pwdErr); return; }

        try {
            User user = new User();
            user.setName(name); user.setEmail(email); user.setPassword(pwd);
            user.setActive(true); user.setRoles("[\"ROLE_USER\"]");
            if (selectedAvatarFile != null)
                user.setProfileImage(ImageStorage.saveAvatar(selectedAvatarFile));
            userService.add(user);
            showSuccess("✅ Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (Exception ignored) {}
                Platform.runLater(() -> ((Stage) nameField.getScene().getWindow()).close());
            }).start();
        } catch (Exception e) {
            showError("Impossible de créer le compte : " + e.getMessage());
        }
    }

    private String validatePassword(String pwd) {
        if (pwd.length() < 8)             return "Le mot de passe doit contenir au moins 8 caractères.";
        if (!pwd.matches(".*[A-Z].*"))     return "Le mot de passe doit contenir au moins une majuscule.";
        if (!pwd.matches(".*[a-z].*"))     return "Le mot de passe doit contenir au moins une minuscule.";
        if (!pwd.matches(".*[0-9].*"))     return "Le mot de passe doit contenir au moins un chiffre.";
        return null;
    }

    @FXML private void handleBackToLogin() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    @FXML private void clearError() {
        hide(errorBox);
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void showError(String msg) {
        if (errorLabel != null) errorLabel.setText(msg);
        setVisible(errorBox, true);
    }
    private void showSuccess(String msg) {
        if (successLabel != null) { successLabel.setText(msg); setVisible(successLabel, true); }
    }
    private void clearMessages() {
        hide(errorBox);
        hide(successLabel);
    }

    private void hide(javafx.scene.Node n) { if (n != null) { n.setVisible(false); n.setManaged(false); } }
    private void setVisible(javafx.scene.Node n, boolean v) { if (n != null) { n.setVisible(v); n.setManaged(v); } }
}
