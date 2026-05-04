package com.gamilha.controllers.admin;

import com.gamilha.services.StreamAnalyticsService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.*;

/**
 * AdminAnalyticsController — Dashboard analytique streams & donations.
 * Utilise UNIQUEMENT stream, donation, user — aucune nouvelle table.
 */
public class AdminAnalyticsController implements Initializable {

    // KPI labels
    @FXML private Label lblRevTotal, lblLiveNow, lblViewersNow;
    @FXML private Label lblAvgDon, lblConversion, lblUniqueDonors;

    // Charts
    @FXML private BarChart<String, Number>  revenueChart;
    @FXML private CategoryAxis              revenueXAxis;
    @FXML private NumberAxis                revenueYAxis;
    @FXML private BarChart<String, Number>  gameChart;
    @FXML private CategoryAxis              gameXAxis;
    @FXML private NumberAxis                gameYAxis;

    // Tables
    @FXML private VBox topStreamersBox;
    @FXML private VBox topStreamsBox;
    @FXML private VBox engagementBox;
    @FXML private VBox alertsBox;

    private final StreamAnalyticsService svc = new StreamAnalyticsService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadAll();
    }

    private void loadAll() {
        new Thread(() -> {
            try {
                // Fetch all data in background
                double revTotal    = svc.getTotalRevenue();
                int    liveCount   = svc.getLiveCount();
                int    viewers     = svc.getTotalViewers();
                double avgDon      = svc.getAvgDonation();
                double conversion  = svc.getConversionRate();
                int    uniqueDon   = svc.getUniqueDonors();
                var    monthly     = svc.getMonthlyRevenue();
                var    byGame      = svc.getRevenueByGame();
                var    topStr      = svc.getTopStreamers();
                var    topStreams   = svc.getTopStreamsByRevenue();
                var    engagement  = svc.getEngagementScores();
                var    alerts      = svc.getAlerts();

                Platform.runLater(() -> {
                    fillKPIs(revTotal, liveCount, viewers, avgDon, conversion, uniqueDon);
                    fillRevenueChart(monthly);
                    fillGameChart(byGame);
                    fillTopStreamers(topStr);
                    fillTopStreams(topStreams);
                    fillEngagement(engagement);
                    fillAlerts(alerts);
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        lblRevTotal.setText("Erreur: " + ex.getMessage()));
            }
        }).start();
    }

    private void fillKPIs(double rev, int live, int viewers, double avg, double conv, int donors) {
        lblRevTotal.setText(String.format("%.2f €", rev));
        lblLiveNow.setText(String.valueOf(live));
        lblViewersNow.setText(String.valueOf(viewers));
        lblAvgDon.setText(String.format("%.2f €", avg));
        lblConversion.setText(String.format("%.1f%%", conv));
        lblUniqueDonors.setText(String.valueOf(donors));
    }

    private void fillRevenueChart(Map<String, Double> monthly) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenus €");
        monthly.forEach((m, v) -> series.getData().add(new XYChart.Data<>(m, v)));
        revenueChart.getData().clear();
        revenueChart.getData().add(series);
        Platform.runLater(() ->
                series.getData().forEach(d -> {
                    if (d.getNode() != null)
                        d.getNode().setStyle("-fx-bar-fill:#8b5cf6;");
                }));
    }

    private void fillGameChart(Map<String, Double> byGame) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenus €");
        byGame.forEach((g, v) -> series.getData().add(new XYChart.Data<>(g, v)));
        gameChart.getData().clear();
        gameChart.getData().add(series);
        String[] colors = {"#3b82f6","#10b981","#f59e0b","#ef4444","#8b5cf6","#ec4899"};
        Platform.runLater(() -> {
            List<XYChart.Data<String,Number>> items = series.getData();
            for (int i = 0; i < items.size(); i++) {
                String col = colors[i % colors.length];
                if (items.get(i).getNode() != null)
                    items.get(i).getNode().setStyle("-fx-bar-fill:" + col + ";");
            }
        });
    }

    private void fillTopStreamers(List<Map<String, Object>> list) {
        topStreamersBox.getChildren().clear();
        String[] medals = {"🥇","🥈","🥉","4️⃣","5️⃣","6️⃣","7️⃣","8️⃣","9️⃣","🔟"};
        for (Map<String, Object> row : list) {
            int rank = (int) row.get("rank") - 1;
            HBox line = makeTableRow(
                    (rank < medals.length ? medals[rank] : "#" + row.get("rank")) + "  " + row.get("name"),
                    row.get("nbStreams") + " streams",
                    String.format("%.2f €", (double) row.get("totalDons")),
                    row.get("avgViewers") + " viewers"
            );
            topStreamersBox.getChildren().add(line);
        }
        if (list.isEmpty()) addEmpty(topStreamersBox, "Aucune donnée");
    }

    private void fillTopStreams(List<Map<String, Object>> list) {
        topStreamsBox.getChildren().clear();
        for (Map<String, Object> row : list) {
            HBox line = makeTableRow(
                    "📡 " + row.get("title"),
                    "🎮 " + row.get("game"),
                    String.format("%.2f €", (double) row.get("total")),
                    row.get("nbDons") + " dons"
            );
            topStreamsBox.getChildren().add(line);
        }
        if (list.isEmpty()) addEmpty(topStreamsBox, "Aucun stream avec donations");
    }

    private void fillEngagement(List<Map<String, Object>> list) {
        engagementBox.getChildren().clear();
        for (Map<String, Object> row : list) {
            int score = (int) row.get("score");
            HBox line = new HBox(12);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setStyle("-fx-background-color:#0d1117;-fx-border-color:#1f2937;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 14;");
            Label lName = new Label("⚡ " + row.get("name"));
            lName.setStyle("-fx-text-fill:#e5e7eb;-fx-font-size:13px;-fx-font-weight:bold;");
            HBox.setHgrow(lName, Priority.ALWAYS);
            // Score bar
            double pct = Math.min(score / 500.0, 1.0);
            StackPane bar = new StackPane();
            bar.setPrefWidth(160); bar.setPrefHeight(18);
            bar.setStyle("-fx-background-color:#1f2937;-fx-background-radius:9;");
            HBox fill = new HBox();
            fill.setPrefWidth(160 * pct); fill.setPrefHeight(18);
            fill.setStyle("-fx-background-color:linear-gradient(to right,#7c3aed,#c84cff);-fx-background-radius:9;");
            fill.setMaxWidth(160 * pct);
            bar.getChildren().add(fill);
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            Label lScore = new Label(score + " pts");
            lScore.setStyle("-fx-text-fill:#a78bfa;-fx-font-size:12px;-fx-font-weight:bold;");
            line.getChildren().addAll(lName, bar, lScore);
            engagementBox.getChildren().add(line);
        }
        if (list.isEmpty()) addEmpty(engagementBox, "Aucun streamer");
    }

    private void fillAlerts(List<Map<String, String>> alerts) {
        alertsBox.getChildren().clear();
        for (Map<String, String> a : alerts) {
            String sev = a.get("severity");
            String bg  = switch (sev) {
                case "warning" -> "-fx-background-color:#422006;-fx-border-color:#92400e;";
                case "success" -> "-fx-background-color:#052e16;-fx-border-color:#166534;";
                default        -> "-fx-background-color:#0c1a2e;-fx-border-color:#1e3a5f;";
            };
            VBox box = new VBox(4);
            box.setStyle(bg + "-fx-border-radius:8;-fx-background-radius:8;-fx-padding:12 16;");
            Label lType = new Label(a.get("type"));
            lType.setStyle("-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:" +
                    (sev.equals("warning") ? "#fbbf24;" : sev.equals("success") ? "#4ade80;" : "#60a5fa;"));
            Label lMsg  = new Label(a.get("message"));
            lMsg.setStyle("-fx-font-size:13px;-fx-text-fill:#d1d5db;"); lMsg.setWrapText(true);
            box.getChildren().addAll(lType, lMsg);
            alertsBox.getChildren().add(box);
        }
    }

    private HBox makeTableRow(String col1, String col2, String col3, String col4) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#0d1117;-fx-border-color:#1f2937;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 14;");
        Label l1 = cell(col1, "#e5e7eb", true,  Priority.ALWAYS);
        Label l2 = cell(col2, "#9ca3af", false, Priority.SOMETIMES);
        Label l3 = cell(col3, "#4ade80", true,  Priority.SOMETIMES);
        Label l4 = cell(col4, "#6b7280", false, Priority.SOMETIMES);
        row.getChildren().addAll(l1, l2, l3, l4);
        return row;
    }

    private Label cell(String text, String color, boolean bold, Priority grow) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:12px;" +
                (bold ? "-fx-font-weight:bold;" : "") + "-fx-padding:0 12 0 0;");
        l.setWrapText(false); l.setMinWidth(0);
        HBox.setHgrow(l, grow);
        return l;
    }

    private void addEmpty(VBox box, String msg) {
        Label l = new Label(msg); l.setStyle("-fx-text-fill:#4b5563;-fx-font-size:13px;");
        box.getChildren().add(l);
    }

    @FXML private void onRefresh(ActionEvent e) { loadAll(); }
}
