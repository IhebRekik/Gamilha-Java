package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.utils.NavigationContext;
import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.services.DonationService;
import com.gamilha.services.AblyService;
import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.SessionContext;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * StreamShowController — identique à show.html.twig Symfony.
 *
 * Sections (même ordre que Symfony) :
 *   1. Header : titre + badge statut
 *   2. Player api.video (WebView) ou écran "Live stream offline."
 *   3. Bloc OBS — streamer uniquement (RTMP + StreamKey)
 *   4. Donations emoji rapides (🍩1€ 🍕5€ 💎10€ 🚀50€)
 *   5. Grille donations reçues
 *   6. Bouton Prédiction IA
 */
public class StreamShowController implements Initializable {

    /* ── Header ──────────────────────────────────────────────────────── */
    @FXML private Label lblTitle;
    @FXML private Label lblStatus;

    /* ── Info panel ──────────────────────────────────────────────────── */
    @FXML private Label     lblGame;
    @FXML private Label     lblViewers;
    @FXML private Label     lblDesc;
    @FXML private Label     lblDate;
    @FXML private Hyperlink lnkUrl;
    @FXML private ImageView imgThumb;

    /* ── Player api.video ────────────────────────────────────────────── */
    @FXML private VBox    playerBox;    // contient le WebView quand live
    @FXML private WebView webPlayer;   // iframe embed.api.video/live/...
    @FXML private VBox    offlineBox;  // écran "Live stream offline."

    /* ── Bloc OBS ────────────────────────────────────────────────────── */
    @FXML private VBox   obsBox;
    @FXML private Label  lblRtmp;
    @FXML private Label  lblKey;
    @FXML private Button btnCopyKey;
    @FXML private Button btnCopyRtmp;

    /* ── Donations ───────────────────────────────────────────────────── */
    @FXML private Label    lblTotal;
    @FXML private Label    lblCount;
    @FXML private FlowPane donationGrid;

    /* ── Prédiction IA ───────────────────────────────────────────────── */
    @FXML private Button btnPrediction;

    /* ── State ───────────────────────────────────────────────────────── */
    private Stream  cur;
    private boolean isStreamer = false;

    private final DonationService donService = new DonationService();
    private ScheduledExecutorService scheduler;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /** Appelé depuis StreamFormController après création → isStreamer = true */
    public void setStream(Stream s, boolean isStreamer) {
        this.cur        = s;
        this.isStreamer = isStreamer;
        fillInfo();
        setupPlayer();
        setupObsBox();
        loadDonations();
        startPolling();
    }

    /** Appelé depuis StreamListController — détecte propriétaire auto */
    public void setStream(Stream s) {
        var user  = SessionContext.getCurrentUser();
        boolean owner = user != null && user.getId() == s.getUserId();
        setStream(s, owner);
    }

    // ─────────────────────────────────────────────────────────────────
    // 1. Informations
    // ─────────────────────────────────────────────────────────────────
    private void fillInfo() {
        lblTitle.setText(cur.getTitle());
        lblGame.setText("🎮 " + cur.getGame());
        lblViewers.setText("👁 " + cur.getViewers() + " spectateurs");
        lblDesc.setText(cur.getDescription() != null && !cur.getDescription().isBlank()
                ? cur.getDescription() : "Aucune description.");
        if (cur.getCreatedAt() != null)
            lblDate.setText("📅 " + cur.getCreatedAt().format(FMT));

        applyStatusBadge();

        String playerUrl = cur.getUrl() != null && !cur.getUrl().isBlank()
                ? cur.getUrl() : "";
        lnkUrl.setText(playerUrl.isBlank() ? "—" : playerUrl);
        if (!playerUrl.isBlank()) lnkUrl.setOnAction(e -> openBrowser(playerUrl));

        if (cur.getThumbnail() != null && !cur.getThumbnail().isBlank()) {
            try { imgThumb.setImage(new Image(cur.getThumbnail(), 520, 292, true, true, true)); }
            catch (Exception ignored) {}
        }

        if (btnPrediction != null) {
            btnPrediction.setVisible(isStreamer);
            btnPrediction.setManaged(isStreamer);
        }
    }

