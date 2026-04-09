package com.gamilha.controllers.video;

import com.gamilha.entity.CoachingVideo;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

public class ShowVideoController {

    @FXML private Label lblTitre;
    @FXML private Label lblDescription;
    @FXML private Label lblUrl;
    @FXML private Label lblNiveau;
    @FXML private Label lblDuration;
    @FXML private Label lblPremium;
    @FXML private Label lblPlaylist;
    @FXML private Button btnRetour;

    private CoachingVideo video;

    @FXML
    public void initialize() {
        btnRetour.setOnAction(e -> retour());
    }

    public void setVideo(CoachingVideo video) {
        this.video = video;
        lblTitre.setText(video.getTitre());
        lblDescription.setText(video.getDescription() != null ? video.getDescription() : "-");
        lblUrl.setText(video.getUrl());
        lblNiveau.setText(video.getNiveau());
        lblDuration.setText(video.getDurationFormatted());
        lblPremium.setText(video.isPremium() ? "✅ Oui (Premium)" : "❌ Non (Gratuit)");
        lblPlaylist.setText(video.getPlaylist() != null ? video.getPlaylist().getTitle() : "-");
    }

    private void retour() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/com/gamilha/interfaces/User/VideoUser.fxml"));
            BorderPane root = (BorderPane) lblTitre.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            contentArea.setCenter(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
