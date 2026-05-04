package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — CoachingVideo")
public class CoachingVideoTest {

    private CoachingVideo video;
    private Playlist      playlist;

    @BeforeEach
    void setUp() {
        video = new CoachingVideo();
        playlist = new Playlist();
        playlist.setId(1);
        playlist.setTitle("Initiation CS2");
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur par défaut
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur par défaut crée un objet non null")
    void testDefaultConstructor() {
        assertNotNull(video);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId fonctionnent correctement")
    void testId() {
        video.setId(5);
        assertEquals(5, video.getId());
    }

    @Test
    @DisplayName("setTitre / getTitre fonctionnent correctement")
    void testTitre() {
        video.setTitre("Leçon 1 — Les bases");
        assertEquals("Leçon 1 — Les bases", video.getTitre());
    }

    @Test
    @DisplayName("setDescription / getDescription fonctionnent correctement")
    void testDescription() {
        video.setDescription("Introduction aux mécaniques de base");
        assertEquals("Introduction aux mécaniques de base", video.getDescription());
    }

    @Test
    @DisplayName("setUrl / getUrl fonctionnent correctement")
    void testUrl() {
        video.setUrl("https://youtube.com/watch?v=abc123");
        assertEquals("https://youtube.com/watch?v=abc123", video.getUrl());
    }

    @Test
    @DisplayName("setNiveau / getNiveau fonctionnent correctement")
    void testNiveau() {
        video.setNiveau("Débutant");
        assertEquals("Débutant", video.getNiveau());
    }

    @Test
    @DisplayName("setPremium / isPremium fonctionnent correctement — true")
    void testPremiumTrue() {
        video.setPremium(true);
        assertTrue(video.isPremium());
    }

    @Test
    @DisplayName("isPremium est false par défaut")
    void testPremiumFalseByDefault() {
        assertFalse(video.isPremium());
    }

    @Test
    @DisplayName("setDuration / getDuration fonctionnent correctement")
    void testDuration() {
        video.setDuration(600); // 10 minutes
        assertEquals(600, video.getDuration());
    }

    @Test
    @DisplayName("setPlaylist / getPlaylist fonctionnent correctement")
    void testPlaylist() {
        video.setPlaylist(playlist);
        assertNotNull(video.getPlaylist());
        assertEquals(1, video.getPlaylist().getId());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getDurationFormatted() — méthode utilitaire
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getDurationFormatted formate 600 secondes en '10 min 00 sec'")
    void testDurationFormatted10Min() {
        video.setDuration(600);
        assertEquals("10 min 00 sec", video.getDurationFormatted());
    }

    @Test
    @DisplayName("getDurationFormatted formate 90 secondes en '1 min 30 sec'")
    void testDurationFormatted1Min30() {
        video.setDuration(90);
        assertEquals("1 min 30 sec", video.getDurationFormatted());
    }

    @Test
    @DisplayName("getDurationFormatted formate 3661 secondes en '61 min 01 sec'")
    void testDurationFormattedOver1Hour() {
        video.setDuration(3661);
        assertEquals("61 min 01 sec", video.getDurationFormatted());
    }

    @Test
    @DisplayName("getDurationFormatted formate 0 seconde en '0 min 00 sec'")
    void testDurationFormattedZero() {
        video.setDuration(0);
        assertEquals("0 min 00 sec", video.getDurationFormatted());
    }

    @Test
    @DisplayName("getDurationFormatted formate 59 secondes en '0 min 59 sec'")
    void testDurationFormattedUnder1Min() {
        video.setDuration(59);
        assertEquals("0 min 59 sec", video.getDurationFormatted());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString contient le titre de la vidéo")
    void testToStringContainsTitre() {
        video.setId(1);
        video.setTitre("Leçon 1");
        video.setDuration(120);
        assertTrue(video.toString().contains("Leçon 1"));
    }

    @Test
    @DisplayName("toString contient le niveau")
    void testToStringContainsNiveau() {
        video.setId(1);
        video.setTitre("Test");
        video.setNiveau("Avancé");
        video.setDuration(300);
        assertTrue(video.toString().contains("Avancé"));
    }

    @Test
    @DisplayName("toString contient 'null' si pas de playlist")
    void testToStringNoPlaylist() {
        video.setId(1);
        video.setTitre("Test");
        video.setDuration(60);
        assertTrue(video.toString().contains("null"));
    }

    @Test
    @DisplayName("toString contient l'ID de la playlist si définie")
    void testToStringWithPlaylist() {
        video.setId(1);
        video.setTitre("Test");
        video.setDuration(60);
        video.setPlaylist(playlist);
        assertTrue(video.toString().contains("1")); // playlist_id=1
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeurs avec paramètres
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Constructeur sans ID initialise correctement les champs")
    void testConstructorWithoutId() {
        CoachingVideo v = new CoachingVideo(
            "Titre", "Desc", "https://url.com", "Avancé", true, 300, playlist);
        assertEquals("Titre", v.getTitre());
        assertEquals("Desc", v.getDescription());
        assertTrue(v.isPremium());
        assertEquals(300, v.getDuration());
        assertEquals(playlist, v.getPlaylist());
    }

    @Test
    @DisplayName("Constructeur avec ID initialise tous les champs")
    void testConstructorWithId() {
        CoachingVideo v = new CoachingVideo(
            10, "Titre", "Desc", "https://url.com", "Avancé", false, 600, playlist);
        assertEquals(10, v.getId());
        assertEquals("Titre", v.getTitre());
        assertFalse(v.isPremium());
        assertEquals(600, v.getDuration());
    }
}
