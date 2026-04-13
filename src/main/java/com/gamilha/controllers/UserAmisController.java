package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.services.FriendService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Vue "Amis" côté utilisateur.
 * Affiche : liste de ses amis + suggestions de nouveaux amis.
 * FXML : UserAmisView.fxml
 */
public class UserAmisController implements Initializable {

    @FXML private VBox   friendsBox;
    @FXML private VBox   suggestionsBox;
    @FXML private Label  friendsCountLabel;
    @FXML private Label  suggestionsCountLabel;
    @FXML private TextField searchFriendsField;

    private final FriendService friendService = new FriendService();
    private User currentUser;
    private List<User> allFriends;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (searchFriendsField != null)
            searchFriendsField.textProperty().addListener((o, ov, nv) -> filterFriends(nv));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadFriends();
        loadSuggestions();
    }

    // ── Amis ─────────────────────────────────────────────────────────────
    private void loadFriends() {
        friendsBox.getChildren().clear();
        try {
            allFriends = friendService.findFriends(currentUser.getId());
            friendsCountLabel.setText(allFriends.size() + " ami(s)");
            if (allFriends.isEmpty()) {
                friendsBox.getChildren().add(gray("Vous n'avez pas encore d'amis."));
            } else {
                for (User u : allFriends)
                    friendsBox.getChildren().add(buildFriendRow(u));
            }
        } catch (SQLException e) {
            friendsBox.getChildren().add(gray("Erreur : " + e.getMessage()));
        }
    }

    private void filterFriends(String kw) {
        if (allFriends == null) return;
        friendsBox.getChildren().clear();
        String k = kw == null ? "" : kw.toLowerCase().trim();
        List<User> filtered = allFriends.stream()
            .filter(u -> k.isEmpty() || u.getName().toLowerCase().contains(k)
                      || u.getEmail().toLowerCase().contains(k))
            .collect(Collectors.toList());
        friendsCountLabel.setText(filtered.size() + " ami(s)");
        if (filtered.isEmpty())
            friendsBox.getChildren().add(gray("Aucun résultat pour « " + kw + " »"));
        else
            filtered.forEach(u -> friendsBox.getChildren().add(buildFriendRow(u)));
    }

    private HBox buildFriendRow(User u) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle(
            "-fx-background-color:#1c1c2e;-fx-background-radius:10;" +
            "-fx-border-radius:10;-fx-border-color:#2a2b4a;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),5,0,0,2);");

        Label av = av(u.getName(), 44);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nm = new Label(u.getName());
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e6edf3;-fx-font-size:14;");
        Label em = new Label(u.getEmail());
        em.setStyle("-fx-text-fill:#8b949e;-fx-font-size:12;");

        // Statut en ligne (fictif pour l'instant)
        Label status = new Label("\uD83D\uDFE2  En ligne");
        status.setStyle("-fx-text-fill:#4caf50;-fx-font-size:11;");

        info.getChildren().addAll(nm, em, status);
        row.getChildren().addAll(av, info);
        return row;
    }

    // ── Suggestions ───────────────────────────────────────────────────────
    private void loadSuggestions() {
        suggestionsBox.getChildren().clear();
        try {
            List<User> suggestions = friendService.findSuggestions(currentUser.getId(), 10);
            suggestionsCountLabel.setText(suggestions.size() + " suggestion(s)");
            if (suggestions.isEmpty()) {
                suggestionsBox.getChildren().add(gray("Aucune suggestion pour le moment."));
                return;
            }
            for (User u : suggestions)
                suggestionsBox.getChildren().add(buildSuggestionRow(u));
        } catch (SQLException e) {
            suggestionsBox.getChildren().add(gray("Erreur : " + e.getMessage()));
        }
    }

    private HBox buildSuggestionRow(User u) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle(
            "-fx-background-color:#16162a;-fx-background-radius:10;" +
            "-fx-border-radius:10;-fx-border-color:#2a2b4a;-fx-border-width:1;");

        Label av = av(u.getName(), 40);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nm = new Label(u.getName());
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:13;");
        Label sub = new Label("Pas de jeu commun");
        sub.setStyle("-fx-text-fill:#4caf50;-fx-font-size:11;");
        info.getChildren().addAll(nm, sub);

        Button btnAdd = new Button("➕  Ajouter");
        btnAdd.setStyle(
            "-fx-background-color:#3b82f6;-fx-text-fill:white;" +
            "-fx-background-radius:20;-fx-padding:6 16;-fx-cursor:hand;-fx-font-size:12;");
        btnAdd.setOnAction(e -> {
            try {
                friendService.addFriend(currentUser.getId(), u.getId());
                loadFriends();
                loadSuggestions();
            } catch (SQLException ex) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).show();
            }
        });

        row.getChildren().addAll(av, info, btnAdd);
        return row;
    }

    @FXML void onRefresh() { loadFriends(); loadSuggestions(); }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Label av(String name, double sz) {
        String init = (name != null && name.length() >= 2)
            ? name.substring(0, 2).toUpperCase() : "??";
        Label l = new Label(init);
        l.setMinSize(sz, sz); l.setMaxSize(sz, sz);
        l.setAlignment(Pos.CENTER);
        l.setStyle(
            "-fx-background-color:#5b21b6;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-font-size:" + (sz * 0.33) + ";-fx-background-radius:" + (sz / 2) + ";");
        return l;
    }

    private Label gray(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill:#8b949e;-fx-font-size:13;");
        return l;
    }
}
