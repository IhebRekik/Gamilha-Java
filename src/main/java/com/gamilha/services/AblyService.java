package com.gamilha.services;

import com.gamilha.utils.AppConfig;
import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import javafx.application.Platform;

import java.util.function.Consumer;

public class AblyService {

    private static final String ABLY_KEY = AppConfig.get("ably.key", "");

    private static final String CH_STREAMS = "streams:new";

    private static AblyService INSTANCE;

    public static synchronized AblyService getInstance() {
        if (INSTANCE == null) INSTANCE = new AblyService();
        return INSTANCE;
    }

    private AblyRealtime ably;
    private Channel chStreams;
    private final boolean enabled;

    public AblyService() {

        boolean ok = ABLY_KEY != null
                && ABLY_KEY.contains(":")
                && !ABLY_KEY.isBlank()
                && !ABLY_KEY.startsWith("VOTRE")
                && !ABLY_KEY.startsWith("METTEZ");
        enabled = ok;

        if (!ok) {
            System.out.println("Ably désactivé");
            return;
        }

        try {
            ably = new AblyRealtime(ABLY_KEY);
            chStreams = ably.channels.get(CH_STREAMS);

            System.out.println("✅ Ably connecté");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void publishNewStream(int id, String title, String name, int userId, String game) {

        if (!enabled) return;

        String payload = String.format(
                "{\"type\":\"new_stream\",\"streamId\":%d,\"title\":\"%s\",\"streamerName\":\"%s\",\"streamerId\":%d,\"game\":\"%s\"}",
                id,
                title != null ? title.replace("\"", "\\\"") : "",
                name != null ? name.replace("\"", "\\\"") : "Streamer",
                userId,
                game != null ? game.replace("\"", "\\\"") : ""
        );

        try {
            chStreams.publish("message", payload);
            System.out.println("📤 envoyé: " + payload);
        } catch (Exception e) {
            System.out.println("Erreur publish");
        }
    }

    public void subscribeToNewStreams(int currentUserId, Consumer<String> callback) {

        if (!enabled) return;

        try {
            chStreams.subscribe("message", msg -> {

                String data = msg.data.toString();
                int streamerId = extractStreamerId(data);
                if (currentUserId > 0 && streamerId == currentUserId) return;

                Platform.runLater(() -> callback.accept(data));
            });

            System.out.println("✅ Abonné streams");

        } catch (Exception e) {
            System.out.println("Erreur subscribe");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private int extractStreamerId(String payload) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\\\\?\"streamerId\\\\?\"\\s*:\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(payload);
        if (!m.find()) return -1;
        try { return Integer.parseInt(m.group(1)); }
        catch (NumberFormatException ex) { return -1; }
    }
}