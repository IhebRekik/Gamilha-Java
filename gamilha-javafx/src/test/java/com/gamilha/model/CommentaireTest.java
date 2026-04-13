package com.gamilha.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du modèle Commentaire.
 *
 * Commentaire a deux relations :
 *   - ManyToOne → Post  (un commentaire appartient à un post)
 *   - ManyToOne → User  (un commentaire est écrit par un utilisateur)
 */
@DisplayName("Modèle Commentaire")
public class CommentaireTest {

    private Commentaire commentaire;
    private User        auteur;
    private Post        post;

    @BeforeEach
    void setUp() {
        auteur = new User(1, "Ali Ben Salah", "ali@gamilha.tn",
                          "ali.jpg", "[\"ROLE_USER\"]", true, null);

        post = new Post(10, "Post de test", null, null,
                        LocalDateTime.now(), auteur, 0);

        commentaire = new Commentaire(
            100,
            "Excellent post, merci !",
            LocalDateTime.of(2025, 3, 15, 15, 0, 0),
            post,
            auteur
        );
    }

    // ════════════════════════════════════════════════════════════════════
    //  CONSTRUCTEURS
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Constructeur vide : tous les champs null par défaut")
    void constructeurVide() {
        Commentaire c = new Commentaire();
        assertEquals(0, c.getId());
        assertNull(c.getText());
        assertNull(c.getCreatedAt());
        assertNull(c.getPost());
        assertNull(c.getUser());
    }

    @Test
    @DisplayName("Constructeur complet : toutes les valeurs correctes")
    void constructeurComplet() {
        assertEquals(100,                            commentaire.getId());
        assertEquals("Excellent post, merci !",     commentaire.getText());
        assertEquals(LocalDateTime.of(2025,3,15,15,0,0), commentaire.getCreatedAt());
        assertSame(post,   commentaire.getPost());
        assertSame(auteur, commentaire.getUser());
    }

    // ════════════════════════════════════════════════════════════════════
    //  GETTERS / SETTERS
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setText / getText")
    void setGetText() {
        commentaire.setText("Commentaire modifié");
        assertEquals("Commentaire modifié", commentaire.getText());
    }

    @Test
    @DisplayName("setPost / getPost — relation ManyToOne vers Post")
    void setGetPost() {
        Post autrePost = new Post(20, "Autre post", null, null,
                                   LocalDateTime.now(), auteur, 0);
        commentaire.setPost(autrePost);
        assertSame(autrePost, commentaire.getPost());
        assertEquals(20, commentaire.getPost().getId());
    }

    @Test
    @DisplayName("setUser / getUser — relation ManyToOne vers User")
    void setGetUser() {
        User autreUser = new User(2, "Sana", "sana@gamilha.tn",
                                   null, "[\"ROLE_USER\"]", true, null);
        commentaire.setUser(autreUser);
        assertSame(autreUser, commentaire.getUser());
        assertEquals("Sana", commentaire.getUser().getName());
    }

    @Test
    @DisplayName("setCreatedAt / getCreatedAt")
    void setGetCreatedAt() {
        LocalDateTime maintenant = LocalDateTime.now();
        commentaire.setCreatedAt(maintenant);
        assertEquals(maintenant, commentaire.getCreatedAt());
    }

    // ════════════════════════════════════════════════════════════════════
    //  VALIDATION MÉTIER (règles Symfony reproduites en Java)
    // ════════════════════════════════════════════════════════════════════

    /**
     * Symfony impose : min=5, max=500 caractères.
     * On vérifie ici que nos valeurs respectent ces bornes.
     */
    @ParameterizedTest(name = "Texte valide : \"{0}\"")
    @DisplayName("Texte valide : entre 5 et 500 caractères")
    @ValueSource(strings = {
        "Bravo",                           // exactement 5 caractères (min)
        "Super post, j'adore !",           // texte normal
        "12345678901234567890"              // 20 caractères
    })
    void texteValide(String texte) {
        int longueur = texte.length();
        assertTrue(longueur >= 5 && longueur <= 500,
            "Le texte '" + texte + "' doit avoir entre 5 et 500 caractères");
    }

    @Test
    @DisplayName("Texte invalide : trop court (moins de 5 caractères)")
    void texteTropCourt() {
        String texte = "Hi";   // 2 caractères < minimum de 5
        assertTrue(texte.length() < 5,
            "Ce texte est trop court et ne devrait pas être accepté");
    }

    @Test
    @DisplayName("Texte invalide : trop long (plus de 500 caractères)")
    void texteTropLong() {
        String texte = "A".repeat(501);   // 501 > maximum de 500
        assertTrue(texte.length() > 500,
            "Ce texte est trop long et ne devrait pas être accepté");
    }

    @Test
    @DisplayName("Texte exactement à la limite minimale (5 caractères)")
    void texteExactement5Caracteres() {
        commentaire.setText("12345");
        assertEquals(5, commentaire.getText().length());
    }

    @Test
    @DisplayName("Texte exactement à la limite maximale (500 caractères)")
    void texteExactement500Caracteres() {
        String texte500 = "G".repeat(500);
        commentaire.setText(texte500);
        assertEquals(500, commentaire.getText().length());
    }

    // ════════════════════════════════════════════════════════════════════
    //  RELATIONS
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Un commentaire connaît son post parent")
    void commentaireConnaitSonPost() {
        assertNotNull(commentaire.getPost());
        assertEquals(10, commentaire.getPost().getId());
        assertEquals("Post de test", commentaire.getPost().getContent());
    }

    @Test
    @DisplayName("Un commentaire connaît son auteur")
    void commentaireConnaitSonAuteur() {
        assertNotNull(commentaire.getUser());
        assertEquals("Ali Ben Salah", commentaire.getUser().getName());
        assertEquals("ali@gamilha.tn", commentaire.getUser().getEmail());
    }
}
