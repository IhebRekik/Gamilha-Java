package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — Inscription")
public class InscriptionTest {

    private Inscription inscription;
    private User        user;
    private Abonnement  abonnement;

    @BeforeEach
    void setUp() {
        inscription = new Inscription();

        user = new User();
        user.setId(1);
        user.setEmail("user@gamilha.com");

        abonnement = new Abonnement();
        abonnement.setId(2);
        abonnement.setType("Premium");
        abonnement.setPrix(9.99f);
        abonnement.setDuree(30);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId fonctionnent correctement")
    void testId() {
        inscription.setId(10);
        assertEquals(10, inscription.getId());
    }

    @Test
    @DisplayName("setDateDebut / getDateDebut fonctionnent correctement")
    void testDateDebut() {
        LocalDate date = LocalDate.of(2025, 1, 1);
        inscription.setDateDebut(date);
        assertEquals(date, inscription.getDateDebut());
    }

    @Test
    @DisplayName("setDateFin / getDateFin fonctionnent correctement")
    void testDateFin() {
        LocalDate date = LocalDate.of(2025, 1, 31);
        inscription.setDateFin(date);
        assertEquals(date, inscription.getDateFin());
    }

    @Test
    @DisplayName("setUser / getUser fonctionnent correctement")
    void testUser() {
        inscription.setUser(user);
        assertNotNull(inscription.getUser());
        assertEquals(1, inscription.getUser().getId());
    }

    @Test
    @DisplayName("setAbonnements / getAbonnements fonctionnent correctement")
    void testAbonnement() {
        inscription.setAbonnements(abonnement);
        assertNotNull(inscription.getAbonnements());
        assertEquals("Premium", inscription.getAbonnements().getType());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Logique métier — durée de validité
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("dateFin est après dateDebut pour une inscription valide")
    void testDateFinApresDateDebut() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin   = LocalDate.of(2025, 1, 31);
        inscription.setDateDebut(debut);
        inscription.setDateFin(fin);
        assertTrue(inscription.getDateFin().isAfter(inscription.getDateDebut()));
    }

    @Test
    @DisplayName("Une inscription non expirée : dateFin dans le futur")
    void testInscriptionActive() {
        inscription.setDateDebut(LocalDate.now().minusDays(5));
        inscription.setDateFin(LocalDate.now().plusDays(25));
        assertTrue(inscription.getDateFin().isAfter(LocalDate.now()));
    }

    @Test
    @DisplayName("Une inscription expirée : dateFin dans le passé")
    void testInscriptionExpiree() {
        inscription.setDateDebut(LocalDate.of(2024, 1, 1));
        inscription.setDateFin(LocalDate.of(2024, 1, 31));
        assertTrue(inscription.getDateFin().isBefore(LocalDate.now()));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  equals() et hashCode()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Deux inscriptions avec le même ID sont égales")
    void testEqualsById() {
        inscription.setId(5);
        Inscription other = new Inscription();
        other.setId(5);
        assertEquals(inscription, other);
    }

    @Test
    @DisplayName("Deux inscriptions avec des IDs différents ne sont pas égales")
    void testNotEquals() {
        inscription.setId(1);
        Inscription other = new Inscription();
        other.setId(2);
        assertNotEquals(inscription, other);
    }

    @Test
    @DisplayName("hashCode est cohérent avec equals")
    void testHashCode() {
        inscription.setId(5);
        Inscription other = new Inscription();
        other.setId(5);
        assertEquals(inscription.hashCode(), other.hashCode());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString contient l'ID de l'inscription")
    void testToStringContainsId() {
        inscription.setId(7);
        assertTrue(inscription.toString().contains("7"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur complet
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur complet initialise tous les champs")
    void testFullConstructor() {
        LocalDate debut = LocalDate.of(2025, 1, 1);
        LocalDate fin   = LocalDate.of(2025, 1, 31);
        Inscription i = new Inscription(1, debut, fin, user, abonnement);
        assertEquals(1, i.getId());
        assertEquals(debut, i.getDateDebut());
        assertEquals(fin, i.getDateFin());
        assertEquals(user, i.getUser());
        assertEquals(abonnement, i.getAbonnements());
    }
}
