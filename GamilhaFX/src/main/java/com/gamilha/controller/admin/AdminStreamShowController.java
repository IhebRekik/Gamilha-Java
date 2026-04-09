package com.gamilha.controller.admin;

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
import javafx.scene.image.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class AdminStreamShowController implements Initializable {

    @FXML private Label     lblTitle, lblStatus, lblGame, lblViewers, lblDesc, lblDate, lblKey, lblRtmp, lblTotal;
    @FXML private Hyperlink lnkUrl;
    @FXML private ImageView imgThumb;
    @FXML private VBox      obsBox;
    @FXML private FlowPane  donGrid;

    private Stream cur;
    private final DonationService service = new DonationService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override public void initialize(URL url, ResourceBundle rb) {}

    public void setStream(Stream s) { cur = s; fill(); loadDon(); }

    private void fill() {
        lblTitle.setText(cur.getTitle());
        lblStatus.setText(cur.getStatusBadge());
        lblGame.setText("🎮 " + cur.getGame());
        lblViewers.setText("👁 " + cur.getViewers() + " spectateurs");
        lblDesc.setText(cur.getDescription() != null ? cur.getDescription() : "Aucune description.");
        if (cur.getCreatedAt() != null) lblDate.setText("📅 " + cur.getCreatedAt().format(FMT));
        if (cur.getUrl() != null && !cur.getUrl().isBlank()) {
            lnkUrl.setText(cur.getUrl());
            lnkUrl.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(cur.getUrl())); } catch (Exception ex) { ex.printStackTrace(); }});
        }
        if (cur.getThumbnail() != null) {
            try { imgThumb.setImage(new Image(cur.getThumbnail(), 480, 270, true, true, true)); } catch (Exception ignored) {}
        }
        if (cur.getStreamKey() != null) {
            lblKey.setText(cur.getStreamKey());
            lblRtmp.setText(cur.getRtmpServer() != null ? cur.getRtmpServer() : "rtmp://broadcast.api.video/s");
            obsBox.setVisible(true); obsBox.setManaged(true);
        }
    }

    private void loadDon() {
        try {
            List<Donation> list = service.findByStream(cur.getId());
            double total = list.stream().mapToDouble(Donation::getAmount).sum();
            lblTotal.setText(String.format("Total donations : %.2f €", total));
            donGrid.getChildren().clear();
            if (list.isEmpty()) { Label e = new Label("Aucune donation"); e.setStyle("-fx-text-fill:#64748b;"); donGrid.getChildren().add(e); return; }
            for (Donation d : list) {
                VBox card = new VBox(4); card.setAlignment(Pos.CENTER); card.setPrefWidth(140);
                card.setStyle("-fx-background-color:#12121a;-fx-border-color:#2a2a40;-fx-border-radius:8;-fx-background-radius:8;-fx-padding:10;");
                Label em = new Label(d.getEmoji()); em.setStyle("-fx-font-size:24px;");
                Label am = new Label(d.getFormattedAmount()); am.setStyle("-fx-text-fill:#4ade80;-fx-font-weight:bold;");
                Label dn = new Label(d.getDonorName()); dn.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:11px;"); dn.setWrapText(true);
                card.getChildren().addAll(em, am, dn);
                donGrid.getChildren().add(card);
            }
        } catch (SQLException ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
    }

    @FXML private void onBack(ActionEvent e)   { MainApp.loadScene("admin/AdminStreamList.fxml"); }
    @FXML private void onEdit(ActionEvent e)   { AdminStreamFormController c = MainApp.loadSceneWithController("admin/AdminStreamForm.fxml"); if(c!=null) c.initEdit(cur); }
    @FXML private void onDonations(ActionEvent e) { AdminDonationListController c = MainApp.loadSceneWithController("admin/AdminDonationList.fxml"); if(c!=null) c.setStream(cur); }
}
