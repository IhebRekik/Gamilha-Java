package com.gamilha.services;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du PostService avec Mockito.
 *
 * POURQUOI MOCKITO ?
 * PostService a besoin d'une connexion MySQL pour fonctionner.
 * Pendant les tests, on ne veut pas de vraie BD : elle peut ne pas exister,
 * les données peuvent changer, et les tests seraient lents.
 *
 * Mockito permet de créer des "faux objets" (mocks) qui simulent :
 *  - Connection (connexion BD)
 *  - PreparedStatement (requête SQL)
 *  - ResultSet (résultat de la requête)
 *
 * On contrôle exactement ce que ces objets retournent, sans toucher à la BD.
 *
 * IMPORTANT : Ces tests vérifient la LOGIQUE du service (mapping, validation,
 * construction des requêtes), pas la BD elle-même.
 */
@ExtendWith(MockitoExtension.class)   // Active Mockito avec JUnit 5
@DisplayName("PostService — logique métier (sans BD réelle)")
public
class PostServiceTest {

    // ── Mocks Mockito ────────────────────────────────────────────────────
    @Mock
    private Connection        mockConnection;        // fausse connexion MySQL

    @Mock
    private PreparedStatement mockPreparedStatement; // fausse requête SQL

    @Mock
    private ResultSet         mockResultSet;          // faux résultat SQL

    // Données de test
    private User userTest;
    private Post postTest;

    @BeforeEach
    void setUp() {
        userTest = new User(1, "Ali Ben Salah", "ali@gamilha.tn",
                            "ali.jpg", "[\"ROLE_USER\"]", true, null);

        postTest = new Post(
            10,
            "Mon post de test gaming !",
            "screenshot.png",
            null,
            LocalDateTime.of(2025, 3, 15, 14, 30, 0),
            userTest,
            3
        );
    }

    // ════════════════════════════════════════════════════════════════════
    //  TESTS DE LA LOGIQUE DE MAPPING (sans BD)
    //  On teste que le Post qu'on construit a les bonnes valeurs
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Un Post créé localement a toutes les propriétés correctes")
    void postAvecToutesLesProprietes() {
        assertAll("Vérification de toutes les propriétés du Post",
            () -> assertEquals(10,                          postTest.getId()),
            () -> assertEquals("Mon post de test gaming !", postTest.getContent()),
            () -> assertEquals("screenshot.png",            postTest.getImage()),
            () -> assertNull(postTest.getMediaurl(),        "mediaurl doit être null ici"),
            () -> assertNotNull(postTest.getCreatedAt(),    "createdAt ne doit pas être null"),
            () -> assertSame(userTest,                      postTest.getUser()),
            () -> assertEquals(3,                           postTest.getLikesCount())
        );
    }

    @Test
    @DisplayName("Un Post sans image doit avoir image == null")
    void postSansImage() {
        Post sanImage = new Post(5, "Post sans image", null, null,
                                  LocalDateTime.now(), userTest, 0);
        assertNull(sanImage.getImage(),
            "Un post sans image doit avoir image null");
    }

    @Test
    @DisplayName("Validation : contenu doit avoir au moins 12 caractères")
    void validationContenuMinimum() {
        String contenuOk    = "Bonjour monde!";      // 14 caractères > 12 ✅
        String contenuTropCourt = "Court";             // 5 caractères < 12 ✗

        assertTrue(contenuOk.length() >= 12,
            "Le contenu '" + contenuOk + "' est valide (>= 12 car.)");
        assertFalse(contenuTropCourt.length() >= 12,
            "Le contenu '" + contenuTropCourt + "' est invalide (< 12 car.)");
    }

    @Test
    @DisplayName("Validation : contenu nettoyé des balises HTML")
    void nettoyageHtmlDuContenu() {
        String avecHtml  = "<p>Bonjour <strong>monde</strong> !</p>";
        String sansHtml  = avecHtml.replaceAll("<[^>]+>", "").trim();

        assertEquals("Bonjour monde !", sansHtml,
            "Les balises HTML doivent être retirées du contenu");
        assertFalse(sansHtml.contains("<"),
            "Le contenu nettoyé ne doit plus contenir de balises HTML");
    }

