package com.gamilha.controllers.playlist;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.services.CoachingVideoService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.ColumnConstraints;

import java.util.List;

public class ShowPlaylistAdminController {

    @FXML private Label lblTitre;
    @FXML private Label lblDescription;
    @FXML private Label lblNiveau;
    @FXML private Label lblCategorie;
    @FXML private Label lblDate;
    @FXML private GridPane videosGrid;
    @FXML private Button btnRetour;

    private final CoachingVideoService videoService = new CoachingVideoService();
    private Playlist playlist;

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
        afficherDetails();
        chargerVideos();
    }

    @FXML
    public void initialize() {
        if (btnRetour != null) {
            btnRetour.setOnAction(e -> retour());
        }
    }

    private void afficherDetails() {
        lblTitre.setText(playlist.getTitle());
        lblDescription.setText(playlist.getDescription() != null ? playlist.getDescription() : "-");
        lblNiveau.setText(playlist.getNiveau() != null ? playlist.getNiveau() : "-");
        lblCategorie.setText(playlist.getCategorie() != null ? playlist.getCategorie() : "-");
        lblDate.setText(playlist.getCreatedAt() != null ? playlist.getCreatedAt().toString() : "-");
    }

    private void chargerVideos() {
        List<CoachingVideo> videos = videoService.afficherVideosByPlaylist(playlist.getId());

        videosGrid.getChildren().clear();
        videosGrid.getColumnConstraints().clear();

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(30);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(30);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(15);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(15);
        ColumnConstraints c5 = new ColumnConstraints(); c5.setPercentWidth(10);
        videosGrid.getColumnConstraints().addAll(c1, c2, c3, c4, c5);

        addVideoHeader("Titre", 0);
        addVideoHeader("Description", 1);
        addVideoHeader("Niveau", 2);
        addVideoHeader("Durée", 3);
        addVideoHeader("Premium", 4);

        int row = 1;
        for (CoachingVideo v : videos) {
            String color = row % 2 == 0 ? "#020617" : "#020c1b";
            addVideoCell(v.getTitre(), row, 0, color);
            addVideoCell(v.getDescription(), row, 1, color);
            addVideoCell(v.getNiveau(), row, 2, color);
            addVideoCell(v.getDurationFormatted(), row, 3, color);
            addVideoCell(v.isPremium() ? "✅ Oui" : "❌ Non", row, 4, color);
            row++;
        }

        if (videos.isEmpty()) {
            Label empty = new Label("Aucune vidéo dans cette playlist.");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-padding:15;");
            videosGrid.add(empty, 0, 1, 5, 1);
        }
    }

    private void addVideoHeader(String text, int col) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-background-color:#020617;-fx-text-fill:#e2e8f0;-fx-font-weight:bold;"
                + "-fx-font-size:13;-fx-padding:12;-fx-border-color:#1e293b;");
        videosGrid.add(label, col, 0);
    }

    private void addVideoCell(String text, int row, int col, String color) {
        Label label = new Label(text != null ? text : "");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill:#cbd5f5;-fx-padding:8;-fx-border-color:#1e293b;-fx-background-color:" + color + ";");
        videosGrid.add(label, col, row);
    }

    private void retour() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/com/gamilha/interfaces/Admin/PlaylistAdmin.fxml"));
            BorderPane root = (BorderPane) lblTitre.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            contentArea.setCenter(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
