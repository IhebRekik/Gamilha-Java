package com.gamilha.controller;

import com.gamilha.model.Commentaire;
import com.gamilha.model.Post;
import com.gamilha.model.User;
import com.gamilha.service.*;
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
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Fil d'actualité Social — miroir exact de social/index.html.twig.
 *
 * Comportement "3 points" :
 *  - Le bouton ⋮ n'apparaît QUE si l'utilisateur est le propriétaire du post/commentaire.
 *  - Au clic → popup contextuel avec Modifier / Supprimer.
 */
public class UserPostController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private VBox  feedBox;
    @FXML private Label welcomeLabel;
    @FXML private VBox  suggestionsBox;
    @FXML private VBox  amisBox;

    private final PostService        postService        = new PostService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final FriendService      friendService      = new FriendService();

    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && welcomeLabel != null)
            welcomeLabel.setText("Bonjour,  " + user.getName() + " \uD83D\uDC4B");
        loadFeed();
        loadSidebar();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FIL D'ACTUALITÉ
    // ══════════════════════════════════════════════════════════════════════

    void loadFeed() {
        feedBox.getChildren().clear();
        try {
            List<Post> posts = postService.findAll();
            if (posts.isEmpty())
                feedBox.getChildren().add(gray("Aucun post pour le moment."));
            else
                for (Post p : posts) feedBox.getChildren().add(buildPostCard(p));
        } catch (SQLException e) {
            feedBox.getChildren().add(gray("Erreur chargement : " + e.getMessage()));
        }
    }

    // ── Card complète d'un post ───────────────────────────────────────────
    private VBox buildPostCard(Post post) {
        VBox card = new VBox(0);
        card.setMaxWidth(660);
        card.setStyle(
            "-fx-background-color:#1c1c2e;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:#2a2b4a;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.45),12,0,0,4);");
        card.getChildren().addAll(
            buildPostHeader(post),
            buildPostContent(post),
            buildPostMedia(post),
            buildPostStats(post),
            sep(16),
            buildCommentsList(post),
            buildAddCommentRow(post)
        );
        return card;
    }

    // ── En-tête : avatar + nom + date + ⋮ (seulement si propriétaire) ────
    private HBox buildPostHeader(Post post) {
        HBox header = new HBox(10);
        header.setPadding(new Insets(14, 16, 8, 16));
        header.setAlignment(Pos.CENTER_LEFT);

        // Avatar avec initiales
        Label av = av(post.getUser() != null ? post.getUser().getName() : "?", 42);

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nm = new Label(post.getUser() != null ? post.getUser().getName() : "Inconnu");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e6edf3;-fx-font-size:14;");
        Label dt = new Label(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#8b949e;-fx-font-size:11;");
        info.getChildren().addAll(nm, dt);

        header.getChildren().addAll(av, info);

        // ── Bouton ⋮ — affiché UNIQUEMENT si c'est le post de l'utilisateur ──
        if (isOwner(post.getUser())) {
            Button menuBtn = menuButton();
            menuBtn.setOnAction(e -> showPostMenu(menuBtn, post));
            header.getChildren().add(menuBtn);
        }

        return header;
    }

    // ── Menu contextuel post (Modifier / Supprimer) ───────────────────────
    private void showPostMenu(Button anchor, Post post) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
            "-fx-background-color:#1f1f3a;-fx-border-color:#3a3a5c;" +
            "-fx-border-width:1;-fx-background-radius:8;");

        MenuItem editItem = new MenuItem("  ✏  Modifier la publication");
        editItem.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");
        editItem.setOnAction(e -> openEditPost(post));

        MenuItem delItem = new MenuItem("  🗑  Supprimer la publication");
        delItem.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12;");
        delItem.setOnAction(e -> confirmDeletePost(post));

        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ── Contenu texte + image locale ──────────────────────────────────────
    private VBox buildPostContent(Post post) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(0, 16, 8, 16));

        String raw = post.getContent() != null
            ? post.getContent().replaceAll("<[^>]+>", "").trim() : "";
        Label content = new Label(raw);
        content.setWrapText(true);
        content.setMaxWidth(628);
        content.setStyle("-fx-text-fill:#c9d1d9;-fx-font-size:13;-fx-line-spacing:2;");
        box.getChildren().add(content);

        // Image uploadée localement (public/uploads/)
        if (post.getImage() != null && !post.getImage().isBlank()) {
            Image img = localImg(post.getImage());
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(628);
                iv.setFitHeight(320);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                Rectangle clip = new Rectangle(628, 320);
                clip.setArcWidth(16); clip.setArcHeight(16);
                iv.setClip(clip);
                box.getChildren().add(iv);
            }
        }
        return box;
    }

    // ── Média : YouTube WebView ou image URL ou lien ──────────────────────
    private VBox buildPostMedia(Post post) {
        VBox box = new VBox();
        String url = post.getMediaurl();
        if (url == null || url.isBlank()) return box;
        box.setPadding(new Insets(0, 16, 10, 16));

        switch (MediaHelper.detect(url)) {
            case YOUTUBE -> {
                String embed = MediaHelper.toEmbedUrl(url);
                if (embed != null) {
                    WebView wv = new WebView();
                    wv.setPrefSize(628, 353);   // 16:9
                    wv.getEngine().load(embed);
                    box.getChildren().add(wv);
                }
            }
            case IMAGE_URL -> {
                try {
                    Image img = new Image(url, 628, 340, true, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(628);
                    iv.setPreserveRatio(true);
                    iv.setSmooth(true);
                    box.getChildren().add(iv);
                } catch (Exception ex) {
                    box.getChildren().add(linkLabel(url));
                }
            }
            case LINK -> box.getChildren().add(linkLabel(url));
            default -> {}
        }
        return box;
    }

    // ── Stats : ❤ likes + 💬 nb commentaires ─────────────────────────────
    private HBox buildPostStats(Post post) {
        HBox h = new HBox(20);
        h.setPadding(new Insets(8, 16, 6, 16));
        h.setAlignment(Pos.CENTER_LEFT);

        Label lk = new Label("\u2764  " + post.getLikesCount());
        lk.setStyle("-fx-text-fill:#ff6b81;-fx-font-size:13;-fx-font-weight:bold;");

        int nbCom = 0;
        try { nbCom = commentaireService.findByPost(post.getId()).size(); }
        catch (SQLException ignored) {}
        Label cm = new Label("\uD83D\uDCAC  " + nbCom + " commentaire(s)");
        cm.setStyle("-fx-text-fill:#8b949e;-fx-font-size:12;");

        h.getChildren().addAll(lk, cm);
        return h;
    }

    // ── Liste des commentaires existants ──────────────────────────────────
    private VBox buildCommentsList(Post post) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 16, 4, 16));
        try {
            List<Commentaire> comments = commentaireService.findByPost(post.getId());
            post.setCommentaires(comments);
            for (Commentaire c : comments)
                box.getChildren().add(buildCommentRow(c, post));
        } catch (SQLException e) {
            box.getChildren().add(gray("Erreur : " + e.getMessage()));
        }
        return box;
    }

    // ── Ligne d'un commentaire avec ⋮ si propriétaire ─────────────────────
    private HBox buildCommentRow(Commentaire c, Post post) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(4, 0, 4, 0));

        Label av = av(c.getUser() != null ? c.getUser().getName() : "?", 32);

        VBox body = new VBox(3);
        HBox.setHgrow(body, Priority.ALWAYS);
        body.setStyle(
            "-fx-background-color:rgba(255,255,255,0.05);" +
            "-fx-background-radius:10;-fx-padding:7 10;");

        // Ligne meta : nom + date + ⋮
        HBox meta = new HBox(6);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label nm = new Label(c.getUser() != null ? c.getUser().getName() : "?");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:12;");

        Label dt = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#6e7681;-fx-font-size:10;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        meta.getChildren().addAll(nm, dt, spacer);

        // ── Bouton ⋮ — uniquement si propriétaire du commentaire ──────────
        if (isOwner(c.getUser())) {
            Button menuBtn = menuButton();
            menuBtn.setOnAction(e -> showCommentMenu(menuBtn, c, post));
            meta.getChildren().add(menuBtn);
        }

        Label txt = new Label(c.getText());
        txt.setWrapText(true);
        txt.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");

        body.getChildren().addAll(meta, txt);
        row.getChildren().addAll(av, body);
        return row;
    }

    // ── Menu contextuel commentaire (Modifier / Supprimer) ────────────────
    private void showCommentMenu(Button anchor, Commentaire c, Post post) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
            "-fx-background-color:#1f1f3a;-fx-border-color:#3a3a5c;" +
            "-fx-border-width:1;-fx-background-radius:8;");

        MenuItem editItem = new MenuItem("  ✏  Modifier le commentaire");
        editItem.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");
        editItem.setOnAction(e -> openEditComment(c));

        MenuItem delItem = new MenuItem("  🗑  Supprimer le commentaire");
        delItem.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12;");
        delItem.setOnAction(e -> confirmDeleteComment(c));

        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ── Champ "Écrire un commentaire…" ───────────────────────────────────
    private HBox buildAddCommentRow(Post post) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 16, 14, 16));

        Label av = av(currentUser != null ? currentUser.getName() : "?", 32);

        TextField field = new TextField();
        field.setPromptText("Écrire un commentaire…");
        field.setStyle(
            "-fx-background-color:#252535;-fx-text-fill:#e6edf3;" +
            "-fx-prompt-text-fill:#6e7681;-fx-background-radius:20;" +
            "-fx-border-color:#3a3a5c;-fx-border-radius:20;-fx-padding:7 14;-fx-font-size:12;");
        HBox.setHgrow(field, Priority.ALWAYS);

        Button send = new Button("\u27A4");
        send.setStyle(
            "-fx-background-color:#c84cff;-fx-text-fill:white;" +
            "-fx-background-radius:50;-fx-padding:7 12;-fx-cursor:hand;-fx-font-size:13;");

        Runnable submit = () -> submitComment(field, post);
        send.setOnAction(e -> submit.run());
        field.setOnAction(e -> submit.run());

        row.getChildren().addAll(av, field, send);
        return row;
    }

    // ── Soumettre commentaire ─────────────────────────────────────────────
    private void submitComment(TextField field, Post post) {
        String text = field.getText().trim();
        if (text.length() < 5)   { toast("Min. 5 caractères requis."); return; }
        if (text.length() > 500) { toast("Max. 500 caractères autorisés."); return; }
        if (currentUser == null) { toast("Non connecté."); return; }
        try {
            Commentaire c = new Commentaire();
            c.setText(text);
            c.setPost(post);
            c.setUser(currentUser);
            c.setCreatedAt(java.time.LocalDateTime.now());
            commentaireService.create(c);
            field.clear();
            loadFeed();
        } catch (SQLException e) { toast("Erreur : " + e.getMessage()); }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ACTIONS POST
    // ══════════════════════════════════════════════════════════════════════

    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/interfaces/User/UserPostFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle(post == null ? "Nouveau Post" : "Modifier ma publication");
            s.setScene(new Scene(loader.load(), 540, 440));
            ((UserPostFormController) loader.getController()).init(post, currentUser);
            s.showAndWait();
            loadFeed();
        } catch (IOException e) { toast("Erreur : " + e.getMessage()); }
    }

    private void confirmDeletePost(Post post) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer cette publication et tous ses commentaires ?",
            ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Supprimer la publication ?");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { postService.delete(post.getId()); loadFeed(); }
                catch (SQLException e) { toast("Erreur : " + e.getMessage()); }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  ACTIONS COMMENTAIRE
    // ══════════════════════════════════════════════════════════════════════

    private void openEditComment(Commentaire c) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/interfaces/User/UserCommentaireFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Modifier mon commentaire");
            s.setScene(new Scene(loader.load(), 480, 270));
            ((UserCommentaireFormController) loader.getController()).init(c);
            s.showAndWait();
            loadFeed();
        } catch (IOException e) { toast("Erreur : " + e.getMessage()); }
    }

    private void confirmDeleteComment(Commentaire c) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Supprimer le commentaire ?");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { commentaireService.delete(c.getId()); loadFeed(); }
                catch (SQLException e) { toast("Erreur : " + e.getMessage()); }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  SIDEBAR amis + suggestions
    // ══════════════════════════════════════════════════════════════════════

    private void loadSidebar() {
        loadSuggestions();
        loadAmis();
    }

    private void loadSuggestions() {
        if (suggestionsBox == null || currentUser == null) return;
        suggestionsBox.getChildren().clear();
        try {
            List<User> list = friendService.findSuggestions(currentUser.getId(), 5);
            if (list.isEmpty()) {
                suggestionsBox.getChildren().add(gray("Aucune suggestion pour le moment."));
                return;
            }
            for (User u : list) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));

                Label avl = av(u.getName(), 36);
                VBox info = new VBox(1);
                HBox.setHgrow(info, Priority.ALWAYS);
                Label nm = new Label(u.getName());
                nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:12;");
                Label sub = new Label("Pas de jeu commun");
                sub.setStyle("-fx-text-fill:#4caf50;-fx-font-size:10;");
                info.getChildren().addAll(nm, sub);

                Button add = new Button("➕");
                add.setStyle(
                    "-fx-background-color:#3b82f6;-fx-text-fill:white;" +
                    "-fx-background-radius:50;-fx-padding:4 8;-fx-cursor:hand;-fx-font-size:12;");
                add.setOnAction(e -> {
                    try { friendService.addFriend(currentUser.getId(), u.getId()); loadSidebar(); }
                    catch (SQLException ex) { toast("Erreur : " + ex.getMessage()); }
                });
                row.getChildren().addAll(avl, info, add);
                suggestionsBox.getChildren().add(row);
            }
        } catch (SQLException e) {
            suggestionsBox.getChildren().add(gray("Erreur : " + e.getMessage()));
        }
    }

    private void loadAmis() {
        if (amisBox == null || currentUser == null) return;
        amisBox.getChildren().clear();
        try {
            List<User> friends = friendService.findFriends(currentUser.getId());
            if (friends.isEmpty()) {
                amisBox.getChildren().add(gray("Aucun ami pour le moment."));
                return;
            }
            for (User u : friends) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(4, 0, 4, 0));
                Label avl = av(u.getName(), 34);
                VBox info = new VBox(1);
                Label nm = new Label(u.getName());
                nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:12;");
                Label st = new Label("\uD83D\uDFE2 En ligne");
                st.setStyle("-fx-text-fill:#4caf50;-fx-font-size:10;");
                info.getChildren().addAll(nm, st);
                row.getChildren().addAll(avl, info);
                amisBox.getChildren().add(row);
            }
        } catch (SQLException e) {
            amisBox.getChildren().add(gray("Erreur : " + e.getMessage()));
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    /** Avatar circulaire violet Gamilha avec initiales */
    private Label av(String name, double sz) {
        String init = (name != null && name.length() >= 2)
            ? name.substring(0, 2).toUpperCase() : "??";
        Label l = new Label(init);
        l.setMinSize(sz, sz);
        l.setMaxSize(sz, sz);
        l.setAlignment(Pos.CENTER);
        l.setStyle(
            "-fx-background-color:#5b21b6;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-font-size:" + (sz * 0.35) + ";-fx-background-radius:" + (sz / 2) + ";");
        return l;
    }

    /** Bouton ⋮ trois points vertical */
    private Button menuButton() {
        Button b = new Button("⋮");
        b.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#8b949e;" +
            "-fx-font-size:18;-fx-cursor:hand;-fx-padding:0 6;");
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:#252535;-fx-text-fill:#c9d1d9;" +
            "-fx-font-size:18;-fx-cursor:hand;-fx-padding:0 6;-fx-background-radius:6;"));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#8b949e;" +
            "-fx-font-size:18;-fx-cursor:hand;-fx-padding:0 6;"));
        return b;
    }

    private Image localImg(String fn) {
        for (String p : new String[]{
            AdminPostController.UPLOADS + fn,
            AdminPostController.UPLOADS + "images/" + fn}) {
            File f = new File(p);
            if (f.exists()) return new Image(f.toURI().toString());
        }
        return null;
    }

    private Hyperlink linkLabel(String url) {
        Hyperlink lk = new Hyperlink("\uD83D\uDD17 " + url);
        lk.setStyle("-fx-text-fill:#58a6ff;-fx-font-size:12;");
        lk.setOnAction(e -> {
            try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); }
            catch (Exception ignored) {}
        });
        return lk;
    }

    private Separator sep(double margin) {
        Separator s = new Separator();
        s.setStyle("-fx-background-color:#2a2b4a;");
        VBox.setMargin(s, new Insets(4, margin, 4, margin));
        return s;
    }

    private Label gray(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill:#8b949e;-fx-font-size:12;");
        return l;
    }

    private boolean isOwner(User u) {
        return currentUser != null && u != null && u.getId() == currentUser.getId();
    }

    private void toast(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML void onNewPost()  { openEditPost(null); }
    @FXML void onRefresh()  { loadFeed(); loadSidebar(); }
}
