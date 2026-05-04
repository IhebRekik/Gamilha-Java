package com.gamilha.controllers.coaching;

import com.gamilha.MainFX;
import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.User;
import com.gamilha.services.FavoriteVideoService;
import com.gamilha.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class FavoritesController {

    private static final String BASE = "/com/gamilha/interfaces/coaching/";

    @FXML private FlowPane cardsPane;
    @FXML private Label lblCount;

    private final FavoriteVideoService favoriteService = new FavoriteVideoService();

    @FXML
    public void initialize() {
        loadFavorites();
    }

    private void loadFavorites() {
        Integer userId = getCurrentUserId();
        List<CoachingVideo> favorites = favoriteService.getFavoriteVideos(userId);
        lblCount.setText(favorites.size() + " video(s) favorite(s)");
        cardsPane.getChildren().clear();

        if (favorites.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("❤");
            icon.setStyle("-fx-text-fill:#f472b6;-fx-font-size:42;");
            Label message = new Label("Aucune video en favoris pour le moment.");
            message.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:14;");
            empty.getChildren().addAll(icon, message);
            cardsPane.getChildren().add(empty);
            return;
        }

        for (CoachingVideo video : favorites) {
            cardsPane.getChildren().add(createFavoriteCard(video));
        }
    }

    private VBox createFavoriteCard(CoachingVideo video) {
        VBox card = new VBox(12);
        card.setPrefWidth(300);
        card.setStyle(
                "-fx-background-color:#1e293b;-fx-background-radius:14;"
                        + "-fx-padding:16;-fx-border-color:#334155;-fx-border-radius:14;"
        );

        Label title = new Label(video.getTitre());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill:white;-fx-font-size:16;-fx-font-weight:bold;");

        Label meta = new Label(
                (video.getPlaylist() != null ? video.getPlaylist().getTitle() : "Sans playlist")
                        + "  |  " + video.getDurationFormatted()
        );
        meta.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:12;");

        String descriptionText = video.getDescription() != null && !video.getDescription().isBlank()
                ? video.getDescription()
                : "Aucune description";
        Label description = new Label(descriptionText);
        description.setWrapText(true);
        description.setStyle("-fx-text-fill:#cbd5e1;-fx-font-size:12;");

        Button open = new Button("Voir la video");
        open.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;-fx-background-radius:8;");
        open.setOnAction(e -> {
            CoachingVideoController.setSelectedVideoStatic(video);
            navigateTo("VideoShow.fxml");
        });

        Button remove = new Button("Retirer");
        remove.setStyle("-fx-background-color:#be123c;-fx-text-fill:white;-fx-background-radius:8;");
        remove.setOnAction(e -> {
            favoriteService.removeFavorite(getCurrentUserId(), video.getId());
            MainFX.refreshNavigationBadges();
            loadFavorites();
        });

        HBox actions = new HBox(10, open, remove);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, meta, description, actions);
        return card;
    }

    private Integer getCurrentUserId() {
        User user = SessionContext.getCurrentUser();
        return user != null ? user.getId() : null;
    }

    private void navigateTo(String fxmlFile) {
        try {
            Node anchor = cardsPane;
            if (anchor == null) {
                return;
            }
            Parent view = FXMLLoader.load(getClass().getResource(BASE + fxmlFile));
            BorderPane root = (BorderPane) anchor.getScene().getRoot();
            BorderPane cArea = (BorderPane) root.lookup("#contentArea");
            if (cArea != null) {
                cArea.setCenter(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
