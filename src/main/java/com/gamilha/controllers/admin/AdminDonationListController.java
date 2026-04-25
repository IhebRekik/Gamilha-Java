package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.services.DonationService;
import com.gamilha.utils.AlertUtil;
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

/**
 * AdminDonationListController — donations d'un stream côté admin.
 * Identique à admin/donation/index.html.twig Symfony.
 */
public class AdminDonationListController implements Initializable {

    @FXML private Label            lblTitle;
    @FXML private Label            lblTotal;
    @FXML private Label            countLabel;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortFilter;
    @FXML private FlowPane         donationGrid;

    private Stream cur;
    private final DonationService service = new DonationService();
    private ObservableList<Donation> all  = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortFilter.setItems(FXCollections.observableArrayList(
                "Plus récentes", "Plus anciens", "Montant ↓", "Montant ↑"));
        sortFilter.setValue("Plus récentes");
        searchField.textProperty().addListener((o, ov, v) -> filter());
        sortFilter.valueProperty().addListener((o, ov, v)  -> filter());
    }

    public void setStream(Stream s) {
        cur = s;
        lblTitle.setText("💰 Donations — " + s.getTitle());
        load();
    }

    private void load() {
        try { all.setAll(service.findByStream(cur.getId())); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur BDD", e.getMessage()); }
    }

    private void filter() {
        String q    = searchField.getText().toLowerCase().trim();
        String sort = sortFilter.getValue();

        List<Donation> res = all.stream()
                .filter(d -> q.isBlank()
                        || d.getDonorName().toLowerCase().contains(q)
                        || String.format("%.2f", d.getAmount()).contains(q))
                .sorted(switch (sort) {
                    case "Plus anciens" -> Comparator.comparing(Donation::getCreatedAt);
                    case "Montant ↓"   -> Comparator.comparingDouble(Donation::getAmount).reversed();
                    case "Montant ↑"   -> Comparator.comparingDouble(Donation::getAmount);
                    default            -> Comparator.comparing(Donation::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()));
                })
                .collect(Collectors.toList());

        double total = res.stream().mapToDouble(Donation::getAmount).sum();
        lblTotal.setText(String.format("Total : %.2f €", total));
        countLabel.setText(res.size() + " donation(s)");
        buildGrid(res);
    }

    private void buildGrid(List<Donation> list) {
        donationGrid.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Aucune donation pour ce stream");
            empty.setStyle("-fx-text-fill:#64748b;-fx-font-size:14px;");
            donationGrid.getChildren().add(empty);
            return;
        }
        for (Donation d : list) donationGrid.getChildren().add(buildCard(d));
    }

    private VBox buildCard(Donation d) {
        VBox card = new VBox(8);
        card.setPrefWidth(200);
        card.setPadding(new Insets(14));
        card.setAlignment(Pos.TOP_CENTER);
        card.setStyle("-fx-background-color:#111827;-fx-border-color:#1f2937;" +
                "-fx-border-radius:12;-fx-background-radius:12;");

        Label emoji  = new Label(d.getEmoji());
        emoji.setStyle("-fx-font-size:36px;");

        Label amount = new Label(d.getFormattedAmount());
        amount.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");

        Label donor  = new Label(d.getDonorName());
        donor.setStyle("-fx-font-size:12px;-fx-text-fill:#e2e8f0;");
        donor.setWrapText(true);

        Label date   = new Label(d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-font-size:11px;-fx-text-fill:#475569;");

        // Bouton suppression admin
        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;" +
                "-fx-background-radius:6;-fx-cursor:hand;-fx-padding:4 10;-fx-font-size:11px;");
        btnDel.setMaxWidth(Double.MAX_VALUE);
        btnDel.setOnAction(e -> confirmDel(d));

        card.getChildren().addAll(emoji, amount, donor, date, btnDel);
        return card;
    }

    private void confirmDel(Donation d) {
        boolean ok = AlertUtil.showConfirm("⚠ Supprimer la donation",
                "Supprimer la donation de " + d.getDonorName() + " (" + d.getFormattedAmount() + ") ?");
        if (!ok) return;
        try { service.delete(d.getId()); AlertUtil.showSuccess("✅ Supprimée", "Donation supprimée."); load(); }
        catch (SQLException ex) { AlertUtil.showError("Erreur BDD", ex.getMessage()); }
    }

    @FXML private void onRefresh(ActionEvent e) { searchField.clear(); sortFilter.setValue("Plus récentes"); load(); }
    @FXML private void onBack(ActionEvent e)    { MainApp.loadScene("Admin/AdminDonationStreams.fxml"); }
}
