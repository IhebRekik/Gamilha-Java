package com.gamilha.controllers;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.PostService;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.stage.Window;
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

public class UserMesPostsController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Chemin FXML correct
    private static final String FORM_FXML =
        "/com/gamilha/interfaces/User/UserPostFormView.fxml";

    @FXML private ScrollPane mesPostsScroll;
    @FXML private FlowPane  cardsPane;
    @FXML private Label     totalLabel;
    @FXML private TextField searchField;

    private final PostService postService = new PostService();
    private User       currentUser;
    private List<Post> mesPosts;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (mesPostsScroll != null) {
            mesPostsScroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getWindow() != null) {
                    mesPostsScroll.prefHeightProperty().bind(
                        newScene.getWindow().heightProperty().subtract(146));
                } else if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                        if (newWin != null)
                            mesPostsScroll.prefHeightProperty().bind(
                                newWin.heightProperty().subtract(146));
                    });
                }
            });
        }

        if (searchField != null)
            searchField.textProperty().addListener((o, ov, nv) -> applyFilter(nv));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) loadMesPosts();
    }

    private void loadMesPosts() {
        try {
            mesPosts = postService.findAll().stream()
                .filter(p -> p.getUser() != null && p.getUser().getId() == currentUser.getId())
                .collect(Collectors.toList());
            applyFilter(searchField != null ? searchField.getText() : "");
        } catch (SQLException e) { alert("Erreur BD : " + e.getMessage()); }
    }

    private void applyFilter(String kw) {
        String k = kw == null ? "" : kw.toLowerCase().trim();
        List<Post> filtered = mesPosts.stream()
            .filter(p -> k.isEmpty() ||
                PostService.stripStylePrefix(p.getContent()).toLowerCase().contains(k))
            .collect(Collectors.toList());
        if (totalLabel != null) totalLabel.setText(filtered.size() + " post(s)");
        cardsPane.getChildren().clear();
        filtered.forEach(p -> cardsPane.getChildren().add(buildCard(p)));
    }

    private VBox buildCard(Post post) {
        VBox card = new VBox(0);
        card.setPrefWidth(310);
        card.setStyle(
            "-fx-background-color:#0d0d1a;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:#2a2a40;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),8,0,0,3);");

        // Image (première si plusieurs)
        String firstImg = post.getFirstImage();
        if (firstImg != null) {
            Image img = loadImg(firstImg);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(310); iv.setFitHeight(160);
                iv.setPreserveRatio(false); iv.setSmooth(true);
                Rectangle clip = new Rectangle(310, 160);
                clip.setArcWidth(22); clip.setArcHeight(22);
                iv.setClip(clip);

                // Badge nb images si > 1
                StackPane stack = new StackPane(iv);
                int nbImgs = post.getAllImages().size();
                if (nbImgs > 1) {
                    Label imgBadge = new Label("📸 ×" + nbImgs);
                    imgBadge.setStyle(
                        "-fx-background-color:rgba(0,0,0,0.6);-fx-text-fill:white;" +
                        "-fx-background-radius:10;-fx-padding:2 7;-fx-font-size:10;");
                    StackPane.setAlignment(imgBadge, Pos.BOTTOM_LEFT);
                    StackPane.setMargin(imgBadge, new Insets(0, 0, 6, 6));
                    stack.getChildren().add(imgBadge);
                }
                // Badge likes
                Label likeBadge = new Label("❤ " + post.getLikesCount());
                likeBadge.setStyle(
                    "-fx-background-color:rgba(0,0,0,0.6);-fx-text-fill:#f43f5e;" +
                    "-fx-background-radius:10;-fx-padding:2 7;-fx-font-size:10;");
                StackPane.setAlignment(likeBadge, Pos.TOP_RIGHT);
                StackPane.setMargin(likeBadge, new Insets(6, 6, 0, 0));
                stack.getChildren().add(likeBadge);

                card.getChildren().add(stack);
            }
        }

        // Corps
        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 14, 12, 14));

        // Style badge
        String style = PostService.extractStyle(post.getContent());
        if (!style.equals("Normal")) {
            Label styleBadge = new Label(switch (style) {
                case "Gras"     -> "B Gras";
                case "Italique" -> "I Italique";
                case "Code"     -> "</> Code";
                case "Citation" -> "❝ Citation";
                default         -> style;
            });
            styleBadge.setStyle(
                "-fx-background-color:rgba(139,92,246,0.15);-fx-text-fill:#a78bfa;" +
                "-fx-background-radius:8;-fx-padding:2 7;-fx-font-size:10;-fx-font-weight:bold;");
            body.getChildren().add(styleBadge);
        }

        Label date = new Label(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "");
        date.setStyle("-fx-text-fill:#475569;-fx-font-size:10;");

        // Contenu nettoyé du préfixe style
        String cleanText = PostService.stripStylePrefix(post.getContent())
            .replaceAll("<[^>]+>", "").trim();
        Label content = new Label(cleanText.length() > 120 ? cleanText.substring(0, 120) + "…" : cleanText);
        content.setWrapText(true);
        content.setStyle("-fx-text-fill:#c9d1d9;-fx-font-size:12;-fx-line-spacing:2;");

        // Boutons action
        HBox btns = new HBox(8);
        btns.setAlignment(Pos.CENTER_RIGHT);
        Button bEdit = btn("✏ Modifier", "#10b981");
        Button bDel  = btn("🗑 Supprimer", "#ef4444");
        bEdit.setOnAction(e -> openEditPost(post));
        bDel .setOnAction(e -> confirmDelete(post));
        btns.getChildren().addAll(bEdit, bDel);

        body.getChildren().addAll(date, content, btns);
        card.getChildren().add(body);
        return card;
    }

    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FORM_FXML));
            if (loader.getLocation() == null)
                throw new IOException("FXML introuvable : " + FORM_FXML);
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier mon post");
            s.setScene(new Scene(loader.load(), 550, 530));
            ((UserPostFormController) loader.getController()).init(post, currentUser);
            s.showAndWait();
            loadMesPosts();
        } catch (IOException e) { alert("Erreur chargement formulaire : " + e.getMessage()); }
    }

    private void confirmDelete(Post post) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer ce post et tous ses commentaires ?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Confirmer la suppression");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { postService.delete(post.getId()); loadMesPosts(); }
                catch (SQLException e) { alert("Erreur BD : " + e.getMessage()); }
            }
        });
    }

    @FXML
    private void onNewPost() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FORM_FXML));
            if (loader.getLocation() == null)
                throw new IOException("FXML introuvable : " + FORM_FXML);
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Nouveau Post");
            s.setScene(new Scene(loader.load(), 550, 530));
            ((UserPostFormController) loader.getController()).init(null, currentUser);
            s.showAndWait();
            loadMesPosts();
        } catch (IOException e) { alert("Erreur chargement formulaire : " + e.getMessage()); }
    }

    @FXML private void onRefresh() { loadMesPosts(); }

    private Image loadImg(String name) {
        for (String p : new String[]{
            AdminPostController.UPLOADS + name,
            AdminPostController.UPLOADS + "images/" + name}) {
            File f = new File(p);
            if (f.exists()) return new Image(f.toURI().toString());
        }
        return null;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                   "-fx-background-radius:8;-fx-font-size:11;-fx-padding:5 12;-fx-cursor:hand;");
        return b;
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
