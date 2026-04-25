package com.gamilha.services;


import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.Message;
import io.ably.lib.types.AblyException;
import javafx.application.Platform;

/**
 * AblyService — équivalent exact de AblyService.php Symfony.
 *
 * Deux rôles :
 *   1. PUBLIER  : quand un user lance un stream → publish()
 *   2. S'ABONNER : écouter les nouveaux streams des autres → subscribe()
 */
public class AblyService {

    private static final String API_KEY = ""; // Mettre votre clé Ably ici
    private AblyRealtime ably;
    private Channel channel;

    public AblyService() throws AblyException {
        if (API_KEY == null || API_KEY.isBlank()) return; // clé non configurée
        ably = new AblyRealtime(API_KEY);
        channel = ably.channels.get("streams:new");
    }

    /**
     * Publier une notification quand un stream est lancé.
     * Appelé dans StreamFormController après service.create(stream).
     * Équivalent de $ablyService->publish() dans StreamController Symfony.
     */
    public void publishNewStream(int streamId, String title,
                                 String streamerName, int streamerId,
                                 String game) throws AblyException {
        // Construire le payload JSON (même structure que Symfony)
        String payload = String.format(
                "{\"type\":\"new_stream\",\"streamId\":%d,\"title\":\"%s\"," +
                        "\"streamerName\":\"%s\",\"streamerId\":%d,\"game\":\"%s\"}",
                streamId, title, streamerName, streamerId, game
        );
        channel.publish("message", payload);
    }

    /**
     * S'abonner aux notifications de nouveaux streams.
     * Appelé au démarrage de l'app (MainApp ou StreamListController).
     * Équivalent du JS Ably dans base.html.twig.
     *
     * @param currentUserId  l'ID du user connecté (pour exclure ses propres streams)
     * @param onNotification callback appelé sur JavaFX Thread avec le message
     */
    public void subscribeToNewStreams(int currentUserId,
                                      java.util.function.Consumer<String> onNotification)
            throws AblyException {
        channel.subscribe("message", msg -> {
            String data = msg.data.toString();

            // Extraire streamerId du JSON pour exclure le streamer lui-même
            // (même logique que le JS : if (data.streamerId === currentUserId) return)
            if (data.contains("\"streamerId\":" + currentUserId)) return;

            // Revenir sur le JavaFX Application Thread avant de toucher l'UI
            Platform.runLater(() -> onNotification.accept(data));
        });
    }

    /**
     * Publier une notification de donation en temps réel.
     * Appelé dans StreamShowController.quickDon() après une donation emoji.
     * Le streamer reçoit la notif sur le canal "streams:donations".
     */
    public void publishDonation(int streamId, String streamTitle,
                                String donorName, double amount,
                                String emoji) throws AblyException {
        if (ably == null) return;
        Channel donChannel = ably.channels.get("streams:donations");
        String payload = String.format(
                "{\"type\":\"new_donation\",\"streamId\":%d,\"streamTitle\":\"%s\"," +
                "\"donorName\":\"%s\",\"amount\":%.2f,\"emoji\":\"%s\"}",
                streamId, streamTitle, donorName, amount, emoji
        );
        donChannel.publish("message", payload);
    }

    public void disconnect() {
        if (ably != null) ably.close();
    }
}