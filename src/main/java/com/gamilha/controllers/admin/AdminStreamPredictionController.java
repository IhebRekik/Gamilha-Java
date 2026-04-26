package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.services.StreamPredictionService;
import com.gamilha.utils.DatabaseConnection;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * AdminStreamPredictionController
 * Page "🤖 Prévision Streams" dans le dashboard admin.
 * Affiche les stats globales + prévision IA de TOUS les streams.
 */
public class AdminStreamPredictionController implements Initializable {

    @FXML private Label lblTotalStreams, lblTotalSub;
    @FXML private Label lblPrediction, lblMoisCible;
    @FXML private Label lblMoyenne, lblTendance;
    @FXML private Label lblMessage, lblAiMessage;
    @FXML private VBox  aiBox, topStreamersBox;

    @FXML private BarChart<String, Number>  barChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis   yAxis;

    private final StreamPredictionService svc = new StreamPredictionService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                // 1. Stats globales depuis BDD
                int total = countTotalStreams();
                Map<String, Integer> monthly = getMonthlyStreamsAll();
                List<String[]> topStreamers  = getTopStreamers();

                // 2. Prévision via StreamPredictionService (userId=0 = global)
                Map<String, Object> pred = predictGlobal(monthly);

                Platform.runLater(() -> {
                    fillKPIs(total, pred);
                    fillChart(monthly, pred);
                    fillTopStreamers(topStreamers);
                    fillMessage(pred);
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                    lblMessage.setText("Erreur : " + ex.getMessage()));
            }
        }).start();
    }

    private void fillKPIs(int total, Map<String, Object> pred) {
        lblTotalStreams.setText(String.valueOf(total));
        lblTotalSub.setText("streams au total");
        lblPrediction.setText("~" + pred.get("prediction") + " streams");
        lblMoisCible.setText((String) pred.get("nextMonthFr"));
        lblMoyenne.setText(String.format("%.1f", (double) pred.get("average")));
        String trend = (String) pred.get("trend");
        lblTendance.setText(trend);
        // Couleur tendance
        if (trend.contains("hausse"))
            lblTendance.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");
        else if (trend.contains("baisse"))
            lblTendance.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#f87171;");
        else
            lblTendance.setStyle("-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#fbbf24;");
    }

    @SuppressWarnings("unchecked")
    private void fillChart(Map<String, Integer> monthly, Map<String, Object> pred) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Streams");
        monthly.forEach((m, c) -> series.getData().add(new XYChart.Data<>(m, c)));

        String nextKey = (String) pred.get("nextMonth");
        int prediction = (int) pred.get("prediction");
        XYChart.Data<String, Number> predBar = new XYChart.Data<>(nextKey + " ★", prediction);
        series.getData().add(predBar);

        barChart.getData().clear();
        barChart.getData().add(series);

        Platform.runLater(() -> {
            if (predBar.getNode() != null)
                predBar.getNode().setStyle("-fx-bar-fill:#8b5cf6;");
            // Style général des barres
            series.getData().forEach(d -> {
                if (d.getNode() != null && !d.getXValue().contains("★"))
                    d.getNode().setStyle("-fx-bar-fill:#3b82f6;");
            });
        });
    }

    private void fillTopStreamers(List<String[]> top) {
        topStreamersBox.getChildren().clear();
        String[] medals = {"🥇", "🥈", "🥉", "4️⃣", "5️⃣"};
        for (int i = 0; i < top.size(); i++) {
            String[] row = top.get(i);
            String medal = i < medals.length ? medals[i] : (i + 1) + ".";
            HBox line = new HBox(10);
            line.setAlignment(Pos.CENTER_LEFT);
            line.setStyle("-fx-background-color:#0d1117;-fx-border-color:#1f2937;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10 12;");
            Label lMedal = new Label(medal); lMedal.setStyle("-fx-font-size:16px;");
            Label lName  = new Label(row[0]); lName.setStyle("-fx-text-fill:#e5e7eb;-fx-font-size:13px;"); HBox.setHgrow(lName, Priority.ALWAYS);
            Label lCount = new Label(row[1] + " streams"); lCount.setStyle("-fx-text-fill:#6b7280;-fx-font-size:12px;");
            line.getChildren().addAll(lMedal, lName, lCount);
            topStreamersBox.getChildren().add(line);
        }
    }

    private void fillMessage(Map<String, Object> pred) {
        lblMessage.setText((String) pred.get("message"));
        String ai = (String) pred.get("aiMessage");
        if (ai != null && !ai.isBlank()) {
            lblAiMessage.setText("🤖 " + ai);
            aiBox.setVisible(true); aiBox.setManaged(true);
        }
    }

    // ── Données BDD ────────────────────────────────────────────────────────
    private int countTotalStreams() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM stream")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Map<String, Integer> getMonthlyStreamsAll() throws SQLException {
        String sql = "SELECT YEAR(created_at) yr, MONTH(created_at) mo, COUNT(*) cnt " +
                     "FROM stream WHERE created_at >= DATE_SUB(NOW(), INTERVAL 180 DAY) " +
                     "GROUP BY yr, mo ORDER BY yr ASC, mo ASC";
        Map<String, Integer> m = new LinkedHashMap<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                m.put(rs.getInt("yr") + "-" + String.format("%02d", rs.getInt("mo")),
                      rs.getInt("cnt"));
        }
        return m;
    }

    private List<String[]> getTopStreamers() throws SQLException {
        String sql = "SELECT u.name, COUNT(s.id) cnt FROM stream s " +
                     "LEFT JOIN user u ON s.user_id = u.id " +
                     "GROUP BY s.user_id, u.name ORDER BY cnt DESC LIMIT 5";
        List<String[]> list = new ArrayList<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                list.add(new String[]{
                    rs.getString("name") != null ? rs.getString("name") : "Inconnu",
                    String.valueOf(rs.getInt("cnt"))});
        }
        return list;
    }

    /** Prévision globale (tous users) à partir des données mensuelles */
    private Map<String, Object> predictGlobal(Map<String, Integer> monthly) {
        List<Integer> counts = new ArrayList<>(monthly.values());
        double avg = counts.isEmpty() ? 0 :
                counts.stream().mapToInt(i -> i).average().orElse(0);
        // Pondération : mois récents ×2
        double wSum = 0, wTot = 0;
        int n = counts.size();
        for (int i = 0; i < n; i++) {
            double w = (i < n / 2) ? 1.0 : 2.0;
            wSum += counts.get(i) * w; wTot += w;
        }
        int pred = (int) Math.round(wTot > 0 ? wSum / wTot : avg);

        String trend = "stable 📊";
        if (n >= 2) {
            int f = counts.subList(0, n / 2).stream().mapToInt(i -> i).sum();
            int s = counts.subList(n / 2, n).stream().mapToInt(i -> i).sum();
            if      (s > f * 1.15) trend = "hausse 📈";
            else if (s < f * 0.85) trend = "baisse 📉";
        }

        var next = java.time.LocalDate.now().plusMonths(1);
        String nextKey = next.getYear() + "-" + String.format("%02d", next.getMonthValue());
        String[] mois = {"Janvier","Février","Mars","Avril","Mai","Juin","Juillet","Août","Septembre","Octobre","Novembre","Décembre"};
        String nextFr = mois[next.getMonthValue() - 1] + " " + next.getYear();

        String msg = counts.isEmpty()
            ? "Pas encore assez de données."
            : String.format("Prévision pour %s : ~%d stream(s). Moyenne sur %d mois : %.1f. Tendance : %s.",
              nextFr, pred, n, avg, trend);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("prediction", pred); r.put("average", avg);
        r.put("trend", trend); r.put("nextMonth", nextKey);
        r.put("nextMonthFr", nextFr); r.put("message", msg);
        r.put("aiMessage", "");
        return r;
    }

    @FXML private void onRefresh(ActionEvent e) { loadData(); }
    @FXML private void onBackToStreams(ActionEvent e) { MainApp.loadScene("User/StreamList.fxml"); }
}
