package com.gamilha.controllers;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.PostService;
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
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Affiche uniquement les posts créés par l'utilisateur connecté.
 * FXML : UserMesPostsView.fxml
 */
public class UserMesPostsController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private FlowPane cardsPane;
    @FXML private Label    totalLabel;
    @FXML private TextField searchField;

    private final PostService postService = new PostService();
    private User currentUser;
    private List<Post> mesPosts;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter(nv));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadMesPosts();
    }

    private void loadMesPosts() {
        try {
            // Récupérer tous les posts et filtrer côté Java par user_id
            mesPosts = postService.findAll().stream()
                .filter(p -> p.getUser() != null && p.getUser().getId() == currentUser.getId())
                .collect(Collectors.toList());
            applyFilter(searchField.getText());
        } catch (SQLException e) {
            showAlert("Erreur BD : " + e.getMessage());
        }
    }

    private void applyFilter(String kw) {
        String k = kw == null ? "" : kw.toLowerCase().trim();
        List<Post> filtered = mesPosts.stream()
            .filter(p -> k.isEmpty() || p.getContent().toLowerCase().contains(k))
            .collect(Collectors.toList());

        totalLabel.setText(filtered.size() + " post(s)");
        cardsPane.getChildren().clear();
        filtered.forEach(p -> cardsPane.getChildren().add(buildCard(p)));
    }

    private VBox buildCard(Post post) {
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setStyle(
            "-fx-background-color:#1e1e2e;" +
            "-fx-background-radius:12;" +
            "-fx-border-radius:12;" +
            "-fx-border-color:#2d2d4a;" +
            "-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),8,0,0,3);");

        // Image
        Image img = loadImage(post.getImage());
        ImageView imgView = new ImageView(img);
        imgView.setFitWidth(300); imgView.setFitHeight(150);
        imgView.setPreserveRatio(false); imgView.setSmooth(true);
        Rectangle clip = new Rectangle(300, 150);
        clip.setArcWidth(24); clip.setArcHeight(24);
        imgView.setClip(clip);

        // Badge likes
        Label badge = new Label("❤ " + post.getLikesCount());
        badge.setStyle("-fx-background-color:rgba(0,0,0,0.65);-fx-text-fill:#ff6b81;" +
                       "-fx-background-radius:20;-fx-padding:3 8;-fx-font-size:11;");
        StackPane imgStack = new StackPane(imgView, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(8));

        // Corps
        VBox body = new VBox(6);
        body.setPadding(new Insets(10, 12, 10, 12));

        Label date = new Label(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-text-fill:#8b949e;-fx-font-size:10;");

        Label content = new Label(trunc(post.getContent(), 100));
        content.setWrapText(true);
        content.setStyle("-fx-text-fill:#c9d1d9;-fx-font-size:12;");

        // Actions
        HBox btns = new HBox(8);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button bEdit = btn("✏ Modifier", "#10b981");
        Button bDel  = btn("🗑 Supprimer", "#ef4444");
        bEdit.setOnAction(e -> openEditPost(post));
        bDel .setOnAction(e -> confirmDelete(post));
        btns.getChildren().addAll(bEdit, bDel);

        body.getChildren().addAll(date, content, btns);
        card.getChildren().addAll(imgStack, body);
        return card;
    }

    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/UserPostFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier mon post");
            s.setScene(new Scene(loader.load(), 530, 430));
            ((UserPostFormController) loader.getController()).init(post, currentUser);
            s.showAndWait();
            loadMesPosts();
        } catch (IOException e) { showAlert("Erreur : " + e.getMessage()); }
    }

    private void confirmDelete(Post post) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer ce post ?  Tous ses commentaires seront supprimés.",
            ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Confirmer la suppression");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { postService.delete(post.getId()); loadMesPosts(); }
                catch (SQLException e) { showAlert("Erreur BD : " + e.getMessage()); }
            }
        });
    }

    @FXML private void onNewPost() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/UserPostFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Nouveau Post");
            s.setScene(new Scene(loader.load(), 530, 430));
            ((UserPostFormController) loader.getController()).init(null, currentUser);
            s.showAndWait();
            loadMesPosts();
        } catch (IOException e) { showAlert("Erreur : " + e.getMessage()); }
    }

    @FXML private void onRefresh() { loadMesPosts(); }

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
