package com.gamilha.entity;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests métier avancés — Stream et Donation ensemble.
 *
 * Couvre les règles métier identiques aux contraintes Symfony :
 *   - @Assert\Choice(choices: ['live', 'offline', 'ended'])
 *   - @Assert\PositiveOrZero sur viewers
 *   - Génération streamKey (bin2hex equivalent)
 *   - api.video champs (apiVideoId, rtmpServer)
 */
@DisplayName("Tests métier — Stream & Donation (règles Symfony)")
public class StreamEntityBusinessTest {

    // ═══════════════════════════════════════════════════════════════════
    //  Stream — statuts valides (identique @Assert\Choice Symfony)
    // ═══════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "Statut ''{0}'' est accepté")
    @ValueSource(strings = {"live", "offline", "ended"})
    @DisplayName("Les trois statuts valides sont acceptés")
    void testStatutsValides(String statut) {
        Stream s = new Stream();
        s.setStatus(statut);
        assertEquals(statut, s.getStatus());
    }

    @ParameterizedTest(name = "Badge pour statut ''{0}'' → ''{1}''")
    @CsvSource({
        "live,    🔴 LIVE",
        "offline, ⚫ OFFLINE",
        "ended,   ✅ TERMINÉ"
    })
    @DisplayName("Chaque statut produit le bon badge")
    void testBadgesStatuts(String statut, String badge) {
        Stream s = new Stream();
        s.setStatus(statut.trim());
        assertEquals(badge.trim(), s.getStatusBadge());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Stream — api.video champs
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("apiVideoId peut être null (stream sans api.video)")
    void testApiVideoIdNullable() {
        Stream s = new Stream();
        assertNull(s.getApiVideoId());
    }

    @Test
    @DisplayName("apiVideoId stocke un ID api.video valide")
    void testApiVideoIdValide() {
        Stream s = new Stream();
        s.setApiVideoId("li77ACFievwSWmBsWRlQvewq");
        assertEquals("li77ACFievwSWmBsWRlQvewq", s.getApiVideoId());
    }

    @Test
    @DisplayName("rtmpServer par défaut est null (configuré après création api.video)")
    void testRtmpServerNullByDefault() {
        Stream s = new Stream();
        assertNull(s.getRtmpServer());
    }

    @Test
    @DisplayName("rtmpServer stocke l'URL RTMP correcte")
    void testRtmpServerValide() {
        Stream s = new Stream();
        s.setRtmpServer("rtmp://broadcast.api.video/s");
        assertEquals("rtmp://broadcast.api.video/s", s.getRtmpServer());
    }

    @Test
    @DisplayName("url (playerUrl) stocke l'URL embed api.video")
    void testUrlPlayerApiVideo() {
        Stream s = new Stream();
        s.setUrl("https://embed.api.video/live/li77ACFievwSWmBsWRlQvewq");
        assertEquals("https://embed.api.video/live/li77ACFievwSWmBsWRlQvewq", s.getUrl());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Stream — transition live/offline
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("isLive passe à true quand setIsLive(true)")
    void testTransitionOfflineToLive() {
        Stream s = new Stream();
        assertFalse(s.isLive()); // Offline par défaut
        s.setIsLive(true);
        assertTrue(s.isLive());
    }

    @Test
    @DisplayName("setIsLive(false) repasse en offline")
    void testTransitionLiveToOffline() {
        Stream s = new Stream();
        s.setIsLive(true);
        s.setIsLive(false);
        assertFalse(s.isLive());
    }

    @Test
    @DisplayName("Combinaison status='live' + isLive=true (état broadcast réel)")
    void testStateLiveBroadcasting() {
        Stream s = new Stream();
        s.setStatus("live");
        s.setIsLive(true);
        assertEquals("live", s.getStatus());
        assertTrue(s.isLive());
        assertEquals("🔴 LIVE", s.getStatusBadge());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Stream — viewers (identique @Assert\PositiveOrZero)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("viewers = 0 est valide (aucun spectateur)")
    void testViewersZeroValide() {
        Stream s = new Stream();
        s.setViewers(0);
        assertEquals(0, s.getViewers());
    }

    @Test
    @DisplayName("viewers positif est valide")
    void testViewersPositifValide() {
        Stream s = new Stream();
        s.setViewers(1500);
        assertEquals(1500, s.getViewers());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Donation — règles montants (identique emojiAmounts Symfony)
    // ═══════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "{0}€ → {1}")
    @CsvSource({
        "1.0,  🍩",
        "2.5,  🍩",
        "4.9,  🍩",
        "5.0,  🍕",
        "7.0,  🍕",
        "9.9,  🍕",
        "10.0, 💎",
        "25.0, 💎",
        "49.9, 💎",
        "50.0, 🚀",
        "100.0, 🚀",
        "999.0, 🚀"
    })
    @DisplayName("Chaque montant produit l'emoji correct")
    void testEmojiParMontant(double montant, String emoji) {
        Donation d = new Donation();
        d.setAmount(montant);
        assertEquals(emoji.trim(), d.getEmoji());
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Donation — createdAt auto
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("createdAt est initialisé automatiquement à la construction")
    void testCreatedAtAutoInit() {
        LocalDateTime avant = LocalDateTime.now().minusSeconds(1);
        Donation d = new Donation();
        LocalDateTime apres = LocalDateTime.now().plusSeconds(1);
        assertNotNull(d.getCreatedAt());
        assertTrue(d.getCreatedAt().isAfter(avant));
        assertTrue(d.getCreatedAt().isBefore(apres));
    }

    @Test
    @DisplayName("Stream.createdAt est initialisé automatiquement à la construction")
    void testStreamCreatedAtAutoInit() {
        LocalDateTime avant = LocalDateTime.now().minusSeconds(1);
        Stream s = new Stream();
        LocalDateTime apres = LocalDateTime.now().plusSeconds(1);
        assertNotNull(s.getCreatedAt());
        assertTrue(s.getCreatedAt().isAfter(avant));
        assertTrue(s.getCreatedAt().isBefore(apres));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Donation — lien stream (clé étrangère)
    // ═══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Une donation peut être associée à un stream via streamId")
    void testDonationStreamId() {
        Donation d = new Donation();
        d.setStreamId(42);
        assertEquals(42, d.getStreamId());
    }

    @Test
    @DisplayName("Une donation peut stocker le titre du stream (pour affichage)")
    void testDonationStreamTitle() {
        Donation d = new Donation();
        d.setStreamTitle("CS2 Road to Global");
        assertEquals("CS2 Road to Global", d.getStreamTitle());
    }

    @Test
    @DisplayName("Le nom du donateur est accessible")
    void testDonorName() {
        Donation d = new Donation();
        d.setDonorName("TestFan");
        assertEquals("TestFan", d.getDonorName());
    }
}
