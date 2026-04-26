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
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDonationStreamsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private Label     countLabel;
    @FXML private FlowPane  streamGrid;

    private final StreamService   streamSvc = new StreamService();
    private final DonationService donSvc    = new DonationService();
    private ObservableList<Stream> all      = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchField.textProperty().addListener((o, ov, v) -> filter());
        load();
    }

    private void load() {
        try {
            all.setAll(streamSvc.findAll());
            filter();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur BDD", e.getMessage());
        }
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
            Label empty = new Label("Aucun stream trouvé");
            empty.setStyle("-fx-text-fill:#4b5563;-fx-font-size:14px;");
            streamGrid.getChildren().add(empty);
            return;
        }

        for (Stream s : res)
            streamGrid.getChildren().add(buildCard(s));
    }

    private VBox buildCard(Stream s) {
        // ── Récupérer stats donations ─────────────────────────────────
        int    donCount = 0;
        double donTotal = 0;
        try {
            var dons = donSvc.findByStream(s.getId());
            donCount = dons.size();
            donTotal = dons.stream().mapToDouble(d -> d.getAmount()).sum();
        } catch (Exception ignored) {}

        // ── Card ──────────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setStyle(
                "-fx-background-color:#111827;" +
                        "-fx-border-color:#1f2937;" +
                        "-fx-border-radius:14;-fx-background-radius:14;" +
                        "-fx-cursor:hand;");

        // ── Header coloré selon statut ────────────────────────────────
        String headerColor = "live".equals(s.getStatus()) ? "#7c3aed" : "#1f2937";
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color:" + headerColor + ";" +
                "-fx-background-radius:14 14 0 0;");

        // Badge statut
        Label statusBadge = new Label(s.getStatusBadge());
        statusBadge.setStyle("live".equals(s.getStatus())
                ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:10px;" +
                  "-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:2 8;"
                : "-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:#9ca3af;" +
                  "-fx-font-size:10px;-fx-background-radius:20;-fx-padding:2 8;");

        Label titleLbl = new Label(s.getTitle());
        titleLbl.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:white;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(200);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        header.getChildren().addAll(titleLbl, statusBadge);

        // ── Corps ─────────────────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 16, 6, 16));

        // Jeu + viewers
        HBox meta = new HBox(10);
        Label gameLbl = new Label("🎮 " + (s.getGame() != null ? s.getGame() : "—"));
        gameLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#67e8f9;-fx-font-weight:500;");
        Label viewLbl = new Label("👁 " + s.getViewers());
        viewLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#6b7280;");
        meta.getChildren().addAll(gameLbl, viewLbl);

        // Date
        Label dateLbl = new Label(s.getCreatedAt() != null
                ? "📅 " + s.getCreatedAt().format(FMT) : "");
        dateLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#4b5563;");

        // Séparateur
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#1f2937;");

        // Stats donations
        HBox donRow = new HBox(0);
        donRow.setAlignment(Pos.CENTER_LEFT);
        donRow.setPadding(new Insets(8, 12, 8, 12));
        donRow.setStyle("-fx-background-color:#0d1117;-fx-border-color:#1f2937;" +
                "-fx-border-radius:8;-fx-background-radius:8;");

        VBox donLeft = new VBox(2);
        Label donCountLbl = new Label(donCount + " donation(s)");
        donCountLbl.setStyle("-fx-font-size:12px;-fx-text-fill:#9ca3af;");
        Label donIcon = new Label("💰");
        donIcon.setStyle("-fx-font-size:20px;");
        donLeft.getChildren().addAll(donIcon, donCountLbl);

        Region donSp = new Region();
        HBox.setHgrow(donSp, Priority.ALWAYS);

        Label totalLbl = new Label(String.format("%.2f €", donTotal));
        totalLbl.setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");

        donRow.getChildren().addAll(donLeft, donSp, totalLbl);

        body.getChildren().addAll(meta, dateLbl, sep, donRow);

        // ── Bouton ────────────────────────────────────────────────────
        HBox btnBox = new HBox();
        btnBox.setPadding(new Insets(10, 16, 14, 16));

        Button btn = new Button("💰  Voir les donations");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle(
                "-fx-background-color:#8b5cf6;-fx-text-fill:white;" +
                        "-fx-font-weight:bold;-fx-font-size:13px;" +
                        "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:9 0;");
        btn.setOnAction(e -> {
            AdminDonationListController c =
                    MainApp.loadSceneWithController("Admin/AdminDonationList.fxml");
            if (c != null) c.setStream(s);
        });
        HBox.setHgrow(btn, Priority.ALWAYS);
        btnBox.getChildren().add(btn);

        card.getChildren().addAll(header, body, btnBox);

        // ── Hover ─────────────────────────────────────────────────────
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#1a1a2e;-fx-border-color:#8b5cf6;" +
                        "-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(139,92,246,0.3),16,0,0,4);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#111827;-fx-border-color:#1f2937;" +
                        "-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;"));

        return card;
    }

    @FXML private void onBack(ActionEvent e) {
        MainApp.loadScene("Admin/AdminStreamList.fxml");
    }

    @FXML private void onRefresh(ActionEvent e) {
        searchField.clear();
        load();
    }
}