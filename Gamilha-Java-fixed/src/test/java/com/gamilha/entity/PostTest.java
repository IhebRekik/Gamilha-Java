package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du modèle Post.
 *
 * Post est l'entité centrale de la partie Social.
 * On teste la construction, les relations (User, Commentaires) et les compteurs.
 */
@DisplayName("Modèle Post")
public class PostTest {

    private Post    post;
    private User    auteur;

    @BeforeEach
    void setUp() {
        auteur = new User(1, "Ali Ben Salah", "ali@gamilha.tn",
                          "ali.jpg", "[\"ROLE_USER\"]", true, null);

        post = new Post(
            10,
            "Mon premier post gaming 🎮",
            "screenshot.png",
            "https://youtube.com/watch?v=abc123",
            LocalDateTime.of(2025, 3, 15, 14, 30, 0),
            auteur,
            5  // 5 likes
        );
    }

    // ════════════════════════════════════════════════════════════════════
    //  CONSTRUCTEURS
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Constructeur vide : liste commentaires initialisée (non null)")
    void constructeurVideListeNonNull() {
        Post p = new Post();
        assertNotNull(p.getCommentaires(),
            "La liste des commentaires ne doit jamais être null");
        assertTrue(p.getCommentaires().isEmpty());
    }

    @Test
    @DisplayName("Constructeur complet : toutes les valeurs correctes")
    void constructeurComplet() {
        assertEquals(10,                                   post.getId());
        assertEquals("Mon premier post gaming 🎮",        post.getContent());
        assertEquals("screenshot.png",                    post.getImage());
        assertEquals("https://youtube.com/watch?v=abc123", post.getMediaurl());
        assertEquals(LocalDateTime.of(2025, 3, 15, 14, 30, 0), post.getCreatedAt());
        assertSame(auteur,                                 post.getUser());
        assertEquals(5,                                    post.getLikesCount());
    }

    // ════════════════════════════════════════════════════════════════════
    //  GETTERS / SETTERS
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setContent / getContent")
    void setGetContent() {
        post.setContent("Nouveau contenu modifié");
        assertEquals("Nouveau contenu modifié", post.getContent());
    }

    @Test
    @DisplayName("setImage / getImage — peut être null (post sans image)")
    void setGetImageNull() {
        post.setImage(null);
        assertNull(post.getImage(),
            "Un post peut ne pas avoir d'image");
    }

    @Test
    @DisplayName("setLikesCount / getLikesCount")
    void setGetLikesCount() {
        post.setLikesCount(42);
        assertEquals(42, post.getLikesCount());
    }

    @Test
    @DisplayName("setLikesCount(0) — un post peut avoir 0 like")
    void zeroLikes() {
        post.setLikesCount(0);
        assertEquals(0, post.getLikesCount());
    }

    @Test
    @DisplayName("setUser / getUser — relation ManyToOne vers User")
    void setGetUser() {
        User nouveauUser = new User(2, "Sana", "sana@gamilha.tn",
                                    null, "[\"ROLE_USER\"]", true, null);
        post.setUser(nouveauUser);
        assertSame(nouveauUser, post.getUser());
        assertEquals("Sana", post.getUser().getName());
    }

    // ════════════════════════════════════════════════════════════════════
    //  RELATION OneToMany → Commentaires
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setCommentaires / getCommentaires — relation OneToMany")
    void setGetCommentaires() {
        List<Commentaire> liste = new ArrayList<>();
        Commentaire c1 = new Commentaire();
        c1.setText("Super post !");
        Commentaire c2 = new Commentaire();
        c2.setText("Bien joué !");
        liste.add(c1);
        liste.add(c2);

        post.setCommentaires(liste);

        assertEquals(2, post.getCommentaires().size());
        assertEquals("Super post !", post.getCommentaires().get(0).getText());
    }

    @Test
    @DisplayName("getCommentaires() ne doit pas retourner null si non initialisé")
    void commentairesNonNull() {
        Post p = new Post();
        assertNotNull(p.getCommentaires());
    }

    // ════════════════════════════════════════════════════════════════════
    //  DATES
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setCreatedAt / getCreatedAt")
    void setGetCreatedAt() {
        LocalDateTime maintenant = LocalDateTime.now();
        post.setCreatedAt(maintenant);
        assertEquals(maintenant, post.getCreatedAt());
    }

    @Test
    @DisplayName("Le post le plus récent a une date après le post le plus ancien")
    void comparerDatesPosts() {
        Post ancien  = new Post(1, "Ancien", null, null,
            LocalDateTime.of(2024, 1, 1, 0, 0), auteur, 0);
        Post recent  = new Post(2, "Récent", null, null,
            LocalDateTime.of(2025, 6, 1, 0, 0), auteur, 0);

        assertTrue(
            recent.getCreatedAt().isAfter(ancien.getCreatedAt()),
            "Le post récent doit avoir une date après le post ancien"
        );
    }
}
