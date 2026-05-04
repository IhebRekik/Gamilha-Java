package com.gamilha.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * AppConfig — lit config.properties une seule fois au démarrage.
 * Accès : AppConfig.get("ably.key")
 */
public final class AppConfig {
    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/com/gamilha/config.properties")) {
            if (in != null) props.load(in);
        } catch (Exception e) {
            System.err.println("config.properties introuvable : " + e.getMessage());
        }
    }

    private AppConfig() {}

    public static String get(String key) {
        return props.getProperty(key, "");
    }

    public static String get(String key, String defaultVal) {
        return props.getProperty(key, defaultVal);
    }

    public static boolean hasKey(String key) {
        String v = get(key);
        return !v.isBlank() && !v.startsWith("VOTRE_");
    }
}
