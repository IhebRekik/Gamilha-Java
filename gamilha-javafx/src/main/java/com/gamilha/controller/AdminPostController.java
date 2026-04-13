package com.gamilha.controller;

import com.gamilha.model.Commentaire;
import com.gamilha.model.Post;
import com.gamilha.service.*;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
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

/**
 * Admin — TOUS les posts en cards.
 * Images locales (public/uploads/) + média YouTube embed / image URL.
 * Bouton ⋮ sur chaque post ET chaque commentaire → Modifier / Supprimer.
 *
 * ⚠️ Adapter UPLOADS à ton chemin local.
 */
public class AdminPostController implements Initializable {

    // ⚠️ Chemin vers public/uploads/ de ton projet Symfony
    public static final String UPLOADS = "C:/wamp64/www/Gamilha-dev/public/uploads/";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane         cardsPane;
    @FXML private Label            totalLabel;

    private final PostService        postService        = new PostService();
    private final CommentaireService commentaireService = new CommentaireService();
    private ObservableList<Post>     allPosts           = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sortCombo.setItems(FXCollections.observableArrayList(
            "Plus récent", "Plus ancien", "Plus de likes", "Auteur A→Z"));
        sortCombo.setValue("Plus récent");
        sortCombo.setOnAction(e -> applyFilter());
        searchField.textProperty().addListener((o, ov, nv) -> applyFilter());
        loadPosts();
    }

    private void loadPosts() {
        try {
            allPosts.setAll(postService.findAll());
            applyFilter();
        } catch (SQLException e) { alert("Erreur BD : " + e.getMessage()); }
    }

    private void applyFilter() {
        String kw   = searchField.getText().toLowerCase().trim();
        String sort = sortCombo.getValue();

        List<Post> filtered = allPosts.stream()
            .filter(p -> kw.isEmpty()
                || p.getContent().toLowerCase().contains(kw)
                || (p.getUser() != null && p.getUser().getName().toLowerCase().contains(kw)))
            .collect(Collectors.toList());

        switch (sort == null ? "" : sort) {
            case "Plus récent"   -> filtered.sort(Comparator.comparing(Post::getCreatedAt).reversed());
            case "Plus ancien"   -> filtered.sort(Comparator.comparing(Post::getCreatedAt));
            case "Plus de likes" -> filtered.sort(Comparator.comparingInt(Post::getLikesCount).reversed());
            case "Auteur A→Z"    -> filtered.sort(Comparator.comparing(
                                        p -> p.getUser() != null ? p.getUser().getName() : ""));
        }

        totalLabel.setText(filtered.size() + " post(s)");
        cardsPane.getChildren().clear();
        filtered.forEach(p -> cardsPane.getChildren().add(buildCard(p)));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTION CARD
    // ══════════════════════════════════════════════════════════════════════

    private VBox buildCard(Post post) {
        VBox card = new VBox(0);
        card.setPrefWidth(370);
        card.setMaxWidth(370);
        card.setStyle(
            "-fx-background-color:#1c1c2e;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:#2a2b4a;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),12,0,0,4);");
        card.getChildren().addAll(
            buildImageSection(post),
            buildInfoSection(post),
            buildMediaSection(post),
            buildCommentsToggle(post)
        );
        return card;
    }

    // ── Image locale du post ─────────────────────────────────────────────
    private StackPane buildImageSection(Post post) {
        StackPane stack = new StackPane();

        if (post.getImage() != null && !post.getImage().isBlank()) {
            Image img = loadLocalImg(post.getImage());
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(370);
                iv.setFitHeight(185);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                Rectangle clip = new Rectangle(370, 185);
                clip.setArcWidth(28); clip.setArcHeight(28);
                iv.setClip(clip);
                stack.getChildren().add(iv);

                // Overlay dégradé en bas
                javafx.scene.shape.Rectangle grad = new javafx.scene.shape.Rectangle(370, 185);
                grad.setFill(new javafx.scene.paint.LinearGradient(
                    0, 0.6, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                    new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.rgb(0,0,0,0.55))));
                stack.getChildren().add(grad);
            }
        } else {
            // Placeholder si pas d'image
            HBox placeholder = new HBox();
            placeholder.setPrefSize(370, 80);
            placeholder.setStyle("-fx-background-color:#13131f;");
            stack.getChildren().add(placeholder);
        }

        // Badge likes (coin haut droit)
        Label badge = new Label("\u2764 " + post.getLikesCount());
        badge.setStyle(
            "-fx-background-color:rgba(0,0,0,0.65);-fx-text-fill:#ff6b81;" +
            "-fx-background-radius:20;-fx-padding:3 9;-fx-font-size:11;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(8));
        stack.getChildren().add(badge);

        return stack;
    }

    // ── Infos : auteur + contenu + bouton ⋮ admin ─────────────────────────
    private VBox buildInfoSection(Post post) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 14, 6, 14));

        // Ligne auteur + ⋮
        HBox authorRow = new HBox(8);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        Label av = av(post.getUser() != null ? post.getUser().getName() : "?", 36);
        VBox ai = new VBox(1);
        HBox.setHgrow(ai, Priority.ALWAYS);
        Label nm = new Label(post.getUser() != null ? post.getUser().getName() : "Inconnu");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e6edf3;-fx-font-size:13;");
        Label dt = new Label(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#8b949e;-fx-font-size:10;");
        ai.getChildren().addAll(nm, dt);

        // ⋮ menu admin (toujours visible pour admin)
        Button menuBtn = menuButton();
        menuBtn.setOnAction(e -> showAdminPostMenu(menuBtn, post));

        authorRow.getChildren().addAll(av, ai, menuBtn);

        // Contenu texte
        String raw = post.getContent() != null
            ? post.getContent().replaceAll("<[^>]+>", "").trim() : "";
        Label content = new Label(trunc(raw, 140));
        content.setWrapText(true);
        content.setStyle("-fx-text-fill:#c9d1d9;-fx-font-size:12;");

        box.getChildren().addAll(authorRow, content);
        return box;
    }

    // ── Menu ⋮ admin sur un post ──────────────────────────────────────────
    private void showAdminPostMenu(Button anchor, Post post) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
            "-fx-background-color:#1f1f3a;-fx-border-color:#3a3a5c;" +
            "-fx-border-width:1;-fx-background-radius:8;");

        MenuItem editItem = new MenuItem("  ✏  Modifier le post");
        editItem.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");
        editItem.setOnAction(e -> openEditPost(post));

        MenuItem delItem = new MenuItem("  🗑  Supprimer le post");
        delItem.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12;");
        delItem.setOnAction(e -> confirmDeletePost(post));

        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ── Média (YouTube ou image URL) ──────────────────────────────────────
    private VBox buildMediaSection(Post post) {
        VBox box = new VBox();
        String url = post.getMediaurl();
        if (url == null || url.isBlank()) return box;
        box.setPadding(new Insets(0, 14, 8, 14));

        switch (MediaHelper.detect(url)) {
            case YOUTUBE -> {
                String embed = MediaHelper.toEmbedUrl(url);
                if (embed != null) {
                    WebView wv = new WebView();
                    wv.setPrefWidth(342);
                    wv.setPrefHeight(193); // 16:9
                    wv.getEngine().load(embed);
                    box.getChildren().add(wv);
                }
            }
            case IMAGE_URL -> {
                try {
                    Image img = new Image(url, 342, 200, true, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(342);
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

    // ── Bouton toggle + zone commentaires inline ──────────────────────────
    private VBox buildCommentsToggle(Post post) {
        VBox wrap = new VBox(0);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#2a2b4a;");
        VBox.setMargin(sep, new Insets(0, 14, 0, 14));

        // Barre basse : nb commentaires + bouton toggle
        HBox bar = new HBox(8);
        bar.setPadding(new Insets(8, 14, 10, 14));
        bar.setAlignment(Pos.CENTER_LEFT);

        Label nbCom = new Label();
        HBox.setHgrow(nbCom, Priority.ALWAYS);
        try {
            int nb = commentaireService.findByPost(post.getId()).size();
            nbCom.setText("\uD83D\uDCAC " + nb + " commentaire(s)");
        } catch (SQLException e) { nbCom.setText("\uD83D\uDCAC commentaires"); }
        nbCom.setStyle("-fx-text-fill:#8b949e;-fx-font-size:11;");

        Button bToggle = btn("Voir commentaires", "#374151");

        VBox commentsZone = new VBox(6);
        commentsZone.setPadding(new Insets(6, 14, 10, 14));
        commentsZone.setVisible(false);
        commentsZone.setManaged(false);

        bToggle.setOnAction(e -> {
            boolean show = !commentsZone.isVisible();
            commentsZone.setVisible(show);
            commentsZone.setManaged(show);
            bToggle.setText(show ? "Masquer" : "Voir commentaires");
            if (show) loadAdminComments(post, commentsZone);
        });

        bar.getChildren().addAll(nbCom, bToggle);
        wrap.getChildren().addAll(sep, bar, commentsZone);
        return wrap;
    }

    // ── Commentaires inline admin avec ⋮ ──────────────────────────────────
    private void loadAdminComments(Post post, VBox zone) {
        zone.getChildren().clear();
        try {
            List<Commentaire> list = commentaireService.findByPost(post.getId());
            if (list.isEmpty()) {
                zone.getChildren().add(gray("Aucun commentaire."));
                return;
            }
            for (Commentaire c : list) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.TOP_LEFT);
                row.setPadding(new Insets(4));
                row.setStyle(
                    "-fx-background-color:rgba(255,255,255,0.04);" +
                    "-fx-background-radius:8;");

                Label avl = av(c.getUser() != null ? c.getUser().getName() : "?", 28);

                VBox body = new VBox(2);
                HBox.setHgrow(body, Priority.ALWAYS);

                HBox meta = new HBox(6);
                meta.setAlignment(Pos.CENTER_LEFT);
                Label nm = new Label(c.getUser() != null ? c.getUser().getName() : "?");
                nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:11;");
                Label dt = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
                dt.setStyle("-fx-text-fill:#6e7681;-fx-font-size:10;");
                Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

                // ⋮ admin sur commentaire
                Button menuBtn = menuButton();
                menuBtn.setOnAction(e -> showAdminCommentMenu(menuBtn, c, post, zone));

                meta.getChildren().addAll(nm, dt, sp, menuBtn);

                Label txt = new Label(c.getText());
                txt.setWrapText(true);
                txt.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:11;");
                body.getChildren().addAll(meta, txt);

                row.getChildren().addAll(avl, body);
                zone.getChildren().add(row);
            }
        } catch (SQLException e) {
            zone.getChildren().add(gray("Erreur : " + e.getMessage()));
        }
    }

    // ── Menu ⋮ admin sur un commentaire ──────────────────────────────────
    private void showAdminCommentMenu(Button anchor, Commentaire c, Post post, VBox zone) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
            "-fx-background-color:#1f1f3a;-fx-border-color:#3a3a5c;" +
            "-fx-border-width:1;-fx-background-radius:8;");

        MenuItem editItem = new MenuItem("  ✏  Modifier le commentaire");
        editItem.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");
        editItem.setOnAction(e -> openEditComment(c, post, zone));

        MenuItem delItem = new MenuItem("  🗑  Supprimer le commentaire");
        delItem.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12;");
        delItem.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
            a.setHeaderText("Confirmer");
            a.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    try { commentaireService.delete(c.getId()); loadAdminComments(post, zone); }
                    catch (SQLException ex) { alert(ex.getMessage()); }
                }
            });
        });

        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ── Édition post (admin) ──────────────────────────────────────────────
    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/interfaces/Admin/PostFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle(post == null ? "Nouveau Post" : "Éditer Post #" + post.getId());
            s.setScene(new Scene(loader.load(), 540, 470));
            ((PostFormController) loader.getController()).init(post);
            s.showAndWait();
            loadPosts();
        } catch (IOException e) { alert("Erreur : " + e.getMessage()); }
    }

    // ── Édition commentaire (admin) ───────────────────────────────────────
    private void openEditComment(Commentaire c, Post post, VBox zone) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/interfaces/Admin/CommentaireFormView.fxml"));
            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Éditer Commentaire #" + c.getId());
            s.setScene(new Scene(loader.load(), 480, 270));
            ((CommentaireFormController) loader.getController()).init(c);
            s.showAndWait();
            loadAdminComments(post, zone);
        } catch (IOException e) { alert("Erreur : " + e.getMessage()); }
    }

    private void confirmDeletePost(Post post) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le post #" + post.getId() + " et tous ses commentaires ?",
            ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Confirmer la suppression");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { postService.delete(post.getId()); loadPosts(); }
                catch (SQLException e) { alert("Erreur BD : " + e.getMessage()); }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private Label av(String name, double sz) {
        String init = (name != null && name.length() >= 2) ? name.substring(0,2).toUpperCase() : "??";
        Label l = new Label(init);
        l.setMinSize(sz,sz); l.setMaxSize(sz,sz);
        l.setAlignment(Pos.CENTER);
        l.setStyle(
            "-fx-background-color:#5b21b6;-fx-text-fill:white;-fx-font-weight:bold;" +
            "-fx-font-size:"+(sz*0.35)+";-fx-background-radius:"+(sz/2)+";");
        return l;
    }

    private Button menuButton() {
        Button b = new Button("⋮");
        b.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#8b949e;" +
            "-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;");
        b.setOnMouseEntered(e -> b.setStyle(
            "-fx-background-color:#252535;-fx-text-fill:#c9d1d9;" +
            "-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;-fx-background-radius:6;"));
        b.setOnMouseExited(e -> b.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#8b949e;" +
            "-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;"));
        return b;
    }

    public Image loadLocalImg(String fn) {
        for (String p : new String[]{UPLOADS+fn, UPLOADS+"images/"+fn}) {
            File f = new File(p); if (f.exists()) return new Image(f.toURI().toString());
        }
        return null;
    }

    private Hyperlink linkLabel(String url) {
        Hyperlink lk = new Hyperlink("\uD83D\uDD17 Ouvrir le lien");
        lk.setStyle("-fx-text-fill:#58a6ff;-fx-font-size:11;");
        lk.setOnAction(e -> {
            try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); }
            catch (Exception ignored) {}
        });
        return lk;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle(
            "-fx-background-color:"+color+";-fx-text-fill:white;" +
            "-fx-background-radius:6;-fx-font-size:11;-fx-padding:4 10;-fx-cursor:hand;");
        return b;
    }

    private Label gray(String msg) {
        Label l = new Label(msg); l.setStyle("-fx-text-fill:#8b949e;-fx-font-size:11;"); return l;
    }

    private String trunc(String s, int max) {
        return s == null ? "" : s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    @FXML void onRefresh() { loadPosts(); }
    @FXML void onNewPost() { openEditPost(null); }
}
