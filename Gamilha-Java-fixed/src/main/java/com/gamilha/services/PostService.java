package com.gamilha.services;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PostService — compat Symfony totale.
 *
 * Colonnes utilisées (toutes existantes dans Symfony) :
 *   - content  : texte, avec préfixe style optionnel [GRAS], [ITALIQUE], [CODE], [CITATION]
 *   - image    : toutes les images séparées par "," (Symfony ne lit que la 1ère)
 *   - mediaurl : URL YouTube/image
 *
 * Colonne optionnelle (créée si absente) :
 *   - shared_from_id : INT, référence au post original partagé
 *
 * NE CRÉE PAS : images, text_style (inutiles, compat garantie).
 */
public class PostService {

    private final Connection conn = DBConnection.getInstance();

    // Créer shared_from_id si absent — seule colonne ajoutée
    static {
        try {
            Connection c = DBConnection.getInstance();
            if (!columnExistsStatic(c, "post", "shared_from_id")) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE post ADD COLUMN shared_from_id INT DEFAULT NULL");
                    System.out.println("✅ Colonne shared_from_id créée");
                } catch (SQLException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("DB init: " + e.getMessage());
        }
    }

    private static boolean columnExistsStatic(Connection c, String table, String column) {
        try {
            DatabaseMetaData meta = c.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, table, column)) {
                return rs.next();
            }
        } catch (SQLException e) { return false; }
    }

    private static final String BASE_SQL =
        "SELECT p.id, p.content, p.image, p.created_at, p.mediaurl, p.user_id, " +
        "       u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic, " +
        "       u.roles, u.is_active, u.ban_until, " +
        "       (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) AS likes_count " +
        "FROM post p " +
        "JOIN `user` u ON u.id = p.user_id ";

    // ── CREATE ────────────────────────────────────────────────────────────
    public void create(Post post) throws SQLException {
        boolean hasShared = columnExistsStatic(conn, "post", "shared_from_id");
        String sql = hasShared
            ? "INSERT INTO post (content, image, created_at, mediaurl, user_id, shared_from_id) VALUES (?,?,?,?,?,?)"
            : "INSERT INTO post (content, image, created_at, mediaurl, user_id) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getContent());
            // image contient toutes les images séparées par ","
            ps.setString(2, post.getImage());
            ps.setTimestamp(3, Timestamp.valueOf(
                post.getCreatedAt() != null ? post.getCreatedAt() : LocalDateTime.now()));
            ps.setString(4, post.getMediaurl());
            ps.setInt(5, post.getUser().getId());
            if (hasShared) {
                if (post.getSharedFromId() != null) ps.setInt(6, post.getSharedFromId());
                else ps.setNull(6, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) post.setId(rs.getInt(1));
            }
        }
    }

    // ── READ ALL ──────────────────────────────────────────────────────────
    public List<Post> findAll() throws SQLException {
        List<Post> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(BASE_SQL + "ORDER BY p.id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ── SMART FEED ────────────────────────────────────────────────────────
    /**
     * Algorithme priorité : amis > score (likes + fraîcheur).
     * Sous-requête pour accéder aux alias calculés dans ORDER BY.
     */
    public List<Post> findSmartFeed(int currentUserId) throws SQLException {
        String sql =
            "SELECT * FROM (" +
            "  SELECT p.id, p.content, p.image, p.created_at, p.mediaurl, p.user_id," +
            "         IFNULL(p.shared_from_id, 0) AS shared_from_id," +
            "         u.name AS u_name, u.email AS u_email, u.profile_image AS u_pic," +
            "         u.roles, u.is_active, u.ban_until," +
            "         (SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id = p.id) AS likes_count," +
            "         (SELECT COUNT(*) FROM friend fw WHERE fw.user_id=? AND fw.friend_id=p.user_id) AS is_friend," +
            "         DATEDIFF(NOW(), p.created_at) AS days_ago," +
            "         ((SELECT COUNT(*) FROM post_likes pl WHERE pl.post_id=p.id)*0.5" +
            "          - DATEDIFF(NOW(), p.created_at)*0.2" +
            "          + (SELECT COUNT(*) FROM friend fw WHERE fw.user_id=? AND fw.friend_id=p.user_id)*3.0" +
            "         ) AS score" +
            "  FROM post p JOIN `user` u ON u.id=p.user_id" +
            ") AS feed ORDER BY is_friend DESC, score DESC";

        List<Post> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setInt(2, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Post p = map(rs);
                    p.setFriendPost(rs.getInt("is_friend") > 0);
                    p.setScore(rs.getDouble("score"));
                    list.add(p);
                }
            }
        }
        return list;
    }

    // ── READ BY ID ────────────────────────────────────────────────────────
    public Post findById(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(BASE_SQL + "WHERE p.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ── SEARCH ────────────────────────────────────────────────────────────
    public List<Post> search(String kw) throws SQLException {
        List<Post> list = new ArrayList<>();
        String sql = BASE_SQL + "WHERE p.content LIKE ? OR u.name LIKE ? ORDER BY p.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + kw + "%"); ps.setString(2, "%" + kw + "%");
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────
    public void update(Post post) throws SQLException {
        String sql = "UPDATE post SET content=?, image=?, mediaurl=?, user_id=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getContent());
            ps.setString(2, post.getImage());
            ps.setString(3, post.getMediaurl());
            ps.setInt(4, post.getUser().getId());
            ps.setInt(5, post.getId());
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM post WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    // ── LIKE / UNLIKE ─────────────────────────────────────────────────────
    public boolean toggleLike(int postId, int userId) throws SQLException {
        if (isLikedByUser(postId, userId)) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM post_likes WHERE post_id=? AND user_id=?")) {
                ps.setInt(1, postId); ps.setInt(2, userId); ps.executeUpdate();
            }
            return false;
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT IGNORE INTO post_likes (post_id, user_id) VALUES (?,?)")) {
                ps.setInt(1, postId); ps.setInt(2, userId); ps.executeUpdate();
            }
            return true;
        }
    }

    public boolean isLikedByUser(int postId, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM post_likes WHERE post_id=? AND user_id=?")) {
            ps.setInt(1, postId); ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        }
    }

    public int countLikes(int postId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM post_likes WHERE post_id=?")) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    // ── SHARE ─────────────────────────────────────────────────────────────
    public Post sharePost(int originalPostId, User sharedBy, String comment) throws SQLException {
        Post original = findById(originalPostId);
        if (original == null) throw new SQLException("Post original introuvable.");
        Post shared = new Post();
        // Commentaire optionnel — si vide, mettre un espace (MySQL rejette string vide selon config)
        String shareContent = (comment == null || comment.isBlank())
            ? " " : comment.trim();
        shared.setContent(shareContent);
        shared.setImage(null);
        shared.setMediaurl(null);
        shared.setUser(sharedBy);
        shared.setCreatedAt(LocalDateTime.now());
        shared.setSharedFromId(originalPostId);
        shared.setSharedPost(original);
        create(shared);
        return shared;
    }

    // ── SUGGESTION IA — simulation locale (sans clé API) ─────────────────
    public static String suggestContent(String draft) {
        try {

            String apiKey = "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";

            String body = "{"
                    + "\"model\":\"liquid/lfm-2.5-1.2b-thinking:free\","
                    + "\"messages\":[{"
                    + "\"role\":\"user\","
                    + "\"content\":\"Complète ce post gaming en une phrase: " + draft + "\""
                    + "}]"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("HTTP-Referer", "http://localhost")
                    .header("X-Title", "Gamilha")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();
            System.out.println("AI RESPONSE = " + json);

            String suggestion = extractContent(json);

            if(suggestion != null && !suggestion.isBlank())
                return suggestion;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return simulateSuggestion(draft);
    }
    private static String extractContent(String json) {

        int start = json.indexOf("\"content\":\"");
        if (start == -1) return "";

        start += 11;

        int end = json.indexOf("\",\"refusal\"", start);
        if (end == -1)
            end = json.indexOf("\"", start);

        String result = json.substring(start, end);

        return result
                .replace("\\n"," ")
                .replace("\\\"","")
                .replace("\"","")
                .trim();
    }


    /**
     * Décode les séquences XXXX en vrais caractères Unicode.
     * Nécessaire pour l'arabe, le chinois, etc. renvoyés par MyMemory en JSON.
     */
    private static String decodeUnicodeEscapes(String s) {
        if (s == null || !s.contains("\\u")) return s;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (i + 5 < s.length() && s.charAt(i) == '\\' && s.charAt(i+1) == 'u') {
                try {
                    int codePoint = Integer.parseInt(s.substring(i+2, i+6), 16);
                    sb.appendCodePoint(codePoint);
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(s.charAt(i)); i++;
                }
            } else {
                sb.append(s.charAt(i)); i++;
            }
        }
        return sb.toString();
    }

    // ── TRADUCTION — MyMemory API (gratuite, sans clé, 5000 mots/jour) ────
    /**
     * Utilise l'API MyMemory — 100% gratuite, pas de clé, pas de compte.
     * Limite : 5000 mots/jour (amplement suffisant).
     * Endpoint : https://api.mymemory.translated.net/get?q=TEXT&langpair=fr|en
     */
    public static String translateText(String text, String targetLang) {
        if (text == null || text.isBlank()) return text;
        // Nettoyer les préfixes de style avant traduction
        String cleanText = text.replaceAll("^\\[(GRAS|ITALIQUE|CODE|CITATION|PARTAGE)\\]", "").trim();
        if (cleanText.length() > 500) cleanText = cleanText.substring(0, 500);

        try {
            String encoded = URLEncoder.encode(cleanText, StandardCharsets.UTF_8);
            String sourceLang = "fr";
            // MyMemory langpair format : "fr|en"
            String apiUrl = "https://api.mymemory.translated.net/get?q=" + encoded
                + "&langpair=" + sourceLang + "|" + targetLang;

            HttpURLConnection con = (HttpURLConnection) new URL(apiUrl).openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            int status = con.getResponseCode();
            if (status == 200) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                String json = sb.toString();
                // Parser "translatedText":"..." et décoder les XXXX (arabe, etc.)
                String key = "\"translatedText\":\"";
                int idx = json.indexOf(key);
                if (idx >= 0) {
                    int start = idx + key.length();
                    int end = json.indexOf("\"", start);
                    if (end > start) {
                        String raw = json.substring(start, end);
                        // Décoder les séquences Unicode XXXX en vrais caractères
                        raw = decodeUnicodeEscapes(raw);
                        return raw.replace("\\n", "\n").replace("\\\"", "\"").trim();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("MyMemory API error: " + e.getMessage());
        }
        // Fallback
        return switch (targetLang) {
            case "en" -> "[EN] " + text;
            case "ar" -> "[AR] " + text;
            case "es" -> "[ES] " + text;
            case "de" -> "[DE] " + text;
            case "it" -> "[IT] " + text;
            default   -> text;
        };
    }

    // ── MAPPER ────────────────────────────────────────────────────────────
    /**
     * Charge un post par id avec une connexion séparée.
     * OBLIGATOIRE : MySQL ne permet pas 2 ResultSet ouverts sur la même connexion.
     * findByIdSafe est appelé depuis map() pendant qu'un RS est déjà ouvert.
     */
    private Post findByIdSafe(int id) {
        // Utiliser une nouvelle connexion pour éviter le conflit de RS
        try (Connection fresh = DBConnection.newConnection();
             PreparedStatement ps = fresh.prepareStatement(BASE_SQL + "WHERE p.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                        rs.getInt("user_id"), rs.getString("u_name"), rs.getString("u_email"),
                        rs.getString("u_pic"), rs.getString("roles"),
                        rs.getBoolean("is_active"), rs.getString("ban_until")
                    );
                    Post p = new Post(
                        rs.getInt("id"), rs.getString("content"), rs.getString("image"),
                        rs.getString("mediaurl"), rs.getTimestamp("created_at").toLocalDateTime(),
                        user, rs.getInt("likes_count")
                    );
                    p.setTextStyle(extractStyle(rs.getString("content")));
                    return p;
                }
            }
        } catch (Exception e) {
            System.err.println("findByIdSafe(" + id + ") : " + e.getMessage());
        }
        return null;
    }

    /** Map sans charger sharedPost (évite récursion infinie) */
    private Post mapSimple(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getInt("user_id"), rs.getString("u_name"), rs.getString("u_email"),
            rs.getString("u_pic"), rs.getString("roles"),
            rs.getBoolean("is_active"), rs.getString("ban_until")
        );
        Post p = new Post(
            rs.getInt("id"), rs.getString("content"), rs.getString("image"),
            rs.getString("mediaurl"), rs.getTimestamp("created_at").toLocalDateTime(),
            user, rs.getInt("likes_count")
        );
        // Extraire le style du préfixe dans le contenu
        p.setTextStyle(extractStyle(rs.getString("content")));
        return p;
    }

    private Post map(ResultSet rs) throws SQLException {
        Post p = mapSimple(rs);
        // Charger le post partagé si présent (shared_from_id peut ne pas exister dans le RS)
        try {
            int sid = rs.getInt("shared_from_id");
            if (!rs.wasNull() && sid > 0) {
                p.setSharedFromId(sid);
                Post orig = findByIdSafe(sid);
                if (orig != null) {
                    p.setSharedPost(orig);
                    System.out.println("✅ sharedPost chargé: post#" + sid + " → " +
                        PostService.stripStylePrefix(orig.getContent()).substring(
                            0, Math.min(30, PostService.stripStylePrefix(orig.getContent()).length())));
                } else {
                    System.err.println("⚠️ findByIdSafe(" + sid + ") → null");
                }
            }
        } catch (Exception e) {
            // shared_from_id absent du ResultSet (anciens posts) — normal
            System.out.println("ℹ️ pas de shared_from_id pour post#" + p.getId());
        }
        return p;
    }

    /**
     * Extrait le style depuis le préfixe du contenu :
     * "[GRAS]texte" → "Gras"
     * "texte normal" → "Normal"
     */
    public static String extractStyle(String content) {
        if (content == null) return "Normal";
        if (content.startsWith("[GRAS]"))     return "Gras";
        if (content.startsWith("[ITALIQUE]")) return "Italique";
        if (content.startsWith("[CODE]"))     return "Code";
        if (content.startsWith("[CITATION]")) return "Citation";
        return "Normal";
    }

    /**
     * Retire le préfixe de style pour obtenir le texte pur.
     */
    public static String stripStylePrefix(String content) {
        if (content == null) return "";
        return content.replaceAll("^\\[(GRAS|ITALIQUE|CODE|CITATION|PARTAGE)\\]", "").trim();
    }
    private static String simulateSuggestion(String draft) {
        if (draft == null || draft.isBlank()) return "Partagez votre expérience de jeu avec la communauté Gamilha ! 🎮";
        String lower = draft.toLowerCase();
        if (lower.contains("tournoi") || lower.contains("compétit"))
            return "... une expérience incroyable ! Le niveau était vraiment élevé, félicitations à tous ! 🏆";
        if (lower.contains("victoire") || lower.contains("gagn") || lower.contains("win"))
            return "... une fierté immense ! L'équipe a joué brillamment du début à la fin 💪";
        if (lower.contains("perdu") || lower.contains("defaite") || lower.contains("loss"))
            return "... une leçon précieuse. On analyse et on revient plus fort la prochaine fois 🔥";
        if (lower.contains("stream") || lower.contains("live"))
            return "... rejoignez-nous en direct, ce sera une session explosive 🎥";
        if (lower.contains("équipe") || lower.contains("team") || lower.contains("recruit"))
            return "... n'hésitez pas à postuler ! On cherche des joueurs motivés et passionnés 👥";
        if (lower.contains("jeu") || lower.contains("game") || lower.contains("gaming"))
            return "... une session vraiment intense, ce genre de moment rappelle pourquoi on aime l'esport ! 🎮";
        return "... votre avis compte pour toute la communauté Gamilha ! 💬";
    }

}
