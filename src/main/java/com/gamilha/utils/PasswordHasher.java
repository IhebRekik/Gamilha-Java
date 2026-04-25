package com.gamilha.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire de hachage BCrypt.
 *
 * – hash(plain)              → hache avec cost=12
 * – check(plain, hashed)     → vérifie (gère $2a$, $2b$, $2y$ Symfony)
 */
public class PasswordHasher {

    private static final int BCRYPT_COST = 12;

    private PasswordHasher() {}

    /**
     * Hache un mot de passe en clair.
     *
     * @throws IllegalArgumentException si le mot de passe est null ou vide
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Vérifie un mot de passe en clair contre un hash stocké.
     * Normalise automatiquement $2y$ (PHP/Symfony) → $2a$ (jBCrypt).
     * Tolère les mots de passe en clair pour les bases non migrées.
     */
    public static boolean check(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;

        String normalized = storedHash.startsWith("$2y$")
                ? "$2a$" + storedHash.substring(4)
                : storedHash;

        if (normalized.startsWith("$2a$") || normalized.startsWith("$2b$")) {
            try {
                return BCrypt.checkpw(plainPassword, normalized);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        // Mot de passe en clair (migration)
        System.out.println("⚠️  Mot de passe en clair — pensez à migrer avec PasswordHasher.hash()");
        return plainPassword.equals(storedHash);
    }
}
