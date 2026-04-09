package com.gamilha;

import com.gamilha.entity.User;
import com.gamilha.utils.SessionContext;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════
 *  TEST — Interface USER / FRONT  (Coaching & Playlist)
 *  Charge directement PlaylistList.fxml (CRUD complet)
 *  Navigation inter-pages opérationnelle via #contentArea
 * ══════════════════════════════════════════════════════
 */
public class MainUser extends Application {

    private static final String BASE = "/com/gamilha/interfaces/coaching/";

    @Override
    public void start(Stage stage) throws Exception {

        // ── 1. Utilisateur User fictif ──────────────────────────────────────
        User user = new User();
        user.setId(2);
        user.setName("Motaz");
        user.setEmail("user@gamilha.com");
        user.setRoles(List.of("ROLE_USER"));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        SessionContext.setCurrentUser(user);

        // ── 2. Barre de navigation minimale ─────────────────────────────────
        Label logo = new Label("🎮 Gamilha — Coaching");
        logo.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:16;-fx-font-weight:bold;");

        Label badge = new Label("✅ Mode User — CRUD activé");
        badge.setStyle("-fx-text-fill:#4ade80;-fx-font-size:12;-fx-background-color:#052e16;"
                + "-fx-background-radius:6;-fx-padding:4 10;");

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnPlaylists = navBtn("🎓 Playlists", "#7c3aed");
        Button btnVideos    = navBtn("🎬 Vidéos",    "#0284c7");

        HBox topBar = new HBox(16, logo, badge, spacer, btnPlaylists, btnVideos);
        topBar.setStyle("-fx-background-color:#0f172a;-fx-padding:14 20;"
                + "-fx-border-color:#1e293b;-fx-border-width:0 0 1 0;");
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── 3. Zone de contenu (identifiée #contentArea) ────────────────────
        BorderPane contentArea = new BorderPane();
        contentArea.setId("contentArea");
        contentArea.setStyle("-fx-background-color:#0f172a;");

        // ── 4. Racine principale ────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(contentArea);
        root.setStyle("-fx-background-color:#0f172a;");

        // ── 5. Charger PlaylistList.fxml par défaut ─────────────────────────
        charger(contentArea, "PlaylistList.fxml");

        // ── 6. Handlers des boutons de navigation ───────────────────────────
        btnPlaylists.setOnAction(e -> charger(contentArea, "PlaylistList.fxml"));
        btnVideos.setOnAction(e    -> charger(contentArea, "VideoList.fxml"));

        // ── 7. Afficher ─────────────────────────────────────────────────────
        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("🎮 Gamilha User — Coaching");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }

    private void charger(BorderPane contentArea, String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource(BASE + fxmlFile)
            );
            contentArea.setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Button navBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-background-radius:7;-fx-padding:8 18;-fx-font-size:13;");
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
