package com.gamilha.controllers.video;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.services.CoachingVideoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.stream.Collectors;

/**
 * Controller Admin pour les CoachingVideos — lecture seule.
 * L'admin peut consulter toutes les vidéos et les filtrer, sans modification possible.
 */
public class CoachingVideoAdminController {

    @FXML private TextField        searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterPremium;
    @FXML private GridPane         tableGrid;

    private final CoachingVideoService service = new CoachingVideoService();
    private ObservableList<CoachingVideo> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        masterData.addAll(service.afficherVideos());

        // Filtre niveau
        filterNiveau.getItems().add("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream().map(CoachingVideo::getNiveau)
                        .filter(n -> n != null && !n.isEmpty()).distinct().toList()
        );
        filterNiveau.setValue("Tous");

        // Filtre premium
        filterPremium.getItems().addAll("Tous", "Premium", "Gratuit");
        filterPremium.setValue("Tous");

        buildTable(masterData);

        searchInput.textProperty().addListener((obs, o, n) -> filter());
        filterNiveau.valueProperty().addListener((obs, o, n) -> filter());
        filterPremium.valueProperty().addListener((obs, o, n) -> filter());
    }

    private void buildTable(ObservableList<CoachingVideo> data) {
        tableGrid.getChildren().clear();
        tableGrid.getColumnConstraints().clear();

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(18);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(24);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(12);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(12);
        ColumnConstraints c5 = new ColumnConstraints(); c5.setPercentWidth(9);
        ColumnConstraints c6 = new ColumnConstraints(); c6.setPercentWidth(25);
        tableGrid.getColumnConstraints().addAll(c1, c2, c3, c4, c5, c6);

        addHeader("Titre",       0);
        addHeader("Description", 1);
        addHeader("Niveau",      2);
        addHeader("Durée",       3);
        addHeader("Premium",     4);
        addHeader("Playlist",    5);

        int row = 1;
        for (CoachingVideo v : data) {
            String color = row % 2 == 0 ? "#020617" : "#020c1b";
            addCell(v.getTitre(),       row, 0, color);
            addCell(v.getDescription(), row, 1, color);
            addCell(v.getNiveau(),      row, 2, color);
            addCell(v.getDurationFormatted(), row, 3, color);
            addCell(v.isPremium() ? "✅ Premium" : "❌ Gratuit", row, 4, color);
            addCell(v.getPlaylist() != null ? v.getPlaylist().getTitle() : "-", row, 5, color);
            row++;
        }

        if (data.isEmpty()) {
            Label empty = new Label("Aucune vidéo disponible.");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-padding:20;-fx-font-size:14;");
            tableGrid.add(empty, 0, 1, 6, 1);
        }
    }

    private void addHeader(String text, int col) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-background-color:#020617;-fx-text-fill:#e2e8f0;-fx-font-weight:bold;"
                + "-fx-font-size:15;-fx-padding:14;-fx-border-color:#1e293b;");
        tableGrid.add(label, col, 0);
    }

    private void addCell(String text, int row, int col, String color) {
        Label label = new Label(text != null ? text : "");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill:#cbd5f5;-fx-padding:10;-fx-border-color:#1e293b;-fx-background-color:" + color + ";");
        tableGrid.add(label, col, row);
    }

    private void filter() {
        String search  = searchInput.getText() == null ? "" : searchInput.getText().toLowerCase();
        String niveau  = filterNiveau.getValue();
        String premium = filterPremium.getValue();

        ObservableList<CoachingVideo> filtered = masterData.stream()
                .filter(v -> v.getTitre().toLowerCase().contains(search)
                        || (v.getDescription() != null && v.getDescription().toLowerCase().contains(search)))
                .filter(v -> niveau == null || niveau.equals("Tous") || niveau.equals(v.getNiveau()))
                .filter(v -> {
                    if ("Premium".equals(premium)) return v.isPremium();
                    if ("Gratuit".equals(premium))  return !v.isPremium();
                    return true;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        buildTable(filtered);
    }
}
