package com.gamilha.util;

import javafx.scene.control.*;

/**
 * ValidationUtil — toutes les règles de validation du projet.
 * Équivalent des @Assert Symfony dans les entités.
 */
public class ValidationUtil {

    private static final String ERR = "-fx-border-color:#ef4444;-fx-border-width:2;-fx-border-radius:8;";
    private static final String OK  = "-fx-border-color:#22c55e;-fx-border-width:2;-fx-border-radius:8;";

    // ══ Règles Stream ════════════════════════════════════════════════════
    public static String validateTitle(String v) {
        if (v == null || v.isBlank())  return "Le titre est obligatoire.";
        if (v.trim().length() < 3)     return "Minimum 3 caractères.";
        if (v.trim().length() > 255)   return "Maximum 255 caractères.";
        return null;
    }

    public static String validateDescription(String v) {
        if (v != null && v.length() > 2000) return "Maximum 2000 caractères.";
        return null;
    }

    public static String validateGame(String v) {
        if (v == null || v.isBlank()) return "Le jeu est obligatoire.";
        return null;
    }

    public static String validateUrl(String v) {
        if (v == null || v.isBlank()) return null; // optionnel
        if (!v.startsWith("http://") && !v.startsWith("https://"))
            return "L'URL doit commencer par http:// ou https://";
        if (v.length() > 255) return "Maximum 255 caractères.";
        return null;
    }

    /**
     * Validation URL de l'image (thumbnail).
     * Doit être une URL valide pointant vers une image reconnue.
     * Le champ est obligatoire lors de la création d'un stream.
     */
    public static String validateThumbnailUrl(String v) {
        if (v == null || v.isBlank())
            return "L'URL de l'image est obligatoire.";
        if (!v.startsWith("http://") && !v.startsWith("https://"))
            return "L'URL doit commencer par http:// ou https://";
        if (v.length() > 255)
            return "Maximum 255 caractères.";
        String lower = v.toLowerCase();
        if (!lower.contains(".jpg") && !lower.contains(".jpeg")
                && !lower.contains(".png") && !lower.contains(".gif")
                && !lower.contains(".webp") && !lower.contains("placehold")
                && !lower.contains("imgur") && !lower.contains("cloudinary")
                && !lower.contains("unsplash") && !lower.contains("image"))
            return "L'URL doit pointer vers une image (.jpg, .png, .webp…)";
        return null;
    }

    // ══ Règles Donation ══════════════════════════════════════════════════
    public static String validateDonorName(String v) {
        if (v == null || v.isBlank()) return "Le nom du donateur est obligatoire.";
        if (v.trim().length() > 255)  return "Maximum 255 caractères.";
        return null;
    }

    public static String validateAmount(Double v) {
        if (v == null || v <= 0)  return "Le montant doit être supérieur à 0 €.";
        if (v > 9999)             return "Maximum 9 999 €.";
        return null;
    }

    public static String validateStreamSelected(Object v) {
        if (v == null) return "Veuillez sélectionner un stream.";
        return null;
    }

    // ══ Marquage visuel ══════════════════════════════════════════════════
    public static void mark(TextField f, String err)   { f.setStyle(err != null ? ERR : OK); }
    public static void mark(TextArea f, String err)    { f.setStyle(err != null ? ERR : OK); }
    public static void mark(ComboBox<?> f, String err) { f.setStyle(err != null ? ERR : OK); }
    public static void mark(Spinner<?> f, String err)  { f.setStyle(err != null ? ERR : OK); }
    public static void reset(Control f)                { f.setStyle(""); }

    /** Affiche ou cache un label d'erreur sous un champ */
    public static void setErr(Label lbl, String err) {
        if (lbl == null) return;
        if (err != null) {
            lbl.setText("⚠ " + err);
            lbl.setVisible(true);
            lbl.setManaged(true);
        } else {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }
}
