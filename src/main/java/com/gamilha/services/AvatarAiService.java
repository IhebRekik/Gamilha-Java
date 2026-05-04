package com.gamilha.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service IA pour générer un avatar anime/cartoon depuis une photo via HuggingFace.
 * Utilise stable-diffusion-xl (text-to-image) ou img2img si disponible.
 */
public class AvatarAiService {

    // ⚙️ Token de l'utilisateur fourni dans la requête
    private static final String HF_TOKEN   = "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";
    private static final String SUBMIT_URL = "https://router.huggingface.co/wavespeed/api/v3/wavespeed-ai/qwen-image/edit-plus-lora";
    private static final String PROMPT     = "Convert this portrait into anime style";

    /**
     * Génère un avatar anime en envoyant l'image au modèle HuggingFace.
     * @param imageFile Fichier image source
     * @return Bytes de l'image générée, ou null en cas d'erreur
     */
    public byte[] generateAnimeAvatar(File imageFile) {
        if (imageFile == null || !imageFile.exists()) return null;

        try {
            // 1. Encoder l'image en base64 data-URL
            byte[] imgBytes = readFileBytes(imageFile);
            String b64 = Base64.getEncoder().encodeToString(imgBytes);
            String dataUrl = "data:image/jpeg;base64," + b64;

            // 2. Construire le payload JSON (même structure que Symfony)
            String json = "{"
                    + "\"images\":[\"" + dataUrl + "\"],"
                    + "\"prompt\":\"" + PROMPT + "\","
                    + "\"loras\":[],"
                    + "\"seed\":-1,"
                    + "\"output_format\":\"jpeg\","
                    + "\"enable_sync_mode\":true"
                    + "}";

            // 3. Appel API HuggingFace
            return callHuggingFace(SUBMIT_URL, json.getBytes(StandardCharsets.UTF_8), "application/json");

        } catch (Exception e) {
            System.err.println("[AvatarAI] Erreur: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private byte[] callHuggingFace(String urlStr, byte[] body, String contentType) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(150_000); // Augmenté pour l'IA
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + HF_TOKEN);
        conn.setRequestProperty("Content-Type", contentType);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            try (InputStream is = conn.getInputStream()) {
                byte[] responseBytes = is.readAllBytes();
                String responseStr = new String(responseBytes, StandardCharsets.UTF_8);
                
                // La réponse peut être du JSON contenant l'URL ou du binaire direct
                if (responseStr.trim().startsWith("{")) {
                    // Parser le JSON sommairement pour extraire l'URL ou la data-URL
                    // Structure attendue: {"outputs": ["data:image/jpeg;base64,..."] }
                    int start = responseStr.indexOf("data:image/jpeg;base64,");
                    if (start != -1) {
                        int end = responseStr.indexOf("\"", start);
                        String base64Data = responseStr.substring(start + "data:image/jpeg;base64,".length(), end);
                        return Base64.getDecoder().decode(base64Data);
                    }
                    // Si c'est une URL HTTP (Hugging Face CDN)
                    int urlStart = responseStr.indexOf("http");
                    if (urlStart != -1) {
                        int urlEnd = responseStr.indexOf("\"", urlStart);
                        String downloadUrl = responseStr.substring(urlStart, urlEnd);
                        return downloadImage(downloadUrl);
                    }
                }
                return responseBytes;
            }
        }

        // Gestion erreur
        InputStream err = conn.getErrorStream();
        if (err != null) {
            String msg = new String(err.readAllBytes(), StandardCharsets.UTF_8);
            System.err.println("[AvatarAI] HTTP " + code + ": " + msg);
        }
        return null;
    }

    private static final String SDXL_URL = "https://router.huggingface.co/hf-inference/models/stabilityai/stable-diffusion-xl-base-1.0";

    /**
     * Génère un avatar depuis un prompt texte uniquement (SDXL).
     * @param customPrompt Prompt personnalisé ou null pour le défaut
     * @return Bytes de l'image générée
     */
    public byte[] generateFromPrompt(String customPrompt) {
        String prompt = (customPrompt != null && !customPrompt.isBlank()) ? customPrompt : PROMPT;
        String json = "{\"inputs\":\"" + prompt.replace("\"", "\\\"") + "\"}";
        try {
            return callHuggingFace(SDXL_URL, json.getBytes(StandardCharsets.UTF_8), "application/json");
        } catch (Exception e) {
            System.err.println("[AvatarAI] Erreur prompt: " + e.getMessage());
            return null;
        }
    }

    private byte[] downloadImage(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        try (InputStream is = url.openStream()) {
            return is.readAllBytes();
        }
    }

    private byte[] readFileBytes(File f) throws IOException {
        try (FileInputStream fis = new FileInputStream(f)) {
            return fis.readAllBytes();
        }
    }
}
