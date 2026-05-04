package com.gamilha.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

/**
 * NavigationContext — contexte de navigation partagé.
 *
 * Permet aux controllers internes (StreamShow, StreamForm, etc.)
 * de naviguer dans le contentArea de la NavBar SANS casser la navbar.
 *
 * Usage :
 *   // Dans NavBarUserController.goStreams() :
 *   NavigationContext.setContentArea(contentArea);
 *
 *   // Dans StreamShowController.onBack() :
 *   NavigationContext.navigate("User/StreamList.fxml");
 */
public final class NavigationContext {

    private static BorderPane contentArea;
    private static final String BASE = "/com/gamilha/interfaces/";

    private NavigationContext() {}

    /** Enregistrer le contentArea de la NavBar active */
    public static void setContentArea(BorderPane pane) {
        contentArea = pane;
    }

    public static BorderPane getContentArea() {
        return contentArea;
    }

    /**
     * Naviguer vers un FXML dans le contentArea de la navbar.
     * Si aucun contentArea enregistré, utilise MainApp.loadScene() en fallback.
     *
     * @param fxmlPath chemin relatif depuis /com/gamilha/interfaces/
     *                 ex : "User/StreamList.fxml"
     * @return le controller chargé, ou null
     */
    public static <T> T navigate(String fxmlPath) {
        if (contentArea == null) {
            com.gamilha.MainApp.loadScene(fxmlPath);
            return null;
        }
        try {
            String fullPath = BASE + fxmlPath;
            FXMLLoader loader = new FXMLLoader(
                    NavigationContext.class.getResource(fullPath));
            Parent root = loader.load();
            contentArea.setCenter(root);
            return loader.getController();
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback
            com.gamilha.MainApp.loadScene(fxmlPath);
            return null;
        }
    }

    /**
     * Naviguer avec accès au controller (pour passer des données).
     */
    public static <T> T navigateWithController(String fxmlPath) {
        return navigate(fxmlPath);
    }

    /** Vrai si la navbar est active */
    public static boolean hasNavbar() {
        return contentArea != null;
    }

    /** Reset lors du logout */
    public static void clear() {
        contentArea = null;
    }
}
