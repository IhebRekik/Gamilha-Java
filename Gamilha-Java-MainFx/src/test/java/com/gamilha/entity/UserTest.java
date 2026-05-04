package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — User")
public class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId fonctionnent correctement")
    void testId() {
        user.setId(1);
        assertEquals(1, user.getId());
    }

    @Test
    @DisplayName("setEmail / getEmail fonctionnent correctement")
    void testEmail() {
        user.setEmail("admin@gamilha.com");
        assertEquals("admin@gamilha.com", user.getEmail());
    }

    @Test
    @DisplayName("setName / getName fonctionnent correctement")
    void testName() {
        user.setName("Gamilha Admin");
        assertEquals("Gamilha Admin", user.getName());
    }

    @Test
    @DisplayName("setPassword / getPassword fonctionnent correctement")
    void testPassword() {
        user.setPassword("hashed_password_123");
        assertEquals("hashed_password_123", user.getPassword());
    }

    @Test
    @DisplayName("setRoles / getRoles fonctionnent correctement")
    void testRoles() {
        String roles = "ROLE_USER";
        user.setRoles(roles);
        assertEquals(roles, user.getRoles());
    }

    @Test
    @DisplayName("setActive / isActive fonctionnent correctement")
    void testActive() {
        user.setActive(true);
        assertTrue(user.isActive());
    }

    @Test
    @DisplayName("setActive false / isActive retourne false")
    void testActiveFalse() {
        user.setActive(false);
        assertFalse(user.isActive());
    }

    @Test
    @DisplayName("setReports / getReports fonctionnent correctement")
    void testReports() {
        user.setReports(3);
        assertEquals(3, user.getReports());
    }

    @Test
    @DisplayName("setProfileImage / getProfileImage fonctionnent correctement")
    void testProfileImage() {
        user.setProfileImage("https://example.com/avatar.jpg");
        assertEquals("https://example.com/avatar.jpg", user.getProfileImage());
    }

    @Test
    @DisplayName("setCreatedAt / getCreatedAt fonctionnent correctement")
    void testCreatedAt() {
        LocalDateTime dt = LocalDateTime.of(2024, 6, 1, 9, 0);
        user.setCreatedAt(Timestamp.valueOf(dt));
        assertEquals(dt, user.getCreatedAt());
    }

    @Test
    @DisplayName("setBanUntil / getBanUntil fonctionnent correctement")
    void testBanUntil() {
        String ban = "banned";
        user.setBanUntil(ban);
        assertEquals(ban, user.getBanUntil());
    }



    // ══════════════════════════════════════════════════════════════════════
    //  equals() et hashCode()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Deux users avec le même ID sont égaux")
    void testEqualsById() {
        user.setId(5);
        User other = new User();
        other.setId(5);
        assertEquals(user, other);
    }

    @Test
    @DisplayName("Deux users avec des IDs différents ne sont pas égaux")
    void testNotEqualsDifferentId() {
        user.setId(1);
        User other = new User();
        other.setId(2);
        assertNotEquals(user, other);
    }

    @Test
    @DisplayName("Un user n'est pas égal à null")
    void testNotEqualsNull() {
        user.setId(1);
        assertNotEquals(user, null);
    }

    @Test
    @DisplayName("hashCode est cohérent avec equals")
    void testHashCode() {
        user.setId(5);
        User other = new User();
        other.setId(5);
        assertEquals(user.hashCode(), other.hashCode());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString contient l'email du user")
    void testToStringContainsEmail() {
        user.setId(1);
        user.setEmail("test@gamilha.com");
        assertTrue(user.toString().contains("test@gamilha.com"));
    }

    @Test
    @DisplayName("toString contient le nom du user")
    void testToStringContainsName() {
        user.setId(1);
        user.setName("TestUser");
        assertTrue(user.toString().contains("TestUser"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur complet
    // ══════════════════════════════════════════════════════════════════════


}
