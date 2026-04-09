package com.gamilha.controller;

import com.gamilha.MainApp;
import com.gamilha.service.DonationService;
import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.util.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class StreamShowController implements Initializable {

    @FXML private Label     lblTitle, lblStatus, lblGame, lblViewers, lblDesc, lblDate;
    @FXML private Hyperlink lnkUrl;
    @FXML private ImageView imgThumb;
    @FXML private VBox      obsBox;
    @FXML private Label     lblKey, lblRtmp, lblTotal, lblCount;
    @FXML private FlowPane  donationGrid;

    private Stream cur;
    private final DonationService service = new DonationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override public void initialize(URL url, ResourceBundle rb) {}

    public void setStream(Stream s) { this.cur = s; fillInfo(); loadDonations(); }

    private void fillInfo() {
        lblTitle.setText(cur.getTitle());
        lblGame.setText("🎮 " + cur.getGame());
        lblViewers.setText("👁 " + cur.getViewers() + " spectateurs");
        lblDesc.setText(cur.getDescription() != null && !cur.getDescription().isBlank()
            ? cur.getDescription() : "Aucune description.");
        if (cur.getCreatedAt() != null) lblDate.setText("📅 " + cur.getCreatedAt().format(FMT));

        lblStatus.setText(cur.getStatusBadge());
        switch (cur.getStatus()) {
            case "live"    -> lblStatus.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 12 4 12;-fx-font-weight:bold;");
            case "offline" -> lblStatus.setStyle("-fx-background-color:#374151;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 12 4 12;");
            case "ended"   -> lblStatus.setStyle("-fx-background-color:#065f46;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:4 12 4 12;");
        }

        if (cur.getUrl() != null && !cur.getUrl().isBlank()) {
            lnkUrl.setText(cur.getUrl());
            lnkUrl.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(cur.getUrl())); } catch (Exception ex) { ex.printStackTrace(); }});
        } else { lnkUrl.setText("Aucun lien"); }

        if (cur.getThumbnail() != null && !cur.getThumbnail().isBlank()) {
            try { imgThumb.setImage(new Image(cur.getThumbnail(), 500, 280, true, true, true)); }
            catch (Exception ignored) {}
        }

        if (cur.getStreamKey() != null && !cur.getStreamKey().isBlank()) {
            lblKey.setText(cur.getStreamKey());
            lblRtmp.setText(cur.getRtmpServer() != null ? cur.getRtmpServer() : "rtmp://broadcast.api.video/s");
            obsBox.setVisible(true); obsBox.setManaged(true);
        }
    }

    private void loadDonations() {
        try {
            List<Donation> list = service.findByStream(cur.getId());
            double total = list.stream().mapToDouble(Donation::getAmount).sum();
            lblTotal.setText(String.format("Total : %.2f €", total));
            lblCount.setText(list.size() + " donation(s)");
            donationGrid.getChildren().clear();

            if (list.isEmpty()) {
                Label e = new Label("Aucune donation pour le moment");
                e.setStyle("-fx-text-fill:#64748b;-fx-font-size:14px;");
                donationGrid.getChildren().add(e);
                return;
            }
            for (Donation d : list) donationGrid.getChildren().add(buildDonCard(d));
        } catch (SQLException ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    // ── Card donation (GridPane/FlowPane — pas de toString) ────────────────
    private VBox buildDonCard(Donation d) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(160);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;");

        Label emoji  = new Label(d.getEmoji());
        emoji.setStyle("-fx-font-size:32px;");

        Label amount = new Label(d.getFormattedAmount());
        amount.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");

        Label donor  = new Label(d.getDonorName());
        donor.setStyle("-fx-font-size:12px;-fx-text-fill:#e2e8f0;");
        donor.setWrapText(true);

        Label date   = new Label(d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");

        card.getChildren().addAll(emoji, amount, donor, date);
        return card;
    }

    // ── Donations emoji rapides ────────────────────────────────────────────
    @FXML private void onDonate1(ActionEvent e)  { quickDon("🍩"); }
    @FXML private void onDonate5(ActionEvent e)  { quickDon("🍕"); }
    @FXML private void onDonate10(ActionEvent e) { quickDon("💎"); }
    @FXML private void onDonate50(ActionEvent e) { quickDon("🚀"); }

    private void quickDon(String emoji) {
        try {
            Donation d = service.donateByEmoji(cur.getId(), 1, "Anonymous", emoji);
            AlertUtil.showSuccess("Merci ! " + emoji, "Donation de " + d.getFormattedAmount() + " enregistrée !");
            loadDonations();
        } catch (Exception ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    @FXML private void onCustomDonate(ActionEvent e) {
        DonationFormController c = MainApp.loadSceneWithController("DonationForm.fxml");
        if (c != null) c.initCreate(cur);
    }

    @FXML private void onAllDonations(ActionEvent e) {
        DonationListController c = MainApp.loadSceneWithController("DonationList.fxml");
        if (c != null) c.setStream(cur);
    }

    @FXML private void onBack(ActionEvent e)  { MainApp.loadScene("StreamList.fxml"); }
    @FXML private void onEdit(ActionEvent e)  {
        StreamFormController c = MainApp.loadSceneWithController("StreamForm.fxml");
        if (c != null) c.initEdit(cur);
    }
}
