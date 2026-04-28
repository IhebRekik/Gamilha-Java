package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.services.AblyService;
import com.gamilha.services.DonationService;
import com.gamilha.services.StreamService;
import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.AppConfig;
import com.gamilha.utils.NavigationContext;
import com.gamilha.utils.SessionContext;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebView;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.io.File;
import java.nio.ByteBuffer;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamShowController implements Initializable {

    @FXML private Label     lblTitle, lblStatus, lblGame, lblViewers, lblDesc, lblDate;
    @FXML private Hyperlink lnkUrl;
    @FXML private ImageView imgThumb;

    @FXML private VBox    playerBox;
    @FXML private WebView playerView;
    @FXML private MediaView playerMediaView;
    @FXML private StackPane playerStack;
    @FXML private Canvas playerCanvas;
    @FXML private VBox    offlineBox;

    @FXML private VBox   obsBox;
    @FXML private Label  lblRtmp, lblKey;
    @FXML private Button btnCopyRtmp, btnCopyKey;

    @FXML private Label    lblTotal, lblCount;
    @FXML private FlowPane donationGrid;
    @FXML private HBox     donateButtonsBox;
    @FXML private Button   btnPrediction;

    private Stream  cur;
    private boolean isStreamer;

    private final DonationService donService    = new DonationService();
    private final StreamService   streamService = new StreamService();

    // ── Clé api.video — lire depuis AppConfig (config.properties) ────
    private static String getApiVideoKey() {
        String k = AppConfig.get("apivideo.key", "");
        if (k.isBlank() || k.startsWith("VOTRE") || k.startsWith("METTEZ")) return "";
        return k;
    }

    private ScheduledExecutorService pollScheduler;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    // Garde-fou : éviter de recharger le WebView si déjà broadcasting
    private boolean alreadyBroadcasting = false;
    private boolean liveNotificationSent = false;
    private boolean browserFallbackOpened = false;
    private String currentHlsUrl = "";
    private MediaPlayer mediaPlayer;
    private MediaPlayerFactory vlcFactory;
    private EmbeddedMediaPlayer vlcPlayer;
    private final WritableImage[] vlcImgRef = {new WritableImage(1280, 720)};
    private boolean switchingToVlc = false;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override public void initialize(URL url, ResourceBundle rb) {
        if (playerMediaView != null && playerStack != null) {
            playerMediaView.setPreserveRatio(true);
            playerMediaView.fitWidthProperty().bind(playerStack.widthProperty());
            playerMediaView.fitHeightProperty().bind(playerStack.heightProperty());
        }
        if (playerCanvas != null && playerStack != null) {
            playerCanvas.widthProperty().bind(playerStack.widthProperty());
            playerCanvas.heightProperty().bind(playerStack.heightProperty());
            playerCanvas.setVisible(false);
            playerCanvas.setManaged(false);
        }
    }

    public void setStream(Stream s, boolean isStreamer) {
        this.cur        = s;
        this.isStreamer = isStreamer;
        // Ne pas faire confiance au status DB pour l'état réel du live.
        // L'état réel vient du polling api.video (broadcasting).
        this.alreadyBroadcasting = false;
        this.liveNotificationSent = false;
        this.browserFallbackOpened = false;
        fillInfo();
        setupPlayer();
        setupDonateButtons();
        loadDonations();
        startPolling();
    }

    public void setStream(Stream s) {
        var user = SessionContext.getCurrentUser();
        setStream(s, user != null && user.getId() == s.getUserId());
    }

    // ── 1. Infos ──────────────────────────────────────────────────────
    private void fillInfo() {
        lblTitle.setText(cur.getTitle());
        lblGame.setText("🎮 " + cur.getGame());
        lblViewers.setText("👁 " + cur.getViewers() + " spectateurs");
        lblDesc.setText(cur.getDescription() != null && !cur.getDescription().isBlank()
                ? cur.getDescription() : "Aucune description.");
        if (cur.getCreatedAt() != null) lblDate.setText("📅 " + cur.getCreatedAt().format(FMT));
        applyStatusBadge(cur.getStatus());

        if (cur.getUrl() != null && !cur.getUrl().isBlank()) {
            lnkUrl.setText(cur.getUrl());
            lnkUrl.setOnAction(e -> openBrowser(cur.getUrl()));
        } else {
            lnkUrl.setText("Lien non disponible");
        }

        if (cur.getThumbnail() != null && !cur.getThumbnail().isBlank())
            try { imgThumb.setImage(new Image(cur.getThumbnail(), 600, 340, true, true, true)); }
            catch (Exception ignored) {}

        if (btnPrediction != null) {
            btnPrediction.setVisible(isStreamer);
            btnPrediction.setManaged(isStreamer);
        }
    }

    private void applyStatusBadge(String status) {
        lblStatus.setText(cur.getStatusBadge());
        String badgeStyle;
        if ("live".equals(status)) {
            badgeStyle = "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;-fx-font-weight:bold;-fx-font-size:12px;";
        } else if ("offline".equals(status)) {
            badgeStyle = "-fx-background-color:#374151;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;-fx-font-size:12px;";
        } else {
            badgeStyle = "-fx-background-color:#065f46;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;-fx-font-size:12px;";
        }
        lblStatus.setStyle(badgeStyle);
    }

    // ── 2. Player + OBS ───────────────────────────────────────────────
    private void setupPlayer() {
        boolean hasKey = cur.getStreamKey() != null && !cur.getStreamKey().isBlank();
        String normalizedUrl = normalizePlayerUrl(cur.getUrl());
        if (normalizedUrl != null && !normalizedUrl.isBlank()) {
            cur.setUrl(normalizedUrl);
        }
        boolean hasUrl = cur.getUrl() != null && !cur.getUrl().isBlank();

        // OBS bloc — streamer uniquement
        if (isStreamer && hasKey) {
            String rtmp = cur.getRtmpServer() != null
                    ? cur.getRtmpServer() : "rtmp://broadcast.api.video/s";
            if (lblRtmp != null) lblRtmp.setText(rtmp);
            if (lblKey  != null) lblKey.setText(cur.getStreamKey());
            if (obsBox  != null) { obsBox.setVisible(true); obsBox.setManaged(true); }
        } else {
            if (obsBox != null) { obsBox.setVisible(false); obsBox.setManaged(false); }
        }

        // Player — même comportement que Symfony:
        // dès qu'on a stream.url, on affiche l'iframe api.video
        // (api.video affiche l'écran d'attente puis passe en live automatiquement)
        if (hasUrl) {
            showVideoPlayer(cur.getUrl(), currentHlsUrl);
        } else {
            showOfflineScreen();
        }
    }

    private void loadPlayer(String embedUrl, String hlsUrl) {
        if (hlsUrl != null && !hlsUrl.isBlank() && playerMediaView != null) {
            try {
                startMediaPlayer(hlsUrl);
                return;
            } catch (Exception ex) {
                System.err.println("[Player] MediaView HLS error: " + ex.getMessage());
                try {
                    startVlcPlayer(hlsUrl);
                    return;
                } catch (Exception vlcEx) {
                    System.err.println("[Player] VLCJ HLS error: " + vlcEx.getMessage());
                }
            }
        }
        // L'utilisateur veut le live dans JavaFX. On évite le fallback navigateur/web ici.
        System.err.println("[Player] HLS indisponible, impossible d'afficher le live en natif.");
        showOfflineScreen();
    }

    private void startMediaPlayer(String hlsUrl) {
        if (playerMediaView == null) return;
        if (hlsUrl.equals(currentHlsUrl) && mediaPlayer != null) return;

        stopMediaPlayer();
        stopVlcPlayer();
        currentHlsUrl = hlsUrl;

        Media media = new Media(hlsUrl);
        mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.setMute(false);
        mediaPlayer.setOnError(() -> {
            String msg = mediaPlayer.getError() != null ? mediaPlayer.getError().getMessage() : "Erreur inconnue";
            System.err.println("[Player] MediaPlayer error: " + msg);
            trySwitchToVlc(hlsUrl);
        });

        playerMediaView.setMediaPlayer(mediaPlayer);
        playerMediaView.setVisible(true);
        playerMediaView.setManaged(true);
        if (playerCanvas != null) {
            playerCanvas.setVisible(false);
            playerCanvas.setManaged(false);
        }
        if (playerView != null) { playerView.setVisible(false); playerView.setManaged(false); }
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    private void startVlcPlayer(String hlsUrl) {
        if (hlsUrl.equals(currentHlsUrl) && vlcPlayer != null && vlcPlayer.status().isPlaying()) return;
        String vlcHome = findVlcPath();
        if (vlcHome == null || vlcHome.isBlank()) {
            throw new IllegalStateException("VLC non trouvé sur cette machine");
        }

        stopMediaPlayer();
        stopVlcPlayer();
        currentHlsUrl = hlsUrl;

        System.setProperty("jna.library.path", vlcHome);
        vlcFactory = new MediaPlayerFactory(vlcHome, "--no-video-title-show", "--quiet", "--network-caching=1000");
        vlcPlayer  = vlcFactory.mediaPlayers().newEmbeddedMediaPlayer();

        BufferFormatCallback bfc = new BufferFormatCallback() {
            @Override public BufferFormat getBufferFormat(int w, int h) {
                if (w > 0 && h > 0) Platform.runLater(() -> vlcImgRef[0] = new WritableImage(w, h));
                return new RV32BufferFormat(w, h);
            }
            @Override public void allocatedBuffers(ByteBuffer[] buffers) {}
        };
        RenderCallback rc = (mp, nativeBuffers, fmt) -> {
            ByteBuffer buf = nativeBuffers[0];
            byte[] bytes = new byte[fmt.getWidth() * fmt.getHeight() * 4];
            buf.get(bytes);
            buf.rewind();
            Platform.runLater(() -> {
                if (playerCanvas == null) return;
                WritableImage img = vlcImgRef[0];
                if (img != null && fmt.getWidth() > 0 && fmt.getHeight() > 0) {
                    img.getPixelWriter().setPixels(
                            0, 0, fmt.getWidth(), fmt.getHeight(),
                            PixelFormat.getByteBgraInstance(), bytes, 0, fmt.getWidth() * 4
                    );
                    playerCanvas.getGraphicsContext2D().drawImage(
                            img, 0, 0, playerCanvas.getWidth(), playerCanvas.getHeight()
                    );
                }
            });
        };

        vlcPlayer.videoSurface().set(vlcFactory.videoSurfaces().newVideoSurface(bfc, rc, true));
        vlcPlayer.media().play(hlsUrl);

        if (playerMediaView != null) { playerMediaView.setVisible(false); playerMediaView.setManaged(false); }
        if (playerCanvas != null) { playerCanvas.setVisible(true); playerCanvas.setManaged(true); }
        if (playerView != null) { playerView.setVisible(false); playerView.setManaged(false); }
        switchingToVlc = false;
    }

    private void stopVlcPlayer() {
        if (vlcPlayer != null) {
            try { vlcPlayer.controls().stop(); } catch (Exception ignored) {}
            try { vlcPlayer.release(); } catch (Exception ignored) {}
            vlcPlayer = null;
        }
        if (vlcFactory != null) {
            try { vlcFactory.release(); } catch (Exception ignored) {}
            vlcFactory = null;
        }
        switchingToVlc = false;
    }

    private void trySwitchToVlc(String hlsUrl) {
        if (switchingToVlc) return;
        switchingToVlc = true;
        Platform.runLater(() -> {
            try {
                startVlcPlayer(hlsUrl);
                System.out.println("[Player] Fallback VLCJ activé.");
            } catch (Exception ex) {
                System.err.println("[Player] VLCJ fallback failed: " + ex.getMessage());
                showOfflineScreen();
            }
        });
    }

    private void showVideoPlayer(String embedUrl, String hlsUrl) {
        if (playerBox  != null) { playerBox.setVisible(true);   playerBox.setManaged(true);  }
        if (offlineBox != null) { offlineBox.setVisible(false);  offlineBox.setManaged(false); }
        loadPlayer(embedUrl, hlsUrl);
    }

    private void showOfflineScreen() {
        if (playerBox  != null) { playerBox.setVisible(false);  playerBox.setManaged(false); }
        if (offlineBox != null) { offlineBox.setVisible(true);  offlineBox.setManaged(true); }
        stopMediaPlayer();
        stopVlcPlayer();
    }

    // ── 3. Donations ──────────────────────────────────────────────────
    private void setupDonateButtons() {
        if (donateButtonsBox != null) {
            donateButtonsBox.setVisible(!isStreamer);
            donateButtonsBox.setManaged(!isStreamer);
        }
    }

    @FXML private void onDonate1(ActionEvent e)  { quickDon("🍩"); }
    @FXML private void onDonate5(ActionEvent e)  { quickDon("🍕"); }
    @FXML private void onDonate10(ActionEvent e) { quickDon("💎"); }
    @FXML private void onDonate50(ActionEvent e) { quickDon("🚀"); }

    private void quickDon(String emoji) {
        var user = SessionContext.getCurrentUser();
        try {
            Donation d = donService.donateByEmoji(
                    cur.getId(),
                    user != null ? user.getId() : 1,
                    user != null ? user.getName() : "Anonymous",
                    emoji);
            AlertUtil.showSuccess("Merci ! " + emoji,
                    "Donation de " + d.getFormattedAmount() + " enregistrée !");
            loadDonations();
        } catch (Exception ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    private void loadDonations() {
        try {
            List<Donation> list = donService.findByStream(cur.getId());
            double total = list.stream().mapToDouble(Donation::getAmount).sum();
            if (lblTotal != null) lblTotal.setText(String.format("Total : %.2f €", total));
            if (lblCount != null) lblCount.setText(list.size() + " donation(s)");
            if (donationGrid != null) {
                donationGrid.getChildren().clear();
                if (list.isEmpty()) {
                    Label empty = new Label("Aucune donation pour le moment 😊");
                    empty.setStyle("-fx-text-fill:#4b5563;-fx-font-size:14px;");
                    donationGrid.getChildren().add(empty);
                } else {
                    for (Donation d : list) donationGrid.getChildren().add(buildDonCard(d));
                }
            }
        } catch (SQLException ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    private VBox buildDonCard(Donation d) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_CENTER); card.setPrefWidth(155);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;" +
                "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;");
        Label emoji  = new Label(d.getEmoji());
        emoji.setStyle("-fx-font-size:30px;");
        Label amount = new Label(d.getFormattedAmount());
        amount.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");
        Label donor  = new Label(d.getDonorName());
        donor.setStyle("-fx-font-size:12px;-fx-text-fill:#e2e8f0;"); donor.setWrapText(true);
        Label date   = new Label(d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-font-size:11px;-fx-text-fill:#4b5563;");
        card.getChildren().addAll(emoji, amount, donor, date);
        return card;
    }

    // ── 4. Polling api.video ──────────────────────────────────────────
    /**
     * Interroge GET /live-streams/{apiVideoId} toutes les 10 secondes.
     *
     * Quand api.video répond {"broadcasting": true} :
     *   → recharge le WebView avec l'URL embed
     *   → met à jour le badge LIVE
     *   → met à jour la BDD
     *
     * "La diffusion en direct n'a pas encore commencé" = OBS connecté
     * mais api.video n'a pas encore reçu les keyframes → attendre 20-60s.
     */
    private void startPolling() {
        String apiVideoId = cur.getApiVideoId();
        if (apiVideoId == null || apiVideoId.isBlank()) {
            apiVideoId = extractApiVideoIdFromUrl(cur.getUrl());
            if (apiVideoId != null && !apiVideoId.isBlank()) {
                cur.setApiVideoId(apiVideoId);
            }
        }
        if (apiVideoId == null || apiVideoId.isBlank()) return;

        pollScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "apivideo-poll");
            t.setDaemon(true);
            return t;
        });

        // Premier check après 5s, puis toutes les 10s
        String finalApiVideoId = apiVideoId;
        pollScheduler.scheduleAtFixedRate(() -> {
            try {
                checkBroadcastingStatus(finalApiVideoId);
            } catch (Exception e) {
                System.err.println("[Poll] " + e.getMessage());
            }
        }, 5, 10, TimeUnit.SECONDS);
    }

    private void checkBroadcastingStatus(String apiVideoId) throws Exception {
        String apiKey = getApiVideoKey();
        if (apiKey.isBlank()) return;

        // 1. Obtenir un token
        HttpResponse<String> tokenResp = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://ws.api.video/auth/api-key"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(8))
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"apiKey\":\"" + apiKey + "\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String token = extractJson(tokenResp.body(), "access_token");
        if (token.isBlank()) return;

        // 2. GET /live-streams/{id}
        HttpResponse<String> liveResp = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://ws.api.video/live-streams/" + apiVideoId))
                        .header("Authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String body = liveResp.body();
        boolean broadcasting = extractJsonBoolean(body, "broadcasting")
                || extractJsonBoolean(body, "live");
        String apiPlayerUrl = extractJsonStringFromPath(body, "assets", "player");
        String apiHlsUrl = extractJson(body, "hls");
        if (apiHlsUrl == null) apiHlsUrl = "";
        final String playerUrl = normalizePlayerUrl((apiPlayerUrl != null && !apiPlayerUrl.isBlank())
                ? apiPlayerUrl
                : "https://embed.api.video/live/" + sanitizeApiVideoId(apiVideoId));
        final String hlsUrl = apiHlsUrl;

        System.out.println("[Poll] apiVideoId=" + apiVideoId
                + " broadcasting=" + broadcasting
                + " alreadyBroadcasting=" + alreadyBroadcasting);

        Platform.runLater(() -> {
            if (broadcasting) {
                // OBS est en live → afficher/recharger le player
                boolean wasAlready = alreadyBroadcasting;
                alreadyBroadcasting = true;
                cur.setStatus("live");
                cur.setIsLive(true);
                cur.setUrl(playerUrl);
                if (!hlsUrl.isBlank()) currentHlsUrl = hlsUrl;
                applyStatusBadge("live");
                showVideoPlayer(playerUrl, currentHlsUrl);

                if (!wasAlready) {
                    // Mettre à jour la BDD seulement au premier passage
                    try { streamService.update(cur); }
                    catch (Exception ignored) {}
                    notifyStreamStarted();

                    System.out.println("[Poll] ✅ Broadcasting détecté — player affiché");
                }

            } else if (!broadcasting && alreadyBroadcasting) {
                // OBS vient de s'arrêter
                alreadyBroadcasting = false;
                liveNotificationSent = false;
                cur.setStatus("offline");
                cur.setIsLive(false);
                applyStatusBadge("offline");
                showOfflineScreen();

                try { streamService.update(cur); }
                catch (Exception ignored) {}
            }
        });
    }

    private void notifyStreamStarted() {
        if (!isStreamer || liveNotificationSent) return;

        var user = SessionContext.getCurrentUser();
        int userId = user != null ? user.getId() : cur.getUserId();
        String userName = user != null ? user.getName() : "Streamer";

        try {
            AblyService ably = AblyService.getInstance();
            if (ably.isEnabled()) {
                ably.publishNewStream(cur.getId(), cur.getTitle(), userName, userId, cur.getGame());
            }
            liveNotificationSent = true;
        } catch (Exception ex) {
            System.err.println("[Ably] publish error: " + ex.getMessage());
        }
    }

    // ── Copier ────────────────────────────────────────────────────────
    @FXML private void onCopyRtmp(ActionEvent e) {
        if (lblRtmp == null || lblRtmp.getText().isBlank()) return;
        copy(lblRtmp.getText());
        btnCopyRtmp.setText("✅");
        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            public void run() { Platform.runLater(() -> btnCopyRtmp.setText("📋")); }
        }, 2000);
    }

    @FXML private void onCopyKey(ActionEvent e) {
        if (lblKey == null || lblKey.getText().isBlank()) return;
        copy(lblKey.getText());
        btnCopyKey.setText("✅");
        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            public void run() { Platform.runLater(() -> btnCopyKey.setText("📋")); }
        }, 2000);
    }

    private void copy(String text) {
        ClipboardContent c = new ClipboardContent();
        c.putString(text);
        Clipboard.getSystemClipboard().setContent(c);
    }

    // ── Navigation ────────────────────────────────────────────────────
    @FXML
    private void onBack(ActionEvent e) {

        stopPolling();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/StreamList.fxml")
            );

            Parent root = loader.load();

            BorderPane contentArea = NavigationContext.getContentArea();

            if (contentArea == null) {
                throw new RuntimeException("contentArea null !");
            }

            contentArea.setCenter(root);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onEdit(ActionEvent e) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/StreamForm.fxml")
            );

            Parent root = loader.load();

            StreamFormController c = loader.getController();

            BorderPane contentArea = NavigationContext.getContentArea();
            contentArea.setCenter(root);

            if (c != null) c.initEdit(cur);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onAllDonations(ActionEvent e) {

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/DonationList.fxml")
            );

            Parent root = loader.load();

            DonationListController c = loader.getController();

            BorderPane contentArea = NavigationContext.getContentArea();
            contentArea.setCenter(root);

            if (c != null) c.setStream(cur);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    @FXML
    private void onPrediction(ActionEvent e) {

        stopPolling();

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/StreamPrediction.fxml")
            );

            Parent root = loader.load();

            StreamPredictionController c = loader.getController();

            BorderPane contentArea = NavigationContext.getContentArea();
            contentArea.setCenter(root);

            if (c != null) {
                c.init(SessionContext.getCurrentUser()); // ✅ CORRECT
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    private void stopPolling() {
        if (pollScheduler != null) {
            pollScheduler.shutdownNow();
            pollScheduler = null;
        }
        stopMediaPlayer();
        stopVlcPlayer();
    }

    private static String findVlcPath() {
        String user = System.getenv("USERPROFILE");
        String[] candidates = {
                "C:\\Program Files\\VideoLAN\\VLC",
                "C:\\Program Files (x86)\\VideoLAN\\VLC",
                user != null ? user + "\\AppData\\Local\\VideoLAN\\VLC" : ""
        };
        for (String c : candidates) {
            if (!c.isEmpty() && new File(c, "libvlc.dll").exists()) return c;
        }
        return null;
    }

    private void openBrowser(String url) {
        if (url == null || url.isBlank()) return;

        // 1) API Desktop standard
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {}

        // 2) Windows cmd start
        try {
            new ProcessBuilder("cmd", "/c", "start", "\"\"", url).start();
            return;
        } catch (Exception ignored) {}

        // 3) Windows rundll32 fallback
        try {
            new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            return;
        } catch (Exception ignored) {}

        // 4) PowerShell fallback
        try {
            new ProcessBuilder("powershell", "-NoProfile", "-Command", "Start-Process '" + url.replace("'", "''") + "'").start();
            return;
        } catch (Exception ignored) {}

        // 5) Ultime secours: copier le lien
        copy(url);
        AlertUtil.showWarning("Ouverture navigateur impossible",
                "Le lien du live a été copié dans le presse-papiers.\nCollez-le dans votre navigateur.");
    }

    private String extractJson(String json, String key) {
        String k = "\"" + key + "\":\"";
        int i = json.indexOf(k);
        if (i == -1) return "";
        int s = i + k.length();
        int e = json.indexOf('"', s);
        return e == -1 ? "" : unescapeJsonString(json.substring(s, e));
    }

    private boolean extractJsonBoolean(String json, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\""+ java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(true|false)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() && "true".equalsIgnoreCase(m.group(1));
    }

    private String extractJsonStringFromPath(String json, String objKey, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\"" + java.util.regex.Pattern.quote(objKey) + "\"\\s*:\\s*\\{[^}]*\"" +
                        java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? unescapeJsonString(m.group(1)) : "";
    }

    private String extractApiVideoIdFromUrl(String url) {
        if (url == null || url.isBlank()) return "";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "embed\\.api\\.video/live/([A-Za-z0-9]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : "";
    }

    private String sanitizeApiVideoId(String raw) {
        if (raw == null) return "";
        String id = raw.trim();
        while (id.startsWith("/")) id = id.substring(1);
        while (id.endsWith("/")) id = id.substring(0, id.length() - 1);
        int q = id.indexOf('?');
        if (q >= 0) id = id.substring(0, q);
        int h = id.indexOf('#');
        if (h >= 0) id = id.substring(0, h);
        return id;
    }

    private String normalizePlayerUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) return "";
        String u = rawUrl.trim();

        // Si on a déjà un ID api.video, reconstruire une URL propre.
        String extractedId = extractApiVideoIdFromUrl(u);
        if (extractedId != null && !extractedId.isBlank()) {
            return "https://embed.api.video/live/" + sanitizeApiVideoId(extractedId);
        }

        // Corriger les doubles slash dans /live//
        u = u.replace("embed.api.video/live//", "embed.api.video/live/");
        return u;
    }

    private String unescapeJsonString(String value) {
        if (value == null || value.isBlank()) return "";
        return value
                .replace("\\/", "/")
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .trim();
    }
}