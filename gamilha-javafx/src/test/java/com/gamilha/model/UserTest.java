package com.gamilha.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du modèle User.
 *
 * On teste ici :
 *  - Le constructeur vide et le constructeur complet
 *  - Chaque getter / setter
 *  - La méthode métier isAdmin() qui lit le champ roles JSON
 *  - La méthode toString()
 *
 * Aucune connexion BD n'est nécessaire : ce sont des tests purs Java.
 */
@DisplayName("Modèle User")
public
class UserTest {

    // Objet réutilisé dans chaque test
    private User user;

    /**
     * @BeforeEach : s'exécute AVANT chaque méthode @Test.
     * On recrée un User propre pour que les tests soient indépendants.
     */
    @BeforeEach
    void setUp() {
        user = new User(
            1,
            "Ali Ben Salah",
            "ali@gamilha.tn",
            "ali.jpg",
            "[\"ROLE_USER\"]",
            true,
            null
        );
    }

    // ════════════════════════════════════════════════════════════════════
    //  CONSTRUCTEURS
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Constructeur vide : tous les champs sont null / par défaut")
    void constructeurVide() {
        User u = new User();
        assertEquals(0, u.getId(),      "id doit être 0 par défaut");
        assertNull(u.getName(),         "name doit être null");
        assertNull(u.getEmail(),        "email doit être null");
        assertNull(u.getProfileImage(), "profileImage doit être null");
        assertNull(u.getRoles(),        "roles doit être null");
        assertFalse(u.isActive(),       "isActive doit être false par défaut");
        assertNull(u.getBanUntil(),     "banUntil doit être null");
    }

    @Test
    @DisplayName("Constructeur complet : toutes les valeurs sont bien stockées")
    void constructeurComplet() {
        assertEquals(1,                     user.getId());
        assertEquals("Ali Ben Salah",       user.getName());
        assertEquals("ali@gamilha.tn",      user.getEmail());
        assertEquals("ali.jpg",             user.getProfileImage());
        assertEquals("[\"ROLE_USER\"]",     user.getRoles());
        assertTrue(user.isActive());
        assertNull(user.getBanUntil());
    }

    // ════════════════════════════════════════════════════════════════════
    //  GETTERS / SETTERS
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId")
    void setGetId() {
        user.setId(42);
        assertEquals(42, user.getId());
    }

    @Test
    @DisplayName("setName / getName")
    void setGetName() {
        user.setName("Sana Trabelsi");
        assertEquals("Sana Trabelsi", user.getName());
    }

    @Test
    @DisplayName("setEmail / getEmail")
    void setGetEmail() {
        user.setEmail("sana@gamilha.tn");
        assertEquals("sana@gamilha.tn", user.getEmail());
    }

    @Test
    @DisplayName("setProfileImage / getProfileImage")
    void setGetProfileImage() {
        user.setProfileImage("sana_avatar.png");
        assertEquals("sana_avatar.png", user.getProfileImage());
    }

    @Test
    @DisplayName("setActive / isActive")
    void setGetActive() {
        user.setActive(false);
        assertFalse(user.isActive());
        user.setActive(true);
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("setBanUntil / getBanUntil")
    void setGetBanUntil() {
        user.setBanUntil("2025-12-31");
        assertEquals("2025-12-31", user.getBanUntil());
    }

    // ════════════════════════════════════════════════════════════════════
    //  MÉTHODE MÉTIER : isAdmin()
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("isAdmin() → false quand roles = ROLE_USER")
    void isAdminFalseAvecRoleUser() {
        user.setRoles("[\"ROLE_USER\"]");
        assertFalse(user.isAdmin(),
            "Un utilisateur avec ROLE_USER ne doit PAS être admin");
    }

    @Test
    @DisplayName("isAdmin() → true quand roles = ROLE_ADMIN")
    void isAdminTrueAvecRoleAdmin() {
        user.setRoles("[\"ROLE_ADMIN\"]");
        assertTrue(user.isAdmin(),
            "Un utilisateur avec ROLE_ADMIN DOIT être admin");
    }

    @Test
    @DisplayName("isAdmin() → true quand roles contient ROLE_ADMIN et ROLE_USER")
    void isAdminTrueAvecDoubleRole() {
        user.setRoles("[\"ROLE_USER\",\"ROLE_ADMIN\"]");
        assertTrue(user.isAdmin(),
            "Un utilisateur avec les deux rôles DOIT être admin");
    }

    @Test
    @DisplayName("isAdmin() → false quand roles est null")
    void isAdminFalseQuandRolesNull() {
        user.setRoles(null);
        assertFalse(user.isAdmin(),
            "isAdmin() ne doit pas lancer d'exception si roles est null");
    }

    @Test
    @DisplayName("isAdmin() → false quand roles est vide")
    void isAdminFalseQuandRolesVide() {
        user.setRoles("");
        assertFalse(user.isAdmin());
    }

    /**
     * @ParameterizedTest : exécute le même test avec plusieurs valeurs.
     * Ici on vérifie que tous ces formats "admin" retournent true.
     */
    @ParameterizedTest(name = "roles = \"{0}\" → isAdmin() doit être true")
    @DisplayName("isAdmin() → true avec plusieurs formats contenant ROLE_ADMIN")
    @ValueSource(strings = {
        "[\"ROLE_ADMIN\"]",
        "[\"ROLE_USER\",\"ROLE_ADMIN\"]",
        "ROLE_ADMIN",
        "ROLE_ADMIN,ROLE_USER"
    })
    void isAdminTrueAvecDifferentsFormats(String roles) {
        user.setRoles(roles);
        assertTrue(user.isAdmin());
    }

    // ════════════════════════════════════════════════════════════════════
    //  toString()
    // ════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString() retourne 'nom <email>'")
    void toStringFormat() {
        String result = user.toString();
        assertEquals("Ali Ben Salah <ali@gamilha.tn>", result);
    }
}
