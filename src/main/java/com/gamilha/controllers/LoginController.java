package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.SavedCredentials;
import com.gamilha.utils.SessionContext;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;

public class LoginController {

    @FXML private TextField     emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private Button        eyeButton;
    @FXML private CheckBox      rememberMeCheckbox;
    @FXML private Label         errorLabel;
    @FXML private Label         lockCountdown;
    @FXML private Button        loginButton;

    private final UserService userService = new UserService();
    private Timeline lockTimer;
    private boolean  passwordShown = false;
    private ContextMenu emailDropdown;

    @FXML
    public void initialize() {
        // Password visibility
        if (passwordVisible != null) {
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            if (passwordField != null)
                passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());
        }
        if (errorLabel    != null) { errorLabel.setVisible(false);    errorLabel.setManaged(false); }
        if (lockCountdown != null) { lockCountdown.setVisible(false); lockCountdown.setManaged(false); }

        // Autocomplete dropdown
        emailDropdown = new ContextMenu();

        // Load saved email
        List<String> saved = SavedCredentials.getSavedEmails();
        if (!saved.isEmpty() && emailField != null) {
            emailField.setText(saved.get(0));
            if (rememberMeCheckbox != null) rememberMeCheckbox.setSelected(true);
            // Auto-fill password for saved email
            String savedPwd = SavedCredentials.getPasswordFor(saved.get(0));
            if (!savedPwd.isEmpty() && passwordField != null) {
                passwordField.setText(savedPwd);
            }
        }

