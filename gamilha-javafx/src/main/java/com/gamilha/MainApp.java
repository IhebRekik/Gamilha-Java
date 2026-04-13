package com.gamilha;

import com.gamilha.controller.NavBarUserController;
import com.gamilha.model.User;
import com.gamilha.service.SessionContext;
import com.gamilha.service.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Point d'entrée — lance la NavBar User avec l'utilisateur connecté.
 *
 * ⚠️ Remplace l'ID 1 par l'ID de l'utilisateur réellement connecté.
 * Pour le mode admin, charge NavBarAdmin.fxml à la place.
 *
 * Lancement : mvn clean javafx:run
 */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // ── Simuler l'utilisateur connecté ──────────────────────────────
        UserService userService = new UserService();
        User currentUser = userService.findById(1); // ← Remplace par ton ID
        SessionContext.setCurrentUser(currentUser);
        // ────────────────────────────────────────────────────────────────

        // ── Choisir la vue : User ou Admin ──────────────────────────────
        // Pour USER :
        String fxml = "/com/gamilha/interfaces/User/NavBarUser.fxml";
        // Pour ADMIN : décommenter la ligne suivante
        // String fxml = "/com/gamilha/interfaces/Admin/NavBarAdmin.fxml";

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Scene scene = new Scene(loader.load(), 1200, 760);
        scene.getStylesheets().add(
            getClass().getResource("/com/gamilha/styles.css").toExternalForm());

        stage.setTitle("Gamilha");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setMinWidth(900);
        stage.setMinHeight(600);

        InputStream icon = getClass().getResourceAsStream("/com/gamilha/images/icon.png");
        if (icon != null) stage.getIcons().add(new Image(icon));

        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}
