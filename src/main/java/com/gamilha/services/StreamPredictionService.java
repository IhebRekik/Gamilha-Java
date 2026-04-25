package com.gamilha.services;

import com.gamilha.entity.Stream;
import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * StreamPredictionService — équivalent exact de StreamPredictionService.php Symfony.
 *
 * Deux modes :
 *   1. predictStreams()   — algorithme statistique local (toujours disponible)
 *   2. predictWithAI()   — via API OpenAI/Claude (si clé configurée), sinon fallback local
 *
 * Identique à Symfony : getStreamerContributions(), calculateBasePrediction(),
 * calculateConfidence(), generateRecommendations()
 */
public class StreamPredictionService {

    // Optionnel : clé OpenAI/Claude pour prédiction IA
    private static final String AI_API_KEY = "";   // laisser vide = mode local
    private static final String AI_API_URL = "https://api.openai.com/v1/chat/completions";

    // ─────────────────────────────────────────────────────────────────
    // Collecte des contributions — identique à getStreamerContributions()
    // ─────────────────────────────────────────────────────────────────
    /**
     * Collecte les statistiques d'un streamer sur les N derniers jours.
     * Identique à StreamPredictionService::getStreamerContributions() Symfony.
     *
     * @param userId      ID de l'utilisateur
     * @param daysHistory nombre de jours d'historique (défaut 90)
     */
    public Map<String, Object> getStreamerContributions(int userId, int daysHistory) throws SQLException {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysHistory);

        // Streams récents
        List<Stream> recentStreams = findStreamsSince(userId, cutoff);
        // Donations récentes
        List<double[]> recentDonations = findDonationsSince(userId, cutoff);

        int    totalStreams   = recentStreams.size();
        int    totalViewers   = recentStreams.stream().mapToInt(Stream::getViewers).sum();
        int    totalDonations = recentDonations.size();
        double donationAmount = recentDonations.stream().mapToDouble(a -> a[0]).sum();

        double weeks          = Math.max(1.0, daysHistory / 7.0);
        double streamsPerWeek = totalStreams / weeks;
        double avgViewers     = totalStreams > 0 ? (double) totalViewers / totalStreams : 0;

        // Dernier stream
        LocalDateTime lastStreamDate = recentStreams.stream()
                .map(Stream::getCreatedAt)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        Long daysSinceLastStream = null;
        if (lastStreamDate != null) {
            daysSinceLastStream = java.time.temporal.ChronoUnit.DAYS.between(
                    lastStreamDate, LocalDateTime.now());
        }

