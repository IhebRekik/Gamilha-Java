package com.gamilha.services;

import com.gamilha.utils.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DashboardService {

    private final Connection conn = ConnectionManager.getInstance().getConnection();

    // =========================
    // TOTAL USERS
    // =========================
    public int getTotalUsers() {
        try {
            String sql = "SELECT COUNT(*) FROM user";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =========================
    // GROWTH GENERIC
    // =========================
    private double calculateGrowth(String table, String dateColumn) {

        try {
            String currentMonth =
                    "SELECT COUNT(*) FROM " + table +
                            " WHERE MONTH(" + dateColumn + ") = MONTH(CURRENT_DATE())" +
                            " AND YEAR(" + dateColumn + ") = YEAR(CURRENT_DATE())";

            String lastMonth =
                    "SELECT COUNT(*) FROM " + table +
                            " WHERE MONTH(" + dateColumn + ") = MONTH(CURRENT_DATE() - INTERVAL 1 MONTH)" +
                            " AND YEAR(" + dateColumn + ") = YEAR(CURRENT_DATE() - INTERVAL 1 MONTH)";

            PreparedStatement ps1 = conn.prepareStatement(currentMonth);
            ResultSet rs1 = ps1.executeQuery();
            rs1.next();
            int current = rs1.getInt(1);

            PreparedStatement ps2 = conn.prepareStatement(lastMonth);
            ResultSet rs2 = ps2.executeQuery();
            rs2.next();
            int last = rs2.getInt(1);

            if (last == 0) return current * 100.0;

            return ((current - last) * 100.0) / last;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public double getUserGrowth() {
        return calculateGrowth("user", "created_at");
    }

    public double getTournoisGrowth() {
        return calculateGrowth("evenement", "created_at");
    }

    public double getStreamsGrowth() {
        return calculateGrowth("stream", "created_at");
    }

    public double getRevenueGrowth() {
        return calculateGrowth("historique_paiement", "created_at");
    }

    // =========================
    // USERS CHART
    // =========================
    public List<String[]> getUsersChartData() {

        List<String[]> list = new ArrayList<>();

        try {
            String sql = """
                SELECT MONTH(created_at), COUNT(*)
                FROM user
                GROUP BY MONTH(created_at)
                ORDER BY MONTH(created_at)
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt(1)),
                        String.valueOf(rs.getInt(2))
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================
    // REVENUE CHART
    // =========================
    public List<String[]> getRevenueChartData() {

        List<String[]> list = new ArrayList<>();

        try {
            String sql = """
                SELECT MONTH(created_at), SUM(montant)
                FROM historique_paiement
                GROUP BY MONTH(created_at)
                ORDER BY MONTH(created_at)
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt(1)),
                        String.valueOf(rs.getDouble(2))
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================
    // GAMES CHART
    // =========================
    public List<String[]> getGamesStats() {

        List<String[]> list = new ArrayList<>();

        String sql = """
        SELECT jeu AS jeu, COUNT(*) AS total
        FROM evenement
        GROUP BY jeu
        ORDER BY total DESC
    """;

        try (Connection conn = ConnectionManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("jeu"),
                        rs.getString("total")
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // =========================
    // PIE CHART
    // =========================
    public List<String[]> getSubscriptionStats() {

        List<String[]> list = new ArrayList<>();

        try {
            String sql = """
                SELECT type, COUNT(*)
                FROM abonnement
                GROUP BY type
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString(1),
                        String.valueOf(rs.getInt(2))
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}