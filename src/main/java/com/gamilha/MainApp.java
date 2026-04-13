package com.gamilha;

import com.gamilha.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        loadScene("StreamList.fxml");
        primaryStage.setTitle("🎮 Gamilha — Streams & Donations");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    @Override
    public void stop() { DatabaseConnection.close(); }

    /** Charge une scène simple (pas de passage de données) */
    public static void loadScene(String fxmlName) {
        try {
            String path = "/com/gamilha/fxml/" + fxmlName;
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(path));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                MainApp.class.getResource("/com/gamilha/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Charge une scène et retourne le contrôleur pour passer des données.
     * Utilisation :
     *   StreamShowController ctrl = MainApp.loadSceneWithController("StreamShow.fxml");
     *   ctrl.setStream(monStream);
     */
    public static <T> T loadSceneWithController(String fxmlName) {
        try {
            String path = "/com/gamilha/fxml/" + fxmlName;
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(path));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1280, 800);
            scene.getStylesheets().add(
                MainApp.class.getResource("/com/gamilha/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) { launch(args); }
}
