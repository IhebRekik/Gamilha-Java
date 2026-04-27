package com.gamilha.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Gère la sauvegarde et la récupération des identifiants (email + password)
 * pour l'auto-complétion dans la page de connexion.
 * Stockage via java.util.prefs.Preferences (registre Windows / fichier Linux).
 */
public class SavedCredentials {

    private static final Preferences PREFS     = Preferences.userRoot().node("gamilha/credentials");
    private static final int         MAX_SAVED = 10;

    /** Sauvegarde un couple email/password. */
    public static void save(String email, String password) {
        if (email == null || email.isBlank()) return;
        List<String> emails = getSavedEmails();
        emails.remove(email);
        emails.add(0, email);
        if (emails.size() > MAX_SAVED) emails = emails.subList(0, MAX_SAVED);
        PREFS.put("emails", String.join("|||", emails));
        PREFS.put("pwd_" + email.hashCode(), encode(password));
        flush();
    }

    /** Retourne tous les emails sauvegardés. */
    public static List<String> getSavedEmails() {
        String raw = PREFS.get("emails", "");
        if (raw.isBlank()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        for (String e : raw.split("\\|\\|\\|")) {
            if (!e.isBlank()) list.add(e);
        }
        return list;
    }

    /** Retourne le mot de passe sauvegardé pour un email, ou "" si absent. */
    public static String getPasswordFor(String email) {
        if (email == null || email.isBlank()) return "";
        String encoded = PREFS.get("pwd_" + email.hashCode(), "");
        return encoded.isBlank() ? "" : decode(encoded);
    }

    /** Supprime les identifiants d'un email. */
    public static void remove(String email) {
        if (email == null) return;
        List<String> emails = getSavedEmails();
        emails.remove(email);
        PREFS.put("emails", String.join("|||", emails));
        PREFS.remove("pwd_" + email.hashCode());
        flush();
    }

    private static void flush() {
        try { PREFS.flush(); } catch (Exception ignored) {}
    }

    private static String encode(String s) {
        return java.util.Base64.getEncoder().encodeToString(s.getBytes());
    }

    private static String decode(String s) {
        try { return new String(java.util.Base64.getDecoder().decode(s)); }
        catch (Exception e) { return ""; }
    }
}
