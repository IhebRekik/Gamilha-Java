package com.gamilha.controllers.coaching;

import com.gamilha.entity.Playlist;
import com.gamilha.entity.PlaylistStatistics;
import com.gamilha.services.PlaylistStatisticsService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PlaylistStatisticsController {

    @FXML private FlowPane kpiPane;
    @FXML private HBox     chartsBox;
    @FXML private Label    lblTopVideo;
    @FXML private Label    lblLastView;

    private final PlaylistStatisticsService statisticsService = new PlaylistStatisticsService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void loadStatistics(Playlist playlist) {
        PlaylistStatistics stats = statisticsService.getStatisticsForPlaylist(playlist);

        // ── KPI cards ─────────────────────────────────────────────────────
        kpiPane.getChildren().clear();
        kpiPane.getChildren().addAll(
                buildKpi("Videos",         String.valueOf(stats.getTotalVideos()),          "#38bdf8"),
                buildKpi("Vues totales",   String.valueOf(stats.getTotalViews()),           "#4ade80"),
                buildKpi("Videos vues",    String.valueOf(stats.getWatchedVideos()),        "#a78bfa"),
                buildKpi("Moy. vues",      String.format("%.1f", stats.getAverageViewsPerVideo()), "#fbbf24"),
                buildKpi("Favoris",        String.valueOf(stats.getTotalFavorites()),       "#f87171")
        );

        // ── Charts ────────────────────────────────────────────────────────
        chartsBox.getChildren().clear();

        // PieChart — progression (vues / non vues / favoris)
        VBox pieBox = buildPieChart(stats);
        HBox.setHgrow(pieBox, Priority.ALWAYS);

        // BarChart — top videos par nombre de vues
        VBox barBox = buildBarChart(stats);
        HBox.setHgrow(barBox, Priority.ALWAYS);

        chartsBox.getChildren().addAll(pieBox, barBox);

        // ── Info labels ───────────────────────────────────────────────────
        lblTopVideo.setText(
                stats.getTopVideoTitle() != null
                        ? stats.getTopVideoTitle() + " (" + stats.getTopVideoViews() + " vues)"
                        : "Aucune vue pour le moment"
        );
        lblLastView.setText(
                stats.getLastViewedAt() != null
                        ? formatter.format(stats.getLastViewedAt())
                        : "Aucune lecture"
        );
    }

    // ── KPI card helper ───────────────────────────────────────────────────────

    private VBox buildKpi(String label, String value, String color) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");

        Label val = new Label(value);
        val.setStyle("-fx-text-fill:" + color + ";-fx-font-size:24;-fx-font-weight:bold;");

        VBox card = new VBox(4, lbl, val);
        card.setPrefWidth(160);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color:#1e293b;-fx-background-radius:10;"
                + "-fx-border-color:#334155;-fx-border-radius:10;-fx-border-width:1;");
        return card;
    }

    // ── PieChart: videos vues vs non vues ─────────────────────────────────────

    private VBox buildPieChart(PlaylistStatistics stats) {
        int watched   = stats.getWatchedVideos();
        int total     = stats.getTotalVideos();
        int unwatched = Math.max(0, total - watched);
        int favorites = stats.getTotalFavorites();

        PieChart pie = new PieChart();
        pie.setTitle("Progression");
        pie.setLegendVisible(true);
        pie.setLabelsVisible(true);
        pie.setPrefSize(300, 280);

        if (total == 0 && favorites == 0) {
            pie.getData().add(new PieChart.Data("Aucune donnee", 1));
        } else {
            if (watched > 0)   pie.getData().add(new PieChart.Data("Vues ("   + watched   + ")", watched));
            if (unwatched > 0) pie.getData().add(new PieChart.Data("Non vues (" + unwatched + ")", unwatched));
            if (favorites > 0) pie.getData().add(new PieChart.Data("Favoris (" + favorites + ")", favorites));
        }

        pie.setStyle("-fx-background-color:transparent;");

        // Color slices
        String[] colors = {"#38bdf8", "#475569", "#f87171"};
        for (int i = 0; i < pie.getData().size(); i++) {
            String c = colors[Math.min(i, colors.length - 1)];
            pie.getData().get(i).getNode().setStyle("-fx-pie-color:" + c + ";");
        }

        Label title = new Label("Progression des videos");
        title.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;-fx-font-weight:bold;");

        VBox box = new VBox(6, title, pie);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color:#1e293b;-fx-background-radius:10;"
                + "-fx-border-color:#334155;-fx-border-radius:10;-fx-border-width:1;");
        return box;
    }

    // ── BarChart: top videos par vues ─────────────────────────────────────────

    private VBox buildBarChart(PlaylistStatistics stats) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Videos");
        yAxis.setLabel("Vues");
        xAxis.setStyle("-fx-tick-label-fill:#94a3b8;");
        yAxis.setStyle("-fx-tick-label-fill:#94a3b8;");

        BarChart<String, Number> bar = new BarChart<>(xAxis, yAxis);
        bar.setTitle("Top videos");
        bar.setLegendVisible(false);
        bar.setPrefSize(340, 280);
        bar.setStyle("-fx-background-color:transparent;");
        bar.setBarGap(4);
        bar.setCategoryGap(16);
        bar.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Vues");

        List<String> summaries = stats.getTopVideoSummaries();
        if (summaries.isEmpty()) {
            // Placeholder bar so chart is not empty
            series.getData().add(new XYChart.Data<>("Aucune donnee", 0));
        } else {
            for (String summary : summaries) {
                // Format: "Titre - N vues"
                int dashIdx = summary.lastIndexOf(" - ");
                if (dashIdx > 0) {
                    String videoTitle = summary.substring(0, dashIdx);
                    String viewsPart  = summary.substring(dashIdx + 3).replace(" vues", "").trim();
                    // Shorten long titles for axis readability
                    String shortTitle = videoTitle.length() > 18
                            ? videoTitle.substring(0, 16) + "…"
                            : videoTitle;
                    try {
                        int views = Integer.parseInt(viewsPart);
                        series.getData().add(new XYChart.Data<>(shortTitle, views));
                    } catch (NumberFormatException ignored) {
                        series.getData().add(new XYChart.Data<>(videoTitle, 0));
                    }
                }
            }
        }

        bar.getData().add(series);

        // Style bars with accent color
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                d.getNode().setStyle("-fx-bar-fill:#38bdf8;");
            }
        }
        // Apply color after layout
        bar.lookupAll(".bar").forEach(n -> n.setStyle("-fx-bar-fill:#38bdf8;"));

        Label title = new Label("Top videos par vues");
        title.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;-fx-font-weight:bold;");

        VBox box = new VBox(6, title, bar);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color:#1e293b;-fx-background-radius:10;"
                + "-fx-border-color:#334155;-fx-border-radius:10;-fx-border-width:1;");
        return box;
    }
}
