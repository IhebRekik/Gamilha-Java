package com.gamilha.controllers;

import com.gamilha.entity.Commentaire;
import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.*;
import com.gamilha.services.PostService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.stage.Window;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin — TOUS les posts en cards.
 * Nouvelles fonctionnalités Sprint 2 :
 *   - Image complète (ratio préservé, pas de crop)
 *   - Bouton "Voir détails" → fenêtre avec image pleine + tous les commentaires
 *   - Ajout commentaire par l'admin directement dans la zone toggle
 */
public class AdminPostController implements Initializable {

    // ⚠️ Adapter ce chemin à ton projet Symfony local
    public static final String UPLOADS = "C:/wamp64/www/Gamilha-dev/public/uploads/";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ScrollPane       adminScroll;
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane         cardsPane;
    @FXML private Label            totalLabel;

    private final PostService        postService        = new PostService();
    private final CommentaireService commentaireService = new CommentaireService();
    private ObservableList<Post>     allPosts           = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (adminScroll != null) {
            adminScroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getWindow() != null) {
                    adminScroll.prefHeightProperty().bind(
                        newScene.getWindow().heightProperty().subtract(146));
                } else if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                        if (newWin != null)
                            adminScroll.prefHeightProperty().bind(
                                newWin.heightProperty().subtract(146));
                    });
                }
            });
        }

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
            case "Auteur A→Z"    -> filtered.sort(Comparator.comparing(p -> p.getUser() != null ? p.getUser().getName() : ""));
        }
        totalLabel.setText(filtered.size() + " post(s)");
        cardsPane.getChildren().clear();
        filtered.forEach(p -> cardsPane.getChildren().add(buildCard(p)));
    }

    // ── Card admin ────────────────────────────────────────────────────────
    private VBox buildCard(Post post) {
        VBox card = new VBox(0);
        card.setPrefWidth(370); card.setMaxWidth(370);
        card.setStyle(
            "-fx-background-color:#1c1c2e;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:#2a2b4a;-fx-border-width:1;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),12,0,0,4);");
        card.getChildren().addAll(
            buildImageSection(post),
            buildInfoSection(post),
            buildMediaSection(post),
            buildCommentsSection(post)
        );
        return card;
    }

    // ── Image complète (ratio préservé, pas de crop forcé) ───────────────
    private StackPane buildImageSection(Post post) {
        StackPane stack = new StackPane();
        if (post.getImage() != null && !post.getImage().isBlank()) {
            Image img = loadLocalImg(post.getImage());
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(370);
                iv.setPreserveRatio(true);   // ← image complète, ratio respecté
                iv.setSmooth(true);
                // Clip arrondi adaptatif
                Rectangle clip = new Rectangle(370, 200);
                clip.setArcWidth(24); clip.setArcHeight(24);
                iv.imageProperty().addListener((obs, ov, nv) -> {
                    if (nv != null) {
                        double h = 370 * (nv.getHeight() / nv.getWidth());
                        iv.setFitHeight(h);
                        clip.setHeight(h);
                    }
                });
                if (!img.isBackgroundLoading() && img.getWidth() > 0) {
                    double h = 370 * (img.getHeight() / img.getWidth());
                    iv.setFitHeight(h);
                    clip.setHeight(h);
                }
                iv.setClip(clip);
                stack.getChildren().add(iv);
                // Dégradé en bas
                javafx.scene.shape.Rectangle grad = new javafx.scene.shape.Rectangle(370, 50);
                grad.setFill(new javafx.scene.paint.LinearGradient(
                    0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new javafx.scene.paint.Stop(0, javafx.scene.paint.Color.TRANSPARENT),
                    new javafx.scene.paint.Stop(1, javafx.scene.paint.Color.rgb(0,0,0,0.5))));
                stack.getChildren().add(grad);
                StackPane.setAlignment(grad, Pos.BOTTOM_CENTER);
            }
        } else {
            HBox ph = new HBox(); ph.setPrefSize(370, 60);
            ph.setStyle("-fx-background-color:#13131f;"); stack.getChildren().add(ph);
        }
        // Badge likes
        Label badge = new Label("\u2764 " + post.getLikesCount());
        badge.setStyle("-fx-background-color:rgba(0,0,0,0.65);-fx-text-fill:#ff6b81;" +
                       "-fx-background-radius:20;-fx-padding:3 9;-fx-font-size:11;");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(8));
        stack.getChildren().add(badge);
        return stack;
    }

    // ── Info + boutons actions ────────────────────────────────────────────
    private VBox buildInfoSection(Post post) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(12, 14, 6, 14));
        // En-tête auteur
        HBox authorRow = new HBox(8); authorRow.setAlignment(Pos.CENTER_LEFT);
        Label av = av(post.getUser() != null ? post.getUser().getName() : "?", 36);
        VBox ai = new VBox(1); HBox.setHgrow(ai, Priority.ALWAYS);
        Label nm = new Label(post.getUser() != null ? post.getUser().getName() : "Inconnu");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e2e8f0;-fx-font-size:13;");
        Label dt = new Label(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#475569;-fx-font-size:10;");
        ai.getChildren().addAll(nm, dt);
        Button menuBtn = menuButton();
        menuBtn.setOnAction(e -> showAdminPostMenu(menuBtn, post));
        authorRow.getChildren().addAll(av, ai, menuBtn);

        // Badges style + images
        HBox badges = new HBox(6); badges.setAlignment(Pos.CENTER_LEFT);
        String style = PostService.extractStyle(post.getContent());
        if (!style.equals("Normal")) {
            String icon = switch (style) {
                case "Gras" -> "B Gras"; case "Italique" -> "I Italique";
                case "Code" -> "</> Code"; case "Citation" -> "\u2760 Citation";
                default -> style;
            };
            Label sb = new Label(icon);
            sb.setStyle("-fx-background-color:rgba(139,92,246,0.15);-fx-text-fill:#a78bfa;" +
                "-fx-background-radius:8;-fx-padding:2 7;-fx-font-size:10;-fx-font-weight:bold;");
            badges.getChildren().add(sb);
        }
        int nbImgs = post.getAllImages().size();
        if (nbImgs > 1) {
            Label ib = new Label("\uD83D\uDCF8 \u00d7" + nbImgs);
            ib.setStyle("-fx-background-color:rgba(52,211,153,0.12);-fx-text-fill:#34d399;" +
                "-fx-background-radius:8;-fx-padding:2 7;-fx-font-size:10;");
            badges.getChildren().add(ib);
        }

        // Contenu avec style visuel
        Label content = buildStyledLabel(post.getContent(), 140);
        content.setWrapText(true);

        // Bouton voir détails
        Button btnDetail = btn("\uD83D\uDD0D Voir d\u00e9tails", "#5b21b6");
        btnDetail.setOnAction(e -> openPostDetail(post));

        box.getChildren().addAll(authorRow, badges, content, btnDetail);
        return box;
    }

    /** Label avec style visuel (comme UserPostController) */
    private Label buildStyledLabel(String rawContent, int maxLen) {
        String style = PostService.extractStyle(rawContent);
        String clean = PostService.stripStylePrefix(rawContent).replaceAll("<[^>]+>","").trim();
        if (clean.length() > maxLen) clean = clean.substring(0, maxLen) + "\u2026";
        Label l = new Label(clean); l.setWrapText(true);
        l.setStyle(switch (style) {
            case "Gras"     -> "-fx-text-fill:#f1f5f9;-fx-font-size:12;-fx-font-weight:bold;";
            case "Italique" -> "-fx-text-fill:#c9d1d9;-fx-font-size:12;-fx-font-style:italic;";
            case "Code"     -> "-fx-text-fill:#86efac;-fx-font-family:'Courier New';-fx-font-size:11;" +
                               "-fx-background-color:#0d1f17;-fx-background-radius:5;-fx-padding:4 8;";
            case "Citation" -> "-fx-text-fill:#94a3b8;-fx-font-size:12;-fx-font-style:italic;" +
                               "-fx-border-color:#7c3aed;-fx-border-width:0 0 0 3;-fx-padding:3 10;";
            default         -> "-fx-text-fill:#c9d1d9;-fx-font-size:12;";
        });
        return l;
    }

    // ── Menu ⋮ admin post ─────────────────────────────────────────────────
    private void showAdminPostMenu(Button anchor, Post post) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color:#1f1f3a;-fx-border-color:#3a3a5c;-fx-border-width:1;-fx-background-radius:8;");
        MenuItem editItem = new MenuItem("  \u270F  Modifier le post");
        editItem.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");
        editItem.setOnAction(e -> openEditPost(post));
        MenuItem delItem = new MenuItem("  \uD83D\uDDD1  Supprimer le post");
        delItem.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12;");
        delItem.setOnAction(e -> confirmDeletePost(post));
        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ── Média ─────────────────────────────────────────────────────────────
    private VBox buildMediaSection(Post post) {
        VBox box = new VBox(); String url = post.getMediaurl();
        if (url == null || url.isBlank()) return box;
        box.setPadding(new Insets(0, 14, 8, 14));
        switch (MediaHelper.detect(url)) {
            case YOUTUBE -> {
                String embed = MediaHelper.toEmbedUrl(url);
                if (embed != null) {
                    WebView wv = new WebView(); wv.setPrefWidth(342); wv.setPrefHeight(193);
                    wv.getEngine().load(embed); box.getChildren().add(wv);
                }
            }
            case IMAGE_URL -> {
                try {
                    Image img = new Image(url, 342, 0, true, true, true);
                    ImageView iv = new ImageView(img); iv.setFitWidth(342); iv.setPreserveRatio(true);
                    box.getChildren().add(iv);
                } catch (Exception ex) { box.getChildren().add(linkLabel(url)); }
            }
            case LINK -> box.getChildren().add(linkLabel(url));
            default -> {}
        }
        return box;
    }

    // ── Section commentaires (toggle + liste + AJOUT admin) ───────────────
    private VBox buildCommentsSection(Post post) {
        VBox wrap = new VBox(0);
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#2a2b4a;");
        VBox.setMargin(sep, new Insets(0, 14, 0, 14));

        HBox bar = new HBox(8); bar.setPadding(new Insets(8, 14, 6, 14)); bar.setAlignment(Pos.CENTER_LEFT);
        Label nbCom = new Label(); HBox.setHgrow(nbCom, Priority.ALWAYS);
        try {
            int nb = commentaireService.findByPost(post.getId()).size();
            nbCom.setText("\uD83D\uDCAC " + nb + " commentaire(s)");
        } catch (SQLException e) { nbCom.setText("\uD83D\uDCAC commentaires"); }
        nbCom.setStyle("-fx-text-fill:#8b949e;-fx-font-size:11;");

        Button bToggle = btn("Commentaires", "#374151");
        VBox commentsZone = new VBox(8);
        commentsZone.setPadding(new Insets(0, 14, 10, 14));
        commentsZone.setVisible(false); commentsZone.setManaged(false);

        bToggle.setOnAction(e -> {
            boolean show = !commentsZone.isVisible();
            commentsZone.setVisible(show); commentsZone.setManaged(show);
            bToggle.setText(show ? "Masquer" : "Commentaires");
            if (show) loadAdminComments(post, commentsZone);
        });

        bar.getChildren().addAll(nbCom, bToggle);
        wrap.getChildren().addAll(sep, bar, commentsZone);
        return wrap;
    }

    // ── Charger commentaires + formulaire d'ajout admin ───────────────────
    private void loadAdminComments(Post post, VBox zone) {
        zone.getChildren().clear();
        try {
            List<Commentaire> list = commentaireService.findByPost(post.getId());
            if (list.isEmpty())
                zone.getChildren().add(gray("Aucun commentaire."));
            else
                for (Commentaire c : list)
                    zone.getChildren().add(buildCommentRow(c, post, zone));
        } catch (SQLException e) {
            zone.getChildren().add(gray("Erreur : " + e.getMessage()));
        }
        // Formulaire ajout commentaire par l'admin
        zone.getChildren().add(buildAddCommentForm(post, zone));
    }

    private HBox buildCommentRow(Commentaire c, Post post, VBox zone) {
        HBox row = new HBox(8); row.setAlignment(Pos.TOP_LEFT); row.setPadding(new Insets(6, 8, 6, 8));
        row.setStyle("-fx-background-color:rgba(255,255,255,0.04);-fx-background-radius:8;");
        Label avl = av(c.getUser() != null ? c.getUser().getName() : "?", 28);
        VBox body = new VBox(2); HBox.setHgrow(body, Priority.ALWAYS);
        HBox meta = new HBox(6); meta.setAlignment(Pos.CENTER_LEFT);
        Label nm = new Label(c.getUser() != null ? c.getUser().getName() : "?");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:11;");
        Label dt = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#6e7681;-fx-font-size:10;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button menuBtn = menuButton();
        menuBtn.setOnAction(e -> showAdminCommentMenu(menuBtn, c, post, zone));
        meta.getChildren().addAll(nm, dt, sp, menuBtn);
        Label txt = new Label(c.getText()); txt.setWrapText(true);
        txt.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:11;");
        body.getChildren().addAll(meta, txt);
        row.getChildren().addAll(avl, body);
        return row;
    }

    // ── Formulaire ajout commentaire Admin ────────────────────────────────
    private VBox buildAddCommentForm(Post post, VBox zone) {
        VBox form = new VBox(6);
        form.setPadding(new Insets(8, 0, 4, 0));
        form.setStyle("-fx-border-color:#2a2b4a;-fx-border-width:1 0 0 0;-fx-padding:8 0 0 0;");

        Label lbl = new Label("\uD83D\uDCAC Ajouter un commentaire (Admin)");
        lbl.setStyle("-fx-text-fill:#8b949e;-fx-font-size:11;-fx-font-weight:bold;");

        TextField field = new TextField();
        field.setPromptText("Écrire un commentaire en tant qu'admin…");
        field.setStyle(
            "-fx-control-inner-background:#1a1a30;-fx-background-color:#1a1a30;" +
            "-fx-text-fill:#e6edf3;-fx-prompt-text-fill:#4b5563;" +
            "-fx-background-radius:8;-fx-border-color:#3a3a5c;-fx-border-radius:8;" +
            "-fx-padding:7 12;-fx-font-size:12;");

        Label errLbl = new Label("");
        errLbl.setStyle("-fx-text-fill:#ef4444;-fx-font-size:11;");

        Button btnSend = btn("Publier", "#c84cff");
        btnSend.setMaxWidth(Double.MAX_VALUE);

        Runnable submit = () -> {
            String text = field.getText().trim();
            errLbl.setText("");
            if (text.length() < 5)   { errLbl.setText("Min. 5 caractères."); return; }
            if (text.length() > 500) { errLbl.setText("Max. 500 caractères."); return; }
            User admin = SessionContext.getCurrentUser();
            if (admin == null) { errLbl.setText("Session expirée."); return; }
            try {
                Commentaire c = new Commentaire();
                c.setText(text); c.setPost(post); c.setUser(admin);
                c.setCreatedAt(java.time.LocalDateTime.now());
                commentaireService.create(c);
                field.clear();
                loadAdminComments(post, zone);
            } catch (SQLException ex) { errLbl.setText("Erreur BD : " + ex.getMessage()); }
        };

        btnSend.setOnAction(e -> submit.run());
        field.setOnAction(e -> submit.run());
        form.getChildren().addAll(lbl, field, errLbl, btnSend);
        return form;
    }

    // ── Menu ⋮ admin commentaire ──────────────────────────────────────────
    private void showAdminCommentMenu(Button anchor, Commentaire c, Post post, VBox zone) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color:#1f1f3a;-fx-border-color:#3a3a5c;-fx-border-width:1;-fx-background-radius:8;");
        MenuItem editItem = new MenuItem("  \u270F  Modifier");
        editItem.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:12;");
        editItem.setOnAction(e -> openEditComment(c, post, zone));
        MenuItem delItem = new MenuItem("  \uD83D\uDDD1  Supprimer");
        delItem.setStyle("-fx-text-fill:#ef4444;-fx-font-size:12;");
        delItem.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ce commentaire ?", ButtonType.YES, ButtonType.NO);
            a.setHeaderText("Confirmer"); a.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) {
                    try { commentaireService.delete(c.getId()); loadAdminComments(post, zone); }
                    catch (SQLException ex) { alert(ex.getMessage()); }
                }
            });
        });
        menu.getItems().addAll(editItem, new SeparatorMenuItem(), delItem);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ── Vue détail complète du post (Stage séparé) ────────────────────────
    private void openPostDetail(Post post) {
        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("Détail du post #" + post.getId());

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background:#111827;-fx-background-color:#111827;");

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:#111827;");

        // Image pleine largeur avec ratio préservé
        if (post.getImage() != null && !post.getImage().isBlank()) {
            Image img = loadLocalImg(post.getImage());
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(620);
                iv.setPreserveRatio(true);   // ← affichage complet
                iv.setSmooth(true);
                content.getChildren().add(iv);
            }
        }

        // Bloc auteur + stats
        VBox meta = new VBox(10); meta.setPadding(new Insets(16, 20, 14, 20));
        meta.setStyle("-fx-background-color:#1c1c2e;");
        HBox authorRow = new HBox(12); authorRow.setAlignment(Pos.CENTER_LEFT);
        Label avl = av(post.getUser() != null ? post.getUser().getName() : "?", 44);
        VBox ai = new VBox(3);
        Label nm = new Label(post.getUser() != null ? post.getUser().getName() : "Inconnu");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e6edf3;-fx-font-size:15;");
        Label em = new Label(post.getUser() != null ? post.getUser().getEmail() : "");
        em.setStyle("-fx-text-fill:#8b949e;-fx-font-size:12;");
        Label dt = new Label(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#8b949e;-fx-font-size:11;");
        ai.getChildren().addAll(nm, em, dt);
        authorRow.getChildren().addAll(avl, ai);
        // Stats likes + commentaires
        HBox stats = new HBox(20); stats.setAlignment(Pos.CENTER_LEFT);
        Label likes = new Label("\u2764 " + post.getLikesCount() + " like(s)");
        likes.setStyle("-fx-text-fill:#ff6b81;-fx-font-size:13;-fx-font-weight:bold;");
        int nbCom = 0;
        try { nbCom = commentaireService.findByPost(post.getId()).size(); } catch (Exception ignored) {}
        Label coms = new Label("\uD83D\uDCAC " + nbCom + " commentaire(s)");
        coms.setStyle("-fx-text-fill:#8b949e;-fx-font-size:13;");
        stats.getChildren().addAll(likes, coms);
        meta.getChildren().addAll(authorRow, stats);
        content.getChildren().add(meta);

        // Contenu texte complet
        VBox textBox = new VBox(10); textBox.setPadding(new Insets(16, 20, 14, 20));
        // Contenu avec style visuel
        Label textLbl = buildStyledLabel(post.getContent(), 2000);
        textLbl.setWrapText(true); textLbl.setMaxWidth(580);
        textBox.getChildren().add(textLbl);

        // Média URL si présent
        if (post.getMediaurl() != null && !post.getMediaurl().isBlank()) {
            String url = post.getMediaurl();
            switch (MediaHelper.detect(url)) {
                case YOUTUBE -> {
                    String embed = MediaHelper.toEmbedUrl(url);
                    if (embed != null) {
                        WebView wv = new WebView(); wv.setPrefWidth(580); wv.setPrefHeight(326);
                        wv.getEngine().load(embed); textBox.getChildren().add(wv);
                    }
                }
                case IMAGE_URL -> {
                    try {
                        Image imgU = new Image(url, 580, 0, true, true, true);
                        ImageView iv2 = new ImageView(imgU); iv2.setFitWidth(580); iv2.setPreserveRatio(true);
                        textBox.getChildren().add(iv2);
                    } catch (Exception ignored) {}
                }
                default -> {
                    Hyperlink lk = new Hyperlink("\uD83D\uDD17 " + url); lk.setStyle("-fx-text-fill:#58a6ff;");
                    textBox.getChildren().add(lk);
                }
            }
        }
        content.getChildren().add(textBox);

        // Titre commentaires
        Separator sepC = new Separator(); sepC.setStyle("-fx-background-color:#2a2b4a;");
        VBox.setMargin(sepC, new Insets(0, 20, 0, 20));
        Label comTitle = new Label("\uD83D\uDCAC Commentaires");
        comTitle.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#e6edf3;");
        VBox.setMargin(comTitle, new Insets(12, 20, 8, 20));
        content.getChildren().addAll(sepC, comTitle);

        // Liste commentaires
        VBox comList = new VBox(8); comList.setPadding(new Insets(0, 20, 16, 20));
        try {
            List<Commentaire> comments = commentaireService.findByPost(post.getId());
            if (comments.isEmpty()) {
                Label empty = new Label("Aucun commentaire pour ce post.");
                empty.setStyle("-fx-text-fill:#8b949e;-fx-font-size:13;");
                comList.getChildren().add(empty);
            } else {
                for (Commentaire c : comments) {
                    HBox comRow = new HBox(10); comRow.setAlignment(Pos.TOP_LEFT);
                    comRow.setPadding(new Insets(8, 12, 8, 12));
                    comRow.setStyle("-fx-background-color:#1e1e30;-fx-background-radius:10;" +
                                    "-fx-border-color:#2a2b4a;-fx-border-radius:10;-fx-border-width:1;");
                    Label avC = av(c.getUser() != null ? c.getUser().getName() : "?", 34);
                    VBox cbody = new VBox(3); HBox.setHgrow(cbody, Priority.ALWAYS);
                    Label cnm = new Label(c.getUser() != null ? c.getUser().getName() : "?");
                    cnm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:12;");
                    Label cdt = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
                    cdt.setStyle("-fx-text-fill:#6e7681;-fx-font-size:10;");
                    Label ctxt = new Label(c.getText()); ctxt.setWrapText(true);
                    ctxt.setStyle("-fx-text-fill:#e6edf3;-fx-font-size:13;");
                    cbody.getChildren().addAll(cnm, cdt, ctxt);
                    comRow.getChildren().addAll(avC, cbody);
                    comList.getChildren().add(comRow);
                }
            }
        } catch (SQLException e) { comList.getChildren().add(gray("Erreur : " + e.getMessage())); }
        content.getChildren().add(comList);

        scroll.setContent(content);
        Scene scene = new Scene(scroll, 640, 700);
        detailStage.setScene(scene);
        detailStage.show();
    }

    // ── Éditions ─────────────────────────────────────────────────────────
    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gamilha/interfaces/Admin/PostFormView.fxml"));
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle(post == null ? "Nouveau Post" : "Éditer Post #" + post.getId());
            s.setScene(new Scene(loader.load(), 540, 470));
            ((PostFormController) loader.getController()).init(post);
            s.showAndWait(); loadPosts();
        } catch (IOException e) { alert("Erreur : " + e.getMessage()); }
    }

    private void openEditComment(Commentaire c, Post post, VBox zone) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/gamilha/interfaces/Admin/CommentaireFormView.fxml"));
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle("Éditer Commentaire #" + c.getId());
            s.setScene(new Scene(loader.load(), 480, 270));
            ((CommentaireFormController) loader.getController()).init(c);
            s.showAndWait(); loadAdminComments(post, zone);
        } catch (IOException e) { alert("Erreur : " + e.getMessage()); }
    }

    private void confirmDeletePost(Post post) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer le post #" + post.getId() + " et tous ses commentaires ?", ButtonType.YES, ButtonType.NO);
        a.setHeaderText("Confirmer la suppression");
        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.YES) {
                try { postService.delete(post.getId()); loadPosts(); }
                catch (SQLException e) { alert("Erreur BD : " + e.getMessage()); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private Label av(String name, double sz) {
        String init = (name != null && name.length() >= 2) ? name.substring(0,2).toUpperCase() : "??";
        Label l = new Label(init); l.setMinSize(sz,sz); l.setMaxSize(sz,sz); l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color:#5b21b6;-fx-text-fill:white;-fx-font-weight:bold;" +
                   "-fx-font-size:"+(sz*0.35)+";-fx-background-radius:"+(sz/2)+";");
        return l;
    }

    private Button menuButton() {
        Button b = new Button("⋮");
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:#8b949e;-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:#252535;-fx-text-fill:#c9d1d9;-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;-fx-background-radius:6;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:transparent;-fx-text-fill:#8b949e;-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;"));
        return b;
    }

    public Image loadLocalImg(String fn) {
        for (String p : new String[]{UPLOADS+fn, UPLOADS+"images/"+fn}) {
            File f = new File(p); if (f.exists()) return new Image(f.toURI().toString());
        }
        return null;
    }

    private Hyperlink linkLabel(String url) {
        Hyperlink lk = new Hyperlink("\uD83D\uDD17 Ouvrir le lien"); lk.setStyle("-fx-text-fill:#58a6ff;-fx-font-size:11;");
        lk.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); } catch (Exception ignored) {} });
        return lk;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:"+color+";-fx-text-fill:white;-fx-background-radius:6;-fx-font-size:11;-fx-padding:4 10;-fx-cursor:hand;");
        return b;
    }

    private Label gray(String msg) { Label l = new Label(msg); l.setStyle("-fx-text-fill:#8b949e;-fx-font-size:11;"); return l; }
    private String trunc(String s, int max) { return s == null ? "" : s.length() > max ? s.substring(0, max) + "…" : s; }
    private void alert(String msg) { Alert a = new Alert(Alert.AlertType.ERROR); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }

    @FXML void onRefresh() { loadPosts(); }
    @FXML void onNewPost() { openEditPost(null); }
}
