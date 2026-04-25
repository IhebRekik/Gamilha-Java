package com.gamilha.controllers.admin;

import com.gamilha.services.DashboardService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.util.List;
import java.util.stream.Collectors;

public class DashboardController {

    @FXML private Label usersCount;
    @FXML private Label userGrowth;
    @FXML private Label tournoisGrowth;
    @FXML private Label streamsGrowth;
    @FXML private Label revenueGrowth;

    @FXML private WebView chartUsers;
    @FXML private WebView chartRevenue;
    @FXML private WebView chartGames;
    @FXML private WebView chartPie;

    private final DashboardService service = new DashboardService();

    @FXML
    public void initialize() {
        loadKPI();
        loadCharts();
    }

    // ================= KPI =================
    private void loadKPI() {

        usersCount.setText(String.valueOf(service.getTotalUsers()));

        setGrowth(userGrowth, service.getUserGrowth());
        setGrowth(tournoisGrowth, service.getTournoisGrowth());
        setGrowth(streamsGrowth, service.getStreamsGrowth());
        setGrowth(revenueGrowth, service.getRevenueGrowth());
    }

    private void setGrowth(Label label, double value) {

        label.setText(String.format("%.0f%%", value));

        if (value < 0) {
            label.setStyle("-fx-text-fill:#ef4444; -fx-font-size:20; -fx-font-weight:bold;");
        } else {
            label.setStyle("-fx-text-fill:#22c55e; -fx-font-size:20; -fx-font-weight:bold;");
        }
    }

    // ================= CHARTS =================
    private void loadCharts() {

        load(chartUsers, usersChart());
        load(chartRevenue, revenueChart());
        load(chartGames, gamesChart());
        load(chartPie, pieChart());
    }

    private void load(WebView webView, String html) {
        WebEngine engine = webView.getEngine();
        engine.loadContent(html);
    }

    // ================= UTILS =================
    private String labels(List<String[]> data) {
        return data.stream()
                .map(d -> d[0] == null ? "'?'" : "'" + d[0] + "'")
                .collect(Collectors.joining(","));
    }

    private String values(List<String[]> data) {
        return data.stream()
                .map(d -> d[1])
                .collect(Collectors.joining(","));
    }

    // ================= USERS =================
    private String usersChart() {

        var data = service.getUsersChartData();

        return """
    <html>
    <head>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    </head>

    <body style="background:#1e293b;">
    <canvas id="c"></canvas>

    <script>
    new Chart(document.getElementById('c'), {
      type: 'line',
      data: {
        labels: [%s],
        datasets: [{
          label:'Utilisateurs',
          data:[%s],
          borderColor:'#a855f7',
          backgroundColor:'rgba(168,85,247,0.2)',
          fill:true,
          tension:0.4
        }]
      },
      options: {
        plugins: {
          legend: { labels: { color: 'white' } }
        },
        scales: {
          x: {
            ticks: { color: 'white' },
            grid: { color: 'rgba(255,255,255,0.05)' }
          },
          y: {
            ticks: { color: 'white' },
            grid: { color: 'rgba(255,255,255,0.05)' }
          }
        }
      }
    });
    </script>
    </body>
    </html>
    """.formatted(labels(data), values(data));
    }
    private String revenueChart() {

        var data = service.getRevenueChartData();

        return """
    <html>
    <head>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    </head>

    <body style="background:#1e293b;">
    <canvas id="c"></canvas>

    <script>
    new Chart(document.getElementById('c'), {
      type: 'bar',
      data: {
        labels: [%s],
        datasets: [{
          label:'Revenus (€)',
          data:[%s],
          backgroundColor:'#22c55e'
        }]
      },
      options: {
        plugins: {
          legend: { labels: { color: 'white' } }
        },
        scales: {
          x: {
            ticks: { color: 'white' },
            grid: { color: 'rgba(255,255,255,0.05)' }
          },
          y: {
            ticks: { color: 'white' },
            grid: { color: 'rgba(255,255,255,0.05)' }
          }
        }
      }
    });
    </script>
    </body>
    </html>
    """.formatted(labels(data), values(data));
    }
    // ================= GAMES =================
    private String gamesChart() {

        var data = service.getGamesStats();

        return """
        <html><head><script src="https://cdn.jsdelivr.net/npm/chart.js"></script></head>
        <body style="background:#1e293b;">
        <canvas id="c"></canvas>
        <script>
        new Chart(document.getElementById('c'), {
          type: 'bar',
          data: {
            labels: [%s],
            datasets: [{
              label:'Tournois',
              data:[%s],
              backgroundColor:'#3b82f6'
            }]
          }
        });
        </script>
        </body></html>
        """.formatted(labels(data), values(data));
    }

    // ================= PIE =================
    private String pieChart() {

        var data = service.getSubscriptionStats();

        return """
        <html><head><script src="https://cdn.jsdelivr.net/npm/chart.js"></script></head>
        <body style="background:#1e293b;">
        <canvas id="c"></canvas>
        <script>
        new Chart(document.getElementById('c'), {
          type: 'doughnut',
          data: {
            labels: [%s],
            datasets: [{
              data:[%s]
            }]
          }
        });
        </script>
        </body></html>
        """.formatted(labels(data), values(data));
    }
}