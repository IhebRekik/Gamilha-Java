package com.gamilha;

import com.gamilha.entity.User;
import com.gamilha.services.FavoriteVideoService;
import com.gamilha.services.NotificationService;
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

public class MainFX extends Application {

    private static final String BASE = "/com/gamilha/interfaces/coaching/";
    private static BorderPane contentAreaRef;
    private static Button btnFavoritesRef;
    private static Button btnNotificationsRef;

    @Override
    public void start(Stage stage) {
        User user = new User();
        user.setId(2);
        user.setName("Motaz");
        user.setEmail("user@gamilha.com");
        user.setRoles(List.of("ROLE_USER"));
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        SessionContext.setCurrentUser(user);

        Label logo = new Label("Gamilha - Coaching");
        logo.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:16;-fx-font-weight:bold;");

        Label badge = new Label("Mode User - CRUD active");
        badge.setStyle(
                "-fx-text-fill:#4ade80;-fx-font-size:12;-fx-background-color:#052e16;"
                        + "-fx-background-radius:6;-fx-padding:4 10;"
        );

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnPlaylists = navBtn("Playlists", "#7c3aed");
        Button btnVideos = navBtn("Videos", "#0284c7");
        Button btnFavorites = navBtn("Favoris", "#be185d");
        Button btnNotifications = navBtn("Notifications", "#1d4ed8");

        HBox topBar = new HBox(16, logo, badge, spacer, btnPlaylists, btnVideos, btnFavorites, btnNotifications);
        topBar.setStyle("-fx-background-color:#0f172a;-fx-padding:14 20;"
                + "-fx-border-color:#1e293b;-fx-border-width:0 0 1 0;");
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        BorderPane contentArea = new BorderPane();
        contentArea.setId("contentArea");
        contentArea.setStyle("-fx-background-color:#0f172a;");
        contentAreaRef = contentArea;
        btnFavoritesRef = btnFavorites;
        btnNotificationsRef = btnNotifications;

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(contentArea);
        root.setStyle("-fx-background-color:#0f172a;");

        charger(contentArea, "PlaylistList.fxml");

        btnPlaylists.setOnAction(e -> charger(contentArea, "PlaylistList.fxml"));
        btnVideos.setOnAction(e -> charger(contentArea, "VideoList.fxml"));
        btnFavorites.setOnAction(e -> charger(contentArea, "FavoritesList.fxml"));
        btnNotifications.setOnAction(e -> charger(contentArea, "NotificationList.fxml"));

        refreshNavigationBadges();

        Scene scene = new Scene(root, 1280, 800);
        stage.setTitle("Gamilha User - Coaching");
        stage.setMaximized(true);
        stage.setScene(scene);
        stage.show();
    }

    private void charger(BorderPane contentArea, String fxmlFile) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(BASE + fxmlFile));
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

    public static void refreshNavigationBadges() {
        User currentUser = SessionContext.getCurrentUser();
        Integer userId = currentUser != null ? currentUser.getId() : null;

        FavoriteVideoService favoriteVideoService = new FavoriteVideoService();
        NotificationService notificationService = new NotificationService();

        if (btnFavoritesRef != null) {
            btnFavoritesRef.setText("Favoris (" + favoriteVideoService.countFavorites(userId) + ")");
        }

        if (btnNotificationsRef != null) {
            int unreadCount = notificationService.countUnread(userId);
            btnNotificationsRef.setText(
                    unreadCount > 0
                            ? "Notifications (" + unreadCount + ")"
                            : "Notifications"
            );
        }
    }

    public static void navigateFromShell(String fxmlFile) {
        if (contentAreaRef == null) {
            return;
        }

        try {
            Parent view = FXMLLoader.load(MainFX.class.getResource(BASE + fxmlFile));
            contentAreaRef.setCenter(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
