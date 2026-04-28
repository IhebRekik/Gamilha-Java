package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.entity.Stream;
import com.gamilha.services.DonationService;
import com.gamilha.services.StreamService;
import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.NavigationContext;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AdminStreamListController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> sortFilter;
    @FXML private ComboBox<String> viewersFilter;
    @FXML private Label            countLabel;
    @FXML private GridPane         streamGrid;

    private final StreamService   streamSvc = new StreamService();
    private final DonationService donSvc    = new DonationService();
    private ObservableList<Stream> all      = FXCollections.observableArrayList();

    private static final int COLS = 3;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gameFilter.setItems(FXCollections.observableArrayList(
                "Tous les jeux", "CS2", "Valorant", "LoL", "Dota2"));
        gameFilter.setValue("Tous les jeux");

        sortFilter.setItems(FXCollections.observableArrayList(
                "Plus récents", "Plus regardés", "Moins regardés"));
        sortFilter.setValue("Plus récents");

        viewersFilter.setItems(FXCollections.observableArrayList(
                "Tous", "+10", "+50", "+100", "+500"));
        viewersFilter.setValue("Tous");

        searchField.textProperty().addListener((o, ov, v) -> filter());
        gameFilter.valueProperty().addListener((o, ov, v)   -> filter());
        sortFilter.valueProperty().addListener((o, ov, v)   -> filter());
        viewersFilter.valueProperty().addListener((o, ov, v) -> filter());

        load();
    }

    private void load() {
        try { all.setAll(streamSvc.findAll()); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur BDD", e.getMessage()); }
    }

    private void filter() {
        String q    = searchField.getText().toLowerCase().trim();
        String game = gameFilter.getValue();
        String sort = sortFilter.getValue();
        int minV    = switch (viewersFilter.getValue()) {
            case "+10"  -> 10; case "+50" -> 50;
            case "+100" -> 100; case "+500" -> 500; default -> 0;
        };

        List<Stream> res = all.stream()
                .filter(s -> q.isBlank()
                        || s.getTitle().toLowerCase().contains(q)
                        || (s.getGame() != null && s.getGame().toLowerCase().contains(q)))
                .filter(s -> "Tous les jeux".equals(game) || game.equalsIgnoreCase(s.getGame()))
                .filter(s -> s.getViewers() >= minV)
                .sorted(switch (sort) {
                    case "Plus regardés"  -> Comparator.comparingInt(Stream::getViewers).reversed();
                    case "Moins regardés" -> Comparator.comparingInt(Stream::getViewers);
                    default               -> Comparator.comparing(Stream::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder()));
                })
                .collect(Collectors.toList());

        countLabel.setText(res.size() + " stream(s)");
        buildGrid(res);
    }

    private void buildGrid(List<Stream> streams) {
        streamGrid.getChildren().clear();
        streamGrid.getColumnConstraints().clear();
        streamGrid.setHgap(20); streamGrid.setVgap(20);

        for (int i = 0; i < COLS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / COLS);
            streamGrid.getColumnConstraints().add(cc);
        }

        if (streams.isEmpty()) {
            Label lbl = new Label("📡 Aucun stream trouvé");
            lbl.setStyle("-fx-text-fill:#4b5563;-fx-font-size:15px;");
            streamGrid.add(lbl, 0, 0);
            return;
        }

        for (int i = 0; i < streams.size(); i++)
            streamGrid.add(buildCard(streams.get(i)), i % COLS, i / COLS);
    }

    private VBox buildCard(Stream s) {
        VBox card = new VBox(0);
        card.setPrefWidth(380); card.setMaxWidth(380);
        card.setStyle("-fx-background-color:#111827;-fx-border-color:#1f2937;" +
                "-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;");

        // ── Thumbnail avec badges ──────────────────────────────────────
        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(190); imgBox.setMaxHeight(190);

        // fond par défaut (gradient violet-noir)
        Pane fallback = new Pane();
        fallback.setPrefSize(380, 190);
        fallback.setStyle("-fx-background-color:linear-gradient(135deg,#1a0a2e,#0a0a0f);");
        Label fallbackIcon = new Label("🎮");
        fallbackIcon.setStyle("-fx-font-size:48px;-fx-opacity:0.3;");
        StackPane.setAlignment(fallbackIcon, Pos.CENTER);

        ImageView img = new ImageView();
        img.setFitWidth(380); img.setFitHeight(190); img.setPreserveRatio(false);
        Rectangle clip = new Rectangle(380, 190);
        clip.setArcWidth(14); clip.setArcHeight(14);
        img.setClip(clip);

        if (s.getThumbnail() != null && !s.getThumbnail().isBlank()) {
            try { img.setImage(new Image(s.getThumbnail(), true)); }
            catch (Exception ignored) {}
        }

        // Badge statut (LIVE / OFFLINE / TERMINÉ)
        Label statusBadge = new Label(s.getStatusBadge());
        statusBadge.setStyle("live".equals(s.getStatus())
                ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10;"
                : "ended".equals(s.getStatus())
                  ? "-fx-background-color:#065f46;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 10;"
                  : "-fx-background-color:#374151;-fx-text-fill:#94a3b8;-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 10;");
        StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
        StackPane.setMargin(statusBadge, new Insets(8, 0, 0, 8));

        // Badge viewers
        Label viewBadge = new Label("👁 " + s.getViewers());
        viewBadge.setStyle("-fx-background-color:rgba(0,0,0,0.7);-fx-text-fill:#e2e8f0;" +
                "-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 8;");
        StackPane.setAlignment(viewBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(viewBadge, new Insets(0, 8, 8, 0));

        // Badge ⚙ ADMIN
        Label adminBadge = new Label("⚙");
        adminBadge.setStyle("-fx-background-color:rgba(139,92,246,0.85);-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 8;");
        StackPane.setAlignment(adminBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(adminBadge, new Insets(8, 8, 0, 0));

        imgBox.getChildren().addAll(fallback, fallbackIcon, img, statusBadge, viewBadge, adminBadge);

        // ── Corps ──────────────────────────────────────────────────────
        VBox body = new VBox(6);
        body.setPadding(new Insets(12, 14, 6, 14));

        Label title = new Label(s.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f9fafb;");
        title.setWrapText(true);

        HBox meta = new HBox(10);
        Label game = new Label("🎮 " + (s.getGame() != null ? s.getGame() : "—"));
        game.setStyle("-fx-font-size:12px;-fx-text-fill:#67e8f9;");
        Label date = new Label(s.getCreatedAt() != null ? "📅 " + s.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-font-size:11px;-fx-text-fill:#4b5563;");
        meta.getChildren().addAll(game, date);

        // Donations total
        double donTotal = 0;
        int    donCount = 0;
        try { var dons = donSvc.findByStream(s.getId()); donCount = dons.size(); donTotal = dons.stream().mapToDouble(d -> d.getAmount()).sum(); }
        catch (Exception ignored) {}
        Label donRow = new Label(String.format("💰 %d donation(s) — %.2f €", donCount, donTotal));
        donRow.setStyle("-fx-font-size:12px;-fx-text-fill:#4ade80;");

        body.getChildren().addAll(title, meta, donRow);

        // ── Boutons admin ──────────────────────────────────────────────
        HBox btns = new HBox(8);
        btns.setPadding(new Insets(8, 14, 14, 14));

        Button btnView = new Button("👁 Voir");
        btnView.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;-fx-font-size:12px;");
        btnView.setOnAction(e -> {
            AdminStreamShowController c = loadWithController("/com/gamilha/interfaces/Admin/AdminStreamShow.fxml");
            if (c != null) c.setStream(s);
        });

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.setStyle("-fx-background-color:#d97706;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;-fx-font-size:12px;");
        btnEdit.setOnAction(e -> {
            AdminStreamFormController c = loadWithController("/com/gamilha/interfaces/Admin/AdminStreamForm.fxml");
            if (c != null) c.initEdit(s);
        });

        Button btnDons = new Button("💰");
        btnDons.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 10;-fx-font-size:13px;");
        btnDons.setOnAction(e -> {
            AdminDonationListController c = loadWithController("/com/gamilha/interfaces/Admin/AdminDonationList.fxml");
            if (c != null) c.setStream(s);
        });

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 10;-fx-font-size:13px;");
        btnDel.setOnAction(e -> confirmDelete(s));

        btns.getChildren().addAll(btnView, btnEdit, btnDons, sp, btnDel);

        card.getChildren().addAll(imgBox, body, btns);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#1a1a2e;-fx-border-color:#8b5cf6;" +
                        "-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;" +
                        "-fx-effect:dropshadow(gaussian,rgba(139,92,246,0.25),16,0,0,4);"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#111827;-fx-border-color:#1f2937;" +
                        "-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;"));

        return card;
    }

    private void confirmDelete(Stream s) {
        boolean ok = AlertUtil.showConfirm("⚠ Supprimer",
                "Supprimer le stream « " + s.getTitle() + " » ?\nToutes les donations seront supprimées.");
        if (!ok) return;
        try { streamSvc.delete(s.getId()); AlertUtil.showSuccess("✅ Supprimé", "Stream supprimé."); load(); }
        catch (SQLException e) { AlertUtil.showError("Erreur", e.getMessage()); }
    }

    @FXML private void onRefresh(ActionEvent e)   { load(); }
    @FXML private void onFront(ActionEvent e)     { loadWithController("/com/gamilha/interfaces/User/StreamList.fxml"); }
    @FXML private void onDonations(ActionEvent e) { loadWithController("/com/gamilha/interfaces/Admin/AdminDonationStreams.fxml"); }
    private <T> T loadWithController(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            // 🔥 récupérer contentArea depuis NavigationContext
            BorderPane contentArea = NavigationContext.getContentArea();

            if (contentArea == null) {
                throw new RuntimeException("contentArea null !");
            }

            contentArea.setCenter(root);

            return loader.getController();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
