package com.gamilha.services;

import com.gamilha.entity.ActivitySession;
import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service de suivi du temps d'activité utilisateur.
 *
 * Utilisation :
 *   ActivityService.getInstance().startSession(userId);   // à la connexion
 *   ActivityService.getInstance().endSession(userId);     // à la déconnexion
 */
public class ActivityService {

    private static ActivityService instance;
    private int currentSessionId = -1;

    private ActivityService() {}

    public static ActivityService getInstance() {
        if (instance == null) instance = new ActivityService();
        return instance;
    }

    // ── Gestion de session ────────────────────────────────────────────────

    /** Démarre une nouvelle session (à appeler après login réussi). */
    public void startSession(int userId) {
        String sql = "INSERT INTO activity_session (user_id, session_start) VALUES (?, NOW())";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) currentSessionId = rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Clôture la session en cours (à appeler au logout ou fermeture). */
    public void endSession(int userId) {
        if (currentSessionId <= 0) {
            // Chercher la session ouverte la plus récente
            currentSessionId = findOpenSession(userId);
        }
        if (currentSessionId <= 0) return;

        String sql = "UPDATE activity_session " +
                "SET session_end = NOW(), " +
                "    duration_sec = TIMESTAMPDIFF(SECOND, session_start, NOW()) " +
                "WHERE id = ? AND session_end IS NULL";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentSessionId);
            ps.executeUpdate();
        } catch (Exception e) { e.printStackTrace(); }
        currentSessionId = -1;
    }

    // ── Statistiques ──────────────────────────────────────────────────────

    /** Temps total passé sur la plateforme (toutes sessions) en secondes. */
    public long getTotalTimeSeconds(int userId) {
        String sql = "SELECT COALESCE(SUM(duration_sec), 0) FROM activity_session " +
                "WHERE user_id = ? AND duration_sec IS NOT NULL";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Temps passé aujourd'hui en secondes. */
    public long getTodayTimeSeconds(int userId) {
        String sql = "SELECT COALESCE(SUM(duration_sec), 0) FROM activity_session " +
                "WHERE user_id = ? AND DATE(session_start) = CURDATE() AND duration_sec IS NOT NULL";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Nombre total de sessions (connexions) de l'utilisateur. */
    public int getTotalSessions(int userId) {
        String sql = "SELECT COUNT(*) FROM activity_session WHERE user_id = ?";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /** Durée moyenne par session en secondes. */
    public long getAverageSessionSeconds(int userId) {
        String sql = "SELECT COALESCE(AVG(duration_sec), 0) FROM activity_session " +
                "WHERE user_id = ? AND duration_sec IS NOT NULL AND duration_sec > 0";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return (long) rs.getDouble(1);
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * Activité des 7 derniers jours.
     * Retourne une Map<Date (yyyy-MM-dd), durée totale en secondes>
     */
    public Map<String, Long> getWeeklyActivity(int userId) {
        Map<String, Long> data = new LinkedHashMap<>();
        // Pré-remplir les 7 derniers jours à 0
        for (int i = 6; i >= 0; i--) {
            String d = LocalDate.now().minusDays(i).toString();
            data.put(d, 0L);
        }
        String sql = "SELECT DATE(session_start) as day, COALESCE(SUM(duration_sec), 0) as total " +
                "FROM activity_session " +
                "WHERE user_id = ? AND session_start >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
                "  AND duration_sec IS NOT NULL " +
                "GROUP BY DATE(session_start)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String day   = rs.getString("day");
                long   total = rs.getLong("total");
                if (data.containsKey(day)) data.put(day, total);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    /**
     * Activité des 30 derniers jours.
     */
    public Map<String, Long> getMonthlyActivity(int userId) {
        Map<String, Long> data = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            String d = LocalDate.now().minusDays(i).toString();
            data.put(d, 0L);
        }
        String sql = "SELECT DATE(session_start) as day, COALESCE(SUM(duration_sec), 0) as total " +
                "FROM activity_session " +
                "WHERE user_id = ? AND session_start >= DATE_SUB(CURDATE(), INTERVAL 29 DAY) " +
                "  AND duration_sec IS NOT NULL " +
                "GROUP BY DATE(session_start)";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String day   = rs.getString("day");
                long   total = rs.getLong("total");
                if (data.containsKey(day)) data.put(day, total);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return data;
    }

    // ── Interne ───────────────────────────────────────────────────────────

    private int findOpenSession(int userId) {
        String sql = "SELECT id FROM activity_session " +
                "WHERE user_id = ? AND session_end IS NULL " +
                "ORDER BY session_start DESC LIMIT 1";
        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (Exception e) { e.printStackTrace(); }
        return -1;
    }
}
