package com.gamilha.controllers;

import com.gamilha.entity.Commentaire;
import com.gamilha.entity.User;
import com.gamilha.services.CommentaireService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Affiche tous les commentaires de l'utilisateur connecté.
 * FXML : UserMesCommentairesView.fxml
 */
public class UserMesCommentairesController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private VBox      listBox;
    @FXML private Label     totalLabel;
    @FXML private TextField searchField;

    private final CommentaireService commentaireService = new CommentaireService();
    private User currentUser;
    private List<Commentaire> mesCommentaires;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter(nv));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadMesCommentaires();
    }

    private void loadMesCommentaires() {
        try {
            mesCommentaires = commentaireService.findAll().stream()
                .filter(c -> c.getUser() != null && c.getUser().getId() == currentUser.getId())
                .collect(Collectors.toList());
            applyFilter(searchField.getText());
        } catch (SQLException e) {
            showAlert("Erreur BD : " + e.getMessage());
        }
    }

    private void applyFilter(String kw) {
        String k = kw == null ? "" : kw.toLowerCase().trim();
        List<Commentaire> filtered = mesCommentaires.stream()
            .filter(c -> k.isEmpty() || c.getText().toLowerCase().contains(k))
            .collect(Collectors.toList());

        totalLabel.setText(filtered.size() + " commentaire(s)");
        listBox.getChildren().clear();
        filtered.forEach(c -> listBox.getChildren().add(buildRow(c)));
    }

    private HBox buildRow(Commentaire c) {
        HBox row = new HBox(12);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle(
            "-fx-background-color:#1e1e2e;" +
            "-fx-background-radius:10;" +
            "-fx-border-radius:10;" +
            "-fx-border-color:#2d2d4a;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.3),5,0,0,2);");

        // Infos commentaire
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Référence au post
        Label postRef = new Label("💬 Post #" +
            (c.getPost() != null ? c.getPost().getId() : "?") +
            (c.getPost() != null && c.getPost().getContent() != null
                ? "  —  " + trunc(c.getPost().getContent(), 40) : ""));
        postRef.setStyle("-fx-text-fill:#58a6ff;-fx-font-size:11;");

        Label text = new Label(c.getText());
        text.setWrapText(true);
        text.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:13;");

        Label date = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-text-fill:#6e7681;-fx-font-size:10;");

        info.getChildren().addAll(postRef, text, date);

        // Actions
        VBox actions = new VBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button bEdit = btn("✏ Modifier", "#10b981");
        Button bDel  = btn("🗑 Supprimer", "#ef4444");
        bEdit.setOnAction(e -> openEditDialog(c));
        bDel .setOnAction(e -> confirmDelete(c));
        actions.getChildren().addAll(bEdit, bDel);

        row.getChildren().addAll(info, actions);
        return row;
    }

    private void openEditDialog(Commentaire c) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/UserCommentaireFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier mon commentaire");
            s.setScene(new Scene(loader.load(), 480, 270));
            ((UserCommentaireFormController) loader.getController()).init(c);
            s.showAndWait();
            loadMesCommentaires();
        } catch (IOException e) { showAlert("Erreur : " + e.getMessage()); }
    }

    private void confirmDelete(Commentaire c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { commentaireService.delete(c.getId()); loadMesCommentaires(); }
                catch (SQLException e) { showAlert("Erreur BD : " + e.getMessage()); }
            }
        });
    }

    @FXML private void onRefresh() { loadMesCommentaires(); }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                   "-fx-background-radius:6;-fx-font-size:11;-fx-padding:4 10;-fx-cursor:hand;");
        return b;
    }

    private String trunc(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
