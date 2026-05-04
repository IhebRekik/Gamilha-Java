package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — Donation")
public class DonationTest {

    private Donation donation;

    @BeforeEach
    void setUp() {
        donation = new Donation();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur par défaut
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur par défaut initialise createdAt à maintenant")
    void testCreatedAtNotNull() {
        assertNotNull(donation.getCreatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setAmount / getAmount fonctionnent correctement")
    void testAmount() {
        donation.setAmount(10.0);
        assertEquals(10.0, donation.getAmount());
    }

    @Test
    @DisplayName("setDonorName / getDonorName fonctionnent correctement")
    void testDonorName() {
        donation.setDonorName("SuperFan42");
        assertEquals("SuperFan42", donation.getDonorName());
    }

    @Test
    @DisplayName("setUserId / getUserId fonctionnent correctement")
    void testUserId() {
        donation.setUserId(7);
        assertEquals(7, donation.getUserId());
    }

    @Test
    @DisplayName("setStreamId / getStreamId fonctionnent correctement")
    void testStreamId() {
        donation.setStreamId(3);
        assertEquals(3, donation.getStreamId());
    }

    @Test
    @DisplayName("setStreamTitle / getStreamTitle fonctionnent correctement")
    void testStreamTitle() {
        donation.setStreamTitle("CS2 Road to Global");
        assertEquals("CS2 Road to Global", donation.getStreamTitle());
    }

    @Test
    @DisplayName("setUserEmail / getUserEmail fonctionnent correctement")
    void testUserEmail() {
        donation.setUserEmail("user@gamilha.com");
        assertEquals("user@gamilha.com", donation.getUserEmail());
    }

    @Test
    @DisplayName("setCreatedAt / getCreatedAt fonctionnent correctement")
    void testCreatedAt() {
        LocalDateTime dt = LocalDateTime.of(2025, 3, 10, 14, 0);
        donation.setCreatedAt(dt);
        assertEquals(dt, donation.getCreatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getEmoji() — logique métier basée sur le montant
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getEmoji retourne 🍩 pour un montant de 1 €")
    void testEmoji1() {
        donation.setAmount(1.0);
        assertEquals("🍩", donation.getEmoji());
    }

    @Test
    @DisplayName("getEmoji retourne 🍩 pour un montant inférieur à 5 €")
    void testEmojiUnder5() {
        donation.setAmount(3.5);
        assertEquals("🍩", donation.getEmoji());
    }

    @Test
    @DisplayName("getEmoji retourne 🍕 pour un montant de 5 €")
    void testEmoji5() {
        donation.setAmount(5.0);
        assertEquals("🍕", donation.getEmoji());
    }

    @Test
    @DisplayName("getEmoji retourne 🍕 pour un montant entre 5 et 10 €")
    void testEmojiSeptEuros() {
        donation.setAmount(7.0);
        assertEquals("🍕", donation.getEmoji());
    }

    @Test
    @DisplayName("getEmoji retourne 💎 pour un montant de 10 €")
    void testEmoji10() {
        donation.setAmount(10.0);
        assertEquals("💎", donation.getEmoji());
    }

    @Test
    @DisplayName("getEmoji retourne 💎 pour un montant entre 10 et 50 €")
    void testEmojiTrenteEuros() {
        donation.setAmount(30.0);
        assertEquals("💎", donation.getEmoji());
    }

    @Test
    @DisplayName("getEmoji retourne 🚀 pour un montant de 50 €")
    void testEmoji50() {
        donation.setAmount(50.0);
        assertEquals("🚀", donation.getEmoji());
    }

    @Test
    @DisplayName("getEmoji retourne 🚀 pour un montant supérieur à 50 €")
    void testEmojiOver50() {
        donation.setAmount(100.0);
        assertEquals("🚀", donation.getEmoji());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  getFormattedAmount()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("getFormattedAmount formate correctement un entier")
    void testFormattedAmountInteger() {
        donation.setAmount(10.0);
        assertEquals("10,00 €", donation.getFormattedAmount());
    }

    @Test
    @DisplayName("getFormattedAmount formate correctement un décimal")
    void testFormattedAmountDecimal() {
        donation.setAmount(5.5);
        assertEquals("5,50 €", donation.getFormattedAmount());
    }

    @Test
    @DisplayName("getFormattedAmount arrondit à 2 décimales")
    void testFormattedAmountRounding() {
        donation.setAmount(1.0);
        assertEquals("1,00 €", donation.getFormattedAmount());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString retourne 'donorName — montant €'")
    void testToString() {
        donation.setDonorName("Fan42");
        donation.setAmount(10.0);
        assertEquals("Fan42 — 10,00 €", donation.toString());
    }
}