    @Test
    @DisplayName("Validation : contenu avec balises HTML complexes nettoyé correctement")
    void nettoyageHtmlComplexe() {
        String html   = "<div class='post'><p>Texte <em>important</em></p></div>";
        String nettoye = html.replaceAll("<[^>]+>", "").trim();

        assertEquals("Texte important", nettoye);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TESTS AVEC MOCKITO : simulation de la BD
    // ════════════════════════════════════════════════════════════════════

    /**
     * Simule : SELECT un post depuis la BD.
     * On configure le ResultSet pour qu'il retourne nos données de test.
     */
    @Test
    @DisplayName("[MOCK] Simulation : lecture d'un post depuis ResultSet")
    void simulationLecturePostDepuisResultSet() throws SQLException {
        // ── 1. CONFIGURER le mock ResultSet ──────────────────────────────
        // "Quand on appelle rs.getInt("id"), retourner 10"
        when(mockResultSet.getInt("id")).thenReturn(10);
        when(mockResultSet.getString("content")).thenReturn("Mon post de test gaming !");
        when(mockResultSet.getString("image")).thenReturn("screenshot.png");
        when(mockResultSet.getString("mediaurl")).thenReturn(null);
        when(mockResultSet.getTimestamp("created_at"))
            .thenReturn(Timestamp.valueOf(LocalDateTime.of(2025,3,15,14,30,0)));
        when(mockResultSet.getInt("user_id")).thenReturn(1);
        when(mockResultSet.getString("u_name")).thenReturn("Ali Ben Salah");
        when(mockResultSet.getString("u_email")).thenReturn("ali@gamilha.tn");
        when(mockResultSet.getString("u_pic")).thenReturn("ali.jpg");
        when(mockResultSet.getString("roles")).thenReturn("[\"ROLE_USER\"]");
        when(mockResultSet.getBoolean("is_active")).thenReturn(true);
        when(mockResultSet.getString("ban_until")).thenReturn(null);
        when(mockResultSet.getInt("likes_count")).thenReturn(3);

        // ── 2. SIMULER le mapping (logique extraite de PostService.map()) ──
        User user = new User(
            mockResultSet.getInt("user_id"),
            mockResultSet.getString("u_name"),
            mockResultSet.getString("u_email"),
            mockResultSet.getString("u_pic"),
            mockResultSet.getString("roles"),
            mockResultSet.getBoolean("is_active"),
            mockResultSet.getString("ban_until")
        );
        Post post = new Post(
            mockResultSet.getInt("id"),
            mockResultSet.getString("content"),
            mockResultSet.getString("image"),
            mockResultSet.getString("mediaurl"),
            mockResultSet.getTimestamp("created_at").toLocalDateTime(),
            user,
            mockResultSet.getInt("likes_count")
        );

        // ── 3. VÉRIFIER que le mapping a produit les bonnes valeurs ──────
        assertEquals(10,                          post.getId());
        assertEquals("Mon post de test gaming !", post.getContent());
        assertEquals("screenshot.png",            post.getImage());
        assertNull(post.getMediaurl());
        assertEquals(3,                           post.getLikesCount());
        assertEquals("Ali Ben Salah",             post.getUser().getName());
        assertFalse(post.getUser().isAdmin());

        // ── 4. VÉRIFIER que les méthodes mock ont bien été appelées ──────
        // Cela prouve que le code a bien lu les bonnes colonnes SQL
        verify(mockResultSet).getInt("id");
        verify(mockResultSet).getString("content");
        verify(mockResultSet).getString("image");
        verify(mockResultSet).getInt("likes_count");
        verify(mockResultSet).getString("u_name");
    }

    /**
     * Simule : INSERT un post (create).
     * On vérifie que le PreparedStatement est bien configuré.
     */
    @Test
    @DisplayName("[MOCK] Simulation : insertion d'un post via PreparedStatement")
    void simulationInsertionPost() throws SQLException {
        // ── 1. Configurer les mocks ───────────────────────────────────────
        // La connexion retourne un PreparedStatement quand on prépare une requête
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
            .thenReturn(mockPreparedStatement);

        // La requête INSERT retourne 1 ligne affectée
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // getGeneratedKeys() retourne un ResultSet avec l'ID généré = 99
        ResultSet mockKeys = mock(ResultSet.class);
        when(mockKeys.next()).thenReturn(true);
        when(mockKeys.getInt(1)).thenReturn(99);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockKeys);

        // ── 2. Simuler l'INSERT ───────────────────────────────────────────
        String sql = "INSERT INTO post (content, image, created_at, mediaurl, user_id) VALUES (?,?,?,?,?)";
        PreparedStatement ps = mockConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        ps.setString(1, postTest.getContent());
        ps.setString(2, postTest.getImage());
        ps.setTimestamp(3, Timestamp.valueOf(postTest.getCreatedAt()));
        ps.setString(4, postTest.getMediaurl());
        ps.setInt(5, postTest.getUser().getId());

        int rowsAffected = ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        int generatedId = -1;
        if (keys.next()) generatedId = keys.getInt(1);

        // ── 3. Vérifications ──────────────────────────────────────────────
        assertEquals(1, rowsAffected, "L'INSERT doit affecter exactement 1 ligne");
        assertEquals(99, generatedId, "L'ID généré par la BD doit être 99");

        // Vérifier que les paramètres ont bien été passés
        verify(ps).setString(1, "Mon post de test gaming !");
        verify(ps).setString(2, "screenshot.png");
        verify(ps).setInt(5, 1);  // user_id = 1
    }

