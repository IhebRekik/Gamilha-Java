package com.gamilha;

import com.gamilha.entity.User;
import com.gamilha.utils.NavigationContext;
import com.gamilha.utils.ConnectionManager;
import com.gamilha.utils.SessionContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Point d'entrée unique de Gamilha.
 *
 * Démarrage : login-view.fxml (gestion BCrypt via UserService.login)
 * Après login  :
 *   – ROLE_ADMIN  → interfaces/Admin/NavBar.fxml  (NavBarAdminController.chargerPage)
 *   – ROLE_USER   → interfaces/User/NavBarUser.fxml (NavBarUserController.chargerPage)
 *
 * Navigation interne : MainApp.navigate("Admin/MainView.fxml") ou
 *                      MainApp.navigate("User/StreamList.fxml")
 */
public class MainApp extends Application {

    // Stage partagé pour toute l'application
    public static Stage primaryStage;

    // Feuilles de style globales
    private static final String CORE_CSS_PATH = "/com/gamilha/css/style.css";
    private static final String DASHBOARD_CSS_PATH = "/com/gamilha/styles/gamilha.css";

    // FXML de démarrage (login de Gamilha-Java)
    private static final String LOGIN_FXML = "/com/gamilha/interfaces/login-view.fxml";



    // ── Démarrage ────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Gamilha");
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/com/gamilha/images/gamilha.png"))
        );
        showLogin();
        primaryStage.show();
    }

    @Override
    public void stop() {
        NavigationContext.clear();
        SessionContext.clear();
    }

    // ── Login ────────────────────────────────────────────────────────────

    /**
     * Affiche la page de connexion (petite fenêtre centrée).
     * Appelée au démarrage et après un logout.
     */
    public static void showLogin() {
        try {
            URL fxmlUrl = MainApp.class.getResource(LOGIN_FXML);
            if (fxmlUrl == null) {
                throw new RuntimeException("FXML introuvable : " + LOGIN_FXML);
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(loader.load(), 700, 520);
            applyCSS(scene);
            primaryStage.setResizable(false);
            primaryStage.setMaximized(false);
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Navigation après login ────────────────────────────────────────────

    /**
     * Appelé par LoginController après une authentification réussie.
     * Charge la NavBar adaptée au rôle de l'utilisateur.
     *
     * @param user utilisateur connecté (stocké dans SessionContext)
     */
    public static void openDashboard(User user) {
        SessionContext.setCurrentUser(user);
        String roles = user.getRoles() != null ? user.getRoles() : "";

        if (roles.contains("ROLE_ADMIN")) {
            loadNavBar("/com/gamilha/interfaces/Admin/NavBar.fxml");
        } else {
            loadNavBar("/com/gamilha/interfaces/User/NavBarUser.fxml");
        }

    }

    /** Charge une NavBar (Admin ou User) et maximise la fenêtre. */
    private static void loadNavBar(String navBarPath) {
        try {
            URL url = MainApp.class.getResource(navBarPath);
            if (url == null) {
                throw new RuntimeException("NavBar FXML introuvable : " + navBarPath);
            }
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            applyCSS(scene);
            primaryStage.setMaximized(true);
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setTitle("Gamilha – " + SessionContext.getCurrentUser().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Navigation interne (utilisée par les NavBarControllers) ──────────

    /**
     * Charge une vue dans le stage principal (sans NavBar — pour les pages
     * "standalone" comme le formulaire d'inscription).
     *
     * @param fxmlName chemin relatif depuis /com/gamilha/interfaces/
     *                 ex : "register.fxml"
     */
    public static void loadScene(String fxmlName) {
        try {
            String path = "/com/gamilha/interfaces/" + fxmlName;
            URL url = MainApp.class.getResource(path);
            if (url == null) throw new RuntimeException("FXML introuvable : " + path);
            Parent root = FXMLLoader.load(url);
            Scene scene = new Scene(root);

            applyCSS(scene);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Charge une vue et retourne son contrôleur pour passer des données.
     *
     * @param fxmlName chemin relatif depuis /com/gamilha/interfaces/
     * @return contrôleur de la vue chargée, ou null en cas d'erreur
     */
    public static <T> T loadSceneWithController(String fxmlName) {
        try {
            String path = "/com/gamilha/interfaces/" + fxmlName;
            URL url = MainApp.class.getResource(path);
            if (url == null) throw new RuntimeException("FXML introuvable : " + path);
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            applyCSS(scene);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Alias compatible avec l'ancien MainApp (streams, donations, etc.).
     * Charge depuis /com/gamilha/fxml/ (modules stream/donation/abonnement).
     */
    public static void loadFxml(String fxmlName) {
        try {
            String path = "/com/gamilha/fxml/" + fxmlName;
            URL url = MainApp.class.getResource(path);
            if (url == null) throw new RuntimeException("FXML introuvable : " + path);
            Parent root = FXMLLoader.load(url);
            Scene scene = new Scene(root);
            applyCSS(scene);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Même chose avec retour de contrôleur. */
    public static <T> T loadFxmlWithController(String fxmlName) {
        try {
            String path = "/com/gamilha/fxml/" + fxmlName;
            URL url = MainApp.class.getResource(path);
            if (url == null) throw new RuntimeException("FXML introuvable : " + path);
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            applyCSS(scene);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void applyCSS(Scene scene) {
        try {
            addStylesheetIfExists(scene, CORE_CSS_PATH);
            addStylesheetIfExists(scene, DASHBOARD_CSS_PATH);
        } catch (Exception ignored) {}
    }

    private static void addStylesheetIfExists(Scene scene, String cssPath) {
        URL css = MainApp.class.getResource(cssPath);
        if (css == null) {
            return;
        }
        String external = css.toExternalForm();
        if (!scene.getStylesheets().contains(external)) {
            scene.getStylesheets().add(external);
        }
    }

    // ── Lancement ─────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}
