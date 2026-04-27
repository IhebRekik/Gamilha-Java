package com.gamilha.services;

import com.gamilha.entity.SubtitleCue;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SubtitleApiService {

    public static final class GeneratedSubtitleTrack {
        private final String transcriptId;
        private final String detectedLanguage;
        private final List<SubtitleCue> cues;

        public GeneratedSubtitleTrack(String transcriptId, String detectedLanguage, List<SubtitleCue> cues) {
            this.transcriptId = transcriptId;
            this.detectedLanguage = detectedLanguage;
            this.cues = cues;
        }

        public String getTranscriptId() {
            return transcriptId;
        }

        public String getDetectedLanguage() {
            return detectedLanguage;
        }

        public List<SubtitleCue> getCues() {
            return cues;
        }
    }

    public static final class LanguageOption {
        private final String code;
        private final String name;

        public LanguageOption(String code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }
    }

    private static final int TIMEOUT_MS = 10000;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final String ASSEMBLYAI_BASE_URL = "https://api.assemblyai.com";
    private static final String DEFAULT_LIBRETRANSLATE_URL = "https://libretranslate.com";
    private static final String GOOGLE_TRANSLATE_URL = "https://translate.googleapis.com/translate_a/single";
    private static final Path LOCAL_ENV_FILE = Path.of(".env.local");
    private static final Path LOCAL_PROPERTIES_FILE = Path.of("local.secrets.properties");
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String LOG_PREFIX = "[SUBTITLE][SERVICE]";
    private static final List<LanguageOption> FALLBACK_TRANSLATION_LANGUAGES = List.of(
            new LanguageOption("af", "Afrikaans"),
            new LanguageOption("ar", "Arabic"),
            new LanguageOption("az", "Azerbaijani"),
            new LanguageOption("bg", "Bulgarian"),
            new LanguageOption("bn", "Bengali"),
            new LanguageOption("ca", "Catalan"),
            new LanguageOption("cs", "Czech"),
            new LanguageOption("da", "Danish"),
            new LanguageOption("de", "German"),
            new LanguageOption("el", "Greek"),
            new LanguageOption("en", "English"),
            new LanguageOption("es", "Spanish"),
            new LanguageOption("et", "Estonian"),
            new LanguageOption("fa", "Persian"),
            new LanguageOption("fi", "Finnish"),
            new LanguageOption("fr", "French"),
            new LanguageOption("he", "Hebrew"),
            new LanguageOption("hi", "Hindi"),
            new LanguageOption("hr", "Croatian"),
            new LanguageOption("hu", "Hungarian"),
            new LanguageOption("id", "Indonesian"),
            new LanguageOption("it", "Italian"),
            new LanguageOption("ja", "Japanese"),
            new LanguageOption("ko", "Korean"),
            new LanguageOption("lt", "Lithuanian"),
            new LanguageOption("lv", "Latvian"),
            new LanguageOption("ms", "Malay"),
            new LanguageOption("nl", "Dutch"),
            new LanguageOption("no", "Norwegian"),
            new LanguageOption("pl", "Polish"),
            new LanguageOption("pt", "Portuguese"),
            new LanguageOption("ro", "Romanian"),
            new LanguageOption("ru", "Russian"),
            new LanguageOption("sk", "Slovak"),
            new LanguageOption("sl", "Slovenian"),
            new LanguageOption("sr", "Serbian"),
            new LanguageOption("sv", "Swedish"),
            new LanguageOption("sw", "Swahili"),
            new LanguageOption("ta", "Tamil"),
            new LanguageOption("th", "Thai"),
            new LanguageOption("tr", "Turkish"),
            new LanguageOption("uk", "Ukrainian"),
            new LanguageOption("ur", "Urdu"),
            new LanguageOption("vi", "Vietnamese"),
            new LanguageOption("zh", "Chinese")
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    public boolean hasAssemblyAiApiKey() {
        String key = readAssemblyAiApiKey();
        boolean present = !key.isBlank();
        logInfo("AssemblyAI API key present=" + present + ", keyLength=" + key.length() + ", preview=" + maskSecret(key));
        return present;
    }

    public String getAssemblyAiSetupUrl() {
        return "https://www.assemblyai.com/dashboard/api-keys";
    }

    public String getLibreTranslateDocsUrl() {
        return "https://docs.libretranslate.com/";
    }

    public String getLibreTranslateDefaultUrl() {
        return DEFAULT_LIBRETRANSLATE_URL;
    }

    public List<LanguageOption> getFallbackTranslationLanguages() {
        List<LanguageOption> languages = new ArrayList<>(FALLBACK_TRANSLATION_LANGUAGES);
        languages.sort(Comparator.comparing(LanguageOption::getName, String.CASE_INSENSITIVE_ORDER));
        return languages;
    }

    public GeneratedSubtitleTrack generateLocalSubtitles(File mediaFile) {
        if (mediaFile == null || !mediaFile.exists()) {
            throw new IllegalArgumentException("Le fichier video local est introuvable.");
        }

        long startedAt = System.currentTimeMillis();
        String apiKey = readAssemblyAiApiKey();
        if (apiKey.isBlank()) {
            throw new IllegalStateException("ASSEMBLYAI_API_KEY n'est pas configure.");
        }
        logInfo("generateLocalSubtitles start: file=" + mediaFile.getAbsolutePath()
                + ", bytes=" + mediaFile.length()
                + ", keyLength=" + apiKey.length()
                + ", keyPreview=" + maskSecret(apiKey));

        try {
            String uploadUrl = uploadLocalMedia(mediaFile, apiKey);
            logInfo("generateLocalSubtitles upload complete: uploadUrl=" + shorten(uploadUrl, 120));

            JsonObject transcriptPayload = new JsonObject();
            transcriptPayload.addProperty("audio_url", uploadUrl);

            JsonArray speechModels = new JsonArray();
            speechModels.add("universal-3-pro");
            speechModels.add("universal-2");
            transcriptPayload.add("speech_models", speechModels);
            transcriptPayload.addProperty("language_detection", true);
            logInfo("generateLocalSubtitles transcript request payload: speech_models=universal-3-pro,universal-2 language_detection=true");

            JsonObject transcript = createAndWaitForTranscript(apiKey, transcriptPayload);
            String transcriptId = getString(transcript, "id");
            String detectedLanguage = normalizeLanguageCode(getString(transcript, "language_code"));
            String srt = exportTranscriptSubtitles(apiKey, transcriptId, "srt");
            List<SubtitleCue> cues = parseSrt(srt);

            logInfo("generateLocalSubtitles success: transcriptId=" + transcriptId
                    + ", detectedLanguage=" + detectedLanguage
                    + ", srtChars=" + srt.length()
                    + ", cues=" + cues.size()
                    + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));

            return new GeneratedSubtitleTrack(
                    transcriptId,
                    detectedLanguage,
                    cues
            );
        } catch (RuntimeException ex) {
            logError("generateLocalSubtitles runtime failure: " + ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            logError("generateLocalSubtitles failure: " + ex.getMessage(), ex);
            throw new IllegalStateException("Impossible de generer les sous-titres automatiques via l'API.", ex);
        }
    }

    public List<LanguageOption> fetchTranslationLanguages() {
        logInfo("fetchTranslationLanguages start: provider=LibreTranslate baseUrl=" + resolveLibreTranslateBaseUrl());
        List<LanguageOption> languages = fetchTranslationLanguagesFromLibreTranslate();
        if (languages.isEmpty()) {
            logWarn("fetchTranslationLanguages LibreTranslate returned 0 languages, using local fallback list.");
            languages = getFallbackTranslationLanguages();
        }
        List<LanguageOption> deduped = dedupeLanguages(languages);
        logInfo("fetchTranslationLanguages end: total=" + deduped.size());
        return deduped;
    }

    private List<LanguageOption> fetchTranslationLanguagesFromLibreTranslate() {
        try {
            long startedAt = System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveLibreTranslateBaseUrl() + "/languages"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            logInfo("fetchTranslationLanguagesFromLibreTranslate response: status=" + response.statusCode()
                    + ", bodyChars=" + response.body().length()
                    + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
            ensureSuccess(response.statusCode(), response.body(), "Impossible de recuperer la liste des langues.");

            JsonArray json = JsonParser.parseString(response.body()).getAsJsonArray();
            List<LanguageOption> languages = new ArrayList<>();
            for (JsonElement element : json) {
                JsonObject object = element.getAsJsonObject();
                String code = normalizeLanguageCode(getString(object, "code"));
                String name = getString(object, "name");
                if (!code.isBlank()) {
                    languages.add(new LanguageOption(code, name.isBlank() ? code.toUpperCase() : name));
                }
            }

            languages.sort(Comparator.comparing(LanguageOption::getName, String.CASE_INSENSITIVE_ORDER));
            return languages;
        } catch (Exception ex) {
            logWarn("fetchTranslationLanguagesFromLibreTranslate failed: " + ex.getMessage());
            return new ArrayList<>();
        }
    }

    private List<LanguageOption> dedupeLanguages(List<LanguageOption> languages) {
        List<LanguageOption> deduped = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (LanguageOption language : languages) {
            String code = normalizeLanguageCode(language.getCode());
            if (code.isBlank() || seen.contains(code)) {
                continue;
            }
            seen.add(code);
            deduped.add(new LanguageOption(code, language.getName()));
        }
        deduped.sort(Comparator.comparing(LanguageOption::getName, String.CASE_INSENSITIVE_ORDER));
        return deduped;
    }

    public List<SubtitleCue> translateSubtitles(List<SubtitleCue> sourceCues, String sourceLanguage, String targetLanguage) {
        if (sourceCues == null || sourceCues.isEmpty()) {
            logWarn("translateSubtitles skipped: source cues empty.");
            return new ArrayList<>();
        }

        String normalizedSource = normalizeLanguageCode(sourceLanguage);
        String normalizedTarget = normalizeLanguageCode(targetLanguage);
        if (normalizedTarget.isBlank() || normalizedTarget.equals(normalizedSource)) {
            logInfo("translateSubtitles skipped: source=" + normalizedSource + ", target=" + normalizedTarget + " (same/blank)");
            return copyCues(sourceCues);
        }

        long startedAt = System.currentTimeMillis();
        logInfo("translateSubtitles start: source=" + normalizedSource
                + ", target=" + normalizedTarget
                + ", cues=" + sourceCues.size());

        List<SubtitleCue> translated = new ArrayList<>();
        List<String> texts = sourceCues.stream().map(SubtitleCue::getText).toList();

        int cursor = 0;
        while (cursor < texts.size()) {
            int end = Math.min(cursor + 40, texts.size());
            List<String> chunk = texts.subList(cursor, end);
            List<String> translatedChunk = translateTextChunk(chunk, normalizedSource, normalizedTarget);
            for (int i = 0; i < translatedChunk.size(); i++) {
                SubtitleCue original = sourceCues.get(cursor + i);
                translated.add(new SubtitleCue(
                        original.getStartMillis(),
                        original.getEndMillis(),
                        translatedChunk.get(i)
                ));
            }
            cursor = end;
        }

        logInfo("translateSubtitles success: source=" + normalizedSource
                + ", target=" + normalizedTarget
                + ", cues=" + translated.size()
                + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
        return translated;
    }

    public List<SubtitleCue> fetchYoutubeCaptions(String videoId) {
        logInfo("fetchYoutubeCaptions start: videoId=" + videoId);
        String[] langs = {"fr", "en", "ar"};
        for (String lang : langs) {
            List<SubtitleCue> cues = fetchYoutubeCaptionsForLang(videoId, lang);
            if (!cues.isEmpty()) {
                logInfo("fetchYoutubeCaptions success: lang=" + lang + ", cues=" + cues.size());
                return cues;
            }
        }
        logWarn("fetchYoutubeCaptions no captions found for videoId=" + videoId);
        return new ArrayList<>();
    }

    private List<SubtitleCue> fetchYoutubeCaptionsForLang(String videoId, String lang) {
        try {
            long startedAt = System.currentTimeMillis();
            String urlStr = "https://www.youtube.com/api/timedtext"
                    + "?v=" + videoId
                    + "&lang=" + lang
                    + "&fmt=vtt"
                    + "&name=";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8");

            int status = conn.getResponseCode();
            if (status == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                String content = sb.toString().trim();
                if (!content.isEmpty() && content.contains("-->")) {
                    logInfo("fetchYoutubeCaptionsForLang success: lang=" + lang
                            + ", contentChars=" + content.length()
                            + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
                    return parseVtt(content);
                }
            }
            logWarn("fetchYoutubeCaptionsForLang unavailable: lang=" + lang + ", httpStatus=" + status);
        } catch (Exception ignored) {
            logWarn("fetchYoutubeCaptionsForLang exception: lang=" + lang + ", msg=" + ignored.getMessage());
        }
        return new ArrayList<>();
    }

    public List<SubtitleCue> parseVtt(String vttContent) {
        List<SubtitleCue> cues = new ArrayList<>();
        Pattern timingPattern = Pattern.compile(
                "(\\d{1,2}:\\d{2}:\\d{2}[.,]\\d{1,3})\\s*-->\\s*(\\d{1,2}:\\d{2}:\\d{2}[.,]\\d{1,3})"
        );

        String[] lines = vttContent.split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            Matcher matcher = timingPattern.matcher(line);
            if (matcher.find()) {
                long start = parseTimestamp(matcher.group(1));
                long end = parseTimestamp(matcher.group(2));
                StringBuilder text = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].trim().isEmpty()) {
                    String textLine = lines[i].trim()
                            .replaceAll("<[^>]+>", "")
                            .replace("&amp;", "&")
                            .replace("&lt;", "<")
                            .replace("&gt;", ">")
                            .replace("&nbsp;", " ");
                    if (!textLine.isEmpty()) {
                        if (text.length() > 0) {
                            text.append(' ');
                        }
                        text.append(textLine);
                    }
                    i++;
                }
                String finalText = text.toString().trim();
                if (!finalText.isEmpty() && start >= 0 && end > start) {
                    cues.add(new SubtitleCue(start, end, finalText));
                }
            } else {
                i++;
            }
        }
        logInfo("parseVtt result: cues=" + cues.size());
        return cues;
    }

    public List<SubtitleCue> parseSrt(String srtContent) {
        List<SubtitleCue> cues = new ArrayList<>();
        Pattern timingPattern = Pattern.compile(
                "(\\d{2}:\\d{2}:\\d{2},\\d{3})\\s*-->\\s*(\\d{2}:\\d{2}:\\d{2},\\d{3})"
        );

        String[] blocks = srtContent.split("\\R\\R+");
        for (String block : blocks) {
            String[] lines = block.trim().split("\\R");
            if (lines.length < 2) {
                continue;
            }

            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = timingPattern.matcher(lines[i]);
                if (matcher.find()) {
                    long start = parseTimestamp(matcher.group(1));
                    long end = parseTimestamp(matcher.group(2));
                    StringBuilder text = new StringBuilder();

                    for (int j = i + 1; j < lines.length; j++) {
                        String value = lines[j].trim().replaceAll("<[^>]+>", "");
                        if (!value.isEmpty()) {
                            if (text.length() > 0) {
                                text.append(' ');
                            }
                            text.append(value);
                        }
                    }

                    String finalText = text.toString().trim();
                    if (!finalText.isEmpty() && start >= 0 && end > start) {
                        cues.add(new SubtitleCue(start, end, finalText));
                    }
                    break;
                }
            }
        }
        logInfo("parseSrt result: cues=" + cues.size());
        return cues;
    }

    public String getSubtitleAt(List<SubtitleCue> cues, long positionMillis) {
        if (cues == null || cues.isEmpty()) {
            return "";
        }
        for (SubtitleCue cue : cues) {
            if (cue.contains(positionMillis)) {
                return cue.getText();
            }
        }
        return "";
    }

    public String normalizeLanguageCode(String languageCode) {
        if (languageCode == null) {
            return "";
        }
        String value = languageCode.trim().toLowerCase();
        if (value.isBlank()) {
            return "";
        }
        if (value.contains("_")) {
            value = value.substring(0, value.indexOf('_'));
        }
        if (value.contains("-")) {
            value = value.substring(0, value.indexOf('-'));
        }
        return value;
    }

    private String uploadLocalMedia(File mediaFile, String apiKey) throws Exception {
        long startedAt = System.currentTimeMillis();
        logInfo("uploadLocalMedia start: file=" + mediaFile.getName()
                + ", bytes=" + mediaFile.length()
                + ", timeoutMs=" + REQUEST_TIMEOUT.toMillis());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ASSEMBLYAI_BASE_URL + "/v2/upload"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", apiKey)
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofFile(mediaFile.toPath()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        logInfo("uploadLocalMedia response: status=" + response.statusCode()
                + ", bodyChars=" + response.body().length()
                + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
        ensureSuccess(response.statusCode(), response.body(), "Upload du media vers AssemblyAI impossible.");

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        String uploadUrl = getString(json, "upload_url");
        if (uploadUrl.isBlank()) {
            throw new IllegalStateException("AssemblyAI n'a pas retourne d'URL d'upload.");
        }
        logInfo("uploadLocalMedia success: uploadUrl=" + shorten(uploadUrl, 120));
        return uploadUrl;
    }

    private JsonObject createAndWaitForTranscript(String apiKey, JsonObject payload) throws Exception {
        long startedAt = System.currentTimeMillis();
        logInfo("createAndWaitForTranscript start");
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(ASSEMBLYAI_BASE_URL + "/v2/transcript"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        logInfo("createAndWaitForTranscript create response: status=" + createResponse.statusCode()
                + ", bodyChars=" + createResponse.body().length());
        ensureSuccess(createResponse.statusCode(), createResponse.body(), "Creation de transcription AssemblyAI impossible.");

        JsonObject created = JsonParser.parseString(createResponse.body()).getAsJsonObject();
        String transcriptId = getString(created, "id");
        if (transcriptId.isBlank()) {
            throw new IllegalStateException("AssemblyAI n'a pas retourne d'identifiant de transcription.");
        }
        logInfo("createAndWaitForTranscript transcript created: transcriptId=" + transcriptId);

        String previousStatus = "";
        for (int attempt = 0; attempt < 120; attempt++) {
            HttpRequest pollRequest = HttpRequest.newBuilder()
                    .uri(URI.create(ASSEMBLYAI_BASE_URL + "/v2/transcript/" + transcriptId))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", apiKey)
                    .GET()
                    .build();

            HttpResponse<String> pollResponse = httpClient.send(pollRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ensureSuccess(pollResponse.statusCode(), pollResponse.body(), "Lecture du statut AssemblyAI impossible.");

            JsonObject transcript = JsonParser.parseString(pollResponse.body()).getAsJsonObject();
            String status = getString(transcript, "status");
            if (!status.equalsIgnoreCase(previousStatus) || attempt % 10 == 0) {
                logInfo("createAndWaitForTranscript poll: transcriptId=" + transcriptId
                        + ", attempt=" + (attempt + 1)
                        + ", status=" + status
                        + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
                previousStatus = status;
            }
            if ("completed".equalsIgnoreCase(status)) {
                logInfo("createAndWaitForTranscript completed: transcriptId=" + transcriptId
                        + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
                return transcript;
            }
            if ("error".equalsIgnoreCase(status)) {
                logWarn("createAndWaitForTranscript API error: transcriptId=" + transcriptId
                        + ", error=" + getString(transcript, "error"));
                throw new IllegalStateException(getString(transcript, "error"));
            }

            Thread.sleep(3000);
        }

        logWarn("createAndWaitForTranscript timeout: transcriptId=" + transcriptId
                + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
        throw new IllegalStateException("Le delai de transcription API a expire.");
    }

    private String exportTranscriptSubtitles(String apiKey, String transcriptId, String format) throws Exception {
        long startedAt = System.currentTimeMillis();
        logInfo("exportTranscriptSubtitles start: transcriptId=" + transcriptId + ", format=" + format);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ASSEMBLYAI_BASE_URL + "/v2/transcript/" + transcriptId + "/" + format + "?chars_per_caption=42"))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", apiKey)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        logInfo("exportTranscriptSubtitles response: status=" + response.statusCode()
                + ", bodyChars=" + response.body().length()
                + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
        ensureSuccess(response.statusCode(), response.body(), "Export des sous-titres AssemblyAI impossible.");
        return response.body();
    }

    private List<String> translateTextChunk(List<String> chunk, String sourceLanguage, String targetLanguage) {
        try {
            logInfo("translateTextChunk trying LibreTranslate: chunkSize=" + chunk.size()
                    + ", source=" + sourceLanguage + ", target=" + targetLanguage);
            return translateTextChunkWithLibreTranslate(chunk, sourceLanguage, targetLanguage);
        } catch (Exception libreEx) {
            logWarn("translateTextChunk LibreTranslate failed, fallback to Google: " + libreEx.getMessage());
            return translateTextChunkWithGoogle(chunk, sourceLanguage, targetLanguage);
        }
    }

    private List<String> translateTextChunkWithLibreTranslate(List<String> chunk, String sourceLanguage, String targetLanguage) {
        try {
            long startedAt = System.currentTimeMillis();
            JsonObject payload = new JsonObject();
            JsonArray texts = new JsonArray();
            chunk.forEach(texts::add);

            payload.add("q", texts);
            payload.addProperty("source", sourceLanguage.isBlank() ? "auto" : sourceLanguage);
            payload.addProperty("target", targetLanguage);
            payload.addProperty("format", "text");

            String apiKey = readLibreTranslateApiKey();
            if (!apiKey.isBlank()) {
                payload.addProperty("api_key", apiKey);
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(resolveLibreTranslateBaseUrl() + "/translate"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            logInfo("translateTextChunkWithLibreTranslate response: status=" + response.statusCode()
                    + ", bodyChars=" + response.body().length()
                    + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
            ensureSuccess(response.statusCode(), response.body(), "Traduction LibreTranslate impossible.");

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement translatedText = json.get("translatedText");
            List<String> translated = new ArrayList<>();

            if (translatedText != null && translatedText.isJsonArray()) {
                for (JsonElement element : translatedText.getAsJsonArray()) {
                    translated.add(element.isJsonNull() ? "" : element.getAsString());
                }
            } else if (translatedText != null && translatedText.isJsonPrimitive()) {
                translated.add(translatedText.getAsString());
            }

            if (translated.size() != chunk.size()) {
                throw new IllegalStateException("La traduction retournee est incomplete.");
            }

            logInfo("translateTextChunkWithLibreTranslate success: chunkSize=" + translated.size());
            return translated;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de traduire les sous-titres via l'API.", ex);
        }
    }

    private List<String> translateTextChunkWithGoogle(List<String> chunk, String sourceLanguage, String targetLanguage) {
        long startedAt = System.currentTimeMillis();
        List<String> translated = new ArrayList<>();
        String source = sourceLanguage == null || sourceLanguage.isBlank() ? "auto" : sourceLanguage;
        String target = targetLanguage == null || targetLanguage.isBlank() ? "en" : targetLanguage;

        for (String text : chunk) {
            translated.add(translateSingleTextWithGoogle(text, source, target));
        }
        logInfo("translateTextChunkWithGoogle success: chunkSize=" + translated.size()
                + ", source=" + source
                + ", target=" + target
                + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
        return translated;
    }

    private String translateSingleTextWithGoogle(String text, String sourceLanguage, String targetLanguage) {
        if (text == null || text.isBlank()) {
            return "";
        }

        try {
            long startedAt = System.currentTimeMillis();
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String source = normalizeLanguageCode(sourceLanguage);
            if (source.isBlank()) {
                source = "auto";
            }

            URI uri = URI.create(
                    GOOGLE_TRANSLATE_URL
                            + "?client=gtx"
                            + "&sl=" + source
                            + "&tl=" + normalizeLanguageCode(targetLanguage)
                            + "&dt=t"
                            + "&q=" + encodedText
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            ensureSuccess(response.statusCode(), response.body(), "Traduction Google gratuite indisponible.");
            String translated = extractGoogleTranslatedText(response.body());
            logInfo("translateSingleTextWithGoogle response: status=" + response.statusCode()
                    + ", translatedChars=" + translated.length()
                    + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
            return translated;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Impossible de traduire via Google Translate gratuit.", ex);
        }
    }

    private String extractGoogleTranslatedText(String body) {
        try {
            JsonArray root = JsonParser.parseString(body).getAsJsonArray();
            if (root.size() == 0 || !root.get(0).isJsonArray()) {
                return "";
            }

            JsonArray segments = root.get(0).getAsJsonArray();
            StringBuilder translated = new StringBuilder();
            for (JsonElement segmentElement : segments) {
                if (!segmentElement.isJsonArray()) {
                    continue;
                }
                JsonArray segment = segmentElement.getAsJsonArray();
                if (segment.size() > 0 && !segment.get(0).isJsonNull()) {
                    translated.append(segment.get(0).getAsString());
                }
            }
            return translated.toString().trim();
        } catch (Exception ex) {
            return "";
        }
    }

    private List<SubtitleCue> copyCues(List<SubtitleCue> sourceCues) {
        List<SubtitleCue> copy = new ArrayList<>();
        for (SubtitleCue cue : sourceCues) {
            copy.add(new SubtitleCue(cue.getStartMillis(), cue.getEndMillis(), cue.getText()));
        }
        return copy;
    }

    private long parseTimestamp(String timestamp) {
        try {
            String normalized = timestamp.replace(',', '.');
            String[] parts = normalized.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0].trim());
                int minutes = Integer.parseInt(parts[1].trim());
                double seconds = Double.parseDouble(parts[2].trim());
                return (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private void ensureSuccess(int statusCode, String body, String message) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        String details = body;
        try {
            JsonElement jsonElement = JsonParser.parseString(body);
            if (jsonElement.isJsonObject()) {
                JsonObject object = jsonElement.getAsJsonObject();
                if (object.has("error")) {
                    details = object.get("error").getAsString();
                }
            }
        } catch (Exception ignored) {
        }

        logWarn("HTTP failure: status=" + statusCode + ", message=" + message + ", details=" + shorten(details, 240));
        throw new IllegalStateException(message + " " + details);
    }

    private String getString(JsonObject object, String field) {
        if (object == null || !object.has(field) || object.get(field).isJsonNull()) {
            return "";
        }
        return object.get(field).getAsString();
    }

    private String resolveLibreTranslateBaseUrl() {
        String envValue = readConfigValue(
                "LIBRETRANSLATE_URL",
                "libretranslate.url",
                DEFAULT_LIBRETRANSLATE_URL
        );
        String value = envValue.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String readAssemblyAiApiKey() {
        return readConfigValue(
                "ASSEMBLYAI_API_KEY",
                "assemblyai.api.key",
                ""
        );
    }

    private String readLibreTranslateApiKey() {
        return readConfigValue(
                "LIBRETRANSLATE_API_KEY",
                "libretranslate.api.key",
                ""
        );
    }

    private String readConfigValue(String envKey, String propertyKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyKey, "");
        }
        if (value == null || value.isBlank()) {
            value = readLocalEnvValue(envKey);
        }
        if (value == null || value.isBlank()) {
            value = readLocalPropertiesValue(propertyKey);
        }
        if (value == null || value.isBlank()) {
            value = defaultValue;
        }
        return value == null ? "" : value.trim();
    }

    private String readLocalEnvValue(String key) {
        if (!Files.exists(LOCAL_ENV_FILE)) {
            return "";
        }
        try {
            for (String line : Files.readAllLines(LOCAL_ENV_FILE, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }
                String left = trimmed.substring(0, separatorIndex).trim();
                if (!left.equals(key)) {
                    continue;
                }
                String right = trimmed.substring(separatorIndex + 1).trim();
                if (right.startsWith("\"") && right.endsWith("\"") && right.length() >= 2) {
                    return right.substring(1, right.length() - 1).trim();
                }
                return right;
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String readLocalPropertiesValue(String key) {
        if (!Files.exists(LOCAL_PROPERTIES_FILE)) {
            return "";
        }
        try {
            Properties properties = new Properties();
            try (var reader = Files.newBufferedReader(LOCAL_PROPERTIES_FILE, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            return properties.getProperty(key, "").trim();
        } catch (Exception ignored) {
        }
        return "";
    }

    private void logInfo(String message) {
        System.out.println(LOG_PREFIX + "[" + LocalDateTime.now().format(LOG_TIME) + "] " + message);
    }

    private void logWarn(String message) {
        System.out.println(LOG_PREFIX + "[" + LocalDateTime.now().format(LOG_TIME) + "][WARN] " + message);
    }

    private void logError(String message, Throwable throwable) {
        System.out.println(LOG_PREFIX + "[" + LocalDateTime.now().format(LOG_TIME) + "][ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "(empty)";
        }
        if (secret.length() <= 8) {
            return "****";
        }
        return secret.substring(0, 4) + "..." + secret.substring(secret.length() - 4);
    }

    private String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ');
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }
}
