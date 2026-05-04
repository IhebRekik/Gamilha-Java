package com.gamilha.controllers;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.BookmarkService;
import com.gamilha.services.PostService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Vue "Mes Favoris" — liste des posts bookmarkés par l'utilisateur connecté.
 * Fonctionnalités :
 *  - Affichage en grille des posts sauvegardés
 *  - Recherche par contenu ou auteur
 *  - Bouton pour retirer un bookmark directement depuis la carte
 *  - Compteur de posts sauvegardés
 *  - Badge "Sauvegardé" avec date
 */
public class UserBookmarksController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ScrollPane bookmarksScroll;
    @FXML private FlowPane   cardsPane;
    @FXML private Label      totalLabel;
    @FXML private TextField  searchField;
    @FXML private Label      emptyLabel;

    private final BookmarkService bookmarkService = new BookmarkService();
    private User       currentUser;
    private List<Post> savedPosts;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Bind hauteur scroll au parent
        if (bookmarksScroll != null) {
            bookmarksScroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getWindow() != null) {
                    bookmarksScroll.prefHeightProperty().bind(
                        newScene.getWindow().heightProperty().subtract(146));
                } else if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                        if (newWin != null)
                            bookmarksScroll.prefHeightProperty().bind(
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
        if (user != null) loadBookmarks();
    }

    // ── Chargement ────────────────────────────────────────────────────────
    private void loadBookmarks() {
        try {
            savedPosts = bookmarkService.findBookmarkedPosts(currentUser.getId());
            applyFilter(searchField != null ? searchField.getText() : "");
        } catch (SQLException e) {
            showAlert("Erreur BD : " + e.getMessage());
        }
    }

    private void applyFilter(String kw) {
        String k = kw == null ? "" : kw.toLowerCase().trim();
        List<Post> filtered = savedPosts == null ? List.of() : savedPosts.stream()
            .filter(p -> k.isEmpty()
                || PostService.stripStylePrefix(p.getContent()).toLowerCase().contains(k)
                || (p.getUser() != null && p.getUser().getName().toLowerCase().contains(k)))
            .collect(Collectors.toList());

        if (totalLabel != null)
            totalLabel.setText(filtered.size() + " post(s) sauvegardé(s)");

        cardsPane.getChildren().clear();

        if (filtered.isEmpty()) {
            VBox empty = buildEmptyState(k.isEmpty());
            cardsPane.getChildren().add(empty);
        } else {
            filtered.forEach(p -> cardsPane.getChildren().add(buildCard(p)));
        }
    }

    // ── Carte d'un post sauvegardé ────────────────────────────────────────
    private VBox buildCard(Post post) {
        VBox card = new VBox(0);
        card.setPrefWidth(310);
        card.setStyle(
            "-fx-background-color:#0d0d1a;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:#7c3aed;-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.2),10,0,0,3);");
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color:#0d0d1a;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:#a78bfa;-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(167,139,250,0.35),14,0,0,5);"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color:#0d0d1a;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:#7c3aed;-fx-border-width:1.5;" +
            "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.2),10,0,0,3);"));

        // ── Image du post ─────────────────────────────────────────────────
        String firstImg = post.getFirstImage();
        if (firstImg != null) {
            Image img = loadImg(firstImg);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(310); iv.setFitHeight(155);
                iv.setPreserveRatio(false); iv.setSmooth(true);
                Rectangle clip = new Rectangle(310, 155);
                clip.setArcWidth(22); clip.setArcHeight(22);
                iv.setClip(clip);

                StackPane imgStack = new StackPane(iv);
                int nbImgs = post.getAllImages().size();
                if (nbImgs > 1) {
                    Label badge = new Label("📸 ×" + nbImgs);
                    badge.setStyle(
                        "-fx-background-color:rgba(0,0,0,0.6);-fx-text-fill:white;" +
                        "-fx-background-radius:8;-fx-padding:2 7;-fx-font-size:11;");
                    StackPane.setAlignment(badge, Pos.BOTTOM_RIGHT);
                    StackPane.setMargin(badge, new Insets(0, 8, 8, 0));
                    imgStack.getChildren().add(badge);
                }
                card.getChildren().add(imgStack);
            }
        }

        // ── Corps de la carte ─────────────────────────────────────────────
        VBox body = new VBox(8);
        body.setPadding(new Insets(12, 14, 12, 14));

        // Badge 🔖 + auteur
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label bookmarkBadge = new Label("🔖 Sauvegardé");
        bookmarkBadge.setStyle(
            "-fx-background-color:rgba(124,58,237,0.18);-fx-text-fill:#a78bfa;" +
            "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:10;-fx-font-weight:bold;");

        Label authorLabel = new Label(post.getUser() != null ? post.getUser().getName() : "?");
        authorLabel.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");
        HBox.setHgrow(authorLabel, Priority.ALWAYS);

        header.getChildren().addAll(bookmarkBadge, authorLabel);

        // Contenu textuel
        String raw = PostService.stripStylePrefix(
            post.getContent() != null ? post.getContent() : "");
        String preview = raw.length() > 120 ? raw.substring(0, 120) + "…" : raw;
        Label contentLabel = new Label(preview.isBlank() ? "(post sans texte)" : preview);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-text-fill:#c9d1d9;-fx-font-size:12;-fx-line-spacing:1.5;");
        contentLabel.setMaxWidth(282);

        // Date du post
        Label dateLabel = new Label(
            post.getCreatedAt() != null ? "📅 " + post.getCreatedAt().format(FMT) : "");
        dateLabel.setStyle("-fx-text-fill:#475569;-fx-font-size:10;");

        // Stats
        HBox stats = new HBox(12);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label likesStat = new Label("❤  " + post.getLikesCount());
        likesStat.setStyle("-fx-text-fill:#f43f5e;-fx-font-size:11;");
        Label sharedBadge = post.getSharedFromId() != null
            ? new Label("🔁 Partagé") : new Label("");
        sharedBadge.setStyle("-fx-text-fill:#60a5fa;-fx-font-size:10;");
        stats.getChildren().addAll(likesStat, sharedBadge);

        // ── Bouton retirer du favori ───────────────────────────────────────
        Button removeBtn = new Button("🗑  Retirer des favoris");
        removeBtn.setMaxWidth(Double.MAX_VALUE);
        removeBtn.setStyle(
            "-fx-background-color:rgba(244,63,94,0.07);-fx-text-fill:#94a3b8;" +
            "-fx-background-radius:8;-fx-padding:6 10;-fx-font-size:11;-fx-cursor:hand;" +
            "-fx-border-color:#2a2a40;-fx-border-radius:8;-fx-border-width:1;");
        removeBtn.setOnMouseEntered(e -> removeBtn.setStyle(
            "-fx-background-color:rgba(244,63,94,0.15);-fx-text-fill:#f43f5e;" +
            "-fx-background-radius:8;-fx-padding:6 10;-fx-font-size:11;-fx-cursor:hand;" +
            "-fx-border-color:#f43f5e;-fx-border-radius:8;-fx-border-width:1;"));
        removeBtn.setOnMouseExited(e -> removeBtn.setStyle(
            "-fx-background-color:rgba(244,63,94,0.07);-fx-text-fill:#94a3b8;" +
            "-fx-background-radius:8;-fx-padding:6 10;-fx-font-size:11;-fx-cursor:hand;" +
            "-fx-border-color:#2a2a40;-fx-border-radius:8;-fx-border-width:1;"));
        removeBtn.setOnAction(e -> {
            try {
                bookmarkService.toggleBookmark(currentUser.getId(), post.getId());
                loadBookmarks(); // Rafraîchir la liste
            } catch (SQLException ex) {
                showAlert("Erreur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(header, contentLabel, dateLabel, stats, removeBtn);
        card.getChildren().add(body);
        return card;
    }

    // ── État vide ─────────────────────────────────────────────────────────
    private VBox buildEmptyState(boolean noBookmarks) {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(60, 40, 40, 40));
        box.setPrefWidth(900);

        Label icon = new Label(noBookmarks ? "🔖" : "🔍");
        icon.setStyle("-fx-font-size:52;");

        Label title = new Label(noBookmarks
            ? "Aucun post sauvegardé"
            : "Aucun résultat");
        title.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:18;-fx-font-weight:bold;");

        Label sub = new Label(noBookmarks
            ? "Utilisez le bouton 🔖 sur n'importe quel post\npour l'ajouter à vos favoris."
            : "Aucun post sauvegardé ne correspond à votre recherche.");
        sub.setStyle("-fx-text-fill:#475569;-fx-font-size:13;-fx-text-alignment:center;");
        sub.setWrapText(true);
        sub.setMaxWidth(400);

        box.getChildren().addAll(icon, title, sub);
        return box;
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Image loadImg(String name) {
        String base = AdminPostController.UPLOADS;
        for (String path : new String[]{base + name, base + "images/" + name}) {
            File f = new File(path);
            if (f.exists()) return new Image(f.toURI().toString());
        }
        return null;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    public void onRefresh() {
        if (searchField != null) searchField.clear();
        loadBookmarks();
    }
}
