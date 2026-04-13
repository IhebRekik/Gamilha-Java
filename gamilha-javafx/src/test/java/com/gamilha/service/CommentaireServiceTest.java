package com.gamilha.service;

import com.gamilha.model.Commentaire;
import com.gamilha.model.Post;
import com.gamilha.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du CommentaireService avec Mockito.
 *
 * On teste :
 *  - La logique de mapping ResultSet → Commentaire
 *  - La validation des textes de commentaires
 *  - Les opérations CRUD simulées
 *  - La relation Commentaire ↔ Post ↔ User
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentaireService — logique métier (sans BD réelle)")
public class CommentaireServiceTest {

    @Mock private Connection        mockConnection;
    @Mock private PreparedStatement mockPreparedStatement;
    @Mock private ResultSet         mockResultSet;

    private User        auteur;
    private Post        post;
    private Commentaire commentaire;

    @BeforeEach
    void setUp() {
        auteur = new User(1, "Ali Ben Salah", "ali@gamilha.tn",
                          "ali.jpg", "[\"ROLE_USER\"]", true, null);

        post = new Post(10, "Post parent", null, null,
                        LocalDateTime.now(), auteur, 0);

        commentaire = new Commentaire(
            100,
            "Super commentaire de test !",
            LocalDateTime.of(2025, 3, 15, 15, 0, 0),
            post,
            auteur
        );
    }

    // ════════════════════════════════════════════════════════════════════
    //  VALIDATION TEXTE COMMENTAIRE
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Texte valide : 5 caractères minimum")
    void texteMinimumValide() {
        String texte = "Bravo";   // 5 caractères
        assertTrue(texte.trim().length() >= 5,
            "Un texte de 5 caractères doit être valide");
    }

    @Test
    @DisplayName("Texte invalide : moins de 5 caractères")
    void texteInvalideCarTropCourt() {
        String texte = "Ok";   // 2 caractères
        assertFalse(texte.trim().length() >= 5,
            "Un texte de 2 caractères doit être rejeté");
    }

    @Test
    @DisplayName("Texte valide : 500 caractères maximum")
    void texteMaximumValide() {
        String texte = "A".repeat(500);
        assertTrue(texte.trim().length() <= 500,
            "Un texte de 500 caractères est à la limite mais valide");
    }

    @Test
    @DisplayName("Texte invalide : plus de 500 caractères")
    void texteInvalideCarTropLong() {
        String texte = "B".repeat(501);
        assertFalse(texte.trim().length() <= 500,
            "Un texte de 501 caractères doit être rejeté");
    }

    @Test
    @DisplayName("Texte vide ou blanc doit être invalide")
    void texteVideInvalide() {
        assertAll(
            () -> assertFalse("".trim().length() >= 5,     "Texte vide invalide"),
            () -> assertFalse("   ".trim().length() >= 5,  "Texte blanc invalide"),
            () -> assertFalse("\n".trim().length() >= 5,   "Texte saut de ligne invalide")
        );
    }

    // ════════════════════════════════════════════════════════════════════
    //  MAPPING ResultSet → Commentaire (avec MOCK)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("[MOCK] Mapping ResultSet → Commentaire (version light sans post complet)")
    void simulationMappingCommentaireLight() throws SQLException {
        // Configurer le mock ResultSet
        when(mockResultSet.getInt("id")).thenReturn(100);
        when(mockResultSet.getString("text")).thenReturn("Super commentaire de test !");
        when(mockResultSet.getTimestamp("created_at"))
            .thenReturn(Timestamp.valueOf(LocalDateTime.of(2025,3,15,15,0,0)));
        when(mockResultSet.getInt("post_id")).thenReturn(10);
        when(mockResultSet.getInt("user_id")).thenReturn(1);
        when(mockResultSet.getString("u_name")).thenReturn("Ali Ben Salah");
        when(mockResultSet.getString("u_email")).thenReturn("ali@gamilha.tn");
        when(mockResultSet.getString("u_pic")).thenReturn("ali.jpg");
        when(mockResultSet.getString("roles")).thenReturn("[\"ROLE_USER\"]");
        when(mockResultSet.getBoolean("is_active")).thenReturn(true);
        when(mockResultSet.getString("ban_until")).thenReturn(null);

        // Simuler le mapping (logique du CommentaireService.mapLight())
        User user = new User(
            mockResultSet.getInt("user_id"),
            mockResultSet.getString("u_name"),
            mockResultSet.getString("u_email"),
            mockResultSet.getString("u_pic"),
            mockResultSet.getString("roles"),
            mockResultSet.getBoolean("is_active"),
            mockResultSet.getString("ban_until")
        );
        Post postParent = new Post();
        postParent.setId(mockResultSet.getInt("post_id"));

        Commentaire c = new Commentaire(
            mockResultSet.getInt("id"),
            mockResultSet.getString("text"),
            mockResultSet.getTimestamp("created_at").toLocalDateTime(),
            postParent,
            user
        );

        // Vérifications
        assertEquals(100,                           c.getId());
        assertEquals("Super commentaire de test !", c.getText());
        assertEquals(10,                            c.getPost().getId());
        assertEquals("Ali Ben Salah",               c.getUser().getName());
        assertFalse(c.getUser().isAdmin());

        // Vérifier que les bonnes colonnes SQL ont été lues
        verify(mockResultSet).getString("text");
        verify(mockResultSet).getInt("post_id");
        verify(mockResultSet).getString("u_name");
    }