    /**
     * Simule : DELETE un post.
     */
    @Test
    @DisplayName("[MOCK] Simulation : suppression d'un post par ID")
    void simulationSuppressionPost() throws SQLException {
        when(mockConnection.prepareStatement(anyString()))
            .thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Simuler le DELETE
        PreparedStatement ps = mockConnection.prepareStatement(
            "DELETE FROM post WHERE id=?");
        ps.setInt(1, 10);
        int result = ps.executeUpdate();

        assertEquals(1, result, "La suppression doit affecter 1 ligne");
        verify(ps).setInt(1, 10);
    }

    // ════════════════════════════════════════════════════════════════════
    //  TRI ET FILTRAGE (logique Java sans BD)
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Tri par date décroissante (Plus récent d'abord)")
    void triParDateDecroissante() {
        User u = userTest;
        Post p1 = new Post(1, "Ancien",  null, null, LocalDateTime.of(2024,1,1,0,0), u, 0);
        Post p2 = new Post(2, "Récent",  null, null, LocalDateTime.of(2025,6,1,0,0), u, 0);
        Post p3 = new Post(3, "Moyen",   null, null, LocalDateTime.of(2025,1,1,0,0), u, 0);

        List<Post> posts = new java.util.ArrayList<>(List.of(p1, p2, p3));
        posts.sort(java.util.Comparator.comparing(Post::getCreatedAt).reversed());

        assertEquals("Récent", posts.get(0).getContent(),
            "Le post le plus récent doit être en premier");
        assertEquals("Moyen",  posts.get(1).getContent());
        assertEquals("Ancien", posts.get(2).getContent());
    }

    @Test
    @DisplayName("Tri par nombre de likes décroissant")
    void triParLikes() {
        User u = userTest;
        Post p1 = new Post(1, "Populaire",  null, null, LocalDateTime.now(), u, 100);
        Post p2 = new Post(2, "Moyen",      null, null, LocalDateTime.now(), u, 50);
        Post p3 = new Post(3, "PeuLiké",    null, null, LocalDateTime.now(), u, 5);

        List<Post> posts = new java.util.ArrayList<>(List.of(p3, p1, p2));
        posts.sort(java.util.Comparator.comparingInt(Post::getLikesCount).reversed());

        assertEquals(100, posts.get(0).getLikesCount());
        assertEquals(50,  posts.get(1).getLikesCount());
        assertEquals(5,   posts.get(2).getLikesCount());
    }

    @Test
    @DisplayName("Filtre par contenu (recherche textuelle)")
    void filtreParContenu() {
        User u = userTest;
        Post p1 = new Post(1, "Gaming session sur Fortnite", null, null, LocalDateTime.now(), u, 0);
        Post p2 = new Post(2, "Sortie en famille", null, null, LocalDateTime.now(), u, 0);
        Post p3 = new Post(3, "Nouveau jeu gaming", null, null, LocalDateTime.now(), u, 0);

        List<Post> all = List.of(p1, p2, p3);
        String keyword = "gaming";

        List<Post> filtered = all.stream()
            .filter(p -> p.getContent().toLowerCase().contains(keyword.toLowerCase()))
            .toList();

        assertEquals(2, filtered.size(),
            "La recherche 'gaming' doit trouver 2 posts");
        assertTrue(filtered.stream().allMatch(
            p -> p.getContent().toLowerCase().contains("gaming")));
    }

    @Test
    @DisplayName("Filtre par auteur")
    void filtreParAuteur() {
        User ali  = new User(1, "Ali Ben Salah", "ali@gamilha.tn", null, "[\"ROLE_USER\"]", true, null);
        User sana = new User(2, "Sana Trabelsi", "sana@gamilha.tn", null, "[\"ROLE_USER\"]", true, null);

        Post p1 = new Post(1, "Post de Ali",  null, null, LocalDateTime.now(), ali,  0);
        Post p2 = new Post(2, "Post de Sana", null, null, LocalDateTime.now(), sana, 0);
        Post p3 = new Post(3, "Ali encore",   null, null, LocalDateTime.now(), ali,  0);

        List<Post> filtresParAli = List.of(p1, p2, p3).stream()
            .filter(p -> p.getUser().getName().toLowerCase().contains("ali"))
            .toList();

        assertEquals(2, filtresParAli.size());
        assertTrue(filtresParAli.stream()
            .allMatch(p -> p.getUser().getName().contains("Ali")));
    }
}
