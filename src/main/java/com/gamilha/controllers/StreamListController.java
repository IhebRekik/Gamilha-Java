package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.services.AbonnementServices;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * StreamListController — liste des streams avec cards, recherche, filtre, tri, suppression.
 * Ably retiré — notifications désactivées tant que la clé n'est pas configurée.
 */
public class StreamListController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> gameFilter;
    @FXML private ComboBox<String> sortFilter;
    @FXML private Label            countLabel;
    @FXML private FlowPane         streamGrid;
    @FXML private Button btnNewStream;

    private final StreamService service = new StreamService();
    private ObservableList<Stream> allStreams = FXCollections.observableArrayList();

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
        boolean hasAccess = AbonnementServices.getAbonementActiveUser()
                .stream()
                .anyMatch(a ->
                        a.getOptions().stream()
                                .anyMatch(opt -> opt.contains("stream"))
                );

        btnNewStream.setDisable(!hasAccess);
        load();
    }

    private void load() {
        try { allStreams.setAll(service.findAll()); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur BDD", e.getMessage()); }
    }

    private void filter() {
        String q = searchField.getText().toLowerCase().trim();
        String game = gameFilter.getValue();
        boolean byViewers = "Plus regardés".equals(sortFilter.getValue());

        List<Stream> result = allStreams.stream()
                .filter(s -> q.isBlank()
                        || s.getTitle().toLowerCase().contains(q)
                        || (s.getDescription() != null && s.getDescription().toLowerCase().contains(q)))
                .filter(s -> "Tous les jeux".equals(game)
                        || (s.getGame() != null && s.getGame().equalsIgnoreCase(game)))
                .sorted(byViewers
                        ? Comparator.comparingInt(Stream::getViewers).reversed()
                        : Comparator.comparing(Stream::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        countLabel.setText(result.size() + " stream(s)");
        buildGrid(result);
    }

    private void buildGrid(List<Stream> streams) {
        streamGrid.getChildren().clear();
        if (streams.isEmpty()) {
            Label lbl = new Label("📡 Aucun stream trouvé");
            lbl.setStyle("-fx-text-fill:#64748b;-fx-font-size:16px;");
            streamGrid.getChildren().add(lbl);
            return;
        }
        for (Stream s : streams) streamGrid.getChildren().add(buildCard(s));
    }

    private VBox buildCard(Stream stream) {
        VBox card = new VBox(0);
        card.setPrefWidth(310); card.setMaxWidth(310);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;");

        // Thumbnail
        StackPane imgBox = new StackPane();
        imgBox.setPrefHeight(175); imgBox.setMaxHeight(175);
        ImageView img = new ImageView();
        img.setFitWidth(310); img.setFitHeight(175); img.setPreserveRatio(false);
        Rectangle clip = new Rectangle(310, 175); clip.setArcWidth(14); clip.setArcHeight(14);
        img.setClip(clip);
        if (stream.getThumbnail() != null && !stream.getThumbnail().isBlank()) {
            try { img.setImage(new Image(stream.getThumbnail(), true)); } catch (Exception ignored) {}
        }
        Label badge = new Label(stream.getStatusBadge());
        badge.setStyle("live".equals(stream.getStatus())
            ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:11px;-fx-font-weight:bold;-fx-background-radius:20;-fx-padding:3 10 3 10;"
            : "-fx-background-color:#374151;-fx-text-fill:white;-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 10 3 10;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(8, 0, 0, 8));
        Label viewers = new Label("👁 " + stream.getViewers());
        viewers.setStyle("-fx-background-color:rgba(0,0,0,0.7);-fx-text-fill:#e2e8f0;-fx-font-size:11px;-fx-background-radius:20;-fx-padding:3 8 3 8;");
        StackPane.setAlignment(viewers, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(viewers, new Insets(0, 8, 8, 0));
        imgBox.getChildren().addAll(img, badge, viewers);

        // Corps
        VBox body = new VBox(5); body.setPadding(new Insets(12, 14, 4, 14));
        Label title = new Label(stream.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;"); title.setWrapText(true);
        String descText = (stream.getDescription() != null && !stream.getDescription().isBlank()) ? stream.getDescription() : "Live en cours";
        Label desc = new Label(descText);
        desc.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;"); desc.setWrapText(true); desc.setMaxHeight(34);
        Label game = new Label("🎮 " + stream.getGame());
        game.setStyle("-fx-font-size:12px;-fx-text-fill:#67e8f9;");
        body.getChildren().addAll(title, desc, game);

        // Boutons
        HBox btns = new HBox(8); btns.setPadding(new Insets(8, 14, 14, 14));
        Button btnWatch = new Button("▶ Regarder");
        btnWatch.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14 6 14;");
        btnWatch.setOnAction(e -> { StreamShowController c = NavigationContext.navigateWithController("User/StreamShow.fxml"); if (c != null) c.setStream(stream); });
        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14 6 14;");
        btnDelete.setOnAction(e -> confirmDelete(stream));
        btns.getChildren().addAll(btnWatch, btnDelete);

        card.getChildren().addAll(imgBox, body, btns);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#1e1e30;-fx-border-color:#8b5cf6;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;-fx-effect:dropshadow(gaussian,rgba(139,92,246,0.3),16,0,0,4);"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;"));
        return card;
    }

    private void confirmDelete(Stream stream) {
        boolean ok = AlertUtil.showConfirm("⚠ Supprimer le stream",
                "Supprimer « " + stream.getTitle() + " » ?\n\nToutes les donations associées seront supprimées.\nCette action est irréversible.");
        if (!ok) return;
        try { service.delete(stream.getId()); AlertUtil.showSuccess("✅ Supprimé", "Stream supprimé avec succès."); load(); }
        catch (SQLException ex) { AlertUtil.showError("Erreur BDD", ex.getMessage()); }
    }

    @FXML private void onNewStream(ActionEvent e) { StreamFormController c = NavigationContext.navigateWithController("User/StreamForm.fxml"); if (c != null) c.initCreate(); }
    @FXML private void onRefresh(ActionEvent e)   { searchField.clear(); gameFilter.setValue("Tous les jeux"); sortFilter.setValue("Plus récents"); load(); }
    @FXML private void onGoAdmin(ActionEvent e)   { MainApp.loadScene("Admin/AdminStreamList.fxml"); }
}
