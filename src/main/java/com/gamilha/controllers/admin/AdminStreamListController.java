package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.utils.NavigationContext;
import com.gamilha.entity.Stream;
import com.gamilha.services.StreamService;
import com.gamilha.utils.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

    private final StreamService service = new StreamService();
    private ObservableList<Stream> all  = FXCollections.observableArrayList();

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
                "Tous", "+50", "+100", "+500"));
        viewersFilter.setValue("Tous");

        searchField.textProperty().addListener((o, ov, v) -> filter());
        gameFilter.valueProperty().addListener((o, ov, v)  -> filter());
        sortFilter.valueProperty().addListener((o, ov, v)  -> filter());
        viewersFilter.valueProperty().addListener((o, ov, v) -> filter());

        load();
    }

    private void load() {
        try { all.setAll(service.findAll()); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur BDD", e.getMessage()); }
    }

    private void filter() {
        String q    = searchField.getText().toLowerCase().trim();
        String game = gameFilter.getValue();
        String sort = sortFilter.getValue();
        String vf   = viewersFilter.getValue();
        int minV    = switch (vf) { case "+50" -> 50; case "+100" -> 100; case "+500" -> 500; default -> 0; };

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

        for (int i = 0; i < COLS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / COLS);
            streamGrid.getColumnConstraints().add(cc);
        }

        if (streams.isEmpty()) {
            Label lbl = new Label("📡 Aucun stream trouvé");
            lbl.setStyle("-fx-text-fill:#64748b;-fx-font-size:16px;");
            streamGrid.add(lbl, 0, 0);
            return;
        }
        for (int i = 0; i < streams.size(); i++) {
            streamGrid.add(buildCard(streams.get(i)), i % COLS, i / COLS);
        }
    }

    private VBox buildCard(Stream s) {
        VBox card = new VBox(0);
        card.setPrefWidth(380); card.setMaxWidth(380);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;" +
                "-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;");

        // Thumbnail avec badges
        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(180); imgBox.setMaxHeight(180);
        ImageView img = new ImageView();
        img.setFitWidth(380); img.setFitHeight(180); img.setPreserveRatio(false);
        Rectangle clip = new Rectangle(380, 180); clip.setArcWidth(14); clip.setArcHeight(14);
        img.setClip(clip);
        if (s.getThumbnail() != null && !s.getThumbnail().isBlank()) {
            try { img.setImage(new Image(s.getThumbnail(), true)); } catch (Exception ignored) {}
        }

        Label badge = new Label(s.getStatusBadge());
        badge.setStyle("live".equals(s.getStatus())
                ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10;"
                : "-fx-background-color:#374151;-fx-text-fill:#94a3b8;-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 10;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(8, 0, 0, 8));

        Label viewers = new Label("👁 " + s.getViewers());
        viewers.setStyle("-fx-background-color:rgba(0,0,0,0.75);-fx-text-fill:#e2e8f0;" +
                "-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 8;");
        StackPane.setAlignment(viewers, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(viewers, new Insets(0, 8, 8, 0));

        // Badge admin
        Label adminBadge = new Label("⚙ ADMIN");
        adminBadge.setStyle("-fx-background-color:rgba(139,92,246,0.85);-fx-text-fill:white;" +
                "-fx-font-size:10px;-fx-background-radius:20;-fx-padding:2 8;");
        StackPane.setAlignment(adminBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(adminBadge, new Insets(8, 8, 0, 0));

        imgBox.getChildren().addAll(img, badge, viewers, adminBadge);

        // Corps
        VBox body = new VBox(6);
        body.setPadding(new Insets(12, 14, 4, 14));

        Label title = new Label(s.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
        title.setWrapText(true);

        HBox meta = new HBox(10);
        Label game = new Label("🎮 " + s.getGame());
        game.setStyle("-fx-font-size:12px;-fx-text-fill:#67e8f9;");
        Label date = new Label(s.getCreatedAt() != null ? "📅 " + s.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-font-size:11px;-fx-text-fill:#475569;");
        meta.getChildren().addAll(game, date);

        if (s.getApiVideoId() != null && !s.getApiVideoId().isBlank()) {
            Label apiLbl = new Label("🔴 api.video : " + s.getApiVideoId());
            apiLbl.setStyle("-fx-font-size:11px;-fx-text-fill:#4ade80;-fx-font-family:'Consolas','Courier New',monospace;");
            apiLbl.setWrapText(true);
            body.getChildren().addAll(title, meta, apiLbl);
        } else {
            body.getChildren().addAll(title, meta);
        }

        // Boutons admin
        HBox btns = new HBox(8);
        btns.setPadding(new Insets(8, 14, 14, 14));

        Button btnView = new Button("👁 Voir");
        btnView.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;");
        btnView.setOnAction(e -> {
            AdminStreamShowController c = MainApp.loadSceneWithController("Admin/AdminStreamShow.fxml");
            if (c != null) c.setStream(s);
        });

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.setStyle("-fx-background-color:#d97706;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;");
        btnEdit.setOnAction(e -> {
            AdminStreamFormController c = MainApp.loadSceneWithController("Admin/AdminStreamForm.fxml");
            if (c != null) c.initEdit(s);
        });

        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 12;");
        btnDel.setOnAction(e -> confirmDelete(s));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        btns.getChildren().addAll(btnView, btnEdit, sp, btnDel);

        card.getChildren().addAll(imgBox, body, btns);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#1e1e30;" +
                "-fx-border-color:#8b5cf6;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;" +
                "-fx-effect:dropshadow(gaussian,rgba(139,92,246,0.3),16,0,0,4);"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color:#1a1a26;" +
                "-fx-border-color:#2a2a40;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;"));
        return card;
    }

    private void confirmDelete(Stream s) {
        boolean ok = AlertUtil.showConfirm("⚠ Supprimer le stream",
                "Supprimer « " + s.getTitle() + " » ?\nToutes les donations seront supprimées.\nCette action est irréversible.");
        if (!ok) return;
        try { service.delete(s.getId()); AlertUtil.showSuccess("✅ Supprimé", "Stream supprimé."); load(); }
        catch (SQLException ex) { AlertUtil.showError("Erreur BDD", ex.getMessage()); }
    }

    @FXML private void onRefresh(ActionEvent e)   { searchField.clear(); gameFilter.setValue("Tous les jeux"); sortFilter.setValue("Plus récents"); viewersFilter.setValue("Tous"); load(); }
    @FXML private void onFront(ActionEvent e)      { NavigationContext.navigate("User/StreamList.fxml"); }
    @FXML private void onDonations(ActionEvent e)  { MainApp.loadScene("Admin/AdminDonationStreams.fxml"); }
}
