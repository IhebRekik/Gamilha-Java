package com.gamilha.controller.admin;

import com.gamilha.MainApp;
import com.gamilha.service.DonationService;
import com.gamilha.service.StreamService;
import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.util.AlertUtil;
import com.gamilha.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ═══════════════════════════════════════════════════════════════════════════
//  AdminDonationListController
// ═══════════════════════════════════════════════════════════════════════════
public class AdminDonationListController implements Initializable {

    @FXML private Label            lblTitle, lblTotal, countLabel;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortFilter;
    @FXML private FlowPane         donationGrid;

    private Stream cur;
    private final DonationService service = new DonationService();
    private ObservableList<Donation> all = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortFilter.setItems(FXCollections.observableArrayList("Plus récentes","Plus anciens","Montant ↓","Montant ↑"));
        sortFilter.setValue("Plus récentes");
        searchField.textProperty().addListener((o,ov,v) -> filter());
        sortFilter.valueProperty().addListener((o,ov,v) -> filter());
    }

    public void setStream(Stream s) { cur = s; lblTitle.setText("💰 Donations — " + s.getTitle()); load(); }

    private void load() {
        try { all.setAll(service.findByStream(cur.getId())); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur", e.getMessage()); }
    }

    private void filter() {
        String q = searchField.getText().toLowerCase().trim();
        List<Donation> res = all.stream()
            .filter(d -> q.isBlank() || d.getDonorName().toLowerCase().contains(q) || String.format("%.2f",d.getAmount()).contains(q))
            .sorted(switch (sortFilter.getValue()) {
                case "Plus anciens" -> Comparator.comparing(Donation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
                case "Montant ↓"   -> Comparator.comparingDouble(Donation::getAmount).reversed();
                case "Montant ↑"   -> Comparator.comparingDouble(Donation::getAmount);
                default            -> Comparator.comparing(Donation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
            }).collect(Collectors.toList());

        double total = res.stream().mapToDouble(Donation::getAmount).sum();
        lblTotal.setText(String.format("Total : %.2f €", total));
        countLabel.setText(res.size() + " donation(s)");

        donationGrid.getChildren().clear();
        if (res.isEmpty()) { Label e=new Label("Aucune donation"); e.setStyle("-fx-text-fill:#64748b;"); donationGrid.getChildren().add(e); return; }
        for (Donation d : res) donationGrid.getChildren().add(buildCard(d));
    }

    private VBox buildCard(Donation d) {
        VBox card = new VBox(8); card.setPrefWidth(250);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:10;-fx-background-radius:10;-fx-padding:14;");

        HBox top = new HBox(10); top.setAlignment(Pos.CENTER_LEFT);
        Label emoji = new Label(d.getEmoji()); emoji.setStyle("-fx-font-size:26px;");
        Label amt   = new Label(d.getFormattedAmount()); amt.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");
        top.getChildren().addAll(emoji, amt);

        Label donor = new Label("👤 " + d.getDonorName()); donor.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:13px;");
        Label date  = new Label(d.getCreatedAt()!=null ? "📅 "+d.getCreatedAt().format(FMT) : ""); date.setStyle("-fx-text-fill:#64748b;-fx-font-size:11px;");

        Region sep = new Region(); sep.setPrefHeight(1); sep.setStyle("-fx-background-color:#2a2a40;");

        HBox btns = new HBox(6);
        Button bEdit = new Button("✏ Modifier"); Button bDel = new Button("🗑 Supprimer");
        bEdit.setStyle("-fx-background-color:#92400e;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 10 5 10;");
        bDel.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 10 5 10;");
        bEdit.setOnAction(e -> { AdminDonationFormController c = MainApp.loadSceneWithController("admin/AdminDonationForm.fxml"); if(c!=null) c.initEdit(d, cur); });
        bDel.setOnAction(e  -> confirmDel(d));
        btns.getChildren().addAll(bEdit, bDel);

        card.getChildren().addAll(top, donor, date, sep, btns);
        return card;
    }

    private void confirmDel(Donation d) {
        if (AlertUtil.showConfirm("⚠ Supprimer", "Supprimer la donation de "+d.getDonorName()+" ("+d.getFormattedAmount()+") ?")) {
            try { service.delete(d.getId()); AlertUtil.showSuccess("✅","Donation supprimée."); load(); }
            catch (SQLException ex) { AlertUtil.showError("Erreur", ex.getMessage()); }
        }
    }

    // Création désactivée pour l'admin
    @FXML private void onBack(ActionEvent e) { AdminStreamShowController c=MainApp.loadSceneWithController("admin/AdminStreamShow.fxml"); if(c!=null) c.setStream(cur); }
}
