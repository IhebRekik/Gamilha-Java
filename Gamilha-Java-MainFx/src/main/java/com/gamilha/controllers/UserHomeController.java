package com.gamilha.controllers;

import com.gamilha.entity.Evenement;
import com.gamilha.services.EvenementService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class UserHomeController {

    @FXML
    private GridPane eventsContainer;

    @FXML
    public void initialize() {
        loadEvents();
    }

    private void loadEvents() {

        EvenementService evenementService = new EvenementService();

        eventsContainer.getChildren().clear();
        eventsContainer.setStyle("-fx-padding:20;");
        eventsContainer.setHgap(20);
        eventsContainer.setVgap(20);

        int col = 0;
        int row = 0;

        for (Evenement e : evenementService.afficherEntite()) {

            VBox card = buildCard(e);

            eventsContainer.add(card, col, row);

            col++;

            if (col == 4) { // 🔥 4 cards par ligne
                col = 0;
                row++;
            }
        }
    }

    // 🔥 Méthode PROPRE (en dehors de loadEvents)
    private VBox buildCard(Evenement e) {

        VBox card = new VBox();
        card.setPrefWidth(250);

        card.setStyle("""
            -fx-background-color:#111827;
            -fx-background-radius:16;
            -fx-border-radius:16;
            -fx-border-color:#1f2937;
        """);

        // IMAGE
        ImageView imageView = new ImageView();
        imageView.setFitWidth(250);
        imageView.setFitHeight(140);

        try {
            if (e.getImage() != null && !e.getImage().isEmpty()) {
                imageView.setImage(new Image("file:" + e.getImage()));
            }
        } catch (Exception ignored) {}

        // CONTENT
        VBox content = new VBox(8);
        content.setStyle("-fx-padding:10;");

        Label badge = new Label(e.getJeu());
        badge.setStyle("""
            -fx-background-color:#7c3aed;
            -fx-text-fill:white;
            -fx-padding:3 8;
            -fx-background-radius:8;
        """);

        Label titre = new Label(e.getNom());
        titre.setStyle("-fx-text-fill:white; -fx-font-weight:bold;");

        Label desc = new Label(e.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-text-fill:#9ca3af;");

        Label info = new Label(e.getDateDebut() + " • " + e.getStatut());
        info.setStyle("-fx-text-fill:#6b7280; -fx-font-size:11px;");

        Button btn = new Button("prévu");
        btn.setStyle("""
            -fx-background-color:#06b6d4;
            -fx-text-fill:white;
            -fx-background-radius:10;
        """);

        content.getChildren().addAll(badge, titre, desc, info, btn);

        card.getChildren().addAll(imageView, content);

        return card;
    }
}