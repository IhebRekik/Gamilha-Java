package com.gamilha.util;

import javafx.scene.control.*;
import java.util.Optional;

public class AlertUtil {

    public static void showSuccess(String title, String msg) {
        show(Alert.AlertType.INFORMATION, title, msg);
    }
    public static void showError(String title, String msg) {
        show(Alert.AlertType.ERROR, title, msg);
    }
    public static void showWarning(String title, String msg) {
        show(Alert.AlertType.WARNING, title, msg);
    }

    /** Retourne true si l'utilisateur clique OK */
    public static boolean showConfirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        applyStyle(a);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    private static void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        applyStyle(a);
        a.showAndWait();
    }

    private static void applyStyle(Alert a) {
        try {
            a.getDialogPane().getStylesheets().add(
                AlertUtil.class.getResource("/com/gamilha/css/style.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}
