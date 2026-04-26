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
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Vue Amis — statut en ligne réel + bouton Retirer un ami.
 *
 * Fonctionnalité avancée :
 *  - Statut en ligne via user_activity.last_seen (< 15 min) ou fallback is_active
 *  - Bouton "Retirer" sur chaque ami → DELETE FROM friend
 *  - Bouton "Ajouter" sur les suggestions
 */
public class UserAmisController implements Initializable {

    @FXML private VBox       friendsBox;
    @FXML private VBox       suggestionsBox;
    @FXML private Label      friendsCountLabel;
    @FXML private Label      suggestionsCountLabel;
    @FXML private TextField  searchFriendsField;

    private final FriendService friendService = new FriendService();
    private User       currentUser;
    private List<User> allFriends;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (searchFriendsField != null)
            searchFriendsField.textProperty().addListener((o, ov, nv) -> filterFriends(nv));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null) return;
        loadFriends();
        loadSuggestions();
    }

    // ── Amis avec statut en ligne réel ────────────────────────────────────
    private void loadFriends() {
        friendsBox.getChildren().clear();
        try {
            allFriends = friendService.findFriends(currentUser.getId());
            Map<Integer, Boolean> onlineMap = friendService.getOnlineStatus(allFriends);

            if (friendsCountLabel != null)
                friendsCountLabel.setText(allFriends.size() + " ami(s)");

            if (allFriends.isEmpty()) {
                friendsBox.getChildren().add(gray("Vous n'avez pas encore d'amis."));
            } else {
                for (User u : allFriends) {
                    boolean online = onlineMap.getOrDefault(u.getId(), false);
                    friendsBox.getChildren().add(buildFriendRow(u, online));
                }
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
        if (friendsCountLabel != null)
            friendsCountLabel.setText(filtered.size() + " ami(s)");
        if (filtered.isEmpty()) {
            friendsBox.getChildren().add(gray("Aucun résultat pour « " + kw + " »"));
        } else {
            Map<Integer, Boolean> onlineMap = friendService.getOnlineStatus(filtered);
            filtered.forEach(u -> friendsBox.getChildren().add(
                buildFriendRow(u, onlineMap.getOrDefault(u.getId(), false))));
        }
    }

    // ── Card ami : avatar + statut en ligne + bouton Retirer ──────────────
    private HBox buildFriendRow(User u, boolean online) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle(
            "-fx-background-color:#1a1e2e;-fx-background-radius:10;" +
            "-fx-border-radius:10;-fx-border-color:rgba(139,92,246,0.20);-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),5,0,0,2);");

        // Avatar + point de statut
        javafx.scene.layout.StackPane sp = new javafx.scene.layout.StackPane();
        Label av = av(u.getName(), 44);
        Label dot = new Label();
        dot.setMinSize(12,12); dot.setMaxSize(12,12);
        dot.setStyle(online
            ? "-fx-background-color:#22c55e;-fx-background-radius:6;" +
              "-fx-border-color:#1a1e2e;-fx-border-width:2;-fx-border-radius:6;"
            : "-fx-background-color:#6b7280;-fx-background-radius:6;" +
              "-fx-border-color:#1a1e2e;-fx-border-width:2;-fx-border-radius:6;");
        javafx.scene.layout.StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
        javafx.scene.layout.StackPane.setMargin(dot, new Insets(0,0,2,0));
        sp.getChildren().addAll(av, dot);

        // Infos
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nm = new Label(u.getName());
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e2e8f0;-fx-font-size:14;");
        Label em = new Label(u.getEmail());
        em.setStyle("-fx-text-fill:#64748b;-fx-font-size:12;");
        Label statusLbl = new Label(online ? "● En ligne" : "● Hors ligne");
        statusLbl.setStyle(online
            ? "-fx-text-fill:#22c55e;-fx-font-size:11;-fx-font-weight:bold;"
            : "-fx-text-fill:#6b7280;-fx-font-size:11;");
        info.getChildren().addAll(nm, em, statusLbl);

        // Bouton Retirer (icône + texte)
        Button btnRetirer = new Button("✖ Retirer");
        btnRetirer.setStyle(
            "-fx-background-color:rgba(239,68,68,0.15);-fx-text-fill:#f87171;" +
            "-fx-background-radius:20;-fx-padding:6 14;-fx-cursor:hand;-fx-font-size:12;" +
            "-fx-border-color:rgba(239,68,68,0.30);-fx-border-radius:20;-fx-border-width:1;");
        btnRetirer.setOnMouseEntered(e -> btnRetirer.setStyle(
            "-fx-background-color:rgba(239,68,68,0.30);-fx-text-fill:#fca5a5;" +
            "-fx-background-radius:20;-fx-padding:6 14;-fx-cursor:hand;-fx-font-size:12;" +
            "-fx-border-color:rgba(239,68,68,0.50);-fx-border-radius:20;-fx-border-width:1;"));
        btnRetirer.setOnMouseExited(e -> btnRetirer.setStyle(
            "-fx-background-color:rgba(239,68,68,0.15);-fx-text-fill:#f87171;" +
            "-fx-background-radius:20;-fx-padding:6 14;-fx-cursor:hand;-fx-font-size:12;" +
            "-fx-border-color:rgba(239,68,68,0.30);-fx-border-radius:20;-fx-border-width:1;"));

        btnRetirer.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Retirer " + u.getName() + " de vos amis ?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Retirer un ami");
            confirm.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    try {
                        friendService.removeFriend(currentUser.getId(), u.getId());
                        loadFriends();
                        loadSuggestions(); // remet dans suggestions
                    } catch (SQLException ex) {
                        new Alert(Alert.AlertType.ERROR, "Erreur : " + ex.getMessage()).show();
                    }
                }
            });
        });

        row.getChildren().addAll(sp, info, btnRetirer);
        return row;
    }

    // ── Suggestions ───────────────────────────────────────────────────────
    private void loadSuggestions() {
        if (suggestionsBox == null || currentUser == null) return;
        suggestionsBox.getChildren().clear();
        try {
            List<User> suggestions = friendService.findSuggestions(currentUser.getId(), 10);
            if (suggestionsCountLabel != null)
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
            "-fx-background-color:#13151f;-fx-background-radius:10;" +
            "-fx-border-radius:10;-fx-border-color:rgba(139,92,246,0.15);-fx-border-width:1;");

        Label av = av(u.getName(), 40);
        VBox info = new VBox(3); HBox.setHgrow(info, Priority.ALWAYS);
        Label nm = new Label(u.getName());
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:13;");
        Label sub = new Label("Nouveau joueur Gamilha");
        sub.setStyle("-fx-text-fill:#64748b;-fx-font-size:11;");
        info.getChildren().addAll(nm, sub);

        Button btnAdd = new Button("➕ Ajouter");
        btnAdd.setStyle(
            "-fx-background-color:linear-gradient(to right,#8b5cf6,#a855f7);-fx-text-fill:white;" +
            "-fx-background-radius:20;-fx-padding:6 16;-fx-cursor:hand;-fx-font-size:12;" +
            "-fx-font-weight:bold;");
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
        String init = (name != null && name.length() >= 2) ? name.substring(0,2).toUpperCase() : "??";
        Label l = new Label(init);
        l.setMinSize(sz,sz); l.setMaxSize(sz,sz); l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color:#5b21b6;-fx-text-fill:white;-fx-font-weight:bold;" +
                   "-fx-font-size:"+(sz*0.33)+";-fx-background-radius:"+(sz/2)+";");
        return l;
    }

    private Label gray(String msg) {
        Label l = new Label(msg); l.setStyle("-fx-text-fill:#64748b;-fx-font-size:13;"); return l;
    }
}
