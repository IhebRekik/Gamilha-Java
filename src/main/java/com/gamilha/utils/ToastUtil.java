package com.gamilha.utils;

import com.gamilha.MainApp;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class ToastUtil {

    private ToastUtil() {}

    public static void show(String title, String message) {
        show(title, message, 2800);
    }

    public static void show(String title, String message, int durationMs) {
        Platform.runLater(() -> {
            Stage owner = MainApp.primaryStage;
            if (owner == null || owner.getScene() == null) return;

            Popup popup = new Popup();
            popup.setAutoFix(true);
            popup.setAutoHide(true);

            Label titleLbl = new Label(title);
            titleLbl.setStyle("-fx-text-fill:#f8fafc;-fx-font-size:13px;-fx-font-weight:bold;");

            Label msgLbl = new Label(message);
            msgLbl.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:12px;");
            msgLbl.setWrapText(true);
            msgLbl.setMaxWidth(320);

            VBox box = new VBox(6, titleLbl, msgLbl);
            box.setPadding(new Insets(12));
            box.setStyle("-fx-background-color:rgba(15,23,42,0.95);" +
                    "-fx-border-color:#8b5cf6;-fx-border-width:1;" +
                    "-fx-background-radius:12;-fx-border-radius:12;" +
                    "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.45),16,0,0,4);");
            box.setOpacity(0);

            popup.getContent().add(box);
            popup.show(owner);

            Scene scene = owner.getScene();
            double x = owner.getX() + scene.getWidth() - 370;
            double y = owner.getY() + 90;
            popup.setX(Math.max(owner.getX() + 20, x));
            popup.setY(Math.max(owner.getY() + 20, y));

            FadeTransition fadeIn = new FadeTransition(Duration.millis(180), box);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            PauseTransition wait = new PauseTransition(Duration.millis(durationMs));
            wait.setOnFinished(evt -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(220), box);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(end -> popup.hide());
                fadeOut.play();
            });
            wait.play();
        });
    }
}