        String userName = getUserName(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user_id",                  userId);
        result.put("user_name",                userName);
        result.put("period_days",              daysHistory);
        result.put("total_streams",            totalStreams);
        result.put("streams_per_week",         Math.round(streamsPerWeek * 100.0) / 100.0);
        result.put("total_viewers",            totalViewers);
        result.put("avg_viewers_per_stream",   Math.round(avgViewers * 100.0) / 100.0);
        result.put("total_donations",          totalDonations);
        result.put("total_donation_amount",    Math.round(donationAmount * 100.0) / 100.0);
        result.put("avg_donation_per_stream",  totalStreams > 0
                ? Math.round((donationAmount / totalStreams) * 100.0) / 100.0 : 0.0);
        result.put("days_since_last_stream",   daysSinceLastStream);
        result.put("last_stream_date",         lastStreamDate != null
                ? lastStreamDate.toString() : null);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // Prédiction statistique locale — identique à predictStreams()
    // ─────────────────────────────────────────────────────────────────
    public Map<String, Object> predictStreams(int userId, int daysAhead) throws SQLException {
        Map<String, Object> contributions = getStreamerContributions(userId, 90);

        double basePrediction = calculateBasePrediction(contributions, daysAhead);
        double confidence     = calculateConfidence(contributions);
        List<Map<String, String>> recommendations = generateRecommendations(contributions, daysAhead);

        Map<String, Object> prediction = new LinkedHashMap<>();
        prediction.put("streams_expected", (long) Math.round(basePrediction));
        prediction.put("streams_min",      (long) Math.round(basePrediction * (1 - (1 - confidence) * 0.5)));
        prediction.put("streams_max",      (long) Math.round(basePrediction * (1 + (1 - confidence) * 0.5)));
        prediction.put("period_days",      daysAhead);
        prediction.put("confidence_level", confidence);
        prediction.put("confidence_text",  getConfidenceText(confidence));

        Map<String, Object> modelInfo = new LinkedHashMap<>();
        modelInfo.put("version",      "1.0.0");
        modelInfo.put("type",         "statistical_prediction");
        modelInfo.put("last_updated", LocalDateTime.now().toString());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prediction",       prediction);
        result.put("contributions",    contributions);
        result.put("recommendations",  recommendations);
        result.put("model_info",       modelInfo);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // Prédiction avec IA — identique à predictWithAI()
    //   → si clé AI non configurée, fallback vers predictStreams()
    // ─────────────────────────────────────────────────────────────────
    public Map<String, Object> predictWithAI(int userId, int daysAhead) throws Exception {
        if (AI_API_KEY == null || AI_API_KEY.isBlank()) {
            return predictStreams(userId, daysAhead);
        }
        Map<String, Object> contributions = getStreamerContributions(userId, 90);
        String prompt = buildAIPrompt(contributions, daysAhead);
        try {
            String response = callAIApi(prompt);
            return parseAIResponse(response, contributions, daysAhead);
        } catch (Exception ex) {
            System.err.println("AI prediction fallback: " + ex.getMessage());
            return predictStreams(userId, daysAhead);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Algorithmes — identiques à Symfony
    // ─────────────────────────────────────────────────────────────────
    private double calculateBasePrediction(Map<String, Object> c, int daysAhead) {
        double streamsPerWeek      = toDouble(c.get("streams_per_week"));
        Long   daysSinceLastStream = (Long) c.get("days_since_last_stream");
        int    totalDonations      = toInt(c.get("total_donations"));
        int    totalStreams         = toInt(c.get("total_streams"));

        double pred = streamsPerWeek * (daysAhead / 7.0);

        if (daysSinceLastStream != null) {
            if      (daysSinceLastStream > 30) pred *= 0.7;
            else if (daysSinceLastStream > 14) pred *= 0.85;
            else if (daysSinceLastStream < 3)  pred *= 1.1;
        }
        if      (totalDonations > 10)                         pred *= 1.15;
        else if (totalDonations == 0 && totalStreams > 0)     pred *= 0.9;

        return Math.max(0, pred);
    }

    private double calculateConfidence(Map<String, Object> c) {
        int totalStreams = toInt(c.get("total_streams"));
        if      (totalStreams >= 20) return 0.85;
        else if (totalStreams >= 10) return 0.70;
        else if (totalStreams >= 5)  return 0.55;
        else if (totalStreams >= 1)  return 0.40;
        return 0.20;
    }

    private String getConfidenceText(double confidence) {
        if      (confidence >= 0.8) return "Très haute";
        else if (confidence >= 0.6) return "Haute";
        else if (confidence >= 0.4) return "Moyenne";
        else if (confidence >= 0.2) return "Basse";
        return "Très basse";
    }

    private List<Map<String, String>> generateRecommendations(Map<String, Object> c, int daysAhead) {
        List<Map<String, String>> recs = new ArrayList<>();

        double streamsPerWeek      = toDouble(c.get("streams_per_week"));
        int    totalDonations      = toInt(c.get("total_donations"));
        int    totalStreams         = toInt(c.get("total_streams"));
        double avgViewers          = toDouble(c.get("avg_viewers_per_stream"));
        Long   daysSinceLastStream = (Long) c.get("days_since_last_stream");

        if (streamsPerWeek < 1) {
            recs.add(Map.of("type", "frequency", "priority", "high",
                    "message", "Augmentez votre fréquence à au moins 1 stream par semaine pour grow votre audience."));
        } else if (streamsPerWeek >= 3) {
            recs.add(Map.of("type", "frequency", "priority", "medium",
                    "message", "Excellente fréquence de streaming ! Maintenez ce rythme."));
        }

        if (totalDonations == 0 && totalStreams > 0) {
            recs.add(Map.of("type", "engagement", "priority", "high",
                    "message", "Travaillez sur l'engagement de votre communauté pour générer des donations."));
        }

        if (daysSinceLastStream != null && daysSinceLastStream > 14) {
            recs.add(Map.of("type", "consistency", "priority", "high",
                    "message", "Revenez streamer régulièrement pour maintenir votre audience."));
        }

        if (avgViewers < 10 && totalStreams > 0) {
            recs.add(Map.of("type", "viewers", "priority", "medium",
                    "message", "Travaillez sur la promotion de vos streams pour augmenter votre audience."));
        }

        return recs;
    }

    // ─────────────────────────────────────────────────────────────────
    // AI API (OpenAI-compatible) — identique à callAIApi() Symfony
    // ─────────────────────────────────────────────────────────────────
    private String buildAIPrompt(Map<String, Object> contributions, int daysAhead) {
        return "Tu es un expert en analyse de données de streaming. " +
               "Basé sur les statistiques suivantes d'un streamer:\n\n" +
               contributions.toString() + "\n\n" +
               "Prédit le nombre de streams qu'il va faire dans les " + daysAhead +
               " prochains jours. Réponds uniquement en JSON avec: " +
               "predicted_streams (nombre), confidence (0-1), recommendations (tableau de conseils).";
    }

    private String callAIApi(String prompt) throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        String body = "{\"model\":\"gpt-3.5-turbo\",\"temperature\":0.7," +
                "\"messages\":[{\"role\":\"user\",\"content\":\"" +
                prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}";

        java.net.http.HttpResponse<String> resp = client.send(
                java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(AI_API_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + AI_API_KEY)
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAIResponse(String rawResponse,
                                                 Map<String, Object> contributions,
                                                 int daysAhead) throws SQLException {
        // Extraire le contenu du message de l'API
        String content = extractJsonField(rawResponse, "content");
        if (content == null) return predictStreams(
                toInt(contributions.get("user_id")), daysAhead);

        long   predicted = parseLong(extractJsonField(content, "predicted_streams"), 0L);
        double conf      = parseDouble(extractJsonField(content, "confidence"), 0.5);

        Map<String, Object> prediction = new LinkedHashMap<>();
        prediction.put("streams_expected", predicted);
        prediction.put("streams_min",      predicted);
        prediction.put("streams_max",      predicted);
        prediction.put("period_days",      daysAhead);
        prediction.put("confidence_level", conf);
        prediction.put("confidence_text",  "AI Generated");

        Map<String, Object> modelInfo = new LinkedHashMap<>();
        modelInfo.put("version",   "1.0.0");
        modelInfo.put("type",      "ai_enhanced_prediction");
        modelInfo.put("ai_model",  "gpt-3.5-turbo");
        modelInfo.put("last_updated", LocalDateTime.now().toString());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prediction",      prediction);
        result.put("contributions",   contributions);
        result.put("recommendations", new ArrayList<>());
        result.put("model_info",      modelInfo);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    // Requêtes SQL
    // ─────────────────────────────────────────────────────────────────
    private List<Stream> findStreamsSince(int userId, LocalDateTime since) throws SQLException {
        List<Stream> list = new ArrayList<>();
        String sql = "SELECT * FROM stream WHERE user_id=? AND created_at >= ? ORDER BY created_at DESC";
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(since));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Stream s = new Stream();
                s.setId(rs.getInt("id"));
                s.setViewers(rs.getInt("viewers"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
                list.add(s);
            }
        }
        return list;
    }

    private List<double[]> findDonationsSince(int userId, LocalDateTime since) throws SQLException {
        List<double[]> list = new ArrayList<>();
        // donations reçues sur les streams du streamer
        String sql = "SELECT d.amount FROM donation d " +
                     "JOIN stream s ON d.stream_id = s.id " +
                     "WHERE s.user_id=? AND d.created_at >= ?";
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(since));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(new double[]{rs.getDouble("amount")});
        }
        return list;
    }

    private String getUserName(int userId) throws SQLException {
        try (PreparedStatement ps = ConnectionManager.getConnection()
                .prepareStatement("SELECT name FROM user WHERE id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("name");
        }
        return "Streamer";
    }

    // ─────────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────────
    private double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0.0; }
    }

    private int toInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; }
    }

    private String extractJsonField(String json, String key) {
        if (json == null) return null;
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        // skip whitespace
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return end == -1 ? null : json.substring(start + 1, end);
        }
        // number
        int end = start;
        while (end < json.length() && ",}\n".indexOf(json.charAt(end)) == -1) end++;
        return json.substring(start, end).trim();
    }

    private long parseLong(String s, long def) {
        if (s == null) return def;
        try { return Long.parseLong(s.trim().replaceAll("[^0-9\\-]", "")); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        if (s == null) return def;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }
}
