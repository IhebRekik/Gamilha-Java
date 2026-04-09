package com.gamilha.controller.admin;

import com.gamilha.MainApp;
import com.gamilha.service.StreamService;
import com.gamilha.entity.Stream;
import com.gamilha.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Équivalent admin/donation/streams.html.twig
 * Affiche tous les streams (sans ID), clic → voir les donations du stream
 */
public class AdminDonationStreamsController implements Initializable {

    @FXML private TextField  searchField;
    @FXML private Label      countLabel;
    @FXML private FlowPane   streamGrid;

    private final StreamService service = new StreamService();
    private ObservableList<Stream> all = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchField.textProperty().addListener((o, ov, v) -> filter());
        load();
    }

    private void load() {
        try { all.setAll(service.findAll()); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur", e.getMessage()); }
    }

    private void filter() {
        String q = searchField.getText().toLowerCase().trim();
        List<Stream> res = all.stream()
            .filter(s -> q.isBlank() || s.getTitle().toLowerCase().contains(q))
            .sorted(Comparator.comparing(Stream::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .collect(Collectors.toList());

        countLabel.setText(res.size() + " stream(s)");
        streamGrid.getChildren().clear();

        if (res.isEmpty()) {
            Label e = new Label("Aucun stream");
            e.setStyle("-fx-text-fill:#64748b;-fx-font-size:15px;");
            streamGrid.getChildren().add(e);
            return;
        }

        for (Stream s : res) {
            VBox card = new VBox(8);
            card.setPrefWidth(280);
            card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;"
                + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;-fx-cursor:hand;");

            Label title = new Label("📡 " + s.getTitle());
            title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
            title.setWrapText(true);

            Label info = new Label("🎮 " + s.getGame() + "  |  👁 " + s.getViewers());
            info.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12px;");

            Label badge = new Label(s.getStatusBadge());
            badge.setStyle("live".equals(s.getStatus())
                ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:2 10 2 10;-fx-font-size:11px;"
                : "-fx-background-color:#374151;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:2 10 2 10;-fx-font-size:11px;");

            Button btn = new Button("💰 Voir les donations");
            btn.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-font-weight:bold;"
                + "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:7 16 7 16;");
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> {
                AdminDonationListController c =
                    MainApp.loadSceneWithController("admin/AdminDonationList.fxml");
                if (c != null) c.setStream(s);
            });

            card.getChildren().addAll(title, info, badge, btn);

            card.setOnMouseEntered(e ->
                card.setStyle("-fx-background-color:#1e1e30;-fx-border-color:#8b5cf6;"
                    + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;-fx-cursor:hand;"));
            card.setOnMouseExited(e ->
                card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;"
                    + "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;-fx-cursor:hand;"));

            streamGrid.getChildren().add(card);
        }
    }

    @FXML private void onBack(ActionEvent e) {
        MainApp.loadScene("admin/AdminStreamList.fxml");
    }
}
