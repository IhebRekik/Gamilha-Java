package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.services.DonationService;
import com.gamilha.services.StreamService;
import com.gamilha.utils.AlertUtil;
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
import javafx.scene.web.WebView;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AdminStreamShowController implements Initializable {

    @FXML private Label     lblTitle, lblStatus, lblGame, lblViewers, lblDesc, lblDate, lblTotal;
    @FXML private Hyperlink lnkUrl;
    @FXML private ImageView imgThumb;

    // Player
    @FXML private VBox    playerBox;
    @FXML private WebView playerView;
    @FXML private VBox    offlineBox;

    // OBS — toujours visible en admin
    @FXML private VBox   obsBox;
    @FXML private Label  lblRtmp, lblKey;
    @FXML private Button btnCopyKey;

    // Donations
    @FXML private FlowPane donGrid;

    private Stream cur;
    private final DonationService donSvc    = new DonationService();
    private final StreamService   streamSvc = new StreamService();
    private ScheduledExecutorService poller;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override public void initialize(URL url, ResourceBundle rb) {}

    public void setStream(Stream s) {
        this.cur = s;
        fillInfo();
        setupPlayer();
        setupObs();
        loadDonations();
        startPolling();
    }

    // ── Infos ──────────────────────────────────────────────────────────
    private void fillInfo() {
        lblTitle.setText(cur.getTitle());
        lblGame.setText("🎮 " + cur.getGame());
        lblViewers.setText("👁 " + cur.getViewers() + " spectateurs");
        lblDesc.setText(cur.getDescription() != null ? cur.getDescription() : "Aucune description.");
        if (cur.getCreatedAt() != null) lblDate.setText("📅 " + cur.getCreatedAt().format(FMT));
        applyStatus(cur.getStatus());
        if (cur.getUrl() != null && !cur.getUrl().isBlank()) {
            lnkUrl.setText(cur.getUrl());
            lnkUrl.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(cur.getUrl())); } catch (Exception ignored) {} });
        }
        if (cur.getThumbnail() != null && !cur.getThumbnail().isBlank()) {
            try { imgThumb.setImage(new Image(cur.getThumbnail(), 460, 260, true, true, true)); } catch (Exception ignored) {}
        }
    }

    private void applyStatus(String status) {
        lblStatus.setText(cur.getStatusBadge());
        lblStatus.setStyle(switch (status) {
            case "live"    -> "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;-fx-font-weight:bold;-fx-font-size:12px;";
            case "offline" -> "-fx-background-color:#374151;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;-fx-font-size:12px;";
            default        -> "-fx-background-color:#065f46;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;-fx-font-size:12px;";
        });
    }

    // ── Player ─────────────────────────────────────────────────────────
    private void setupPlayer() {
        boolean live = "live".equals(cur.getStatus()) || cur.isLive();
        boolean hasUrl = cur.getUrl() != null && !cur.getUrl().isBlank();
        if (live && hasUrl) showPlayer(cur.getUrl());
        else showOffline();
    }

    private void showPlayer(String url) {
        if (playerBox  != null) { playerBox.setVisible(true);  playerBox.setManaged(true);  }
        if (offlineBox != null) { offlineBox.setVisible(false); offlineBox.setManaged(false); }
        if (playerView != null) {
            playerView.getEngine().loadContent(
                "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;background:#000;}html,body{width:100%;height:100%;overflow:hidden;}iframe{width:100%;height:100%;border:none;display:block;}</style></head><body>" +
                "<iframe src='" + url + "' allow='autoplay;encrypted-media' allowfullscreen></iframe></body></html>");
        }
    }

    private void showOffline() {
        if (playerBox  != null) { playerBox.setVisible(false);  playerBox.setManaged(false); }
        if (offlineBox != null) { offlineBox.setVisible(true);  offlineBox.setManaged(true); }
    }

    // ── OBS — admin voit TOUJOURS ─────────────────────────────────────
    private void setupObs() {
        if (obsBox != null) { obsBox.setVisible(true); obsBox.setManaged(true); }
        String rtmp = cur.getRtmpServer() != null ? cur.getRtmpServer() : "rtmp://broadcast.api.video/s";
        String key  = cur.getStreamKey() != null   ? cur.getStreamKey() : "—";
        if (lblRtmp != null) lblRtmp.setText(rtmp);
        if (lblKey  != null) lblKey.setText(key);
    }

    @FXML private void onCopyKey(ActionEvent e) {
        if (cur.getStreamKey() == null) return;
        ClipboardContent cc = new ClipboardContent(); cc.putString(cur.getStreamKey());
        Clipboard.getSystemClipboard().setContent(cc);
        btnCopyKey.setText("✅");
        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            public void run() { Platform.runLater(() -> btnCopyKey.setText("📋")); }
        }, 2000);
    }

    // ── Donations ─────────────────────────────────────────────────────
    private void loadDonations() {
        try {
            List<Donation> list = donSvc.findByStream(cur.getId());
            double total = list.stream().mapToDouble(Donation::getAmount).sum();
            if (lblTotal != null) lblTotal.setText(String.format("Total : %.2f €", total));
            if (donGrid  != null) {
                donGrid.getChildren().clear();
                if (list.isEmpty()) {
                    Label empty = new Label("Aucune donation");
                    empty.setStyle("-fx-text-fill:#4b5563;-fx-font-size:13px;");
                    donGrid.getChildren().add(empty);
                } else {
                    for (Donation d : list) {
                        VBox card = new VBox(4); card.setAlignment(Pos.TOP_CENTER); card.setPrefWidth(140);
                        card.setStyle("-fx-background-color:#111827;-fx-border-color:#1f2937;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12;");
                        Label em  = new Label(d.getEmoji());             em.setStyle("-fx-font-size:26px;");
                        Label amt = new Label(d.getFormattedAmount());   amt.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");
                        Label don = new Label(d.getDonorName());         don.setStyle("-fx-font-size:11px;-fx-text-fill:#e2e8f0;"); don.setWrapText(true);
                        Label dt  = new Label(d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : ""); dt.setStyle("-fx-font-size:10px;-fx-text-fill:#4b5563;");
                        card.getChildren().addAll(em, amt, don, dt);
                        donGrid.getChildren().add(card);
                    }
                }
            }
        } catch (SQLException ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    // ── Polling ────────────────────────────────────────────────────────
    private void startPolling() {
        poller = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r,"admin-poll"); t.setDaemon(true); return t; });
        poller.scheduleAtFixedRate(() -> {
            try {
                Stream fresh = streamSvc.findById(cur.getId());
                if (fresh == null) return;
                Platform.runLater(() -> {
                    boolean wasLive = "live".equals(cur.getStatus());
                    boolean nowLive = "live".equals(fresh.getStatus()) || fresh.isLive();
                    cur.setStatus(fresh.getStatus()); cur.setIsLive(fresh.isLive());
                    cur.setViewers(fresh.getViewers()); if (fresh.getUrl() != null) cur.setUrl(fresh.getUrl());
                    lblViewers.setText("👁 " + cur.getViewers() + " spectateurs");
                    applyStatus(cur.getStatus());
                    if (!wasLive && nowLive && cur.getUrl() != null) showPlayer(cur.getUrl());
                    else if (wasLive && !nowLive) showOffline();
                });
            } catch (Exception ignored) {}
        }, 30, 30, TimeUnit.SECONDS);
    }

    // ── Navigation ─────────────────────────────────────────────────────
    @FXML private void onBack(ActionEvent e)      { if (poller != null) poller.shutdownNow(); MainApp.loadScene("Admin/AdminStreamList.fxml"); }
    @FXML private void onEdit(ActionEvent e)      { AdminStreamFormController c = MainApp.loadSceneWithController("Admin/AdminStreamForm.fxml"); if (c != null) c.initEdit(cur); }
    @FXML private void onDonations(ActionEvent e) { AdminDonationListController c = MainApp.loadSceneWithController("Admin/AdminDonationList.fxml"); if (c != null) c.setStream(cur); }
}
