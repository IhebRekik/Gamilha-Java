package com.gamilha.controllers;

import com.gamilha.entity.Commentaire;
import com.gamilha.entity.Post;
import com.gamilha.services.CommentaireService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class PostDetailController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ImageView postImage;
    @FXML private Label     authorLabel;
    @FXML private Label     dateLabel;
    @FXML private Label     likesLabel;
    @FXML private TextArea  contentArea;
    @FXML private VBox      commentsBox;
    @FXML private Label     commentsCountLabel;

    private final CommentaireService commentaireService = new CommentaireService();
    private Post post;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setPost(Post p) {
        this.post = p;
        displayPost();
        loadComments();
    }

    private void displayPost() {
        Image img = loadImage(post.getImage());
        if (img != null) postImage.setImage(img);
        postImage.setFitWidth(580); postImage.setFitHeight(230);
        postImage.setPreserveRatio(false);

        authorLabel.setText("✍  " + (post.getUser() != null
            ? post.getUser().getName() + "  ·  " + post.getUser().getEmail() : "Inconnu"));
        dateLabel.setText("📅  " + (post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : ""));
        likesLabel.setText("❤  " + post.getLikesCount() + " like(s)");
        contentArea.setText(post.getContent());
        contentArea.setEditable(false);
        contentArea.setWrapText(true);
    }

    private void loadComments() {
        commentsBox.getChildren().clear();
        try {
            List<Commentaire> list = commentaireService.findByPost(post.getId());
            commentsCountLabel.setText(list.size() + " commentaire(s)");
            list.forEach(c -> commentsBox.getChildren().add(buildCommentRow(c)));
        } catch (SQLException e) {
            commentsBox.getChildren().add(new Label("Erreur : " + e.getMessage()));
        }
    }

    private HBox buildCommentRow(Commentaire c) {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-background-color:#252535;-fx-background-radius:8;");

        // Avatar
        ImageView av = new ImageView(loadAvatar(c.getUser()));
        av.setFitWidth(34); av.setFitHeight(34);
        av.setClip(new Circle(17, 17, 17));

        // Corps du commentaire
        VBox body = new VBox(3);
        HBox.setHgrow(body, Priority.ALWAYS);

        HBox meta = new HBox(8);
        Label name = new Label(c.getUser() != null ? c.getUser().getName() : "?");
        name.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:12;");
        Label date = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-text-fill:#8b949e;-fx-font-size:10;");
        meta.getChildren().addAll(name, date);

        Label text = new Label(c.getText());
        text.setWrapText(true);
        text.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");
        body.getChildren().addAll(meta, text);

        // Bouton supprimer
        Button del = new Button("🗑");
        del.setStyle("-fx-background-color:transparent;-fx-text-fill:#ef4444;" +
                     "-fx-font-size:14;-fx-cursor:hand;");
        del.setOnAction(e -> deleteComment(c));

        row.getChildren().addAll(av, body, del);
        return row;
    }

    private void deleteComment(Commentaire c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { commentaireService.delete(c.getId()); loadComments(); }
                catch (SQLException ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).show(); }
            }
        });
    }

    // ── helpers image ────────────────────────────────────────────────────
    private Image loadImage(String filename) {
        if (filename != null && !filename.isBlank()) {
            File f1 = new File(AdminPostController.UPLOADS + filename);
            if (f1.exists()) return new Image(f1.toURI().toString());
            File f2 = new File(AdminPostController.UPLOADS + "images/" + filename);
            if (f2.exists()) return new Image(f2.toURI().toString());
        }
        var s = getClass().getResourceAsStream("/com/gamilha/images/placeholder.png");
        return s != null ? new Image(s) : null;
    }

    private Image loadAvatar(com.gamilha.entity.User user) {
        if (user != null && user.getProfileImage() != null) {
            File f = new File(AdminPostController.UPLOADS + user.getProfileImage());
            if (f.exists()) return new Image(f.toURI().toString());
        }
        var s = getClass().getResourceAsStream("/com/gamilha/images/default-avatar.png");
        return s != null ? new Image(s) : null;
    }
}
