package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.entity.Stream;
import com.gamilha.services.DonationService;
import com.gamilha.services.StreamService;
import com.gamilha.utils.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminDonationStreamsController — liste des streams avec total donations.
 * Identique à admin/donation/streams.html.twig Symfony.
 */
public class AdminDonationStreamsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label     countLabel;
    @FXML private FlowPane  streamGrid;

    private final StreamService   streamService  = new StreamService();
    private final DonationService donService     = new DonationService();
    private ObservableList<Stream> all = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchField.textProperty().addListener((o, ov, v) -> filter());
        load();
    }

    private void load() {
        try { all.setAll(streamService.findAll()); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur BDD", e.getMessage()); }
    }

    private void filter() {
        String q = searchField.getText().toLowerCase().trim();
        List<Stream> res = all.stream()
                .filter(s -> q.isBlank() || s.getTitle().toLowerCase().contains(q))
                .sorted(Comparator.comparing(Stream::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        countLabel.setText(res.size() + " stream(s)");
        buildGrid(res);
    }

    private void buildGrid(List<Stream> streams) {
        streamGrid.getChildren().clear();
        if (streams.isEmpty()) {
            Label lbl = new Label("Aucun stream trouvé");
            lbl.setStyle("-fx-text-fill:#64748b;-fx-font-size:14px;");
            streamGrid.getChildren().add(lbl);
            return;
        }
        for (Stream s : streams) streamGrid.getChildren().add(buildCard(s));
    }

    private VBox buildCard(Stream s) {
        VBox card = new VBox(10);
        card.setPrefWidth(340);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:#111827;-fx-border-color:#1f2937;" +
                "-fx-border-radius:12;-fx-background-radius:12;-fx-cursor:hand;");

        // Titre + badge statut
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(s.getTitle());
        title.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        Label status = new Label(s.getStatusBadge());
        status.setStyle("live".equals(s.getStatus())
                ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:3 10;-fx-font-size:11px;"
                : "-fx-background-color:#374151;-fx-text-fill:#94a3b8;-fx-background-radius:20;-fx-padding:3 10;-fx-font-size:11px;");
        header.getChildren().addAll(title, status);

        // Infos
        HBox meta = new HBox(12);
        Label game = new Label("🎮 " + s.getGame());
        game.setStyle("-fx-font-size:12px;-fx-text-fill:#67e8f9;");
        Label views = new Label("👁 " + s.getViewers());
        views.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
        meta.getChildren().addAll(game, views);

        // Total donations (calculé en live)
        double total = 0;
        int count = 0;
        try {
            var dons = donService.findByStream(s.getId());
            total = dons.stream().mapToDouble(d -> d.getAmount()).sum();
            count = dons.size();
        } catch (Exception ignored) {}

        HBox donRow = new HBox(10);
        donRow.setAlignment(Pos.CENTER_LEFT);
        donRow.setPadding(new Insets(8, 12, 8, 12));
        donRow.setStyle("-fx-background-color:#0a0f1a;-fx-border-color:#1e3a5f;" +
                "-fx-border-radius:8;-fx-background-radius:8;");
        Label donCount = new Label("💰 " + count + " donation(s)");
        donCount.setStyle("-fx-font-size:13px;-fx-text-fill:#94a3b8;");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);
        Label donTotal = new Label(String.format("%.2f €", total));
        donTotal.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");
        donRow.getChildren().addAll(donCount, sp2, donTotal);

        // Bouton
        Button btn = new Button("💰 Voir les donations");
        btn.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:8 20;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnAction(e -> {
            AdminDonationListController c =
                    MainApp.loadSceneWithController("Admin/AdminDonationList.fxml");
            if (c != null) c.setStream(s);
        });

        card.getChildren().addAll(header, meta, donRow, btn);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#1a1a2e;-fx-border-color:#8b5cf6;" +
                "-fx-border-radius:12;-fx-background-radius:12;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(139,92,246,0.25),12,0,0,3);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color:#111827;-fx-border-color:#1f2937;" +
                "-fx-border-radius:12;-fx-background-radius:12;-fx-cursor:hand;"));
        return card;
    }

    @FXML private void onRefresh(ActionEvent e) { searchField.clear(); load(); }
    @FXML private void onStreams(ActionEvent e)  { MainApp.loadScene("Admin/AdminStreamList.fxml"); }
    @FXML private void onBack(ActionEvent e)     { MainApp.loadScene("Admin/AdminStreamList.fxml"); }
}
