package com.gamilha.controllers;

import com.gamilha.entity.ActivitySession;
import com.gamilha.entity.User;
import com.gamilha.services.ActivityService;
import com.gamilha.services.PdfExportService;
import com.gamilha.utils.SessionContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Controller de la section "Statistiques d'activité".
 * Affiche les cards de stats et les graphiques hebdomadaires/mensuels.
 */
public class UserActivityController implements Initializable {

    // ── Stats cards ───────────────────────────────────────────────────────
    @FXML private Label todayTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Label totalSessionsLabel;
    @FXML private Label avgSessionLabel;
    @FXML private Label lastLoginLabel;
    @FXML private Label totalLoginsLabel;

    // ── Graphiques ────────────────────────────────────────────────────────
    @FXML private StackPane weeklyChartPane;
    @FXML private StackPane monthlyChartPane;

    // ── Export PDF ────────────────────────────────────────────────────────
    @FXML private Label exportStatusLabel;

    private final ActivityService     activityService     = ActivityService.getInstance();
    private final PdfExportService    pdfService          = new PdfExportService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadStats();
    }

    private void loadStats() {
        User user = SessionContext.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            long today    = activityService.getTodayTimeSeconds(user.getId());
            long total    = activityService.getTotalTimeSeconds(user.getId());
            int  sessions = activityService.getTotalSessions(user.getId());
            long avg      = activityService.getAverageSessionSeconds(user.getId());
            Map<String,Long> weekly  = activityService.getWeeklyActivity(user.getId());
            Map<String,Long> monthly = activityService.getMonthlyActivity(user.getId());

            Platform.runLater(() -> {
                setLabel(todayTimeLabel,    ActivitySession.formatDuration(today));
                setLabel(totalTimeLabel,    ActivitySession.formatDuration(total));
                setLabel(totalSessionsLabel, String.valueOf(sessions));
                setLabel(avgSessionLabel,   ActivitySession.formatDuration(avg));
                setLabel(totalLoginsLabel,  String.valueOf(sessions));

                String lastSeen = user.getLastSeen() != null
                        ? user.getLastSeen().toLocalDateTime()
                               .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                        : "—";
                setLabel(lastLoginLabel, lastSeen);

                renderBarChart(weeklyChartPane,  weekly,  "Activité — 7 derniers jours");
                renderBarChart(monthlyChartPane, monthly, "Activité — 30 derniers jours");
            });
        }, "LoadActivityThread").start();
    }

    /**
     * Dessine un graphique à barres simple sur un Canvas JavaFX.
     */
    private void renderBarChart(StackPane pane, Map<String, Long> data, String title) {
        if (pane == null || data == null || data.isEmpty()) return;
        pane.getChildren().clear();

        int    chartW    = 680;
        int    chartH    = 200;
        int    barCount  = data.size();
        int    padLeft   = 50;
        int    padRight  = 20;
        int    padTop    = 30;
        int    padBottom = 40;
        int    innerW    = chartW - padLeft - padRight;
        int    innerH    = chartH - padTop - padBottom;

        long maxVal = data.values().stream().mapToLong(Long::longValue).max().orElse(1L);
        if (maxVal == 0) maxVal = 1;

        Canvas canvas = new Canvas(chartW, chartH);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fond
        gc.setFill(Color.web("#0d1117"));
        gc.fillRect(0, 0, chartW, chartH);

        // Titre
        gc.setFill(Color.web("#94a3b8"));
        gc.setFont(javafx.scene.text.Font.font("System", 12));
        gc.fillText(title, padLeft, 18);

        // Lignes de grille horizontales
        gc.setStroke(Color.web("#1e2a45"));
        gc.setLineWidth(1);
        for (int i = 0; i <= 4; i++) {
            double y = padTop + innerH - (innerH * i / 4.0);
            gc.strokeLine(padLeft, y, chartW - padRight, y);
            long labelVal = maxVal * i / 4;
            gc.setFill(Color.web("#475569"));
            gc.fillText(ActivitySession.formatDuration(labelVal), 0, y + 4);
        }

        // Barres
        double barW   = (double) innerW / barCount;
        double barGap = Math.max(2, barW * 0.15);
        double bw     = barW - barGap * 2;

        int idx = 0;
        List<String> keys = new ArrayList<>(data.keySet());
        for (String key : keys) {
            long val = data.get(key);
            double barH = (double) val / maxVal * innerH;
            double x    = padLeft + idx * barW + barGap;
            double y    = padTop + innerH - barH;

            // Dégradé violet
            LinearGradient gradient = new LinearGradient(
                    0, y, 0, padTop + innerH, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#7c3aed")),
                    new Stop(1, Color.web("#4f46e5"))
            );
            gc.setFill(gradient);
            gc.fillRoundRect(x, y, bw, barH, 6, 6);

            // Label date (raccourci : jour/mois)
            String dayLabel = key.length() >= 10 ? key.substring(5) : key; // MM-dd
            gc.setFill(Color.web("#64748b"));
            gc.setFont(javafx.scene.text.Font.font("System", 10));
            gc.fillText(dayLabel, x, chartH - 6);

            // Valeur au-dessus si > 0
            if (val > 0) {
                gc.setFill(Color.web("#c4b5fd"));
                gc.setFont(javafx.scene.text.Font.font("System", 9));
                String v = val >= 3600
                        ? (val / 3600) + "h"
                        : val >= 60 ? (val / 60) + "m" : val + "s";
                gc.fillText(v, x + 2, y - 4);
            }
            idx++;
        }

        pane.getChildren().add(canvas);
    }

    // ── Export PDF ────────────────────────────────────────────────────────

    @FXML
    private void handleExportPdf() {
        User user = SessionContext.getCurrentUser();
        if (user == null) return;

        setExportStatus("⏳ Génération du profil en cours...", "#fbbf24");

        new Thread(() -> {
            String path = pdfService.generateUserProfilePdf(user);
            Platform.runLater(() -> {
                if (path != null) {
                    setExportStatus("✅ Profil généré ! Ouverture...", "#86efac");
                    try {
                        Desktop.getDesktop().open(new File(path));
                    } catch (Exception e) {
                        setExportStatus("✅ Fichier généré : " + path, "#86efac");
                    }
                } else {
                    setExportStatus("❌ Erreur lors de la génération.", "#fca5a5");
                }
            });
        }, "PdfExportThread").start();
    }

    @FXML
    private void handleRefresh() {
        loadStats();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void setLabel(Label l, String text) {
        if (l != null) l.setText(text);
    }

    private void setExportStatus(String msg, String color) {
        if (exportStatusLabel != null) {
            exportStatusLabel.setText(msg);
            exportStatusLabel.setStyle("-fx-text-fill:" + color + ";-fx-font-size:13px;");
            exportStatusLabel.setVisible(true);
            exportStatusLabel.setManaged(true);
        }
    }
}