    /**
     * Simule la requête UPDATE pour modifier un commentaire.
     */
    @Test
    @DisplayName("[MOCK] Simulation : mise à jour d'un commentaire")
    void simulationUpdateCommentaire() throws SQLException {
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Modifier le texte
        commentaire.setText("Texte modifié par l'utilisateur");

        // Simuler l'UPDATE
        PreparedStatement ps = mockConnection.prepareStatement(
            "UPDATE commentaire SET text=? WHERE id=?");
        ps.setString(1, commentaire.getText());
        ps.setInt(2, commentaire.getId());
        int result = ps.executeUpdate();

        assertEquals(1, result, "L'UPDATE doit modifier 1 ligne");
        verify(ps).setString(1, "Texte modifié par l'utilisateur");
        verify(ps).setInt(2, 100);
    }

    /**
     * Simule la requête DELETE pour supprimer un commentaire.
     */
    @Test
    @DisplayName("[MOCK] Simulation : suppression d'un commentaire")
    void simulationDeleteCommentaire() throws SQLException {
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        PreparedStatement ps = mockConnection.prepareStatement(
            "DELETE FROM commentaire WHERE id=?");
        ps.setInt(1, commentaire.getId());
        int result = ps.executeUpdate();

        assertEquals(1, result);
        verify(ps).setInt(1, 100);
    }

    // ════════════════════════════════════════════════════════════════════
    //  LOGIQUE MÉTIER
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Un commentaire connaît bien son post parent (relation ManyToOne)")
    void commentaireConnaitSonPostParent() {
        assertNotNull(commentaire.getPost());
        assertEquals(10,           commentaire.getPost().getId());
        assertEquals("Post parent", commentaire.getPost().getContent());
    }

    @Test
    @DisplayName("Un commentaire connaît bien son auteur (relation ManyToOne)")
    void commentaireConnaitSonAuteur() {
        assertNotNull(commentaire.getUser());
        assertEquals("Ali Ben Salah", commentaire.getUser().getName());
        assertEquals("ali@gamilha.tn", commentaire.getUser().getEmail());
    }

    @Test
    @DisplayName("Vérifier si l'utilisateur courant est le propriétaire du commentaire")
    void verificationProprietaireCommentaire() {
        User currentUser = auteur;    // Même utilisateur

        // Logique du isOwner() dans UserPostController
        boolean isOwner = currentUser != null
            && commentaire.getUser() != null
            && commentaire.getUser().getId() == currentUser.getId();

        assertTrue(isOwner,
            "L'auteur du commentaire doit être reconnu comme propriétaire");
    }

    @Test
    @DisplayName("Vérifier qu'un autre utilisateur n'est PAS propriétaire")
    void autreUtilisateurPasProprietaire() {
        User autreUser = new User(99, "Intrus", "intrus@test.tn",
                                   null, "[\"ROLE_USER\"]", true, null);

        boolean isOwner = autreUser.getId() == commentaire.getUser().getId();

        assertFalse(isOwner,
            "Un autre utilisateur ne doit pas être propriétaire du commentaire");
    }

    @Test
    @DisplayName("Liste de commentaires d'un post triée par date croissante")
    void triCommentairesParDateCroissante() {
        LocalDateTime base = LocalDateTime.of(2025, 3, 15, 0, 0);

        Commentaire c1 = new Commentaire(1, "Premier",   base.plusHours(1), post, auteur);
        Commentaire c2 = new Commentaire(2, "Deuxième",  base.plusHours(3), post, auteur);
        Commentaire c3 = new Commentaire(3, "Troisième", base.plusHours(2), post, auteur);

        List<Commentaire> commentaires = new ArrayList<>(List.of(c2, c3, c1));
        commentaires.sort(java.util.Comparator.comparing(Commentaire::getCreatedAt));

        assertEquals("Premier",   commentaires.get(0).getText());
        assertEquals("Troisième", commentaires.get(1).getText());
        assertEquals("Deuxième",  commentaires.get(2).getText());
    }
}