        // Email field listeners for autocomplete
        if (emailField != null) {
            emailField.textProperty().addListener((obs, old, nv) -> showEmailSuggestions(nv));
            emailField.focusedProperty().addListener((obs, old, focused) -> {
                if (focused) showEmailSuggestions(emailField.getText());
                else Platform.runLater(() -> emailDropdown.hide());
            });
            emailField.setOnAction(e -> handleLogin());
        }
        if (passwordField != null) passwordField.setOnAction(e -> handleLogin());
    }

    // ── Autocomplete ──────────────────────────────────────────────────────
    private void showEmailSuggestions(String filter) {
        List<String> all = SavedCredentials.getSavedEmails();
        if (all.isEmpty()) { emailDropdown.hide(); return; }

        List<String> matches = all.stream()
                .filter(e -> filter == null || filter.isEmpty()
                        || e.toLowerCase().contains(filter.toLowerCase()))
                .toList();

        if (matches.isEmpty()) { emailDropdown.hide(); return; }

        emailDropdown.getItems().clear();

        for (String email : matches) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 14, 8, 14));

            // Avatar circle
            Label avatar = new Label("👤");
            avatar.setStyle("-fx-background-color:#7c3aed;-fx-background-radius:999;" +
                    "-fx-padding:3 7;-fx-text-fill:white;-fx-font-size:11px;");

            VBox info = new VBox(2);
            Label emailLbl = new Label(email);
            emailLbl.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:bold;");
            String pwd = SavedCredentials.getPasswordFor(email);
            Label pwdHint = new Label(pwd.isEmpty() ? "" : "••••••••");
            pwdHint.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");
            info.getChildren().addAll(emailLbl, pwdHint);

            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button del = new Button("✕");
            del.setStyle("-fx-background-color:transparent;-fx-text-fill:#ef4444;" +
                    "-fx-cursor:hand;-fx-font-size:11px;-fx-padding:2 5;");
            del.setOnAction(ev -> {
                SavedCredentials.remove(email);
                emailDropdown.hide();
                ev.consume();
            });

            row.getChildren().addAll(avatar, info, spacer, del);
            row.setStyle("-fx-background-color:transparent;");

            CustomMenuItem item = new CustomMenuItem(row, false);
            item.setOnAction(ev -> {
                emailField.setText(email);
                emailDropdown.hide();
                String p = SavedCredentials.getPasswordFor(email);
                if (!p.isEmpty() && passwordField != null) {
                    passwordField.setText(p);
                    if (passwordVisible != null) passwordVisible.setText(p);
                }
                Platform.runLater(() -> { if (loginButton != null) loginButton.requestFocus(); });
            });
            emailDropdown.getItems().add(item);
        }

        emailDropdown.setStyle("-fx-background-color:#1a1e2e;-fx-border-color:#374151;" +
                "-fx-border-radius:10;-fx-background-radius:10;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),16,0,0,4);");

        if (emailField.getScene() != null) {
            emailDropdown.show(emailField, javafx.geometry.Side.BOTTOM, 0, 2);
        }
    }

    // ── Eye toggle ────────────────────────────────────────────────────────
    @FXML
    private void toggleEye() {
        passwordShown = !passwordShown;
        if (passwordField != null && passwordVisible != null) {
            passwordField.setVisible(!passwordShown);
            passwordField.setManaged(!passwordShown);
            passwordVisible.setVisible(passwordShown);
            passwordVisible.setManaged(passwordShown);
        }
        if (eyeButton != null) eyeButton.setText(passwordShown ? "🙈" : "👁");
    }

    // ── Login ─────────────────────────────────────────────────────────────
    @FXML
    private void handleLogin() {
        String email = emailField    != null ? emailField.getText().trim() : "";
        String pwd   = passwordField != null ? passwordField.getText()     : "";

        hideError();

        if (email.isEmpty() || pwd.isEmpty()) {
            showError("Veuillez remplir l'email et le mot de passe."); return;
        }

        try {
            User user = userService.login(email, pwd);

            if (user == null) { showError("Email ou mot de passe incorrect."); return; }
            if (!user.isActive()) { showError("Votre compte est désactivé."); return; }
            if (user.getBanUntil() != null && !user.getBanUntil().isEmpty()) {
                showError("Compte banni jusqu'au : " + user.getBanUntil()); return;
            }

            // Save credentials if "remember me"
            if (rememberMeCheckbox != null && rememberMeCheckbox.isSelected()) {
                SavedCredentials.save(email, pwd);
            }

            SessionContext.setCurrentUser(user);
            MainApp.openDashboard(user);

        } catch (RuntimeException re) {
            String msg = re.getMessage();
            if (msg != null && msg.startsWith("LOCKED:")) {
                long secs = Long.parseLong(msg.split(":")[1]);
                startLockCountdown(secs);
            } else {
                showError(msg != null ? msg : "Erreur de connexion.");
            }
        }
    }

    private void startLockCountdown(long seconds) {
        if (loginButton != null) loginButton.setDisable(true);
        showError("Trop de tentatives ! Compte bloqué temporairement.");
        if (lockCountdown != null) { lockCountdown.setVisible(true); lockCountdown.setManaged(true); }
        final long[] rem = {seconds};
        if (lockTimer != null) lockTimer.stop();
        lockTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            rem[0]--;
            if (lockCountdown != null) lockCountdown.setText("⏳ Réessayez dans : " + rem[0] + "s");
            if (rem[0] <= 0) {
                lockTimer.stop();
                if (loginButton != null) loginButton.setDisable(false);
                if (lockCountdown != null) { lockCountdown.setVisible(false); lockCountdown.setManaged(false); }
                hideError();
            }
        }));
        lockTimer.setCycleCount((int) seconds);
        lockTimer.play();
    }

    // ── Forgot password ───────────────────────────────────────────────────
    @FXML
    private void handleForgotPassword() {
        Stage dialog = new Stage();
        dialog.setTitle("Réinitialisation du mot de passe");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:#1a1e2e;");

        Label title = new Label("🔑 Mot de passe oublié");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#a78bfa"));

        // Container for steps
        StackPane container = new StackPane();
        
        // --- STEP 1: EMAIL ---
        VBox step1 = new VBox(15);
        step1.setAlignment(Pos.CENTER);
        Label info1 = new Label("Entrez votre email pour recevoir un code OTP.");
        info1.setTextFill(Color.web("#94a3b8"));
        TextField emailInput = new TextField();
        emailInput.setPromptText("votre@email.com");
        emailInput.setStyle("-fx-background-color:#0d1117;-fx-text-fill:white;-fx-border-color:#374151;-fx-border-radius:8; -fx-padding:10;");
        Button next1 = new Button("Suivant ➔");
        next1.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:10 25;-fx-cursor:hand;-fx-font-weight:bold;");
        Label error1 = new Label(); error1.setTextFill(Color.web("#ef4444")); error1.setVisible(false);
        step1.getChildren().addAll(info1, emailInput, next1, error1);

        // --- STEP 2: OTP ---
        VBox step2 = new VBox(15);
        step2.setAlignment(Pos.CENTER);
        step2.setVisible(false);
        Label info2 = new Label("Code envoyé ! Saisissez l'OTP et le nouveau mot de passe.");
        info2.setTextFill(Color.web("#94a3b8"));
        TextField otpInput = new TextField();
        otpInput.setPromptText("Code OTP");
        otpInput.setStyle("-fx-background-color:#0d1117;-fx-text-fill:white;-fx-border-color:#374151;-fx-border-radius:8; -fx-padding:10;");
        PasswordField newPwdInput = new PasswordField();
        newPwdInput.setPromptText("Nouveau mot de passe");
        newPwdInput.setStyle("-fx-background-color:#0d1117;-fx-text-fill:white;-fx-border-color:#374151;-fx-border-radius:8; -fx-padding:10;");
        Button submit = new Button("Réinitialiser ✓");
        submit.setStyle("-fx-background-color:#22c55e;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:10 25;-fx-cursor:hand;-fx-font-weight:bold;");
        Label error2 = new Label(); error2.setTextFill(Color.web("#ef4444")); error2.setVisible(false);
        step2.getChildren().addAll(info2, otpInput, newPwdInput, submit, error2);

        container.getChildren().addAll(step1, step2);

        next1.setOnAction(e -> {
            String email = emailInput.getText().trim();
            if (email.isEmpty()) { error1.setText("Email requis."); error1.setVisible(true); return; }
            next1.setDisable(true); next1.setText("Envoi...");
            new Thread(() -> {
                boolean ok = userService.sendResetOtp(email);
                Platform.runLater(() -> {
                    if (ok) {
                        step1.setVisible(false);
                        step2.setVisible(true);
                    } else {
                        error1.setText("Email introuvable."); error1.setVisible(true);
                        next1.setDisable(false); next1.setText("Suivant ➔");
                    }
                });
            }).start();
        });

        submit.setOnAction(e -> {
            String email = emailInput.getText().trim();
            String otp = otpInput.getText().trim();
            String pwd = newPwdInput.getText();
            if (otp.isEmpty() || pwd.isEmpty()) { error2.setText("Champs requis."); error2.setVisible(true); return; }
            submit.setDisable(true); submit.setText("Validation...");
            new Thread(() -> {
                boolean ok = userService.resetPasswordWithOtp(email, otp, pwd);
                Platform.runLater(() -> {
                    if (ok) {
                        title.setText("✅ Succès !");
                        info2.setText("Votre mot de passe a été mis à jour.");
                        otpInput.setVisible(false); newPwdInput.setVisible(false); submit.setVisible(false);
                        Timeline close = new Timeline(new KeyFrame(Duration.seconds(2), ev -> dialog.close()));
                        close.play();
                    } else {
                        error2.setText("Code OTP incorrect."); error2.setVisible(true);
                        submit.setDisable(false); submit.setText("Réinitialiser ✓");
                    }
                });
            }).start();
        });

        root.getChildren().addAll(title, container);
        dialog.setScene(new Scene(root, 450, 400));
        dialog.show();
    }

    // ── Register ──────────────────────────────────────────────────────────
    @FXML
    private void handleRegister() {
        try {
            URL url = getClass().getResource("/com/gamilha/interfaces/register.fxml");
            if (url == null) { showError("register.fxml introuvable"); return; }
            FXMLLoader loader = new FXMLLoader(url);
            Stage stage = new Stage();
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
            stage.setTitle("Créer un compte - Gamilha");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) { showError(e.getMessage()); }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText("⚠  " + msg);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }
}
