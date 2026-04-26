package com.gamilha.controllers;

import com.gamilha.utils.SessionContext;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class BaseController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        loadUsersList();
    }

    @FXML
    public void loadUsersList() {
        loadFXML("/com/gamilha/fxml/index.fxml");
    }

    @FXML
    public void goToDashboard() {
        System.out.println("Dashboard cliqué");
    }

    @FXML
    public void logout() {
        // ✅ Session : clear au logout
        com.gamilha.utils.SessionContext.clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gamilha/interfaces/login-view.fxml"));
            Scene scene = new Scene(loader.load(), 700, 520);
            URL css = getClass().getResource("/com/gamilha/css/styles.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login - Gamilha");
            stage.setMaximized(false);
            stage.setWidth(700);
            stage.setHeight(520);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de se déconnecter");
        }
    }

    private void loadFXML(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();
            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Erreur de chargement : " + fxmlPath);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}