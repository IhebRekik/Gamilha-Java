package com.gamilha.controllers.playlist;

import com.gamilha.entity.Playlist;
import com.gamilha.services.PlaylistService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.stream.Collectors;

public class PlaylistAdminController {

    @FXML private TextField searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private GridPane tableGrid;

    private final PlaylistService service = new PlaylistService();
    private ObservableList<Playlist> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        masterData.addAll(service.afficherPlaylists());

        filterNiveau.getItems().add("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream().map(Playlist::getNiveau).distinct().toList()
        );
        filterNiveau.setValue("Tous");

        buildTable(masterData);

        searchInput.textProperty().addListener((obs, o, n) -> filter());
        filterNiveau.valueProperty().addListener((obs, o, n) -> filter());
    }

    private void buildTable(ObservableList<Playlist> data) {
        tableGrid.getChildren().clear();
        tableGrid.getColumnConstraints().clear();

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(25);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(35);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(15);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(15);
        ColumnConstraints c5 = new ColumnConstraints(); c5.setPercentWidth(10);
        tableGrid.getColumnConstraints().addAll(c1, c2, c3, c4, c5);

        addHeader("Titre", 0);
        addHeader("Description", 1);
        addHeader("Niveau", 2);
        addHeader("Catégorie", 3);
        addHeader("Actions", 4);

        int row = 1;
        for (Playlist p : data) {
            String color = row % 2 == 0 ? "#020617" : "#020c1b";
            addCell(p.getTitle(), row, 0, color);
            addCell(p.getDescription(), row, 1, color);
            addCell(p.getNiveau(), row, 2, color);
            addCell(p.getCategorie(), row, 3, color);

            HBox actions = new HBox(8);
            actions.setStyle("-fx-alignment:center-left;-fx-background-color:" + color + ";-fx-padding:8;");

            Button voir = new Button("👁 Voir");
            voir.setStyle("-fx-background-color:#2563eb;-fx-text-fill:white;-fx-background-radius:6;-fx-padding:6 12;");
            voir.setOnAction(e -> openShowPlaylist(p));

            actions.getChildren().add(voir);
            tableGrid.add(actions, 4, row);
            row++;
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
        String search = searchInput.getText() == null ? "" : searchInput.getText().toLowerCase();
        String niveau = filterNiveau.getValue();

        ObservableList<Playlist> filtered = masterData.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(search)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(search)))
                .filter(p -> niveau == null || niveau.equals("Tous") || niveau.equals(p.getNiveau()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        buildTable(filtered);
    }

    private void openShowPlaylist(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/Admin/ShowPlaylistAdmin.fxml"));
            Parent view = loader.load();

            ShowPlaylistAdminController ctrl = loader.getController();
            ctrl.setPlaylist(playlist);

            BorderPane root = (BorderPane) tableGrid.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            contentArea.setCenter(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
