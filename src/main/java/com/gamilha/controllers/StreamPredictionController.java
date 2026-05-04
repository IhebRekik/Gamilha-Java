package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.utils.NavigationContext;
import com.gamilha.entity.User;
import com.gamilha.services.StreamPredictionService;
import com.gamilha.utils.AlertUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * StreamPredictionController — équivalent de prediction/show.html.twig Symfony.
 *
 * Affiche :
 *   • Badge de confiance (couleur selon niveau — identique Twig)
 *   • 3 KPIs : streams attendus / min / max
 *   • Statistiques contributions (90 jours)
 *   • Recommandations (priority high=rouge, medium=orange)
 *   • Info modèle
 *
 * Appel : init(user) depuis StreamShowController.onPrediction()
 */
public class StreamPredictionController implements Initializable {

    /* ── Header ──────────────────────────────────────────────────────── */
    @FXML private Label lblUserName;
    @FXML private Label lblConfidenceBadge;
    @FXML private Label lblPeriod;

    /* ── KPIs prédiction ─────────────────────────────────────────────── */
    @FXML private Label lblExpected;
    @FXML private Label lblMin;
    @FXML private Label lblMax;

    /* ── Contributions (stats 90j) ───────────────────────────────────── */
    @FXML private Label lblTotalStreams;
    @FXML private Label lblStreamsPerWeek;
    @FXML private Label lblTotalViewers;
    @FXML private Label lblAvgViewers;
    @FXML private Label lblTotalDonations;
    @FXML private Label lblDonationAmount;
    @FXML private Label lblAvgDonPerStream;
    @FXML private Label lblLastStream;

    /* ── Recommandations ─────────────────────────────────────────────── */
    @FXML private VBox recBox;

    /* ── Info modèle ─────────────────────────────────────────────────── */
    @FXML private Label lblModelType;
    @FXML private Label lblModelVersion;
    @FXML private Label lblModelDate;

    /* ── Spinner chargement ──────────────────────────────────────────── */
    @FXML private VBox  loadingBox;
    @FXML private VBox  contentBox;

    private final StreamPredictionService predService = new StreamPredictionService();
    private User user;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /** Appelé depuis StreamShowController */
    public void init(User u) {
        this.user = u;
        lblUserName.setText("📊 Prédiction IA — " + u.getName());
        showLoading(true);
        runPrediction();
    }

