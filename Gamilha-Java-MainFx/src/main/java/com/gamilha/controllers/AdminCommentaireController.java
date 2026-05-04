package com.gamilha.controllers;

import com.gamilha.entity.Commentaire;
import com.gamilha.services.CommentaireService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminCommentaireController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane         cardsPane;
    @FXML private Label            totalLabel;

    private final CommentaireService commentaireService = new CommentaireService();
    private ObservableList<Commentaire> allComments = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.setItems(FXCollections.observableArrayList(
            "Plus récent", "Plus ancien", "Auteur A→Z", "Post ID ↑"));
        sortCombo.setValue("Plus récent");
        sortCombo.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        loadComments();
    }

    private void loadComments() {
        try {
            allComments.setAll(commentaireService.findAll());
            applyFilter();
        } catch (SQLException e) {
            alert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage());
        }
    }

    private void applyFilter() {
        String kw   = searchField.getText().toLowerCase().trim();
        String sort = sortCombo.getValue();

        List<Commentaire> filtered = allComments.stream()
            .filter(c -> kw.isEmpty()
                || c.getText().toLowerCase().contains(kw)
                || (c.getUser() != null && c.getUser().getName().toLowerCase().contains(kw)))
            .collect(Collectors.toList());

        switch (sort == null ? "" : sort) {
            case "Plus récent" -> filtered.sort(Comparator.comparing(Commentaire::getCreatedAt).reversed());
            case "Plus ancien" -> filtered.sort(Comparator.comparing(Commentaire::getCreatedAt));
            case "Auteur A→Z"  -> filtered.sort(Comparator.comparing(
                                      c -> c.getUser() != null ? c.getUser().getName() : ""));
            case "Post ID ↑"   -> filtered.sort(Comparator.comparingInt(
                                      c -> c.getPost() != null ? c.getPost().getId() : 0));
        }

        totalLabel.setText(filtered.size() + " commentaire(s)");
        cardsPane.getChildren().clear();
        filtered.forEach(c -> cardsPane.getChildren().add(buildCard(c)));
    }

    private VBox buildCard(Commentaire c) {
        VBox card = new VBox(8);
        card.setPrefWidth(335);
        card.setPadding(new Insets(12));
        card.setStyle(
            "-fx-background-color:#1e1e2e;" +
            "-fx-background-radius:10;" +
            "-fx-border-radius:10;" +
            "-fx-border-color:#3a3a5c;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.35),7,0,0,3);");

        // Auteur
        HBox authorRow = new HBox(8);
        authorRow.setAlignment(Pos.CENTER_LEFT);
        ImageView av = new ImageView(loadAvatar(c.getUser()));
        av.setFitWidth(34); av.setFitHeight(34);
        av.setClip(new Circle(17, 17, 17));

        VBox authorInfo = new VBox(2);
        Label name = new Label(c.getUser() != null ? c.getUser().getName() : "?");
        name.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:12;");
        Label date = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-text-fill:#8b949e;-fx-font-size:10;");
        authorInfo.getChildren().addAll(name, date);
        authorRow.getChildren().addAll(av, authorInfo);

        // Texte
        Label text = new Label(c.getText());
        text.setWrapText(true);
        text.setMaxWidth(311);
        text.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");

        // Référence post
        Label postRef = new Label("💬 Lié au Post #" +
            (c.getPost() != null ? c.getPost().getId() : "?"));
        postRef.setStyle("-fx-text-fill:#58a6ff;-fx-font-size:11;");

        // Actions
        HBox btns = new HBox(8);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button bEdit = btn("✏ Éditer", "#10b981");
        Button bDel  = btn("🗑 Suppr.", "#ef4444");
        bEdit.setOnAction(e -> openEditDialog(c));
        bDel .setOnAction(e -> confirmDelete(c));
        btns.getChildren().addAll(bEdit, bDel);

        card.getChildren().addAll(authorRow, text, postRef, btns);
        return card;
    }

    private void openEditDialog(Commentaire c) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/CommentaireFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Éditer Commentaire #" + c.getId());
            s.setScene(new Scene(loader.load(), 480, 270));
            ((CommentaireFormController) loader.getController()).init(c);
            s.showAndWait();
            loadComments();
        } catch (IOException e) { alert(Alert.AlertType.ERROR, "Erreur", e.getMessage()); }
    }

    private void confirmDelete(Commentaire c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le commentaire #" + c.getId() + " ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { commentaireService.delete(c.getId()); loadComments(); }
                catch (SQLException e) { alert(Alert.AlertType.ERROR, "Erreur BD", e.getMessage()); }
            }
        });
    }

    private Image loadAvatar(com.gamilha.entity.User user) {
        if (user != null && user.getProfileImage() != null) {
            File f = new File(AdminPostController.UPLOADS + user.getProfileImage());
            if (f.exists()) return new Image(f.toURI().toString());
        }
        var s = getClass().getResourceAsStream("/com/gamilha/images/default-avatar.png");
        return s != null ? new Image(s) : null;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                   "-fx-background-radius:6;-fx-font-size:11;-fx-padding:4 10;-fx-cursor:hand;");
        return b;
    }

    private void alert(Alert.AlertType t, String h, String m) {
        Alert a = new Alert(t); a.setHeaderText(h); a.setContentText(m); a.showAndWait();
    }

    @FXML private void onRefresh() { loadComments(); }
}
