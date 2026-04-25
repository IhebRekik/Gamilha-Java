package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.services.DonationService;
import com.gamilha.utils.AlertUtil;
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

/**
 * AdminStreamShowController — vue détail stream côté admin.
 * L'admin voit TOUJOURS le bloc OBS (clé + RTMP) pour modération.
 * Player api.video intégré via WebView.
 */
public class AdminStreamShowController implements Initializable {

    @FXML private Label     lblTitle, lblStatus, lblGame, lblViewers, lblDesc, lblDate, lblTotal;
    @FXML private Label     lblKey, lblRtmp, lblApiId;
    @FXML private Hyperlink lnkUrl;
    @FXML private ImageView imgThumb;
    @FXML private VBox      obsBox, playerBox, offlineBox;
    @FXML private WebView   webPlayer;
    @FXML private FlowPane  donGrid;

    private Stream cur;
    private final DonationService service = new DonationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override public void initialize(URL url, ResourceBundle rb) {}

    public void setStream(Stream s) {
        cur = s;
        fill();
        setupPlayer();
        loadDonations();
    }

    private void fill() {
        lblTitle.setText(cur.getTitle());
        lblGame.setText("🎮 " + cur.getGame());
        lblViewers.setText("👁 " + cur.getViewers() + " spectateurs");
        lblDesc.setText(cur.getDescription() != null ? cur.getDescription() : "Aucune description.");
        if (cur.getCreatedAt() != null) lblDate.setText("📅 " + cur.getCreatedAt().format(FMT));

        lblStatus.setText(cur.getStatusBadge());
        switch (cur.getStatus()) {
            case "live"    -> lblStatus.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;-fx-font-weight:bold;");
            case "offline" -> lblStatus.setStyle("-fx-background-color:#374151;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;");
            case "ended"   -> lblStatus.setStyle("-fx-background-color:#065f46;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 14;");
        }

        if (cur.getUrl() != null && !cur.getUrl().isBlank()) {
            lnkUrl.setText(cur.getUrl());
            lnkUrl.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(cur.getUrl())); } catch (Exception ex) { ex.printStackTrace(); } });
        } else {
            lnkUrl.setText("—");
        }

        if (cur.getThumbnail() != null && !cur.getThumbnail().isBlank()) {
            try { imgThumb.setImage(new Image(cur.getThumbnail(), 480, 270, true, true, true)); }
            catch (Exception ignored) {}
        }

        // L'admin voit TOUJOURS le bloc OBS (pour modération)
        if (obsBox != null) {
            obsBox.setVisible(true);
            obsBox.setManaged(true);
            lblRtmp.setText(cur.getRtmpServer() != null ? cur.getRtmpServer() : "rtmp://broadcast.api.video/s");
            lblKey.setText(cur.getStreamKey() != null ? cur.getStreamKey() : "—");
            if (lblApiId != null) lblApiId.setText(cur.getApiVideoId() != null ? cur.getApiVideoId() : "—");
        }
    }

    private void setupPlayer() {
        boolean hasUrl = cur.getUrl() != null && !cur.getUrl().isBlank();
        if (playerBox  != null) { playerBox.setVisible(hasUrl);  playerBox.setManaged(hasUrl); }
        if (offlineBox != null) { offlineBox.setVisible(!hasUrl); offlineBox.setManaged(!hasUrl); }
        if (hasUrl && webPlayer != null) {
            WebEngine engine = webPlayer.getEngine();
            String html = "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;background:#000;overflow:hidden;}iframe{width:100%;height:100%;border:none;}</style></head><body>" +
                    "<iframe src=\"" + cur.getUrl() + "\" allow=\"autoplay;encrypted-media\" allowfullscreen></iframe></body></html>";
            engine.loadContent(html);
        }
    }

    private void loadDonations() {
        try {
            List<Donation> list = service.findByStream(cur.getId());
            double total = list.stream().mapToDouble(Donation::getAmount).sum();
            lblTotal.setText(String.format("Total donations : %.2f €", total));
            donGrid.getChildren().clear();
            if (list.isEmpty()) {
                Label empty = new Label("Aucune donation");
                empty.setStyle("-fx-text-fill:#64748b;");
                donGrid.getChildren().add(empty);
                return;
            }
            for (Donation d : list) {
                VBox card = new VBox(4);
                card.setAlignment(Pos.CENTER);
                card.setPrefWidth(150);
                card.setStyle("-fx-background-color:#12121a;-fx-border-color:#2a2a40;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12;");
                Label emoji  = new Label(d.getEmoji());  emoji.setStyle("-fx-font-size:28px;");
                Label amount = new Label(d.getFormattedAmount()); amount.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;-fx-font-size:15px;");
                Label donor  = new Label(d.getDonorName()); donor.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11px;"); donor.setWrapText(true);
                Label date   = new Label(d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : ""); date.setStyle("-fx-text-fill:#475569;-fx-font-size:10px;");
                card.getChildren().addAll(emoji, amount, donor, date);
                donGrid.getChildren().add(card);
            }
        } catch (SQLException ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    @FXML private void onBack(ActionEvent e)      { MainApp.loadScene("Admin/AdminStreamList.fxml"); }
    @FXML private void onEdit(ActionEvent e)      { AdminStreamFormController c = MainApp.loadSceneWithController("Admin/AdminStreamForm.fxml"); if (c != null) c.initEdit(cur); }
    @FXML private void onDonations(ActionEvent e) { AdminDonationListController c = MainApp.loadSceneWithController("Admin/AdminDonationList.fxml"); if (c != null) c.setStream(cur); }
    @FXML private void onCopyKey(ActionEvent e)   { if (cur.getStreamKey() != null) { ClipboardContent cc = new ClipboardContent(); cc.putString(cur.getStreamKey()); Clipboard.getSystemClipboard().setContent(cc); AlertUtil.showSuccess("Copié", "Clé de flux copiée ✅"); } }
}
