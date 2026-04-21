package com.gamilha.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires — ChatMessage")
public class ChatMessageTest {

    private ChatMessage message;
    private User        sender;
    private User        recipient;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1);
        sender.setName("Alice");

        recipient = new User();
        recipient.setId(2);
        recipient.setName("Bob");

        message = new ChatMessage();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur par défaut
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur par défaut crée un objet non null")
    void testDefaultConstructor() {
        assertNotNull(message);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Constructeur complet
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur complet initialise tous les champs")
    void testFullConstructor() {
        ChatMessage msg = new ChatMessage(1, "Bonjour !", sender, recipient);
        assertEquals(1, msg.getId());
        assertEquals("Bonjour !", msg.getContent());
        assertEquals(sender, msg.getSender());
        assertEquals(recipient, msg.getRecipient());
        assertNotNull(msg.getCreatedAt()); // initialisé par le constructeur
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Getters / Setters
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setId / getId fonctionnent correctement")
    void testId() {
        message.setId(5);
        assertEquals(5, message.getId());
    }

    @Test
    @DisplayName("setSender / getSender fonctionnent correctement")
    void testSender() {
        message.setSender(sender);
        assertEquals(sender, message.getSender());
        assertEquals("Alice", message.getSender().getName());
    }

    @Test
    @DisplayName("setRecipient / getRecipient fonctionnent correctement")
    void testRecipient() {
        message.setRecipient(recipient);
        assertEquals(recipient, message.getRecipient());
        assertEquals("Bob", message.getRecipient().getName());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  setContent() — logique de validation intégrée dans le setter
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("setContent accepte un contenu valide")
    void testSetContentValid() {
        message.setContent("Salut, comment tu vas ?");
        assertEquals("Salut, comment tu vas ?", message.getContent());
    }

    @Test
    @DisplayName("setContent lève une exception si le contenu est null")
    void testSetContentNull() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> message.setContent(null)
        );
        assertEquals("Le contenu ne peut pas être vide", ex.getMessage());
    }

    @Test
    @DisplayName("setContent lève une exception si le contenu est vide")
    void testSetContentEmpty() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> message.setContent("")
        );
        assertEquals("Le contenu ne peut pas être vide", ex.getMessage());
    }

    @Test
    @DisplayName("setContent lève une exception si le contenu dépasse 255 caractères")
    void testSetContentTooLong() {
        String trop_long = "A".repeat(256);
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> message.setContent(trop_long)
        );
        assertEquals("Le contenu ne peut pas dépasser 255 caractères", ex.getMessage());
    }

    @Test
    @DisplayName("setContent accepte exactement 255 caractères")
    void testSetContentExactly255() {
        String exactly255 = "A".repeat(255);
        assertDoesNotThrow(() -> message.setContent(exactly255));
        assertEquals(255, message.getContent().length());
    }

    @Test
    @DisplayName("setContent accepte 1 seul caractère")
    void testSetContentOneChar() {
        assertDoesNotThrow(() -> message.setContent("A"));
        assertEquals("A", message.getContent());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  createdAt — initialisé dans le constructeur complet
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Le constructeur complet initialise createdAt automatiquement")
    void testCreatedAtSetInConstructor() {
        ChatMessage msg = new ChatMessage(1, "Test", sender, recipient);
        assertNotNull(msg.getCreatedAt());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  equals() et hashCode()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Deux messages avec le même ID sont égaux")
    void testEqualsById() {
        message.setId(3);
        ChatMessage other = new ChatMessage();
        other.setId(3);
        assertEquals(message, other);
    }

    @Test
    @DisplayName("Deux messages avec des IDs différents ne sont pas égaux")
    void testNotEquals() {
        message.setId(1);
        ChatMessage other = new ChatMessage();
        other.setId(2);
        assertNotEquals(message, other);
    }

    @Test
    @DisplayName("hashCode est cohérent avec equals")
    void testHashCode() {
        message.setId(3);
        ChatMessage other = new ChatMessage();
        other.setId(3);
        assertEquals(message.hashCode(), other.hashCode());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  toString()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("toString contient le contenu du message")
    void testToStringContainsContent() {
        message.setId(1);
        message.setContent("Hello !");
        assertTrue(message.toString().contains("Hello !"));
    }
}
