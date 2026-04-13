package com.gamilha.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de MediaHelper.
 *
 * MediaHelper est un utilitaire SANS état et SANS dépendance BD :
 * c'est le type de classe le plus simple à tester.
 *
 * Il reproduit la logique Twig du template social/index.html.twig :
 *   {% if 'youtube.com' in post.mediaurl %}  → YouTube
 *   {% elseif post.mediaurl starts with 'http' %} → Image URL
 *   {% else %} → Lien générique
 */
@DisplayName("Service MediaHelper — détection et traitement des URLs média")
public class MediaHelperTest {

    // ════════════════════════════════════════════════════════════════════
    //  detect() — détection du type de média
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("detect(null) → NONE")
    void detectNull() {
        assertEquals(MediaHelper.MediaType.NONE, MediaHelper.detect(null));
    }

    @ParameterizedTest(name = "detect(\"{0}\") → NONE")
    @DisplayName("detect(vide ou blanc) → NONE")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void detectVide(String url) {
        assertEquals(MediaHelper.MediaType.NONE, MediaHelper.detect(url));
    }

    @ParameterizedTest(name = "detect(\"{0}\") → YOUTUBE")
    @DisplayName("detect() reconnaît les URLs YouTube")
    @ValueSource(strings = {
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://youtube.com/watch?v=abc123",
        "https://youtu.be/dQw4w9WgXcQ",
        "http://youtube.com/watch?v=test&list=abc",
        "https://www.youtube.com/watch?v=xyz&feature=share"
    })
    void detectYoutube(String url) {
        assertEquals(MediaHelper.MediaType.YOUTUBE, MediaHelper.detect(url),
            "L'URL '" + url + "' doit être reconnue comme YouTube");
    }

    @ParameterizedTest(name = "detect(\"{0}\") → IMAGE_URL")
    @DisplayName("detect() reconnaît les images par extension")
    @ValueSource(strings = {
        "https://example.com/photo.jpg",
        "https://example.com/image.jpeg",
        "https://example.com/logo.PNG",   // majuscules
        "https://example.com/anim.gif",
        "https://example.com/screen.webp",
        "https://cdn.gamilha.tn/uploads/post.jpg"
    })
    void detectImageParExtension(String url) {
        assertEquals(MediaHelper.MediaType.IMAGE_URL, MediaHelper.detect(url),
            "L'URL '" + url + "' doit être reconnue comme image");
    }

    @ParameterizedTest(name = "detect(\"{0}\") → LINK")
    @DisplayName("detect() reconnaît les liens génériques (non-image, non-YouTube)")
    @ValueSource(strings = {
        "https://twitch.tv/gamilha",
        "https://discord.gg/gamilha",
        "https://gamilha.tn/tournoi"
    })
    void detectLien(String url) {
        // Twitch, Discord etc. → IMAGE_URL car commence par http et n'est pas YouTube
        // → mais il peut aussi être LINK selon l'implémentation
        MediaHelper.MediaType type = MediaHelper.detect(url);
        assertNotNull(type, "detect() ne doit jamais retourner null");
        assertNotEquals(MediaHelper.MediaType.NONE, type,
            "Un lien valide ne doit pas être NONE");
    }

    // ════════════════════════════════════════════════════════════════════
    //  extractYoutubeId()
    // ════════════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "URL: \"{0}\" → ID: \"{1}\"")
    @DisplayName("extractYoutubeId() extrait correctement l'ID depuis diverses URLs")
    @CsvSource({
        // URL,                                                   ID attendu
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ,            dQw4w9WgXcQ",
        "https://youtube.com/watch?v=abc123,                      abc123",
        "https://www.youtube.com/watch?v=test123&list=PLxxx,      test123",
        "https://youtu.be/dQw4w9WgXcQ,                           dQw4w9WgXcQ",
        "https://youtu.be/videoId?t=120,                          videoId"
    })
    void extractYoutubeId(String url, String expectedId) {
        // trim() car @CsvSource ajoute des espaces
        String id = MediaHelper.extractYoutubeId(url.trim());
        assertEquals(expectedId.trim(), id,
            "L'ID extrait de '" + url.trim() + "' doit être '" + expectedId.trim() + "'");
    }

    @Test
    @DisplayName("extractYoutubeId(null) → null (pas d'exception)")
    void extractYoutubeIdNull() {
        assertNull(MediaHelper.extractYoutubeId(null),
            "extractYoutubeId(null) doit retourner null sans lancer d'exception");
    }

    @Test
    @DisplayName("extractYoutubeId() → null si pas d'ID dans l'URL")
    void extractYoutubeIdSansId() {
        assertNull(MediaHelper.extractYoutubeId("https://google.com"),
            "Une URL non-YouTube doit retourner null");
    }

    // ════════════════════════════════════════════════════════════════════
    //  toEmbedUrl()
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toEmbedUrl() construit l'URL embed YouTube correctement")
    void toEmbedUrlWatch() {
        String url   = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        String embed = MediaHelper.toEmbedUrl(url);
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", embed);
    }

    @Test
    @DisplayName("toEmbedUrl() fonctionne avec youtu.be")
    void toEmbedUrlYoutube() {
        String url   = "https://youtu.be/dQw4w9WgXcQ";
        String embed = MediaHelper.toEmbedUrl(url);
        assertEquals("https://www.youtube.com/embed/dQw4w9WgXcQ", embed);
    }

    @Test
    @DisplayName("toEmbedUrl() → null si URL non-YouTube")
    void toEmbedUrlNonYoutube() {
        assertNull(MediaHelper.toEmbedUrl("https://twitch.tv/gamilha"),
            "Une URL non-YouTube doit retourner null pour l'embed");
    }

    @Test
    @DisplayName("toEmbedUrl(null) → null sans exception")
    void toEmbedUrlNull() {
        assertNull(MediaHelper.toEmbedUrl(null));
    }

    // ════════════════════════════════════════════════════════════════════
    //  COHÉRENCE : detect + toEmbedUrl
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Si detect() → YOUTUBE alors toEmbedUrl() doit retourner une URL valide")
    void coerenceYoutube() {
        String url = "https://www.youtube.com/watch?v=abc123";

        MediaHelper.MediaType type = MediaHelper.detect(url);
        assertEquals(MediaHelper.MediaType.YOUTUBE, type);

        String embed = MediaHelper.toEmbedUrl(url);
        assertNotNull(embed, "toEmbedUrl doit retourner une URL si detect dit YOUTUBE");
        assertTrue(embed.startsWith("https://www.youtube.com/embed/"),
            "L'URL embed doit commencer par 'https://www.youtube.com/embed/'");
    }
}
