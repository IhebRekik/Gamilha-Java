package com.gamilha.services;

import com.gamilha.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * StreamAnalyticsService — Analytiques avancées des streams et donations.
 *
 * Utilise UNIQUEMENT les tables existantes : stream, donation, user.
 * Aucune nouvelle table ni entité requise.
 *
 * Métriques calculées :
 *  - Revenus totaux / par mois / par stream
 *  - Taux de conversion (viewers → donateurs)
 *  - Top jeux par viewers moyens
 *  - Heures de pointe (créneaux avec le plus de streams)
 *  - Score d'engagement par streamer
 *  - Alertes automatiques (stream sans donation, streams inactifs)
 */
public class StreamAnalyticsService {

    // ── 1. KPIs GLOBAUX ──────────────────────────────────────────────────

    /** Revenu total de toute la plateforme */
    public double getTotalRevenue() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(amount),0) FROM donation")) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    /** Nombre total de streams */
    public int getTotalStreams() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM stream")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Nombre de streams LIVE en ce moment */
    public int getLiveCount() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM stream WHERE status='live'")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Viewers totaux en cours */
    public int getTotalViewers() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(viewers),0) FROM stream WHERE status='live'")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Don moyen par donation */
    public double getAvgDonation() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(AVG(amount),0) FROM donation")) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    /** Nombre de donateurs uniques */
    public int getUniqueDonors() throws SQLException {
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(DISTINCT user_id) FROM donation")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ── 2. REVENUS MENSUELS (6 derniers mois) ───────────────────────────

    /**
     * Retourne les revenus par mois sur les 6 derniers mois.
     * @return Map<"YYYY-MM", montant>
     */
    public Map<String, Double> getMonthlyRevenue() throws SQLException {
        String sql = "SELECT DATE_FORMAT(created_at,'%Y-%m') mo, COALESCE(SUM(amount),0) total " +
                     "FROM donation WHERE created_at >= DATE_SUB(NOW(), INTERVAL 6 MONTH) " +
                     "GROUP BY mo ORDER BY mo ASC";
        Map<String, Double> map = new LinkedHashMap<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("mo"), rs.getDouble("total"));
        }
        return map;
    }

    /**
     * Revenus par jeu (top 6 jeux les plus lucratifs).
     */
    public Map<String, Double> getRevenueByGame() throws SQLException {
        String sql = "SELECT s.game, COALESCE(SUM(d.amount),0) total " +
                     "FROM donation d JOIN stream s ON d.stream_id=s.id " +
                     "GROUP BY s.game ORDER BY total DESC LIMIT 6";
        Map<String, Double> map = new LinkedHashMap<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("game") != null ? rs.getString("game") : "Autre",
                                      rs.getDouble("total"));
        }
        return map;
    }

    // ── 3. TOP STREAMERS ────────────────────────────────────────────────

    /**
     * Top 10 streamers par revenus générés.
     * @return Liste de [nom, nbStreams, totalDons, avgViewers]
     */
    public List<Map<String, Object>> getTopStreamers() throws SQLException {
        String sql = "SELECT u.name, COUNT(DISTINCT s.id) nb_streams, " +
                     "COALESCE(SUM(d.amount),0) total_dons, " +
                     "COALESCE(AVG(s.viewers),0) avg_viewers " +
                     "FROM stream s " +
                     "LEFT JOIN donation d ON d.stream_id=s.id " +
                     "LEFT JOIN user u ON s.user_id=u.id " +
                     "GROUP BY s.user_id, u.name ORDER BY total_dons DESC LIMIT 10";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int rank = 1;
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rank",       rank++);
                row.put("name",       rs.getString("name") != null ? rs.getString("name") : "Anonyme");
                row.put("nbStreams",  rs.getInt("nb_streams"));
                row.put("totalDons",  rs.getDouble("total_dons"));
                row.put("avgViewers", (int) rs.getDouble("avg_viewers"));
                list.add(row);
            }
        }
        return list;
    }

    // ── 4. TOP STREAMS PAR DON ──────────────────────────────────────────

    public List<Map<String, Object>> getTopStreamsByRevenue() throws SQLException {
        String sql = "SELECT s.title, s.game, COALESCE(SUM(d.amount),0) total, " +
                     "COUNT(d.id) nb_dons, s.viewers " +
                     "FROM stream s LEFT JOIN donation d ON d.stream_id=s.id " +
                     "GROUP BY s.id ORDER BY total DESC LIMIT 8";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("title",   rs.getString("title"));
                row.put("game",    rs.getString("game") != null ? rs.getString("game") : "—");
                row.put("total",   rs.getDouble("total"));
                row.put("nbDons",  rs.getInt("nb_dons"));
                row.put("viewers", rs.getInt("viewers"));
                list.add(row);
            }
        }
        return list;
    }

    // ── 5. TAUX DE CONVERSION ───────────────────────────────────────────

    /**
     * Taux de conversion = (streams ayant au moins 1 donation) / total streams × 100
     */
    public double getConversionRate() throws SQLException {
        String sql = "SELECT " +
                     "(SELECT COUNT(DISTINCT stream_id) FROM donation) * 100.0 / " +
                     "NULLIF((SELECT COUNT(*) FROM stream),0)";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    // ── 6. RÉPARTITION PAR JEU ──────────────────────────────────────────

    /** Nombre de streams et viewers moyens par jeu */
    public List<Map<String, Object>> getStatsByGame() throws SQLException {
        String sql = "SELECT game, COUNT(*) nb, COALESCE(AVG(viewers),0) avg_v, " +
                     "COALESCE(MAX(viewers),0) max_v " +
                     "FROM stream GROUP BY game ORDER BY nb DESC LIMIT 8";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("game",    rs.getString("game") != null ? rs.getString("game") : "Inconnu");
                row.put("nb",      rs.getInt("nb"));
                row.put("avgV",    (int) rs.getDouble("avg_v"));
                row.put("maxV",    rs.getInt("max_v"));
                list.add(row);
            }
        }
        return list;
    }

    // ── 7. ALERTES AUTOMATIQUES ─────────────────────────────────────────

    /**
     * Génère des alertes automatiques basées sur les données existantes.
     * @return Liste de [type, message, severity]
     */
    public List<Map<String, String>> getAlerts() throws SQLException {
        List<Map<String, String>> alerts = new ArrayList<>();

        // Alerte : streams live sans aucune donation
        String sql1 = "SELECT COUNT(*) FROM stream s WHERE s.status='live' " +
                      "AND NOT EXISTS (SELECT 1 FROM donation d WHERE d.stream_id=s.id)";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql1)) {
            if (rs.next() && rs.getInt(1) > 0) {
                Map<String, String> a = new LinkedHashMap<>();
                a.put("type",     "💡 Opportunité");
                a.put("message",  rs.getInt(1) + " stream(s) live sans donation — encourager les spectateurs");
                a.put("severity", "info");
                alerts.add(a);
            }
        }

        // Alerte : streamers inactifs depuis > 30 jours
        String sql2 = "SELECT COUNT(DISTINCT user_id) FROM stream " +
                      "WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                      "AND user_id NOT IN (SELECT user_id FROM stream WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY))";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql2)) {
            if (rs.next() && rs.getInt(1) > 0) {
                Map<String, String> a = new LinkedHashMap<>();
                a.put("type",     "⚠️ Inactivité");
                a.put("message",  rs.getInt(1) + " streamer(s) inactif(s) depuis +30 jours");
                a.put("severity", "warning");
                alerts.add(a);
            }
        }

        // Alerte : forte donation récente (> 20€ dans les dernières 24h)
        String sql3 = "SELECT COUNT(*), COALESCE(SUM(amount),0) FROM donation " +
                      "WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR) AND amount >= 20";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql3)) {
            if (rs.next() && rs.getInt(1) > 0) {
                Map<String, String> a = new LinkedHashMap<>();
                a.put("type",     "🚀 Performance");
                a.put("message",  rs.getInt(1) + " grande(s) donation(s) ≥20€ dans les 24h — " +
                                  String.format("%.2f€", rs.getDouble(2)) + " collectés");
                a.put("severity", "success");
                alerts.add(a);
            }
        }

        if (alerts.isEmpty()) {
            Map<String, String> a = new LinkedHashMap<>();
            a.put("type",     "✅ Tout va bien");
            a.put("message",  "Aucune alerte en ce moment");
            a.put("severity", "success");
            alerts.add(a);
        }
        return alerts;
    }

    // ── 8. SCORE D'ENGAGEMENT ───────────────────────────────────────────

    /**
     * Score d'engagement par streamer = (donations × 10) + (viewers × 0.5)
     * Utilise uniquement les données existantes.
     */
    public List<Map<String, Object>> getEngagementScores() throws SQLException {
        String sql = "SELECT u.name, " +
                     "COALESCE(SUM(d.amount),0) total_dons, " +
                     "COALESCE(AVG(s.viewers),0) avg_viewers, " +
                     "COUNT(DISTINCT s.id) nb_streams, " +
                     "(COALESCE(SUM(d.amount),0) * 2 + COALESCE(AVG(s.viewers),0) * 0.5) score " +
                     "FROM stream s " +
                     "LEFT JOIN donation d ON d.stream_id=s.id " +
                     "LEFT JOIN user u ON s.user_id=u.id " +
                     "GROUP BY s.user_id, u.name " +
                     "ORDER BY score DESC LIMIT 5";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name",      rs.getString("name") != null ? rs.getString("name") : "Anonyme");
                row.put("score",     (int) rs.getDouble("score"));
                row.put("donations", rs.getDouble("total_dons"));
                row.put("viewers",   (int) rs.getDouble("avg_viewers"));
                row.put("streams",   rs.getInt("nb_streams"));
                list.add(row);
            }
        }
        return list;
    }
}
