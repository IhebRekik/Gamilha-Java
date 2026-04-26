package com.gamilha.controllers.admin;

import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;
import com.gamilha.services.DonationService;
import com.gamilha.utils.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDonationListController implements Initializable {

    @FXML private Label            lblTitle, lblTotal, countLabel;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortFilter;
    @FXML private VBox             donationRows;   // tableau lignes (remplace FlowPane)

    private Stream cur;
    private final DonationService service = new DonationService();
    private ObservableList<Donation> all = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortFilter.setItems(FXCollections.observableArrayList(
                "Plus récentes", "Plus anciennes", "Montant ↓", "Montant ↑"));
        sortFilter.setValue("Plus récentes");
        searchField.textProperty().addListener((o, ov, v) -> filter());
        sortFilter.valueProperty().addListener((o, ov, v) -> filter());
    }

    /** Appelé depuis AdminStreamListController.onViewDonations() */
    public void setStream(Stream s) {
        cur = s;
        lblTitle.setText("💰 Donations — " + s.getTitle());
        load();
    }

    private void load() {
        try {
            all.setAll(service.findByStream(cur.getId()));
            filter();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur", e.getMessage());
        }
    }

    private void filter() {
        String q = searchField.getText().toLowerCase().trim();

        Comparator<Donation> cmp;
        String sort = sortFilter.getValue();
        if ("Plus anciennes".equals(sort))
            cmp = Comparator.comparing(Donation::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        else if ("Montant ↓".equals(sort))
            cmp = Comparator.comparingDouble(Donation::getAmount).reversed();
        else if ("Montant ↑".equals(sort))
            cmp = Comparator.comparingDouble(Donation::getAmount);
        else
            cmp = Comparator.comparing(Donation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

        List<Donation> res = all.stream()
                .filter(d -> q.isBlank()
                        || d.getDonorName().toLowerCase().contains(q)
                        || String.format("%.2f", d.getAmount()).contains(q))
                .sorted(cmp)
                .collect(Collectors.toList());

        double total = res.stream().mapToDouble(Donation::getAmount).sum();
        lblTotal.setText(String.format("Total : %.2f €", total));
        countLabel.setText(res.size() + " donation(s)");

        donationRows.getChildren().clear();

        if (res.isEmpty()) {
            Label empty = new Label("Aucune donation pour ce stream");
            empty.setStyle("-fx-text-fill:#4b5563;-fx-font-size:14px;-fx-padding:20;");
            donationRows.getChildren().add(empty);
            return;
        }

        // Header du tableau
        HBox header = new HBox();
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle("-fx-background-color:#111827;-fx-border-color:#1f2937;-fx-border-radius:10 10 0 0;-fx-background-radius:10 10 0 0;");
        header.getChildren().addAll(
                hCell("Donateur",  "#9ca3af", Priority.ALWAYS),
                hCell("Montant",   "#9ca3af", Priority.SOMETIMES),
                hCell("Type",      "#9ca3af", Priority.SOMETIMES),
                hCell("Date",      "#9ca3af", Priority.SOMETIMES),
                hCell("Actions",   "#9ca3af", Priority.NEVER)
        );
        donationRows.getChildren().add(header);

        for (int i = 0; i < res.size(); i++)
            donationRows.getChildren().add(buildRow(res.get(i), i % 2 == 0));
    }

    private Label hCell(String text, String color, Priority grow) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill:" + color + ";-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:0 12 0 0;");
        if (grow == Priority.ALWAYS)    { l.setPrefWidth(200); HBox.setHgrow(l, Priority.ALWAYS); }
        else if (grow == Priority.SOMETIMES) { l.setPrefWidth(120); }
        else                            { l.setPrefWidth(130); }
        return l;
    }

    private HBox buildRow(Donation d, boolean even) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-background-color:" + (even ? "#0f1117" : "#111827") + ";" +
                "-fx-border-color:transparent transparent #1f2937 transparent;-fx-border-width:0 0 1 0;");

        // Donateur
        VBox donorBox = new VBox(2);
        HBox.setHgrow(donorBox, Priority.ALWAYS);
        Label donor = new Label("👤 " + d.getDonorName());
        donor.setStyle("-fx-text-fill:#f9fafb;-fx-font-size:13px;-fx-font-weight:bold;");
        donorBox.getChildren().add(donor);

        // Montant
        Label amt = new Label(d.getFormattedAmount());
        amt.setPrefWidth(120);
        amt.setStyle("-fx-text-fill:#4ade80;-fx-font-size:15px;-fx-font-weight:bold;");

        // Emoji / type
        Label emoji = new Label(d.getEmoji() != null ? d.getEmoji() : "💸");
        emoji.setPrefWidth(120);
        emoji.setStyle("-fx-font-size:20px;");

        // Date
        String dateStr = d.getCreatedAt() != null ? d.getCreatedAt().format(FMT) : "—";
        Label date = new Label(dateStr);
        date.setPrefWidth(120);
        date.setStyle("-fx-text-fill:#6b7280;-fx-font-size:12px;");

        // Actions
        HBox actions = new HBox(6);
        actions.setPrefWidth(130);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button btnDel = new Button("🗑 Supprimer");
        btnDel.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:#fca5a5;" +
                "-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 12;-fx-font-size:12px;");
        btnDel.setOnAction(e -> confirmDel(d));

        actions.getChildren().add(btnDel);

        row.getChildren().addAll(donorBox, amt, emoji, date, actions);
        return row;
    }

    private void confirmDel(Donation d) {
        boolean ok = AlertUtil.showConfirm("⚠ Supprimer",
                "Supprimer la donation de " + d.getDonorName() +
                        " (" + d.getFormattedAmount() + ") ?");
        if (!ok) return;
        try {
            service.delete(d.getId());
            load();
        } catch (SQLException ex) {
            AlertUtil.showError("Erreur", ex.getMessage());
        }
    }

    // ── onBack : retour vers AdminStreamList ─────────────────────────────────
    // Architecture : NavBar.fxml (BorderPane root) → contentArea (BorderPane center)
    //                → AdminDonationList.fxml (VBox chargé dans contentArea)
    // On remonte jusqu'au root de la scène (NavBar.fxml) et on accède
    // au NavBarAdminController via UserData, puis on appelle loadPage().
    @FXML
    private void onBack(ActionEvent e) {
        try {
            javafx.scene.Scene scene = donationRows.getScene();
            javafx.scene.Node root   = scene.getRoot();

            // Le root de la scène EST NavBar.fxml (BorderPane)
            // Le contrôleur y est stocké dans userData par FXMLLoader
            Object userData = root.getUserData();
            if (userData instanceof com.gamilha.controllers.NavBarAdminController) {
                ((com.gamilha.controllers.NavBarAdminController) userData)
                        .loadPage("AdminStreamList.fxml");
                return;
            }

            // Fallback : chercher le contentArea (child center de NavBar BorderPane)
            // et y injecter directement AdminStreamList
            if (root instanceof BorderPane) {
                BorderPane navBarRoot = (BorderPane) root;
                // center = contentArea (BorderPane) défini dans NavBar.fxml
                javafx.scene.Node center = navBarRoot.getCenter();
                if (center instanceof BorderPane) {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource("/com/gamilha/interfaces/Admin/AdminStreamList.fxml"));
                    Parent streamList = loader.load();
                    ((BorderPane) center).setCenter(streamList);
                    return;
                }
            }

            // Dernier recours : recharger toute la NavBar
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/Admin/NavBar.fxml"));
            Parent navBar = loader.load();
            scene.setRoot(navBar);
            com.gamilha.controllers.NavBarAdminController navCtrl = loader.getController();
            navCtrl.loadPage("AdminStreamList.fxml");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
