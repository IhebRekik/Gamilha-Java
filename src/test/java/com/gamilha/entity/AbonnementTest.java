package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — Abonnement")
public class AbonnementTest {

    private Abonnement abonnement;

    @BeforeEach
    void setUp() {
        abonnement = new Abonnement();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur par défaut
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur par défaut crée un objet non null")
    void testDefaultConstructor() {
        assertNotNull(abonnement);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId fonctionnent correctement")
    void testId() {
        abonnement.setId(1);
        assertEquals(1, abonnement.getId());
    }

    @Test
    @DisplayName("setType / getType fonctionnent correctement")
    void testType() {
        abonnement.setType("Premium");
        assertEquals("Premium", abonnement.getType());
    }

    @Test
    @DisplayName("setPrix / getPrix fonctionnent correctement")
    void testPrix() {
        abonnement.setPrix(9.99f);
        assertEquals(9.99f, abonnement.getPrix(), 0.001f);
    }

    @Test
    @DisplayName("setDuree / getDuree fonctionnent correctement")
    void testDuree() {
        abonnement.setDuree(30);
        assertEquals(30, abonnement.getDuree());
    }

    @Test
    @DisplayName("setAvantages / getAvantages fonctionnent correctement")
    void testAvantages() {
        List<String> avantages = Arrays.asList("Accès illimité", "Support prioritaire");
        abonnement.setAvantages(avantages);
        assertEquals(2, abonnement.getAvantages().size());
        assertEquals("Accès illimité", abonnement.getAvantages().get(0));
    }

    @Test
    @DisplayName("setOptions / getOptions fonctionnent correctement")
    void testOptions() {
        List<String> options = Arrays.asList("stream", "coaching");
        abonnement.setOptions(options);
        assertEquals(2, abonnement.getOptions().size());
        assertTrue(abonnement.getOptions().contains("stream"));
    }

    @Test
    @DisplayName("Le prix peut être 0 (abonnement gratuit)")
    void testPrixZero() {
        abonnement.setPrix(0.0f);
        assertEquals(0.0f, abonnement.getPrix());
    }

    @Test
    @DisplayName("La durée peut être 365 (abonnement annuel)")
    void testDureeAnnuelle() {
        abonnement.setDuree(365);
        assertEquals(365, abonnement.getDuree());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  equals() et hashCode()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Deux abonnements avec le même ID sont égaux")
    void testEqualsById() {
        abonnement.setId(3);
        Abonnement other = new Abonnement();
        other.setId(3);
        assertEquals(abonnement, other);
    }

    @Test
    @DisplayName("Deux abonnements avec des IDs différents ne sont pas égaux")
    void testNotEquals() {
        abonnement.setId(1);
        Abonnement other = new Abonnement();
        other.setId(2);
        assertNotEquals(abonnement, other);
    }

    @Test
    @DisplayName("Un abonnement n'est pas égal à null")
    void testNotEqualsNull() {
        abonnement.setId(1);
        assertNotEquals(abonnement, null);
    }

    @Test
    @DisplayName("hashCode est cohérent avec equals")
    void testHashCode() {
        abonnement.setId(3);
        Abonnement other = new Abonnement();
        other.setId(3);
        assertEquals(abonnement.hashCode(), other.hashCode());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString contient le type de l'abonnement")
    void testToStringContainsType() {
        abonnement.setId(1);
        abonnement.setType("Gold");
        assertTrue(abonnement.toString().contains("Gold"));
    }

    @Test
    @DisplayName("toString contient le prix")
    void testToStringContainsPrix() {
        abonnement.setId(1);
        abonnement.setPrix(19.99f);
        assertTrue(abonnement.toString().contains("19.99"));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur complet
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur complet initialise tous les champs")
    void testFullConstructor() {
        List<String> avantages = List.of("Streaming illimité");
        List<String> options   = List.of("stream", "coaching");
        Abonnement a = new Abonnement(1, "Premium", 9.99f, avantages, 30, options);
        assertEquals(1, a.getId());
        assertEquals("Premium", a.getType());
        assertEquals(9.99f, a.getPrix(), 0.001f);
        assertEquals(30, a.getDuree());
        assertEquals(avantages, a.getAvantages());
        assertEquals(options, a.getOptions());
    }
}
