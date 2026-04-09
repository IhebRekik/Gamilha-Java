package com.gamilha.controllers;


import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    private final UserService authService = new UserService();

    @FXML
    public void onLogin(ActionEvent event) {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email et mot de passe obligatoires.");
            return;
        }

        try {
            User user = authService.login(email, password);
            if (user == null) {
                errorLabel.setText("Adresse email ou mot de passe incorrect.");
                return;
            }

            SessionContext.setCurrentUser(user);
            if(user.getRoles().contains("ROLE_ADMIN")){
                openDashboardAdmin((Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow());
            }
            if(user.getRoles().contains("ROLE_USER")){
                openDashboardUser((Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow());
            }

        } catch (Exception e) {
            errorLabel.setText("Erreur de connexion: " + e.getMessage());
        }
    }
    private void openDashboardAdmin(Stage stage) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/Admin/NavBar.fxml")
            );

            Parent root = loader.load();

            Scene scene = new Scene(root, 1400, 900);

            scene.getStylesheets().add(
                    getClass().getResource("/com/gamilha/interfaces/css/gamilha.css").toExternalForm()
            );
            stage.setMaximized(true);
            stage.setScene(scene);
            stage.setTitle("Gamilha - Dashboard");
            stage.show();

        } catch (Exception e) {
            errorLabel.setText("Impossible d'ouvrir le dashboard: " + e.getMessage());
        }
    }private void openDashboardUser(Stage stage) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/NavBarUser.fxml")
            );

            Parent root = loader.load();

            Scene scene = new Scene(root, 1400, 900);

            scene.getStylesheets().add(
                    getClass().getResource("/com/gamilha/interfaces/css/gamilha.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("Gamilha - Dashboard");
            stage.show();

        } catch (Exception e) {
            errorLabel.setText("Impossible d'ouvrir le dashboard: " + e.getMessage());
        }
    }
}
