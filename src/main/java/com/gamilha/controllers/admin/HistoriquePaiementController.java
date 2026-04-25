package com.gamilha.controllers.admin;

import com.gamilha.entity.HistoriquePaiement;
import com.gamilha.services.HistoriquePaiementService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class HistoriquePaiementController {

    @FXML private GridPane gridPane;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> sortMontant;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private TextField searchField;

    private int currentPage = 1;
    private final int pageSize = 6;

    private final HistoriquePaiementService service = new HistoriquePaiementService();
    private List<HistoriquePaiement> allData;

    @FXML
    public void initialize() {
        gridPane.getColumnConstraints().clear();

        for (int i = 0; i < 4; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(25); // 5 colonnes
            gridPane.getColumnConstraints().add(col);
        }
        filterType.getItems().addAll("Tous", "Pro", "Premium", "Gold");
        sortMontant.getItems().addAll("Défaut", "Asc", "Desc");

        allData = service.getPaginatedByUser(1, 100);
        gridPane.setHgap(0);
        gridPane.setVgap(0);
        gridPane.setStyle("""
    -fx-background-color:#020617;
    -fx-border-color:#1e293b;
    -fx-border-radius:8;
""");
        renderTable(allData);
    }

    // 🔥 RENDER TABLE
    private void renderTable(List<HistoriquePaiement> list) {

        gridPane.getChildren().clear();

        addHeader();

        int row = 1;

        for (HistoriquePaiement p : list) {

            addCell(p.getUser().getEmail(), 0, row);
            addCell(p.getAbonnement().getType(), 1, row);
            addCell(String.valueOf(p.getMontant()), 2, row);
            addCell(p.getCreatedAt().toString(), 3, row);




            row++;
        }
    }

    // 🔥 FILTER
    @FXML
    private void applyFilter() {

        List<HistoriquePaiement> filtered = allData.stream()

                .filter(p -> searchField.getText() == null || searchField.getText().isEmpty()
                        || p.getUser().getEmail().toLowerCase().contains(searchField.getText().toLowerCase()))

                .filter(p -> filterType.getValue() == null || filterType.getValue().equals("Tous")
                        || p.getAbonnement().getType().equalsIgnoreCase(filterType.getValue()))

                .filter(p -> dateDebut.getValue() == null
                        || p.getCreatedAt().toLocalDate().isAfter(dateDebut.getValue()))

                .filter(p -> dateFin.getValue() == null
                        || p.getCreatedAt().toLocalDate().isBefore(dateFin.getValue()))

                .collect(Collectors.toList());
        // SORT
        if ("Asc".equals(sortMontant.getValue())) {
            filtered.sort(Comparator.comparingDouble(HistoriquePaiement::getMontant));
        } else if ("Desc".equals(sortMontant.getValue())) {
            filtered.sort(Comparator.comparingDouble(HistoriquePaiement::getMontant).reversed());
        }

        renderTable(filtered);
    }

    // HEADER
    private void addHeader() {
        addHeaderCell("Utilisateur", 0);
        addHeaderCell("Abonnement", 1);
        addHeaderCell("Montant", 2);
        addHeaderCell("Date", 3);
    }

    private void addHeaderCell(String text, int col) {

        Label label = new Label(text);

        label.setStyle("""
        -fx-text-fill: #e2e8f0;
        -fx-font-weight: bold;
        -fx-padding: 14;
        -fx-background-color: #020617;
        -fx-border-color: #1e293b;
        -fx-border-width: 0 0 1 0;
        -fx-font-size: 13px;
    """);

        gridPane.add(label, col, 0);
    }

    private void addCell(String text, int col, int row) {

        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);

        label.setStyle("""
        -fx-text-fill: #cbd5f5;
        -fx-padding: 14;
        -fx-border-color: #1e293b;
        -fx-border-width: 0 0 1 0;
        -fx-font-size: 13px;
    """);

        // 🔥 hover léger (cellule seulement)
        label.setOnMouseEntered(e ->
                label.setStyle(label.getStyle() + "-fx-background-color:#111827;"));

        label.setOnMouseExited(e ->
                label.setStyle("""
                -fx-text-fill: #cbd5f5;
                -fx-padding: 14;
                -fx-border-color: #1e293b;
                -fx-border-width: 0 0 1 0;
                -fx-font-size: 13px;
            """));

        gridPane.add(label, col, row);
    }

    @FXML
    private void nextPage() {
        currentPage++;
        allData = service.getPaginatedByUser(currentPage, pageSize);
        renderTable(allData);
    }

    @FXML
    private void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            allData = service.getPaginatedByUser(currentPage, pageSize);
            renderTable(allData);
        }
    }
}