package com.gamilha.controller.admin;

import com.gamilha.MainApp;
import com.gamilha.entity.Stream;
import com.gamilha.service.StreamService;
import com.gamilha.util.AlertUtil;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * AdminStreamListController
 * - ADMIN : Read / Update / Delete UNIQUEMENT (pas de Create)
 * - GridPane pour les cards (exigence prof)
 * - Pas d'ID affiché
 */
public class AdminStreamListController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> gameFilter, sortFilter, viewersFilter;
    @FXML private Label            countLabel;
    @FXML private GridPane         streamGrid;

    private final StreamService service = new StreamService();
    private ObservableList<Stream> all = FXCollections.observableArrayList();
    private static final int COLS = 3;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gameFilter.setItems(FXCollections.observableArrayList("Tous les jeux","CS2","Valorant","LoL","Dota2"));
        gameFilter.setValue("Tous les jeux");
        sortFilter.setItems(FXCollections.observableArrayList("Plus récents","Plus regardés","Moins regardés","Titre A->Z"));
        sortFilter.setValue("Plus récents");
        viewersFilter.setItems(FXCollections.observableArrayList("Tous","+50","+100","+500","+1000","+5000"));
        viewersFilter.setValue("Tous");
        streamGrid.getColumnConstraints().clear();
        for (int i = 0; i < COLS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / COLS);
            cc.setHgrow(Priority.ALWAYS);
            streamGrid.getColumnConstraints().add(cc);
        }
        streamGrid.setHgap(16); streamGrid.setVgap(16);
        searchField.textProperty().addListener((o,ov,v) -> filter());
        gameFilter.valueProperty().addListener((o,ov,v)    -> filter());
        sortFilter.valueProperty().addListener((o,ov,v)    -> filter());
        viewersFilter.valueProperty().addListener((o,ov,v) -> filter());
        load();
    }

    private void load() {
        try { all.setAll(service.findAll()); filter(); }
        catch (SQLException e) { AlertUtil.showError("Erreur BDD", e.getMessage()); }
    }

    private void filter() {
        String q = searchField.getText().toLowerCase().trim();
        String game = gameFilter.getValue();
        String sort = sortFilter.getValue();
        int minView = parseMin(viewersFilter.getValue());
        List<Stream> res = all.stream()
            .filter(s -> q.isBlank() || s.getTitle().toLowerCase().contains(q)
                || (s.getUrl()!=null && s.getUrl().toLowerCase().contains(q)))
            .filter(s -> "Tous les jeux".equals(game)
                || (s.getGame()!=null && s.getGame().equalsIgnoreCase(game)))
            .filter(s -> s.getViewers() >= minView)
            .sorted(switch(sort) {
                case "Plus regardés"  -> Comparator.comparingInt(Stream::getViewers).reversed();
                case "Moins regardés" -> Comparator.comparingInt(Stream::getViewers);
                case "Titre A->Z"     -> Comparator.comparing(Stream::getTitle, String.CASE_INSENSITIVE_ORDER);
                default               -> Comparator.comparing(Stream::getCreatedAt,Comparator.nullsLast(Comparator.reverseOrder()));
            }).collect(Collectors.toList());
        countLabel.setText(res.size() + " stream(s)");
        buildGridPane(res);
    }

    private int parseMin(String v) {
        if (v==null || "Tous".equals(v)) return 0;
        try { return Integer.parseInt(v.replace("+","").replace(" ","")); } catch(Exception e){ return 0; }
    }

    private void buildGridPane(List<Stream> streams) {
        streamGrid.getChildren().clear();
        if (streams.isEmpty()) {
            Label e = new Label("Aucun stream");
            e.setStyle("-fx-text-fill:#64748b;-fx-font-size:15px;");
            streamGrid.add(e, 0, 0, COLS, 1); return;
        }
        for (int i = 0; i < streams.size(); i++) {
            streamGrid.add(buildCard(streams.get(i)), i % COLS, i / COLS);
        }
    }

    private VBox buildCard(Stream s) {
        VBox card = new VBox(8);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:16;");
        HBox header = new HBox(8); header.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(s.getStatusBadge());
        badge.setStyle("live".equals(s.getStatus())
            ? "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:2 8 2 8;-fx-font-size:10px;"
            : "-fx-background-color:#374151;-fx-text-fill:white;-fx-background-radius:20;-fx-padding:2 8 2 8;-fx-font-size:10px;");
        Label title = new Label(s.getTitle());
        title.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
        title.setWrapText(true); HBox.setHgrow(title,Priority.ALWAYS);
        header.getChildren().addAll(badge, title);
        Label game    = new Label("🎮 " + s.getGame()); game.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12px;");
        Label viewers = new Label("👁 " + s.getViewers() + " spectateurs"); viewers.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12px;");
        Region sep = new Region(); sep.setPrefHeight(1); sep.setStyle("-fx-background-color:#2a2a40;");
        HBox btns = new HBox(6);
        Button bView = new Button("👁 Voir");    bView.setStyle("-fx-background-color:#0e7490;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 10 5 10;");
        Button bEdit = new Button("✏ Modifier"); bEdit.setStyle("-fx-background-color:#92400e;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 10 5 10;");
        Button bDon  = new Button("💰");         bDon.setStyle("-fx-background-color:#14532d;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 10 5 10;");
        Button bDel  = new Button("🗑");         bDel.setStyle("-fx-background-color:#7f1d1d;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:5 10 5 10;");
        bView.setOnAction(e -> { AdminStreamShowController c=MainApp.loadSceneWithController("admin/AdminStreamShow.fxml"); if(c!=null) c.setStream(s); });
        bEdit.setOnAction(e -> { AdminStreamFormController c=MainApp.loadSceneWithController("admin/AdminStreamForm.fxml");  if(c!=null) c.initEdit(s); });
        bDon.setOnAction(e  -> { AdminDonationListController c=MainApp.loadSceneWithController("admin/AdminDonationList.fxml"); if(c!=null) c.setStream(s); });
        bDel.setOnAction(e  -> confirmDelete(s));
        btns.getChildren().addAll(bView,bEdit,bDon,bDel);
        card.getChildren().addAll(header,game,viewers,sep,btns);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color:#1e1e30;-fx-border-color:#8b5cf6;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:16;"));
        card.setOnMouseExited(e  -> card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;-fx-border-radius:12;-fx-background-radius:12;-fx-padding:16;"));
        return card;
    }

    private void confirmDelete(Stream s) {
        if (AlertUtil.showConfirm("Confirmer la suppression",
            "Supprimer « "+s.getTitle()+" » ?\nToutes ses donations seront supprimées.")) {
            try { service.delete(s.getId()); AlertUtil.showSuccess("Supprimé","Stream supprimé."); load(); }
            catch (SQLException ex) { AlertUtil.showError("Erreur BDD",ex.getMessage()); }
        }
    }

    // Pas de onNew() — l'admin ne peut pas créer de stream
    @FXML private void onRefresh(ActionEvent e)   { searchField.clear(); gameFilter.setValue("Tous les jeux"); sortFilter.setValue("Plus récents"); viewersFilter.setValue("Tous"); load(); }
    @FXML private void onFront(ActionEvent e)     { MainApp.loadScene("StreamList.fxml"); }
    @FXML private void onDonations(ActionEvent e) { MainApp.loadScene("admin/AdminDonationStreams.fxml"); }
}