    // ─────────────────────────────────────────────────────────────────
    // Chargement async — ne bloque pas l'UI
    // ─────────────────────────────────────────────────────────────────
    private void runPrediction() {
        Task<Map<String, Object>> task = new Task<>() {
            @Override
            protected Map<String, Object> call() throws Exception {
                return predService.predictStreams(user.getId(), 30);
            }
        };

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                fillData(task.getValue());
                showLoading(false);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                AlertUtil.showError("Erreur prédiction", task.getException().getMessage());
                showLoading(false);
            });
        });

        Thread t = new Thread(task, "prediction-thread");
        t.setDaemon(true);
        t.start();
    }

    // ─────────────────────────────────────────────────────────────────
    // Remplissage — identique à prediction/show.html.twig Symfony
    // ─────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void fillData(Map<String, Object> data) {
        Map<String, Object> pred  = (Map<String, Object>) data.get("prediction");
        Map<String, Object> contribs = (Map<String, Object>) data.get("contributions");
        List<Map<String, String>> recs = (List<Map<String, String>>) data.get("recommendations");
        Map<String, Object> model = (Map<String, Object>) data.get("model_info");

        // Badge confiance (identique Twig : bg-success/bg-warning/bg-danger)
        double confidence = toDouble(pred.get("confidence_level"));
        String confText   = str(pred.get("confidence_text"));
        lblConfidenceBadge.setText("Confiance : " + confText);
        if      (confidence >= 0.8) lblConfidenceBadge.setStyle(
                "-fx-background-color:#16a34a;-fx-text-fill:white;" +
                "-fx-background-radius:20;-fx-padding:6 16;-fx-font-weight:bold;-fx-font-size:14px;");
        else if (confidence >= 0.5) lblConfidenceBadge.setStyle(
                "-fx-background-color:#d97706;-fx-text-fill:white;" +
                "-fx-background-radius:20;-fx-padding:6 16;-fx-font-weight:bold;-fx-font-size:14px;");
        else                        lblConfidenceBadge.setStyle(
                "-fx-background-color:#dc2626;-fx-text-fill:white;" +
                "-fx-background-radius:20;-fx-padding:6 16;-fx-font-weight:bold;-fx-font-size:14px;");

        lblPeriod.setText("Prévision sur " + pred.get("period_days") + " jours");

        // KPIs
        lblExpected.setText(str(pred.get("streams_expected")));
        lblMin.setText(str(pred.get("streams_min")));
        lblMax.setText(str(pred.get("streams_max")));

        // Contributions
        lblTotalStreams.setText(str(contribs.get("total_streams")));
        lblStreamsPerWeek.setText(str(contribs.get("streams_per_week")) + " / semaine");
        lblTotalViewers.setText(str(contribs.get("total_viewers")));
        lblAvgViewers.setText(str(contribs.get("avg_viewers_per_stream")) + " / stream");
        lblTotalDonations.setText(str(contribs.get("total_donations")));
        lblDonationAmount.setText(str(contribs.get("total_donation_amount")) + " €");
        lblAvgDonPerStream.setText(str(contribs.get("avg_donation_per_stream")) + " € / stream");

        Object lastDate = contribs.get("last_stream_date");
        lblLastStream.setText(lastDate != null ? str(lastDate).replace("T", " ") : "—");

        // Recommandations
        recBox.getChildren().clear();
        if (recs == null || recs.isEmpty()) {
            Label empty = new Label("✅ Aucune recommandation particulière — continuez ainsi !");
            empty.setStyle("-fx-text-fill:#4ade80;-fx-font-size:13px;");
            recBox.getChildren().add(empty);
        } else {
            for (Map<String, String> rec : recs) recBox.getChildren().add(buildRecCard(rec));
        }

        // Modèle
        if (model != null) {
            lblModelType.setText(str(model.get("type")));
            lblModelVersion.setText("v" + str(model.get("version")));
            lblModelDate.setText(str(model.get("last_updated")).replace("T", " "));
        }
    }

    private HBox buildRecCard(Map<String, String> rec) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-padding:12 16;-fx-background-radius:8;-fx-border-radius:8;" +
                        ("high".equals(rec.get("priority"))
                                ? "-fx-background-color:#450a0a;-fx-border-color:#dc2626;"
                                : "-fx-background-color:#1c1a08;-fx-border-color:#d97706;")
        );

        Label icon = new Label("high".equals(rec.get("priority")) ? "🔴" : "🟡");
        icon.setStyle("-fx-font-size:16px;");

        VBox text = new VBox(2);
        Label type = new Label(capitalize(rec.getOrDefault("type", "")));
        type.setStyle("-fx-font-weight:bold;-fx-font-size:11px;-fx-text-fill:#94a3b8;");
        Label msg = new Label(rec.getOrDefault("message", ""));
        msg.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:13px;");
        msg.setWrapText(true);
        text.getChildren().addAll(type, msg);
        HBox.setHgrow(text, Priority.ALWAYS);

        card.getChildren().addAll(icon, text);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────
    // Actions
    // ─────────────────────────────────────────────────────────────────
    @FXML private void onBack(ActionEvent e)    { NavigationContext.navigate("User/StreamList.fxml"); }
    @FXML private void onRefresh(ActionEvent e) { showLoading(true); runPrediction(); }

    private void showLoading(boolean loading) {
        if (loadingBox != null) { loadingBox.setVisible(loading);  loadingBox.setManaged(loading); }
        if (contentBox != null) { contentBox.setVisible(!loading); contentBox.setManaged(!loading); }
    }

    // ─────────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────────
    private String str(Object o) { return o != null ? o.toString() : "—"; }

    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
