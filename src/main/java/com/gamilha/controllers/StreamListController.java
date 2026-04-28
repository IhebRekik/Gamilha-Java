package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.Stream;
import com.gamilha.services.StreamService;
import com.gamilha.services.AblyService;
import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.NavigationContext;
import com.gamilha.utils.SessionContext;
import com.gamilha.utils.ToastUtil;
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
import java.util.*;
import java.util.stream.Collectors;

public class StreamListController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> sortFilter;
    @FXML private Label            countLabel;
    @FXML private FlowPane         streamGrid;

    private final StreamService          service   = new StreamService();
    private ObservableList<Stream>        allStreams = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gameFilter.setItems(FXCollections.observableArrayList(
                "Tous les jeux", "CS2", "Valorant", "LoL", "Dota2"));
        gameFilter.setValue("Tous les jeux");

        sortFilter.setItems(FXCollections.observableArrayList(
                "Plus récents", "Plus regardés"));
        sortFilter.setValue("Plus récents");

        searchField.textProperty().addListener((o, old, v) -> filter());
        gameFilter.valueProperty().addListener((o, old, v) -> filter());
        sortFilter.valueProperty().addListener((o, old, v) -> filter());

        load();

        // ── Ably : singleton — UNE seule connexion WebSocket ──────────
        try {
            var user = SessionContext.getCurrentUser();
            int myId = (user != null) ? user.getId() : -1;

            AblyService ably = AblyService.getInstance(); // ← SINGLETON obligatoire
            if (ably.isEnabled()) {
                ably.subscribeToNewStreams(myId, payload -> {
                    int streamerId = extractJsonInt(payload, "streamerId", -1);
                    if (streamerId == myId) return; // Ne pas notifier le streamer lui-même

                    String title    = extractJsonString(payload, "title");
                    String streamer = extractJsonString(payload, "streamerName");

                    // Toast non bloquant
                    ToastUtil.show("🔴 Nouveau stream", "📡 " + streamer + " a lancé « " + title + " »");
                    load(); // rafraîchir la grille
                });
            } else {
                System.out.println("[Ably] Désactivé — vérifier ably.key dans config.properties");
            }
        } catch (Exception ablyEx) {
            System.err.println("[Ably] subscribe error: " + ablyEx.getMessage());
        }
    }

    // ── Helper JSON minimaliste ───────────────────────────────────────
    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        int start = idx + search.length();
        int end   = json.indexOf('"', start);
        return end == -1 ? "" : json.substring(start, end);
    }

    private int extractJsonInt(String json, String key, int fallback) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(json);
        if (!m.find()) return fallback;
        try { return Integer.parseInt(m.group(1)); }
        catch (NumberFormatException ex) { return fallback; }
    }

    // ── Chargement BDD ────────────────────────────────────────────────
    private void load() {
        try {
            allStreams.setAll(service.findAll());
            filter();
        } catch (SQLException e) {
            AlertUtil.showError("Erreur BDD", e.getMessage());
        }
    }

    // ── Filtrage + tri ────────────────────────────────────────────────
    private void filter() {
        String  q         = searchField.getText().toLowerCase().trim();
        String  game      = gameFilter.getValue();
        boolean byViewers = "Plus regardés".equals(sortFilter.getValue());

        List<Stream> result = allStreams.stream()
                .filter(s -> q.isBlank()
                        || s.getTitle().toLowerCase().contains(q)
                        || (s.getDescription() != null
                        && s.getDescription().toLowerCase().contains(q)))
                .filter(s -> "Tous les jeux".equals(game)
                        || (s.getGame() != null && s.getGame().equalsIgnoreCase(game)))
                .sorted(byViewers
                        ? Comparator.comparingInt(Stream::getViewers).reversed()
                        : Comparator.comparing(Stream::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        countLabel.setText(result.size() + " stream(s)");
        buildGrid(result);
    }

    // ── Construction grille ───────────────────────────────────────────
    private void buildGrid(List<Stream> streams) {
        streamGrid.getChildren().clear();
        if (streams.isEmpty()) {
            Label lbl = new Label("📡 Aucun stream trouvé");
            lbl.setStyle("-fx-text-fill:#64748b;-fx-font-size:15px;");
            streamGrid.getChildren().add(lbl);
            return;
        }
        for (Stream s : streams)
            streamGrid.getChildren().add(buildCard(s));
    }

    private VBox buildCard(Stream stream) {
        VBox card = new VBox(0);
        card.setPrefWidth(310); card.setMaxWidth(310);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;");

        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(175);

        ImageView img = new ImageView();
        img.setFitWidth(310); img.setFitHeight(175); img.setPreserveRatio(false);
        Rectangle clip = new Rectangle(310, 175);
        clip.setArcWidth(14); clip.setArcHeight(14);
        img.setClip(clip);

        if (stream.getThumbnail() != null && !stream.getThumbnail().isBlank()) {
            try { img.setImage(new Image(stream.getThumbnail(), true)); }
            catch (Exception ignored) {}
        }

        Label badge = new Label(stream.getStatusBadge());
        badge.setStyle("live".equals(stream.getStatus())
                ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10;"
                : "-fx-background-color:#374151;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 10;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(8, 0, 0, 8));

        Label viewers = new Label("👁 " + stream.getViewers());
        viewers.setStyle("-fx-background-color:rgba(0,0,0,0.7);-fx-text-fill:#e2e8f0;-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 8;");
        StackPane.setAlignment(viewers, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(viewers, new Insets(0, 8, 8, 0));

        imgBox.getChildren().addAll(img, badge, viewers);

        VBox body = new VBox(5);
        body.setPadding(new Insets(12, 14, 4, 14));

        Label title = new Label(stream.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
        title.setWrapText(true);

        String descText = (stream.getDescription() != null && !stream.getDescription().isBlank())
                ? stream.getDescription() : "Live en cours";
        Label desc = new Label(descText);
        desc.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");
        desc.setWrapText(true); desc.setMaxHeight(34);

        Label game = new Label("🎮 " + stream.getGame());
        game.setStyle("-fx-font-size:12px;-fx-text-fill:#67e8f9;");

        body.getChildren().addAll(title, desc, game);

        HBox btns = new HBox(8);
        btns.setPadding(new Insets(8, 14, 14, 14));

        Button btnWatch = new Button("▶ Regarder");
        btnWatch.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;");
        btnWatch.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/gamilha/interfaces/User/StreamShow.fxml")
                );

                Parent root = loader.load();

                StreamShowController c = loader.getController();

                BorderPane contentArea = NavigationContext.getContentArea();
                contentArea.setCenter(root);

                if (c != null) c.setStream(stream);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button btnDonate = new Button("💰 Donner");
        btnDonate.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;");

        btnDonate.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/gamilha/interfaces/User/DonationForm.fxml")
                );

                Parent root = loader.load();

                DonationFormController c = loader.getController();

                // 🔥 injecter dans contentArea
                BorderPane contentArea = NavigationContext.getContentArea();

                if (contentArea == null) {
                    throw new RuntimeException("contentArea null !");
                }

                contentArea.setCenter(root);

                if (c != null) {
                    c.initCreate(stream);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        Button btnDelete = new Button("🗑");
        btnDelete.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 10;");
        btnDelete.setOnAction(e -> confirmDelete(stream));

        btns.getChildren().addAll(btnWatch, btnDonate, btnDelete);
        card.getChildren().addAll(imgBox, body, btns);

        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#1e1e30;-fx-border-color:#8b5cf6;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(139,92,246,0.3),16,0,0,4);"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;"));
        return card;
    }

    private void confirmDelete(Stream stream) {
        boolean ok = AlertUtil.showConfirm("⚠ Supprimer", "Supprimer « " + stream.getTitle() + " » ?\nCette action est irréversible.");
        if (!ok) return;
        try { service.delete(stream.getId()); AlertUtil.showSuccess("✅ Supprimé", "Stream supprimé."); load(); }
        catch (SQLException ex) { AlertUtil.showError("Erreur BDD", ex.getMessage()); }
    }

    @FXML
    private void onNewStream(ActionEvent e) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/StreamForm.fxml")
            );

            Parent root = loader.load();

            StreamFormController c = loader.getController();

            BorderPane contentArea = NavigationContext.getContentArea();
            contentArea.setCenter(root);

            if (c != null) c.initCreate();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }    @FXML private void onRefresh(ActionEvent e)   { searchField.clear(); gameFilter.setValue("Tous les jeux"); sortFilter.setValue("Plus récents"); load(); }
    @FXML private void onGoAdmin(ActionEvent e)   { MainApp.loadScene("Admin/AdminStreamList.fxml"); }
    @FXML private void onPrediction(ActionEvent e){ MainApp.loadScene("Admin/AdminStreamPrediction.fxml"); }
}