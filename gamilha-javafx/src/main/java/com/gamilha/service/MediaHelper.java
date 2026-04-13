package com.gamilha.service;

/**
 * Utilitaire pour traiter les URLs média (YouTube, image directe, lien générique).
 * Reproduit la logique Twig du template social/index.html.twig.
 */
public class MediaHelper {

    public enum MediaType { YOUTUBE, IMAGE_URL, LINK, NONE }

    public static MediaType detect(String url) {
        if (url == null || url.isBlank()) return MediaType.NONE;
        if (url.contains("youtube.com") || url.contains("youtu.be")) return MediaType.YOUTUBE;
        String lower = url.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".png") || lower.endsWith(".gif") ||
            lower.endsWith(".webp")) return MediaType.IMAGE_URL;
        if (url.startsWith("http")) return MediaType.IMAGE_URL; // tente comme image
        return MediaType.LINK;
    }

    /**
     * Extrait l'ID vidéo d'une URL YouTube.
     * Supporte : youtube.com/watch?v=ID  et  youtu.be/ID
     */
    public static String extractYoutubeId(String url) {
        if (url == null) return null;
        if (url.contains("watch?v=")) {
            String after = url.split("watch\\?v=")[1];
            return after.split("&")[0];
        }
        if (url.contains("youtu.be/")) {
            String after = url.split("youtu\\.be/")[1];
            return after.split("\\?")[0];
        }
        return null;
    }

    /**
     * Construit l'URL embed YouTube pour affichage dans WebView.
     */
    public static String toEmbedUrl(String url) {
        String id = extractYoutubeId(url);
        if (id != null) return "https://www.youtube.com/embed/" + id;
        return null;
    }
}
