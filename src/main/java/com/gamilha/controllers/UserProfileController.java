package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.User;
import com.gamilha.services.AvatarAiService;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import com.gamilha.utils.PasswordHasher;
import com.gamilha.utils.SessionContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class UserProfileController implements Initializable {

    // ── Header labels ─────────────────────────────────────────────────────
    @FXML private Label     headerNameLabel;
    @FXML private Label     headerEmailLabel;
    @FXML private Label     headerRoleLabel;
    @FXML private Label     headerStatusLabel;
    @FXML private Label     presenceLabel;
    @FXML private ImageView profileImageView;

    // ── Info labels ────────────────────────────────────────────────────────
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Label lastSeenLabel;

    // ── Banners ────────────────────────────────────────────────────────────
    @FXML private Label successBanner;
    @FXML private Label errorBanner;

    // ── Edit section ───────────────────────────────────────────────────────
    @FXML private VBox      editSection;
    @FXML private TextField editNameField;
    @FXML private ImageView editAvatarPreview;
    @FXML private Label     editAvatarLabel;

    // ── Password section ───────────────────────────────────────────────────
    @FXML private VBox          passwordSection;
    @FXML private PasswordField oldPwdField;
    @FXML private PasswordField newPwdField;
    @FXML private PasswordField confirmPwdField;
    @FXML private Label         pwdStrengthLabel;

    @FXML private Label aiStatusLabel;
    @FXML private DatePicker deactivationDatePicker;

    private User currentUser;
    private File selectedAvatarFile;
    private final UserService     userService     = new UserService();
    private final AvatarAiService avatarAiService = new AvatarAiService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Listen for password strength
        if (newPwdField != null) {
            newPwdField.textProperty().addListener((obs, o, n) -> updateStrength(n));
        }
        hide(aiStatusLabel);
    }

    @FXML
    private void generateAiAvatar() {
        if (selectedAvatarFile == null && (currentUser == null || currentUser.getProfileImage() == null)) {
            showError("Choisissez d'abord une photo !");
            return;
        }

        File sourceFile = selectedAvatarFile;
        if (sourceFile == null && currentUser.getProfileImage() != null) {
            Path p = ImageStorage.resolveToPath(currentUser.getProfileImage());
            if (p != null) sourceFile = p.toFile();
        }

        if (sourceFile == null) { showError("Source image introuvable."); return; }

        if (aiStatusLabel != null) {
            aiStatusLabel.setText("⏳ Génération avatar IA...");
            aiStatusLabel.setTextFill(Color.web("#fbbf24"));
            show(aiStatusLabel);
        }

        final File finalSource = sourceFile;
        new Thread(() -> {
            try {
                byte[] result = avatarAiService.generateAnimeAvatar(finalSource);
                if (result != null && result.length > 0) {
                    File tmp = new File(System.getProperty("user.home"), ".gamilha/avatars/ai_" + System.currentTimeMillis() + ".jpg");
                    tmp.getParentFile().mkdirs();
                    java.nio.file.Files.write(tmp.toPath(), result);
                    selectedAvatarFile = tmp;
                    Platform.runLater(() -> {
                        refreshAvatar(tmp.toURI().toString(), editAvatarPreview, 70);
                        if (aiStatusLabel != null) {
                            aiStatusLabel.setText("✅ Avatar IA prêt — Enregistrez pour valider !");
                            aiStatusLabel.setTextFill(Color.web("#22c55e"));
                        }
                        if (editAvatarLabel != null) editAvatarLabel.setText("Avatar généré par IA");
                    });
                } else {
                    Platform.runLater(() -> {
                        if (aiStatusLabel != null) {
                            aiStatusLabel.setText("❌ Échec de la génération IA.");
                            aiStatusLabel.setTextFill(Color.web("#ef4444"));
                        }
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (aiStatusLabel != null) {
                        aiStatusLabel.setText("❌ Erreur: " + ex.getMessage());
                        aiStatusLabel.setTextFill(Color.web("#ef4444"));
                    }
                });
            }
        }).start();
    }

    // ── Called by NavBar / login after setting session ─────────────────────
    public void setUser(User user) {
        this.currentUser = user;
        if (user == null) return;
        refreshAll(user);
    }

    private void refreshAll(User user) {
        // Header
        setText(headerNameLabel,  user.getName());
        setText(headerEmailLabel, user.getEmail());

        String roleText = (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN"))
                ? "Administrateur" : "Utilisateur";
        if (headerRoleLabel != null) {
            headerRoleLabel.setText(roleText);
            headerRoleLabel.setStyle(
                (user.isAdmin() ? "-fx-background-color:#eab308;-fx-text-fill:black;"
                                : "-fx-background-color:#7c3aed;-fx-text-fill:white;")
                + "-fx-padding:3 12;-fx-background-radius:20;-fx-font-size:12px;");
        }
        if (headerStatusLabel != null) {
            headerStatusLabel.setText(user.isActive() ? "✅ Actif" : "⛔ Inactif");
            headerStatusLabel.setStyle((user.isActive()
                ? "-fx-background-color:#22c55e;" : "-fx-background-color:#ef4444;")
                + "-fx-text-fill:white;-fx-padding:3 12;-fx-background-radius:20;-fx-font-size:12px;");
        }
        if (presenceLabel != null) presenceLabel.setText(user.getPresenceLabel());

        // Info card
        setText(nameLabel,   user.getName());
        setText(emailLabel,  user.getEmail());
        setText(roleLabel,   roleText);
        if (statusLabel != null) {
            statusLabel.setText(user.isActive() ? "Actif" : "Inactif");
            statusLabel.setStyle(user.isActive()
                ? "-fx-text-fill:#22c55e;-fx-font-weight:bold;"
                : "-fx-text-fill:#ef4444;-fx-font-weight:bold;");
        }
        if (lastSeenLabel != null) lastSeenLabel.setText(user.getPresenceLabel());

        // Pre-fill edit form
        if (editNameField != null) editNameField.setText(user.getName());

        // Avatar
        refreshAvatar(user.getProfileImage(), profileImageView, 90);
        refreshAvatar(user.getProfileImage(), editAvatarPreview, 70);
    }

    private void refreshAvatar(String stored, ImageView iv, double size) {
        if (iv == null) return;
        try {
            Image img = null;
            if (stored != null && !stored.isBlank()) {
                Path p = ImageStorage.resolveToPath(stored);
                if (p != null) img = new Image(p.toUri().toString(), size, size, true, true);
            }
            if (img == null) {
                var is = getClass().getResourceAsStream("/com/gamilha/images/logo.jpg");
                if (is != null) img = new Image(is, size, size, true, true);
            }
            if (img != null) {
                iv.setImage(img);
                iv.setClip(new Circle(size / 2, size / 2, size / 2));
            }
        } catch (Exception ignored) {}
    }

    // ── EDIT SECTION ──────────────────────────────────────────────────────
    @FXML
    private void showEditSection() {
        show(editSection);
        hide(passwordSection);
        if (editNameField != null && currentUser != null)
            editNameField.setText(currentUser.getName());
    }

    @FXML private void hideEditSection() { hide(editSection); }

    @FXML
    private void chooseNewAvatar() {
        Stage stage = (Stage) editNameField.getScene().getWindow();
        FileChooser fc = ImageStorage.createAvatarFileChooser();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedAvatarFile = f;
            if (editAvatarLabel != null) editAvatarLabel.setText(f.getName());
            refreshAvatar(f.toURI().toString(), editAvatarPreview, 70);
        }
    }

    @FXML
    private void saveProfile() {
        if (currentUser == null) return;
        clearBanners();

        String name = editNameField != null ? editNameField.getText().trim() : "";
        if (name.isEmpty()) { showError("Le nom ne peut pas être vide."); return; }

        currentUser.setName(name);

        try {
            if (selectedAvatarFile != null) {
                currentUser.setProfileImage(ImageStorage.saveAvatar(selectedAvatarFile));
                selectedAvatarFile = null;
            }
        } catch (IOException e) {
            showError("Erreur upload image : " + e.getMessage()); return;
        }

        // Only update name + avatar (user cannot change email/roles)
        userService.update(currentUser);
        SessionContext.setCurrentUser(currentUser);

        hide(editSection);
        refreshAll(currentUser);
        showSuccess("✅ Profil mis à jour avec succès !");
    }

    // ── PASSWORD SECTION ──────────────────────────────────────────────────
    @FXML
    private void showPasswordSection() {
        show(passwordSection);
        hide(editSection);
        if (oldPwdField     != null) oldPwdField.clear();
        if (newPwdField     != null) newPwdField.clear();
        if (confirmPwdField != null) confirmPwdField.clear();
    }

    @FXML private void hidePasswordSection() { hide(passwordSection); }

    private void updateStrength(String pwd) {
        if (pwdStrengthLabel == null) return;
        int s = 0;
        if (pwd.length() >= 8)               s++;
        if (pwd.matches(".*[A-Z].*"))         s++;
        if (pwd.matches(".*[a-z].*"))         s++;
        if (pwd.matches(".*[0-9].*"))         s++;
        if (pwd.matches(".*[^A-Za-z0-9].*")) s++;
        show(pwdStrengthLabel);
        switch (s) {
            case 0,1 -> { pwdStrengthLabel.setText("⚡ Très faible"); pwdStrengthLabel.setTextFill(Color.web("#ef4444")); }
            case 2   -> { pwdStrengthLabel.setText("⚡ Faible");       pwdStrengthLabel.setTextFill(Color.web("#f97316")); }
            case 3   -> { pwdStrengthLabel.setText("⚡ Moyen");        pwdStrengthLabel.setTextFill(Color.web("#eab308")); }
            case 4   -> { pwdStrengthLabel.setText("✅ Fort");         pwdStrengthLabel.setTextFill(Color.web("#22c55e")); }
            case 5   -> { pwdStrengthLabel.setText("🔒 Très fort");   pwdStrengthLabel.setTextFill(Color.web("#8b5cf6")); }
        }
        if (pwd.isEmpty()) hide(pwdStrengthLabel);
    }

    @FXML
    private void savePassword() {
        if (currentUser == null) return;
        clearBanners();

        String oldPwd  = oldPwdField     != null ? oldPwdField.getText()     : "";
        String newPwd  = newPwdField     != null ? newPwdField.getText()      : "";
        String confirm = confirmPwdField != null ? confirmPwdField.getText()  : "";

        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            showError("Tous les champs mot de passe sont obligatoires."); return;
        }
        if (!PasswordHasher.check(oldPwd, currentUser.getPassword())) {
            showError("Ancien mot de passe incorrect !"); return;
        }
        if (!newPwd.equals(confirm)) {
            showError("Les nouveaux mots de passe ne correspondent pas !"); return;
        }
        // Validate strength
        if (newPwd.length() < 8 || !newPwd.matches(".*[A-Z].*") ||
            !newPwd.matches(".*[0-9].*") || !newPwd.matches(".*[^A-Za-z0-9].*")) {
            showError("Mot de passe trop faible : min 8 car., 1 majuscule, 1 chiffre, 1 symbole."); return;
        }

        currentUser.setPassword(newPwd);
        userService.update(currentUser);
        SessionContext.setCurrentUser(currentUser);

        hide(passwordSection);
        if (oldPwdField != null)     oldPwdField.clear();
        if (newPwdField != null)     newPwdField.clear();
        if (confirmPwdField != null) confirmPwdField.clear();

        showSuccess("✅ Mot de passe modifié avec succès !");
    }

    @FXML
    private void handleDeactivate() {
        if (currentUser == null) return;

        java.time.LocalDate date = deactivationDatePicker.getValue();
        java.sql.Timestamp until = null;
        if (date != null) {
            until = java.sql.Timestamp.valueOf(date.atStartOfDay());
        }

        // Confirmation simple
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Désactiver le compte");
        alert.setHeaderText("Souhaitez-vous désactiver votre compte ?");
        alert.setContentText("Vous serez déconnecté. Votre compte sera réactivé lors de votre prochaine connexion.");

        if (alert.showAndWait().orElse(null) == ButtonType.OK) {
            userService.deactivateAccount(currentUser.getId(), until);
            logout();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    @FXML
    private void deleteUser() {
        if (currentUser == null) return;

        // Custom confirmation dialog
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Supprimer mon compte");

        VBox root = new VBox(18);
        root.setPadding(new Insets(32)); root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#1a1e2e;");

        Label ico  = new Label("⚠");
        ico.setStyle("-fx-font-size:52px;-fx-text-fill:#ef4444;");
        Label msg  = new Label("Êtes-vous sûr de vouloir supprimer\ndéfinitivement votre compte ?");
        msg.setStyle("-fx-text-fill:white;-fx-font-size:16px;-fx-font-weight:bold;");
        msg.setWrapText(true);
        Label warn = new Label("Cette action est irréversible. Toutes vos données seront perdues.");
        warn.setStyle("-fx-text-fill:#fca5a5;-fx-background-color:#2d1111;-fx-padding:12;-fx-background-radius:8;");
        warn.setWrapText(true);

        javafx.scene.layout.HBox btns = new javafx.scene.layout.HBox(12);
        btns.setAlignment(Pos.CENTER);
        Button cancel  = new Button("✕ Annuler");
        cancel.setStyle("-fx-background-color:#374151;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:10 20;-fx-cursor:hand;");
        Button confirm = new Button("🗑 Oui, supprimer");
        confirm.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:10 20;-fx-cursor:hand;-fx-font-weight:bold;");

        cancel.setOnAction(e -> dialog.close());
        confirm.setOnAction(e -> {
            userService.delete(currentUser.getId());
            SessionContext.clear();
            dialog.close();
            MainApp.showLogin();
        });
        btns.getChildren().addAll(cancel, confirm);
        root.getChildren().addAll(ico, msg, warn, btns);
        dialog.setScene(new javafx.scene.Scene(root, 460, 310));
        dialog.show();
    }

    // ── LOGOUT ────────────────────────────────────────────────────────────
    @FXML
    private void logout() {
        if (currentUser != null) userService.logout(currentUser.getId());
        SessionContext.clear();
        MainApp.showLogin();
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void setText(Label l, String v) {
        if (l != null) l.setText(v != null ? v : "—");
    }
    private void show(javafx.scene.Node n) {
        if (n != null) { n.setVisible(true);  n.setManaged(true); }
    }
    private void hide(javafx.scene.Node n) {
        if (n != null) { n.setVisible(false); n.setManaged(false); }
    }
    private void showSuccess(String msg) {
        if (successBanner != null) { successBanner.setText(msg); show(successBanner); }
        // Auto-hide after 4s
        new Thread(() -> {
            try { Thread.sleep(4000); } catch (Exception ignored) {}
            Platform.runLater(() -> hide(successBanner));
        }).start();
    }
    private void showError(String msg) {
        if (errorBanner != null) { errorBanner.setText("⚠ " + msg); show(errorBanner); }
    }
    private void clearBanners() {
        hide(successBanner); hide(errorBanner);
    }
}