    private void applyStatusBadge() {
        lblStatus.setText(cur.getStatusBadge());
        switch (cur.getStatus()) {
            case "live"    -> lblStatus.setStyle(
                    "-fx-background-color:#dc2626;-fx-text-fill:white;" +
                    "-fx-background-radius:20;-fx-padding:4 14;-fx-font-weight:bold;");
            case "offline" -> lblStatus.setStyle(
                    "-fx-background-color:#374151;-fx-text-fill:white;" +
                    "-fx-background-radius:20;-fx-padding:4 14;");
            case "ended"   -> lblStatus.setStyle(
                    "-fx-background-color:#065f46;-fx-text-fill:white;" +
                    "-fx-background-radius:20;-fx-padding:4 14;");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. Player api.video
    //    Identique au bloc Twig :
    //      {% if stream.url %} → iframe api.video
    //      {% else %}          → "Live stream offline."
    // ─────────────────────────────────────────────────────────────────
    private void setupPlayer() {
        boolean hasUrl = cur.getUrl() != null && !cur.getUrl().isBlank();
        if (hasUrl) {
            showVideoPlayer(cur.getUrl());
        } else {
            showOfflineScreen();
        }
    }

    private void showVideoPlayer(String embedUrl) {
        if (playerBox  != null) { playerBox.setVisible(true);  playerBox.setManaged(true); }
        if (offlineBox != null) { offlineBox.setVisible(false); offlineBox.setManaged(false); }
        if (webPlayer  == null) return;

        WebEngine engine = webPlayer.getEngine();
        // HTML minimal avec iframe — ratio 16:9 géré par le FXML
        String html = "<!DOCTYPE html><html><head>" +
                "<style>*{margin:0;padding:0;background:#000;overflow:hidden;}" +
                "iframe{width:100%;height:100%;border:none;}</style></head><body>" +
                "<iframe src=\"" + embedUrl + "\"" +
                " allow=\"autoplay;encrypted-media\" allowfullscreen></iframe>" +
                "</body></html>";
        engine.loadContent(html);
    }

    /**
     * Écran "Live stream offline." — identique au screenshot :
     *   logo api.video + titre + sous-titre + provider
     */
    private void showOfflineScreen() {
        if (playerBox  != null) { playerBox.setVisible(false);  playerBox.setManaged(false); }
        if (offlineBox != null) { offlineBox.setVisible(true);  offlineBox.setManaged(true); }
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. Bloc OBS
    //    Visible si : isStreamer == true ET streamKey non nul
    //    Identique à : {% if app.user == stream.user and stream.streamKey %}
    // ─────────────────────────────────────────────────────────────────
    private void setupObsBox() {
        if (obsBox == null) return;
        boolean show = isStreamer
                && cur.getStreamKey() != null
                && !cur.getStreamKey().isBlank();
        obsBox.setVisible(show);
        obsBox.setManaged(show);
        if (show) {
            String rtmp = cur.getRtmpServer() != null && !cur.getRtmpServer().isBlank()
                    ? cur.getRtmpServer() : "rtmp://broadcast.api.video/s";
            lblRtmp.setText(rtmp);
            lblKey.setText(cur.getStreamKey());
        }
    }

    @FXML private void onCopyKey(ActionEvent e) {
        if (cur.getStreamKey() == null) return;
        copyToClipboard(cur.getStreamKey());
        btnCopyKey.setText("✅ Copié !");
        new java.util.Timer().schedule(new java.util.TimerTask() {
            public void run() { Platform.runLater(() -> btnCopyKey.setText("📋 Copier")); }
        }, 2000);
    }

    @FXML private void onCopyRtmp(ActionEvent e) {
        String rtmp = cur.getRtmpServer() != null
                ? cur.getRtmpServer() : "rtmp://broadcast.api.video/s";
        copyToClipboard(rtmp);
        if (btnCopyRtmp != null) {
            btnCopyRtmp.setText("✅ Copié !");
            new java.util.Timer().schedule(new java.util.TimerTask() {
                public void run() { Platform.runLater(() -> btnCopyRtmp.setText("📋 Copier")); }
            }, 2000);
        }
    }

    private void copyToClipboard(String text) {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    // ─────────────────────────────────────────────────────────────────
    // 4 & 5. Donations
    // ─────────────────────────────────────────────────────────────────
    private void loadDonations() {
        try {
            List<Donation> list = donService.findByStream(cur.getId());
            double total = list.stream().mapToDouble(Donation::getAmount).sum();
            lblTotal.setText(String.format("Total : %.2f €", total));
            lblCount.setText(list.size() + " donation(s)");
            donationGrid.getChildren().clear();
            if (list.isEmpty()) {
                Label empty = new Label("Aucune donation pour le moment 😊");
                empty.setStyle("-fx-text-fill:#64748b;-fx-font-size:14px;");
                donationGrid.getChildren().add(empty);
                return;
            }
            for (Donation d : list) donationGrid.getChildren().add(buildDonCard(d));
        } catch (SQLException ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    private VBox buildDonCard(Donation d) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(160);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;" +
                "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;");
        Label emoji  = new Label(d.getEmoji());             emoji.setStyle("-fx-font-size:32px;");
        Label amount = new Label(d.getFormattedAmount());   amount.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");
        Label donor  = new Label(d.getDonorName());         donor.setStyle("-fx-font-size:12px;-fx-text-fill:#e2e8f0;"); donor.setWrapText(true);
        Label date   = new Label(d.getCreatedAt() != null  ? d.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        card.getChildren().addAll(emoji, amount, donor, date);
        return card;
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

            // Notification Ably au streamer
            try {
                AblyService ably = new AblyService();
                ably.publishDonation(cur.getId(), cur.getTitle(),
                        user != null ? user.getName() : "Anonymous",
                        d.getAmount(), emoji);
            } catch (Exception ignored) {}

        } catch (Exception ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────
    // 6. Prédiction IA
    // ─────────────────────────────────────────────────────────────────
    @FXML private void onPrediction(ActionEvent e) {
        var user = SessionContext.getCurrentUser();
        if (user == null) { AlertUtil.showError("Non connecté", "Connectez-vous d'abord."); return; }
        stopPolling();
        StreamPredictionController c =
                NavigationContext.navigateWithController("User/StreamPrediction.fxml");
        if (c != null) c.init(user);
    }

    // ─────────────────────────────────────────────────────────────────
    // Polling statut live (équivalent "rafraîchis cette page" Symfony)
    //   Toutes les 30s : si stream offline mais a un apiVideoId,
    //   vérifie si OBS a commencé à streamer → affiche le player
    // ─────────────────────────────────────────────────────────────────
    private void startPolling() {
        if (cur.getApiVideoId() == null || cur.getApiVideoId().isBlank()) return;
        if (scheduler != null) scheduler.shutdownNow();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "stream-poll");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            // Si stream était offline mais a un ID api.video → tente d'afficher le player
            if (!cur.isLive() && playerBox != null && !playerBox.isVisible()) {
                String embedUrl = "https://embed.api.video/live/" + cur.getApiVideoId();
                cur.setUrl(embedUrl);
                showVideoPlayer(embedUrl);
            }
        }), 30, 30, TimeUnit.SECONDS);
    }

    private void stopPolling() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    // ─────────────────────────────────────────────────────────────────
    // Navigation
    // ─────────────────────────────────────────────────────────────────
    @FXML private void onBack(ActionEvent e)         { stopPolling(); NavigationContext.navigate("User/StreamList.fxml"); }
    @FXML private void onAllDonations(ActionEvent e) {
        DonationListController c = NavigationContext.navigateWithController("User/DonationList.fxml");
        if (c != null) c.setStream(cur);
    }
    @FXML private void onEdit(ActionEvent e) {
        stopPolling();
        StreamFormController c = NavigationContext.navigateWithController("User/StreamForm.fxml");
        if (c != null) c.initEdit(cur);
    }

    private void openBrowser(String url) {
        try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); }
        catch (Exception ignored) {}
    }
}
