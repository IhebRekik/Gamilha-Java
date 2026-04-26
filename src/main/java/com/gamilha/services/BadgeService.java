package com.gamilha.services;

import com.gamilha.utils.DatabaseConnection;

import java.sql.*;
import java.util.*;

/**
 * BadgeService — Métier Badges & Récompenses.
 *
 * Calcule dynamiquement les badges, stats et rang d'un user
 * à partir des tables EXISTANTES (stream, donation, user).
 * AUCUNE nouvelle table en base.
 *
 * Badges débloqués selon des seuils (identique à un système de achievements) :
 *  - STREAM : Premier Stream, Streamer Régulier, Streamer Pro, Légende
 *  - DONATION : Généreux, Grand Donateur, Mécène
 *  - VIEWERS : Populaire (viewers cumulés)
 *  - DIVERSITÉ : Gamer Polyvalent (jeux différents)
 *  - FIDÉLITÉ : Ancienneté compte
 */
public class BadgeService {

    // ── Définition des badges ─────────────────────────────────────────────
    public static final List<BadgeDef> ALL_BADGES = List.of(
        // Streams
        new BadgeDef("first_stream",   "🎬 Premier Stream",      "Lance ton premier stream",           "stream",   1,    "bronze"),
        new BadgeDef("streamer_5",     "📡 Streamer Régulier",   "5 streams lancés",                   "stream",   5,    "silver"),
        new BadgeDef("streamer_20",    "🔴 Streamer Pro",        "20 streams lancés",                  "stream",   20,   "gold"),
        new BadgeDef("streamer_50",    "🏆 Légende du Stream",   "50 streams lancés",                  "stream",   50,   "diamond"),
        // Donations données
        new BadgeDef("donor_1",        "💝 Généreux",            "1ère donation effectuée",            "donated",  1,    "bronze"),
        new BadgeDef("donor_5",        "💰 Grand Donateur",      "5 donations effectuées",             "donated",  5,    "silver"),
        new BadgeDef("donor_20",       "👑 Mécène",              "20 donations effectuées",            "donated",  20,   "gold"),
        // Donations reçues
        new BadgeDef("received_10",    "🌟 Soutenu",             "Recevoir 10 donations",              "received", 10,   "silver"),
        new BadgeDef("received_50",    "💎 Star de la Plateforme","Recevoir 50 donations",             "received", 50,   "gold"),
        // Viewers cumulés
        new BadgeDef("viewers_100",    "👁 Influenceur",         "100 viewers cumulés",                "viewers",  100,  "silver"),
        new BadgeDef("viewers_1000",   "🚀 Megastar",            "1000 viewers cumulés",               "viewers",  1000, "gold"),
        // Jeux différents
        new BadgeDef("games_2",        "🎮 Gamer Polyvalent",    "Streamer 2 jeux différents",         "games",    2,    "bronze"),
        new BadgeDef("games_4",        "🕹 Maître des Jeux",     "Streamer 4 jeux différents",         "games",    4,    "gold"),
        // Montant total donné
        new BadgeDef("amount_50",      "💸 Bienfaiteur",         "Donner 50€ au total",                "amount",   50,   "silver"),
        new BadgeDef("amount_200",     "🏅 Philanthrope",        "Donner 200€ au total",               "amount",   200,  "gold")
    );

    /** Récupère toutes les stats d'un user depuis la BDD */
    public UserStats getStats(int userId) throws SQLException {
        UserStats s = new UserStats(userId);

        // Nombre de streams lancés
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT COUNT(*), COALESCE(SUM(viewers),0), COUNT(DISTINCT game) FROM stream WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                s.streamCount     = rs.getInt(1);
                s.totalViewers    = rs.getInt(2);
                s.distinctGames   = rs.getInt(3);
            }
        }

        // Donations DONNÉES par ce user
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT COUNT(*), COALESCE(SUM(amount),0) FROM donation WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                s.donationsGiven  = rs.getInt(1);
                s.totalAmountGiven = rs.getDouble(2);
            }
        }

        // Donations REÇUES sur les streams de ce user
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT COUNT(*), COALESCE(SUM(d.amount),0) FROM donation d " +
                                  "INNER JOIN stream st ON d.stream_id=st.id WHERE st.user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                s.donationsReceived   = rs.getInt(1);
                s.totalAmountReceived = rs.getDouble(2);
            }
        }

        // Rang global (classement par nombre de streams)
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT COUNT(*)+1 FROM (SELECT user_id, COUNT(*) cnt FROM stream GROUP BY user_id HAVING cnt > " +
                                  "(SELECT COUNT(*) FROM stream WHERE user_id=?)) ranked")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) s.rank = rs.getInt(1);
        }

        return s;
    }

    /** Calcule les badges débloqués pour un user */
    public List<BadgeResult> computeBadges(UserStats stats) {
        List<BadgeResult> results = new ArrayList<>();
        for (BadgeDef def : ALL_BADGES) {
            int current = switch (def.type) {
                case "stream"   -> stats.streamCount;
                case "donated"  -> stats.donationsGiven;
                case "received" -> stats.donationsReceived;
                case "viewers"  -> stats.totalViewers;
                case "games"    -> stats.distinctGames;
                case "amount"   -> (int) stats.totalAmountGiven;
                default -> 0;
            };
            boolean unlocked = current >= def.threshold;
            int progress = unlocked ? 100 : (def.threshold > 0 ? (int)(current * 100.0 / def.threshold) : 0);
            results.add(new BadgeResult(def, unlocked, current, progress));
        }
        return results;
    }

    /** Calcule le niveau global (1–5) basé sur les badges débloqués */
    public int computeLevel(List<BadgeResult> badges) {
        long unlocked = badges.stream().filter(b -> b.unlocked).count();
        if (unlocked >= 12) return 5;
        if (unlocked >= 8)  return 4;
        if (unlocked >= 5)  return 3;
        if (unlocked >= 2)  return 2;
        return 1;
    }

    /** XP total = badges × points selon rareté */
    public int computeXP(List<BadgeResult> badges) {
        return badges.stream().filter(b -> b.unlocked)
                .mapToInt(b -> switch (b.def.rarity) {
                    case "bronze"  -> 10;
                    case "silver"  -> 25;
                    case "gold"    -> 50;
                    case "diamond" -> 100;
                    default -> 10;
                }).sum();
    }

    // ── Records imbriqués ─────────────────────────────────────────────────

    public record BadgeDef(
        String id, String name, String description,
        String type, int threshold, String rarity
    ) {}

    public record BadgeResult(
        BadgeDef def, boolean unlocked, int current, int progress
    ) {}

    public static class UserStats {
        public int    userId;
        public int    streamCount;
        public int    totalViewers;
        public int    distinctGames;
        public int    donationsGiven;
        public int    donationsReceived;
        public double totalAmountGiven;
        public double totalAmountReceived;
        public int    rank = 1;
        public UserStats(int userId) { this.userId = userId; }
    }
}
