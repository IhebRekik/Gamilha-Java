package com.gamilha.services;

import com.gamilha.entity.User;
import com.gamilha.utils.SessionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires de SessionContext.
 *
 * SessionContext gère l'utilisateur connecté (équivalent du app.user de Symfony).
 * C'est un Singleton statique, donc on doit nettoyer après chaque test
 * pour éviter que les tests se contaminent.
 */
@DisplayName("Service SessionContext — gestion de la session utilisateur")
public class SessionContextTest {

    private User userTest;

    @BeforeEach
    void setUp() {
        // Créer un utilisateur de test
        userTest = new User(1, "Ali Ben Salah", "ali@gamilha.tn",
                            "ali.jpg", "[\"ROLE_USER\"]", true, null);
        // S'assurer que la session est propre avant chaque test
        SessionContext.clear();
    }

    /**
     * @AfterEach : s'exécute APRÈS chaque test.
     * Crucial ici car SessionContext est statique :
     * si un test set un user, il faut le nettoyer pour ne pas polluer le test suivant.
     */
    @AfterEach
    void tearDown() {
        SessionContext.clear();
    }

    // ════════════════════════════════════════════════════════════════════
    //  ÉTAT INITIAL
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Au départ, aucun utilisateur connecté")
    void sessionVideeAuDepart() {
        assertNull(SessionContext.getCurrentUser(),
            "La session doit être vide au départ");
    }

    @Test
    @DisplayName("isLoggedIn() → false quand aucun utilisateur connecté")
    void isLoggedInFalseAuDepart() {
        assertFalse(SessionContext.isLoggedIn(),
            "isLoggedIn() doit être false quand la session est vide");
    }

    // ════════════════════════════════════════════════════════════════════
    //  setCurrentUser() + getCurrentUser()
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setCurrentUser() stocke l'utilisateur, getCurrentUser() le récupère")
    void setEtGetCurrentUser() {
        SessionContext.setCurrentUser(userTest);

        User recovered = SessionContext.getCurrentUser();
        assertNotNull(recovered, "Après setCurrentUser(), getCurrentUser() ne doit pas être null");
        assertSame(userTest, recovered,
            "getCurrentUser() doit retourner le MÊME objet User stocké");
    }

    @Test
    @DisplayName("isLoggedIn() → true après connexion")
    void isLoggedInTrueApresConnexion() {
        SessionContext.setCurrentUser(userTest);
        assertTrue(SessionContext.isLoggedIn());
    }

    @Test
    @DisplayName("Les informations de l'utilisateur sont accessibles depuis la session")
    void informationsUtilisateurAccessibles() {
        SessionContext.setCurrentUser(userTest);
        User u = SessionContext.getCurrentUser();

        assertEquals(1,              u.getId());
        assertEquals("Ali Ben Salah", u.getName());
        assertEquals("ali@gamilha.tn", u.getEmail());
        assertFalse(u.isAdmin(), "L'utilisateur de test n'est pas admin");
    }

    // ════════════════════════════════════════════════════════════════════
    //  clear()
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("clear() supprime l'utilisateur connecté (simuler logout)")
    void clearVideLaSession() {
        // 1. Connecter un utilisateur
        SessionContext.setCurrentUser(userTest);
        assertNotNull(SessionContext.getCurrentUser(), "Prérequis : user connecté");

        // 2. Déconnecter
        SessionContext.clear();

        // 3. Vérifier que la session est vide
        assertNull(SessionContext.getCurrentUser(),
            "Après clear(), getCurrentUser() doit être null");
        assertFalse(SessionContext.isLoggedIn(),
            "Après clear(), isLoggedIn() doit être false");
    }

    @Test
    @DisplayName("clear() sur une session déjà vide ne lève pas d'exception")
    void clearSurSessionVide() {
        assertDoesNotThrow(() -> SessionContext.clear(),
            "clear() sur une session vide ne doit pas lancer d'exception");
    }

    // ════════════════════════════════════════════════════════════════════
    //  CHANGEMENT D'UTILISATEUR
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Changer d'utilisateur connecté remplace le précédent")
    void changerUtilisateur() {
        User admin = new User(2, "Admin Gamilha", "admin@gamilha.tn",
                              null, "[\"ROLE_ADMIN\"]", true, null);

        // Connecter user normal
        SessionContext.setCurrentUser(userTest);
        assertEquals("Ali Ben Salah", SessionContext.getCurrentUser().getName());

        // Changer pour l'admin
        SessionContext.setCurrentUser(admin);
        assertEquals("Admin Gamilha", SessionContext.getCurrentUser().getName());
        assertTrue(SessionContext.getCurrentUser().isAdmin());
    }

    // ════════════════════════════════════════════════════════════════════
    //  CAS LIMITES
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setCurrentUser(null) — peut mettre null (équivaut à déconnexion)")
    void setCurrentUserNull() {
        SessionContext.setCurrentUser(userTest);
        assertNotNull(SessionContext.getCurrentUser(), "Prérequis : user connecté");

        SessionContext.setCurrentUser(null);
        assertNull(SessionContext.getCurrentUser());
        assertFalse(SessionContext.isLoggedIn());
    }

    @Test
    @DisplayName("Flux complet : connexion → utilisation → déconnexion")
    void fluxCompletConnexionDeconnexion() {
        // 1. Non connecté au départ
        assertFalse(SessionContext.isLoggedIn());

        // 2. Connexion
        SessionContext.setCurrentUser(userTest);
        assertTrue(SessionContext.isLoggedIn());
        assertEquals("Ali Ben Salah", SessionContext.getCurrentUser().getName());

        // 3. Déconnexion (logout)
        SessionContext.clear();
        assertFalse(SessionContext.isLoggedIn());
        assertNull(SessionContext.getCurrentUser());
    }
}
