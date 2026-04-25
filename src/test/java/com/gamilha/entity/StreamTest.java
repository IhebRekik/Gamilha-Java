package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — Stream")
public class StreamTest {

    private Stream stream;

    @BeforeEach
    void setUp() {
        stream = new Stream();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur par défaut
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur par défaut initialise viewers à 0")
    void testDefaultViewers() {
        assertEquals(0, stream.getViewers());
    }

    @Test
    @DisplayName("Le constructeur par défaut initialise status à 'live'")
    void testDefaultStatus() {
        assertEquals("live", stream.getStatus());
    }

    @Test
    @DisplayName("Le constructeur par défaut génère une streamKey non nulle")
    void testStreamKeyGeneratedNotNull() {
        assertNotNull(stream.getStreamKey());
    }

    @Test
    @DisplayName("La streamKey générée fait 16 caractères hexadécimaux")
    void testStreamKeyLength() {
        assertEquals(16, stream.getStreamKey().length());
    }

    @Test
    @DisplayName("Deux streams générés ont des streamKeys différentes")
    void testStreamKeyUnique() {
        Stream other = new Stream();
        assertNotEquals(stream.getStreamKey(), other.getStreamKey());
    }

    @Test
    @DisplayName("Le constructeur par défaut initialise createdAt à maintenant")
    void testCreatedAtNotNull() {
        assertNotNull(stream.getCreatedAt());
    }

    @Test
    @DisplayName("isLive est false par défaut")
    void testIsLiveFalseByDefault() {
        assertFalse(stream.isLive());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setTitle / getTitle fonctionnent correctement")
    void testTitle() {
        stream.setTitle("CS2 Road to Global");
        assertEquals("CS2 Road to Global", stream.getTitle());
    }

    @Test
    @DisplayName("setDescription / getDescription fonctionnent correctement")
    void testDescription() {
        stream.setDescription("Ranked grind every evening");
        assertEquals("Ranked grind every evening", stream.getDescription());
    }

    @Test
    @DisplayName("setGame / getGame fonctionnent correctement")
    void testGame() {
        stream.setGame("Valorant");
        assertEquals("Valorant", stream.getGame());
    }

    @Test
    @DisplayName("setViewers / getViewers fonctionnent correctement")
    void testViewers() {
        stream.setViewers(1500);
        assertEquals(1500, stream.getViewers());
    }

    @Test
    @DisplayName("setStatus / getStatus fonctionnent correctement")
    void testStatus() {
        stream.setStatus("offline");
        assertEquals("offline", stream.getStatus());
    }

    @Test
    @DisplayName("setIsLive / isLive fonctionnent correctement")
    void testIsLive() {
        stream.setIsLive(true);
        assertTrue(stream.isLive());
    }

    @Test
    @DisplayName("setUrl / getUrl fonctionnent correctement")
    void testUrl() {
        stream.setUrl("https://player.api.video/live/abc");
        assertEquals("https://player.api.video/live/abc", stream.getUrl());
    }

    @Test
    @DisplayName("setThumbnail / getThumbnail fonctionnent correctement")
    void testThumbnail() {
        stream.setThumbnail("https://example.com/thumb.jpg");
        assertEquals("https://example.com/thumb.jpg", stream.getThumbnail());
    }

    @Test
    @DisplayName("setUserId / getUserId fonctionnent correctement")
    void testUserId() {
        stream.setUserId(42);
        assertEquals(42, stream.getUserId());
    }

    @Test
    @DisplayName("setCreatedAt / getCreatedAt fonctionnent correctement")
    void testCreatedAt() {
        LocalDateTime dt = LocalDateTime.of(2025, 1, 15, 10, 30);
        stream.setCreatedAt(dt);
        assertEquals(dt, stream.getCreatedAt());
    }

    @Test
    @DisplayName("setStreamKey / getStreamKey fonctionnent correctement")
    void testStreamKey() {
        stream.setStreamKey("abc123def456gh78");
        assertEquals("abc123def456gh78", stream.getStreamKey());
    }

    @Test
    @DisplayName("setRtmpServer / getRtmpServer fonctionnent correctement")
    void testRtmpServer() {
        stream.setRtmpServer("rtmp://broadcast.api.video/s");
        assertEquals("rtmp://broadcast.api.video/s", stream.getRtmpServer());
    }

    @Test
    @DisplayName("setApiVideoId / getApiVideoId fonctionnent correctement")
    void testApiVideoId() {
        stream.setApiVideoId("li77ACFievwSWmBsWRlQvewq");
        assertEquals("li77ACFievwSWmBsWRlQvewq", stream.getApiVideoId());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getStatusBadge()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getStatusBadge retourne '🔴 LIVE' quand status est 'live'")
    void testStatusBadgeLive() {
        stream.setStatus("live");
        assertEquals("🔴 LIVE", stream.getStatusBadge());
    }

    @Test
    @DisplayName("getStatusBadge retourne '⚫ OFFLINE' quand status est 'offline'")
    void testStatusBadgeOffline() {
        stream.setStatus("offline");
        assertEquals("⚫ OFFLINE", stream.getStatusBadge());
    }

    @Test
    @DisplayName("getStatusBadge retourne '✅ TERMINÉ' quand status est 'ended'")
    void testStatusBadgeEnded() {
        stream.setStatus("ended");
        assertEquals("✅ TERMINÉ", stream.getStatusBadge());
    }

    @Test
    @DisplayName("getStatusBadge retourne le statut en majuscules pour une valeur inconnue")
    void testStatusBadgeUnknown() {
        stream.setStatus("paused");
        assertEquals("PAUSED", stream.getStatusBadge());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString retourne 'titre (jeu)'")
    void testToString() {
        stream.setTitle("CS2 Ranked");
        stream.setGame("CS2");
        assertEquals("CS2 Ranked (CS2)", stream.toString());
    }
}
