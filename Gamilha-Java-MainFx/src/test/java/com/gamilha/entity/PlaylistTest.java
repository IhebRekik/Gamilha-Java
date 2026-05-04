package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — Playlist")
public class PlaylistTest {

    private Playlist playlist;

    @BeforeEach
    void setUp() {
        playlist = new Playlist();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur par défaut
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur par défaut initialise la liste de vidéos vide")
    void testVideosListInitialized() {
        assertNotNull(playlist.getVideos());
        assertTrue(playlist.getVideos().isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId fonctionnent correctement")
    void testId() {
        playlist.setId(1);
        assertEquals(1, playlist.getId());
    }

    @Test
    @DisplayName("setTitle / getTitle fonctionnent correctement")
    void testTitle() {
        playlist.setTitle("Initiation CS2");
        assertEquals("Initiation CS2", playlist.getTitle());
    }

    @Test
    @DisplayName("setDescription / getDescription fonctionnent correctement")
    void testDescription() {
        playlist.setDescription("Cours pour débutants");
        assertEquals("Cours pour débutants", playlist.getDescription());
    }

    @Test
    @DisplayName("setNiveau / getNiveau fonctionnent correctement")
    void testNiveau() {
        playlist.setNiveau("Débutant");
        assertEquals("Débutant", playlist.getNiveau());
    }

    @Test
    @DisplayName("setCategorie / getCategorie fonctionnent correctement")
    void testCategorie() {
        playlist.setCategorie("FPS");
        assertEquals("FPS", playlist.getCategorie());
    }

    @Test
    @DisplayName("setImage / getImage fonctionnent correctement")
    void testImage() {
        playlist.setImage("https://example.com/cover.jpg");
        assertEquals("https://example.com/cover.jpg", playlist.getImage());
    }

    @Test
    @DisplayName("setCreatedAt / getCreatedAt fonctionnent correctement")
    void testCreatedAt() {
        LocalDateTime dt = LocalDateTime.of(2025, 2, 10, 8, 0);
        playlist.setCreatedAt(dt);
        assertEquals(dt, playlist.getCreatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  addVideo() — relation bidirectionnelle
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("addVideo ajoute une vidéo à la liste")
    void testAddVideo() {
        CoachingVideo video = new CoachingVideo();
        video.setTitre("Leçon 1 — Les bases");
        playlist.addVideo(video);
        assertEquals(1, playlist.getVideos().size());
    }

    @Test
    @DisplayName("addVideo lie la vidéo à la playlist (relation bidirectionnelle)")
    void testAddVideoSetsPlaylist() {
        CoachingVideo video = new CoachingVideo();
        playlist.setId(5);
        playlist.addVideo(video);
        assertEquals(playlist, video.getPlaylist());
    }

    @Test
    @DisplayName("Plusieurs vidéos peuvent être ajoutées")
    void testAddMultipleVideos() {
        playlist.addVideo(new CoachingVideo());
        playlist.addVideo(new CoachingVideo());
        playlist.addVideo(new CoachingVideo());
        assertEquals(3, playlist.getVideos().size());
    }

    @Test
    @DisplayName("setVideos remplace la liste existante")
    void testSetVideos() {
        playlist.addVideo(new CoachingVideo());
        java.util.List<CoachingVideo> newList = new java.util.ArrayList<>();
        newList.add(new CoachingVideo());
        newList.add(new CoachingVideo());
        playlist.setVideos(newList);
        assertEquals(2, playlist.getVideos().size());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString contient le titre de la playlist")
    void testToStringContainsTitle() {
        playlist.setId(1);
        playlist.setTitle("Initiation CS2");
        assertTrue(playlist.toString().contains("Initiation CS2"));
    }

    @Test
    @DisplayName("toString affiche le nombre de vidéos")
    void testToStringContainsVideoCount() {
        playlist.setId(1);
        playlist.addVideo(new CoachingVideo());
        playlist.addVideo(new CoachingVideo());
        assertTrue(playlist.toString().contains("2"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeurs avec paramètres
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Constructeur sans ID initialise correctement les champs")
    void testConstructorWithoutId() {
        LocalDateTime now = LocalDateTime.now();
        Playlist p = new Playlist("Titre", "Desc", "Avancé", "FPS", "img.jpg", now);
        assertEquals("Titre", p.getTitle());
        assertEquals("Desc", p.getDescription());
        assertEquals("Avancé", p.getNiveau());
        assertEquals("FPS", p.getCategorie());
    }

    @Test
    @DisplayName("Constructeur avec ID initialise tous les champs")
    void testConstructorWithId() {
        LocalDateTime now = LocalDateTime.now();
        Playlist p = new Playlist(10, "Titre", "Desc", "Avancé", "FPS", "img.jpg", now);
        assertEquals(10, p.getId());
        assertEquals("Titre", p.getTitle());
    }
}
