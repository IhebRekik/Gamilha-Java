package com.gamilha.utils;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Utilitaire centralisé pour :
 * - choisir une image (FileChooser)
 * - la copier dans un dossier local persistant
 * - retourner le chemin relatif à stocker en DB (ex: "uploads/avatars/xxx.png")
 */
public final class ImageStorage {

    /** Dossier (relatif au répertoire d'exécution) où on stocke les images uploadées. */
    public static final String UPLOAD_DIR = "uploads/avatars";

    private static final Set<String> ALLOWED_EXT = Set.of("png", "jpg", "jpeg", "gif");

    private ImageStorage() {}

    public static FileChooser createAvatarFileChooser() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image de profil");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        return fc;
    }

    /**
     * Copie le fichier choisi vers {@link #UPLOAD_DIR} et renvoie le chemin RELATIF à stocker.
     */
    public static String saveAvatar(File sourceFile) throws IOException {
        if (sourceFile == null) return null;

        String ext = getExtension(sourceFile.getName());
        if (ext == null || !ALLOWED_EXT.contains(ext)) {
            throw new IOException("Format d'image non supporté: " + ext);
        }

        Path uploadDir = Paths.get(UPLOAD_DIR);
        Files.createDirectories(uploadDir);

        String safeName = UUID.randomUUID() + "." + ext;
        Path target = uploadDir.resolve(safeName);

        Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

        // Chemin relatif pour pouvoir le recharger plus tard
        // (et éviter de stocker un chemin absolu Windows dans la DB)
        return UPLOAD_DIR.replace('\\', '/') + "/" + safeName;
    }

    /**
     * Résout un chemin stocké (relatif) vers un fichier local.
     */
    public static Path resolveToPath(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return null;
        return Paths.get(storedPath);
    }

    private static String getExtension(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return null;
        return name.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}

