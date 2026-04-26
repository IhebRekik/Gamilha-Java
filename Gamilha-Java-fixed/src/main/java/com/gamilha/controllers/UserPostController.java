package com.gamilha.controllers;

import com.gamilha.entity.Commentaire;
import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.*;
import com.gamilha.services.PostService;
import com.gamilha.utils.SessionContext;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
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
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class UserPostController implements Initializable {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final double FEED_WIDTH = 660.0;

    @FXML private ScrollPane mainScroll;
    @FXML private VBox  feedBox;
    @FXML private Label welcomeLabel;
    @FXML private VBox  suggestionsBox;
    @FXML private VBox  amisBox;

    private final PostService        postService        = new PostService();
    private final CommentaireService commentaireService = new CommentaireService();
    private final FriendService      friendService      = new FriendService();
    private User currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // SCROLL FIX : lier au Stage.heightProperty (fenêtre maximisée)
        // sceneProperty est trop tôt, on utilise windowProperty
        if (mainScroll != null) {
            mainScroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getWindow() != null) {
                    bindScroll(newScene.getWindow());
                } else if (newScene != null) {
                    newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                        if (newWin != null) bindScroll(newWin);
                    });
                }
            });
        }
    }

    private void bindScroll(javafx.stage.Window window) {
        // Soustraire hauteur navbar (~110px) et sous-menu (~36px)
        mainScroll.prefHeightProperty().bind(window.heightProperty().subtract(146));
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null && welcomeLabel != null)
            welcomeLabel.setText("Bonjour,  " + user.getName() + " 👋");
        loadFeed();
        loadSidebar();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  FIL INTELLIGENT (Smart Feed)
    // ══════════════════════════════════════════════════════════════════════
    void loadFeed() {
        feedBox.getChildren().clear();
        if (currentUser == null) return;
        try {
            List<Post> posts = postService.findSmartFeed(currentUser.getId());
            if (posts.isEmpty())
                feedBox.getChildren().add(emptyLabel("Aucun post pour le moment."));
            else
                for (Post p : posts) feedBox.getChildren().add(buildCard(p));
        } catch (SQLException e) {
            feedBox.getChildren().add(emptyLabel("Erreur : " + e.getMessage()));
        }
    }

    // ── CARD ─────────────────────────────────────────────────────────────
    private VBox buildCard(Post post) {
        VBox card = new VBox(0);
        // La card prend toute la largeur du feedBox (pas de prefWidth fixe)
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(card, javafx.scene.layout.Priority.NEVER);
        String bc = post.isFriendPost() ? "#8b5cf6" : "#2a2a40";
        String bw = post.isFriendPost() ? "2" : "1";
        card.setStyle(
            "-fx-background-color:#0d0d1a;-fx-background-radius:14;" +
            "-fx-border-radius:14;-fx-border-color:" + bc + ";-fx-border-width:" + bw + ";");
        card.getChildren().addAll(
            buildHeader(post),
            buildContent(post),
            buildMedia(post),
            buildStats(post),
            sep(),
            buildComments(post),
            buildCommentInput(post)
        );
        return card;
    }

    // ── HEADER ───────────────────────────────────────────────────────────
    private HBox buildHeader(Post post) {
        HBox h = new HBox(10);
        h.setPadding(new Insets(14, 16, 8, 16));
        h.setAlignment(Pos.CENTER_LEFT);

        Label av = av(post.getUser() != null ? post.getUser().getName() : "?", 42);
        VBox info = new VBox(3); HBox.setHgrow(info, Priority.ALWAYS);

        HBox nameRow = new HBox(8); nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nm = new Label(post.getUser() != null ? post.getUser().getName() : "Inconnu");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e2e8f0;-fx-font-size:14;");
        nameRow.getChildren().add(nm);

        if (post.isFriendPost()) {
            Label badge = new Label("👥 Ami");
            badge.setStyle("-fx-background-color:rgba(139,92,246,0.2);-fx-text-fill:#a78bfa;" +
                           "-fx-background-radius:10;-fx-padding:2 7;-fx-font-size:10;-fx-font-weight:bold;");
            nameRow.getChildren().add(badge);
        }
        if (post.getSharedFromId() != null) {
            Label shareTag = new Label("🔁 Partagé");
            shareTag.setStyle("-fx-background-color:rgba(59,130,246,0.15);-fx-text-fill:#60a5fa;" +
                              "-fx-background-radius:10;-fx-padding:2 7;-fx-font-size:10;");
            nameRow.getChildren().add(shareTag);
        }

        Label dt = new Label(post.getCreatedAt() != null ? post.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#475569;-fx-font-size:11;");
        info.getChildren().addAll(nameRow, dt);
        h.getChildren().addAll(av, info);

        if (isOwner(post.getUser())) {
            Button mb = menuBtn();
            mb.setOnAction(e -> showPostMenu(mb, post));
            h.getChildren().add(mb);
        }
        return h;
    }

    private void showPostMenu(Button anchor, Post post) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color:#1a1e2e;-fx-border-color:#3a3a5c;" +
                      "-fx-border-width:1;-fx-background-radius:8;");
        MenuItem edit = new MenuItem("  ✏  Modifier");
        edit.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:12;");
        edit.setOnAction(e -> openEditPost(post));
        MenuItem del = new MenuItem("  🗑  Supprimer");
        del.setStyle("-fx-text-fill:#f87171;-fx-font-size:12;");
        del.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette publication ?", ButtonType.YES, ButtonType.NO);
            a.setHeaderText("Confirmer"); a.showAndWait().ifPresent(b -> {
                if (b == ButtonType.YES) { try { postService.delete(post.getId()); loadFeed(); }
                    catch (SQLException ex) { toast(ex.getMessage()); } }
            });
        });
        menu.getItems().addAll(edit, new SeparatorMenuItem(), del);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    // ── CONTENT : texte stylé + post partagé (comme FB) + grille images ─
    private VBox buildContent(Post post) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(0, 16, 8, 16));

        // Commentaire du partage (ou texte normal)
        String raw = post.getContent() != null
            ? post.getContent().replaceAll("<[^>]+>", "").trim() : "";
        // N'afficher le texte que s'il y a quelque chose à dire
        if (!raw.isBlank()) {
            box.getChildren().add(styledLabel(raw, null));
        }

        // ── POST PARTAGÉ (comme Facebook) ────────────────────────────────
        // Chercher le post original : d'abord sharedPost chargé, sinon via sharedFromId
        Post orig = post.getSharedPost();
        if (orig == null && post.getSharedFromId() != null) {
            try { orig = postService.findById(post.getSharedFromId()); }
            catch (Exception ignored) {}
        }

        if (orig != null) {
            final Post origFinal = orig;
            VBox sharedBox = new VBox(0);
            sharedBox.setMaxWidth(FEED_WIDTH - 32);
            sharedBox.setStyle(
                "-fx-background-color:#060614;-fx-background-radius:12;" +
                "-fx-border-color:#7c3aed;-fx-border-radius:12;-fx-border-width:1.5;");

            // En-tête auteur original
            HBox origHeader = new HBox(10);
            origHeader.setAlignment(Pos.CENTER_LEFT);
            origHeader.setPadding(new Insets(12, 14, 8, 14));
            Label origAv = av(origFinal.getUser() != null ? origFinal.getUser().getName() : "?", 34);
            VBox origInfo = new VBox(2);
            HBox.setHgrow(origInfo, Priority.ALWAYS);
            Label origNm = new Label(origFinal.getUser() != null ? origFinal.getUser().getName() : "?");
            origNm.setStyle("-fx-font-weight:bold;-fx-text-fill:#a78bfa;-fx-font-size:13;");
            Label origDt = new Label(origFinal.getCreatedAt() != null
                ? origFinal.getCreatedAt().format(FMT) : "");
            origDt.setStyle("-fx-text-fill:#475569;-fx-font-size:10;");
            origInfo.getChildren().addAll(origNm, origDt);
            origHeader.getChildren().addAll(origAv, origInfo);

            // Contenu original avec style visuel
            String origRaw = origFinal.getContent() != null ? origFinal.getContent() : "";
            VBox origBody = new VBox(8);
            origBody.setPadding(new Insets(0, 14, 12, 14));

            if (!origRaw.isBlank()) {
                Label origContent = styledLabel(origRaw, null);
                origContent.setMaxWidth(FEED_WIDTH - 60);
                origBody.getChildren().add(origContent);
            }

            // Images du post original (grille)
            List<String> origImgs = origFinal.getAllImages();
            if (!origImgs.isEmpty()) {
                double origW = FEED_WIDTH - 64;
                javafx.scene.Node imgGrid = buildImageGrid(origImgs, origW);
                origBody.getChildren().add(imgGrid);
            }

            // Média URL du post original
            String origUrl = origFinal.getMediaurl();
            if (origUrl != null && !origUrl.isBlank()) {
                switch (MediaHelper.detect(origUrl)) {
                    case IMAGE_URL -> {
                        try {
                            ImageView iv = new ImageView(new Image(origUrl, FEED_WIDTH-64, 0, true, true, true));
                            iv.setFitWidth(FEED_WIDTH-64); iv.setPreserveRatio(true);
                            origBody.getChildren().add(iv);
                        } catch (Exception ignored) {}
                    }
                    default -> {}
                }
            }

            // Stats du post original (likes)
            Label origStats = new Label("❤ " + origFinal.getLikesCount() + " like(s)");
            origStats.setStyle("-fx-text-fill:#475569;-fx-font-size:11;-fx-padding:0 0 4 0;");
            origBody.getChildren().add(origStats);

            sharedBox.getChildren().addAll(origHeader,
                new Separator() {{ setStyle("-fx-background-color:#2a2a40;"); }},
                origBody);
            box.getChildren().add(sharedBox);
        }

        // Grille images Instagram-like
        List<String> imgs = post.getAllImages();
        if (!imgs.isEmpty()) {
            box.getChildren().add(buildImageGrid(imgs));
        }
        return box;
    }

    /**
     * Grille d'images comme Instagram :
     * 1 image → pleine largeur
     * 2 images → côte à côte 50/50
     * 3 images → 1 grande à gauche + 2 petites à droite
     * 4+ images → grille 2×2
     */
    private javafx.scene.Node buildImageGrid(List<String> imgs) {
        return buildImageGrid(imgs, FEED_WIDTH - 32);
    }

    private javafx.scene.Node buildImageGrid(List<String> imgs, double W) {

        if (imgs.size() == 1) {
            Image img = localImg(imgs.get(0));
            if (img == null) return new HBox();
            ImageView iv = new ImageView(img);
            iv.setFitWidth(W);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        }

        if (imgs.size() == 2) {
            HBox row = new HBox(4);
            for (String name : imgs) {
                Image img = localImg(name);
                if (img == null) continue;
                ImageView iv = new ImageView(img);
                double w = (W - 4) / 2.0;
                iv.setFitWidth(w);
                iv.setFitHeight(w * 0.75);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
                iv.setClip(roundedClip(w, w * 0.75));
                row.getChildren().add(iv);
            }
            return row;
        }

        if (imgs.size() == 3) {
            // Gauche grande + droite 2 petites
            HBox row = new HBox(4);
            Image img0 = localImg(imgs.get(0));
            if (img0 != null) {
                double bigW = W * 0.60;
                double bigH = bigW * 0.75;
                ImageView iv = new ImageView(img0);
                iv.setFitWidth(bigW); iv.setFitHeight(bigH);
                iv.setPreserveRatio(false); iv.setSmooth(true);
                iv.setClip(roundedClip(bigW, bigH));
                row.getChildren().add(iv);
            }
            VBox rightCol = new VBox(4);
            for (int i = 1; i <= 2 && i < imgs.size(); i++) {
                Image img = localImg(imgs.get(i));
                if (img == null) continue;
                double sw = W * 0.40 - 4;
                double sh = (W * 0.60 * 0.75 - 4) / 2.0;
                ImageView iv = new ImageView(img);
                iv.setFitWidth(sw); iv.setFitHeight(sh);
                iv.setPreserveRatio(false); iv.setSmooth(true);
                iv.setClip(roundedClip(sw, sh));
                rightCol.getChildren().add(iv);
            }
            row.getChildren().add(rightCol);
            return row;
        }

        // 4+ images → grille 2×2
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(4); grid.setVgap(4);
        double cw = (W - 4) / 2.0;
        double ch = cw * 0.65;
        int col = 0, gridRow = 0;
        for (int i = 0; i < Math.min(imgs.size(), 4); i++) {
            Image img = localImg(imgs.get(i));
            if (img == null) { col++; if (col >= 2) { col = 0; gridRow++; } continue; }
            ImageView iv = new ImageView(img);
            iv.setFitWidth(cw); iv.setFitHeight(ch);
            iv.setPreserveRatio(false); iv.setSmooth(true);
            iv.setClip(roundedClip(cw, ch));
            // Badge "+N" sur la dernière si plus de 4
            if (i == 3 && imgs.size() > 4) {
                StackPane sp = new StackPane(iv);
                Label more = new Label("+" + (imgs.size() - 4));
                more.setStyle("-fx-background-color:rgba(0,0,0,0.55);-fx-text-fill:white;" +
                              "-fx-font-size:18;-fx-font-weight:bold;");
                sp.getChildren().add(more);
                grid.add(sp, col, gridRow);
            } else {
                grid.add(iv, col, gridRow);
            }
            col++; if (col >= 2) { col = 0; gridRow++; }
        }
        return grid;
    }

    private javafx.scene.shape.Rectangle roundedClip(double w, double h) {
        javafx.scene.shape.Rectangle r = new javafx.scene.shape.Rectangle(w, h);
        r.setArcWidth(10); r.setArcHeight(10); return r;
    }

    /**
     * Label avec style visuel — le style est lu depuis le PRÉFIXE du contenu.
     * Ex : "[GRAS]Mon texte" → texte en gras, préfixe retiré.
     */
    private Label styledLabel(String rawContent, String ignoredStyle) {
        // Le style est encodé dans le contenu avec PostService
        String style = PostService.extractStyle(rawContent);
        String clean = PostService.stripStylePrefix(rawContent);

        Label l = new Label(clean);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.setStyle(switch (style) {
            case "Gras"     -> "-fx-text-fill:#f1f5f9;-fx-font-size:14;-fx-font-weight:bold;";
            case "Italique" -> "-fx-text-fill:#c9d1d9;-fx-font-size:13;-fx-font-style:italic;";
            case "Code"     -> "-fx-text-fill:#86efac;-fx-font-family:'Courier New',monospace;-fx-font-size:12;" +
                               "-fx-background-color:#0d1f17;-fx-background-radius:6;-fx-padding:6 10;";
            case "Citation" -> "-fx-text-fill:#94a3b8;-fx-font-size:13;-fx-font-style:italic;" +
                               "-fx-border-color:#7c3aed;-fx-border-width:0 0 0 3;-fx-padding:4 12;";
            default         -> "-fx-text-fill:#c9d1d9;-fx-font-size:13;-fx-line-spacing:2;";
        });
        return l;
    }

    // ── MEDIA ─────────────────────────────────────────────────────────────
    private VBox buildMedia(Post post) {
        VBox box = new VBox();
        String url = post.getMediaurl();
        if (url == null || url.isBlank()) return box;
        box.setPadding(new Insets(0, 16, 8, 16));
        switch (MediaHelper.detect(url)) {
            case YOUTUBE -> {
                String embed = MediaHelper.toEmbedUrl(url);
                if (embed != null) {
                    WebView wv = new WebView();
                    wv.setPrefSize(FEED_WIDTH - 32, (FEED_WIDTH - 32) * 9 / 16);
                    wv.getEngine().load(embed);
                    box.getChildren().add(wv);
                }
            }
            case IMAGE_URL -> {
                try {
                    Image img = new Image(url, FEED_WIDTH - 32, 0, true, true, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(FEED_WIDTH - 32); iv.setPreserveRatio(true);
                    box.getChildren().add(iv);
                } catch (Exception ex) { box.getChildren().add(linkLabel(url)); }
            }
            case LINK -> box.getChildren().add(linkLabel(url));
            default -> {}
        }
        return box;
    }

    // ── STATS : Like + Commentaires + Share + Translate + Trending ────────
    private HBox buildStats(Post post) {
        HBox h = new HBox(10);
        h.setPadding(new Insets(8, 16, 6, 16));
        h.setAlignment(Pos.CENTER_LEFT);

        // LIKE interactif
        boolean liked;
        try { liked = postService.isLikedByUser(post.getId(), currentUser.getId()); }
        catch (Exception ex) { liked = false; }
        final boolean[] isLiked = {liked};
        final int[]     nbLikes = {post.getLikesCount()};
        Button likeBtn = new Button();
        updateLikeBtn(likeBtn, isLiked[0], nbLikes[0]);
        likeBtn.setOnAction(e -> {
            try {
                boolean nowLiked = postService.toggleLike(post.getId(), currentUser.getId());
                isLiked[0] = nowLiked;
                nbLikes[0] = postService.countLikes(post.getId());
                post.setLikesCount(nbLikes[0]);
                updateLikeBtn(likeBtn, isLiked[0], nbLikes[0]);
                likeBtn.setScaleX(1.3); likeBtn.setScaleY(1.3);
                PauseTransition pt = new PauseTransition(Duration.millis(150));
                pt.setOnFinished(ev -> { likeBtn.setScaleX(1.0); likeBtn.setScaleY(1.0); });
                pt.play();
            } catch (SQLException ex) { toast("Erreur : " + ex.getMessage()); }
        });

        // Commentaires
        int nbCom = 0;
        try { nbCom = commentaireService.findByPost(post.getId()).size(); } catch (Exception ignored) {}
        Label cm = new Label("💬  " + nbCom);
        cm.setStyle("-fx-text-fill:#64748b;-fx-font-size:12;");

        // SHARE
        Button shareBtn = iconBtn("🔁", "#60a5fa");
        shareBtn.setTooltip(new Tooltip("Partager ce post"));
        shareBtn.setOnAction(e -> openShareDialog(post));

        // TRADUCTION avec menu langue stylé
        MenuButton translateBtn = new MenuButton("🌐");
        translateBtn.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#64748b;" +
            "-fx-font-size:15;-fx-cursor:hand;-fx-padding:2 6;" +
            "-fx-border-color:transparent;");
        translateBtn.setTooltip(new Tooltip("Traduire le post"));

        String[][] langs = {
            {"🇫🇷  Français",  "fr"},
            {"🇬🇧  English",   "en"},
            {"🇸🇦  العربية",  "ar"},
            {"🇪🇸  Español",   "es"},
            {"🇩🇪  Deutsch",   "de"}
        };
        for (String[] lang : langs) {
            MenuItem item = new MenuItem(lang[0]);
            // Texte lisible sur fond sombre
            item.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:13;-fx-padding:4 8;");
            String code = lang[1];
            item.setOnAction(e -> translatePost(post, code));
            translateBtn.getItems().add(item);
        }
        // Style du popup du MenuButton
        translateBtn.showingProperty().addListener((obs, ov, nv) -> {
            if (nv) {
                translateBtn.getScene().lookup(".context-menu");
            }
        });

        h.getChildren().addAll(likeBtn, cm, shareBtn, translateBtn);

        // Badges
        // Trending : post très liké (top) OU post d'un ami avec likes
        // Adaptatif : trending si le post a des likes ET est soit récent soit d'un ami
        boolean isTrending = post.getLikesCount() >= 2
            || (post.isFriendPost() && post.getLikesCount() >= 1)
            || post.getScore() > 1.5;
        if (isTrending) {
            Label t = new Label("🔥 Trending");
            t.setStyle("-fx-background-color:rgba(251,146,60,0.15);-fx-text-fill:#fb923c;" +
                       "-fx-background-radius:10;-fx-padding:2 8;-fx-font-size:11;-fx-font-weight:bold;" +
                       "-fx-border-color:rgba(251,146,60,0.3);-fx-border-radius:10;-fx-border-width:1;");
            h.getChildren().add(t);
        }
        return h;
    }

    private void updateLikeBtn(Button btn, boolean liked, int count) {
        if (liked) {
            btn.setText("❤  " + count);
            btn.setStyle("-fx-background-color:rgba(244,63,94,0.15);-fx-text-fill:#f43f5e;" +
                         "-fx-background-radius:20;-fx-padding:5 14;-fx-font-size:13;" +
                         "-fx-font-weight:bold;-fx-cursor:hand;" +
                         "-fx-border-color:#f43f5e;-fx-border-radius:20;-fx-border-width:1;");
        } else {
            btn.setText("🤍  " + count);
            btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;" +
                         "-fx-background-radius:20;-fx-padding:5 14;-fx-font-size:13;" +
                         "-fx-cursor:hand;-fx-border-color:#3a3a5c;" +
                         "-fx-border-radius:20;-fx-border-width:1;");
        }
    }

    // ── SHARE dialog ──────────────────────────────────────────────────────
    private void openShareDialog(Post post) {
        Stage dlgStage = new Stage();
        dlgStage.initModality(Modality.APPLICATION_MODAL);
        dlgStage.setTitle("🔁 Partager ce post");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color:#0a0a0f;");
        root.setPrefWidth(460);

        Label title = new Label("🔁 Partager le post de " +
            (post.getUser() != null ? post.getUser().getName() : "?"));
        title.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");

        // Aperçu du post original
        VBox preview = new VBox(6);
        preview.setPadding(new Insets(10));
        preview.setStyle("-fx-background-color:#0d0d1a;-fx-background-radius:8;" +
                         "-fx-border-color:#7c3aed;-fx-border-radius:8;-fx-border-width:1;");
        String origTxt = post.getContent() != null
            ? post.getContent().replaceAll("<[^>]+>","").trim() : "";
        Label previewLbl = new Label(origTxt.length() > 100 ? origTxt.substring(0,100)+"…" : origTxt);
        previewLbl.setWrapText(true);
        previewLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");
        preview.getChildren().add(previewLbl);

        Label commentLbl = new Label("Ajouter un commentaire (optionnel)");
        commentLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");

        TextField commentField = new TextField();
        commentField.setPromptText("Votre commentaire...");
        commentField.setStyle(
            "-fx-control-inner-background:#1a1a26;-fx-background-color:#1a1a26;" +
            "-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#475569;" +
            "-fx-background-radius:8;-fx-border-color:#3a3a5c;-fx-border-radius:8;" +
            "-fx-padding:9 12;-fx-font-size:13;");

        HBox btns = new HBox(10); btns.setAlignment(Pos.CENTER_RIGHT);
        Button cancel = new Button("Annuler");
        cancel.setStyle("-fx-background-color:#21273a;-fx-text-fill:#94a3b8;" +
                        "-fx-background-radius:8;-fx-padding:8 20;-fx-cursor:hand;");
        cancel.setOnAction(e -> dlgStage.close());

        Button share = new Button("🔁 Partager");
        share.setStyle("-fx-background-color:linear-gradient(to right,#3b82f6,#60a5fa);" +
                       "-fx-text-fill:white;-fx-background-radius:8;-fx-padding:8 20;" +
                       "-fx-font-weight:bold;-fx-cursor:hand;");
        share.setOnAction(e -> {
            try {
                postService.sharePost(post.getId(), currentUser, commentField.getText().trim());
                dlgStage.close();
                toast("✅ Post partagé !");
                loadFeed();
            } catch (SQLException ex) { toast("Erreur : " + ex.getMessage()); }
        });
        btns.getChildren().addAll(cancel, share);

        root.getChildren().addAll(title, preview, commentLbl, commentField, btns);
        dlgStage.setScene(new Scene(root));
        dlgStage.show();
    }

    // ── TRADUCTION avec fenêtre lisible ───────────────────────────────────
    private void translatePost(Post post, String langCode) {
        String raw = post.getContent() != null
            ? post.getContent().replaceAll("<[^>]+>","").trim() : "";

        Task<String> task = new Task<>() {
            @Override protected String call() { return PostService.translateText(raw, langCode); }
        };
        task.setOnSucceeded(ev -> {
            String translated = task.getValue();

            Stage win = new Stage();
            win.initModality(Modality.APPLICATION_MODAL);
            String langName = switch (langCode) {
                case "en" -> "English 🇬🇧"; case "ar" -> "العربية 🇸🇦";
                case "es" -> "Español 🇪🇸";  case "de" -> "Deutsch 🇩🇪";
                default   -> "Français 🇫🇷";
            };
            win.setTitle("Traduction — " + langName);

            VBox root = new VBox(14);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color:#0a0a0f;");
            root.setPrefWidth(460);

            Label titleLbl = new Label("🌐 Traduction — " + langName);
            titleLbl.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");

            // Texte traduit — fond sombre, texte blanc
            TextArea ta = new TextArea(translated);
            ta.setWrapText(true); ta.setEditable(false); ta.setPrefHeight(160);
            ta.setStyle(
                "-fx-control-inner-background:#0d0d1a;" +
                "-fx-background-color:#0d0d1a;" +
                "-fx-text-fill:#e2e8f0;" +
                "-fx-font-size:14;" +
                "-fx-border-color:#3a3a5c;-fx-border-radius:8;-fx-background-radius:8;");
            // Forcer le style du nœud interne
            javafx.application.Platform.runLater(() -> {
                javafx.scene.Node n = ta.lookup(".content");
                if (n != null) n.setStyle("-fx-background-color:#0d0d1a;-fx-control-inner-background:#0d0d1a;");
            });

            Button close = new Button("Fermer");
            close.setStyle("-fx-background-color:#21273a;-fx-text-fill:#e2e8f0;" +
                           "-fx-background-radius:8;-fx-padding:8 24;-fx-cursor:hand;" +
                           "-fx-max-width:Infinity;");
            close.setMaxWidth(Double.MAX_VALUE);
            close.setOnAction(e -> win.close());

            root.getChildren().addAll(titleLbl, ta, close);
            win.setScene(new Scene(root));
            win.show();
        });
        new Thread(task).start();
    }

    // ── COMMENTAIRES ─────────────────────────────────────────────────────
    private VBox buildComments(Post post) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(4, 16, 4, 16));
        try {
            List<Commentaire> comments = commentaireService.findByPost(post.getId());
            post.setCommentaires(comments);
            for (Commentaire c : comments) box.getChildren().add(buildCommentRow(c, post));
        } catch (SQLException e) { box.getChildren().add(emptyLabel("Erreur : " + e.getMessage())); }
        return box;
    }

    private HBox buildCommentRow(Commentaire c, Post post) {
        HBox row = new HBox(8); row.setAlignment(Pos.TOP_LEFT); row.setPadding(new Insets(3, 0, 3, 0));
        Label av = av(c.getUser() != null ? c.getUser().getName() : "?", 30);
        VBox body = new VBox(3); HBox.setHgrow(body, Priority.ALWAYS);
        body.setStyle("-fx-background-color:rgba(255,255,255,0.04);-fx-background-radius:10;-fx-padding:6 10;");
        HBox meta = new HBox(6); meta.setAlignment(Pos.CENTER_LEFT);
        Label nm = new Label(c.getUser() != null ? c.getUser().getName() : "?");
        nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#c9d1d9;-fx-font-size:12;");
        Label dt = new Label(c.getCreatedAt() != null ? c.getCreatedAt().format(FMT) : "");
        dt.setStyle("-fx-text-fill:#475569;-fx-font-size:10;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        meta.getChildren().addAll(nm, dt, sp);
        if (isOwner(c.getUser())) {
            Button mb = menuBtn();
            mb.setOnAction(e -> {
                ContextMenu cm2 = new ContextMenu();
                cm2.setStyle("-fx-background-color:#1a1e2e;-fx-border-color:#3a3a5c;-fx-background-radius:8;");
                MenuItem edit = new MenuItem("  ✏  Modifier");
                edit.setStyle("-fx-text-fill:#e2e8f0;");
                edit.setOnAction(ev -> openEditComment(c));
                MenuItem del = new MenuItem("  🗑  Supprimer");
                del.setStyle("-fx-text-fill:#f87171;");
                del.setOnAction(ev -> {
                    Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer ?", ButtonType.YES, ButtonType.NO);
                    a.showAndWait().ifPresent(b -> {
                        if (b == ButtonType.YES) { try { commentaireService.delete(c.getId()); loadFeed(); }
                            catch (SQLException ex) { toast(ex.getMessage()); } }
                    });
                });
                cm2.getItems().addAll(edit, new SeparatorMenuItem(), del);
                cm2.show(mb, javafx.geometry.Side.BOTTOM, 0, 4);
            });
            meta.getChildren().add(mb);
        }
        Label txt = new Label(c.getText()); txt.setWrapText(true);
        txt.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:12;");
        body.getChildren().addAll(meta, txt);
        row.getChildren().addAll(av, body);
        return row;
    }

    private HBox buildCommentInput(Post post) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 16, 14, 16));
        Label av = av(currentUser != null ? currentUser.getName() : "?", 32);
        TextField field = new TextField();
        field.setPromptText("Écrire un commentaire…");
        field.setStyle("-fx-control-inner-background:#1a1e2e;-fx-background-color:#1a1e2e;" +
                       "-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#475569;" +
                       "-fx-background-radius:20;-fx-border-color:#3a3a5c;" +
                       "-fx-border-radius:20;-fx-padding:7 14;-fx-font-size:12;");
        HBox.setHgrow(field, Priority.ALWAYS);
        Button send = new Button("➤");
        send.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;" +
                      "-fx-background-radius:50;-fx-padding:7 12;-fx-cursor:hand;-fx-font-size:13;");
        Runnable submit = () -> {
            String text = field.getText().trim();
            if (text.length() < 5) { toast("Min. 5 caractères."); return; }
            if (text.length() > 500) { toast("Max. 500 caractères."); return; }
            if (currentUser == null) { toast("Non connecté."); return; }
            try {
                Commentaire com = new Commentaire();
                com.setText(text); com.setPost(post); com.setUser(currentUser);
                com.setCreatedAt(java.time.LocalDateTime.now());
                commentaireService.create(com);
                field.clear(); loadFeed();
            } catch (SQLException e) { toast("Erreur : " + e.getMessage()); }
        };
        send.setOnAction(e -> submit.run());
        field.setOnAction(e -> submit.run());
        row.getChildren().addAll(av, field, send);
        return row;
    }

    // ── SIDEBAR ──────────────────────────────────────────────────────────
    private void loadSidebar() { loadSuggestions(); loadAmis(); }

    private void loadSuggestions() {
        if (suggestionsBox == null || currentUser == null) return;
        suggestionsBox.getChildren().clear();
        try {
            List<User> list = friendService.findSuggestions(currentUser.getId(), 5);
            if (list.isEmpty()) {
                Label lbl = emptyLabel("Aucune suggestion pour le moment.");
                lbl.setPadding(new Insets(8, 14, 8, 14));
                suggestionsBox.getChildren().add(lbl);
                return;
            }
            for (User u : list) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 14, 8, 14));
                row.setStyle("-fx-cursor:default;");
                // Hover
                row.setOnMouseEntered(ev -> row.setStyle("-fx-background-color:rgba(139,92,246,0.07);-fx-cursor:default;"));
                row.setOnMouseExited(ev -> row.setStyle("-fx-cursor:default;"));

                Label avl = av(u.getName(), 40);
                VBox info = new VBox(2); HBox.setHgrow(info, Priority.ALWAYS);
                Label nm = new Label(u.getName());
                nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e2e8f0;-fx-font-size:13;");
                Label sub = new Label("Pas de jeu commun");
                sub.setStyle("-fx-text-fill:#475569;-fx-font-size:11;");
                info.getChildren().addAll(nm, sub);

                // Bouton + rond comme image 2
                Button add = new Button("+");
                add.setMinSize(30, 30); add.setMaxSize(30, 30);
                add.setStyle(
                    "-fx-background-color:linear-gradient(to bottom,#3b82f6,#2563eb);" +
                    "-fx-text-fill:white;-fx-background-radius:15;" +
                    "-fx-font-size:16;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:0;");
                add.setOnMouseEntered(ev -> add.setStyle(
                    "-fx-background-color:linear-gradient(to bottom,#60a5fa,#3b82f6);" +
                    "-fx-text-fill:white;-fx-background-radius:15;" +
                    "-fx-font-size:16;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:0;"));
                add.setOnMouseExited(ev -> add.setStyle(
                    "-fx-background-color:linear-gradient(to bottom,#3b82f6,#2563eb);" +
                    "-fx-text-fill:white;-fx-background-radius:15;" +
                    "-fx-font-size:16;-fx-font-weight:bold;-fx-cursor:hand;-fx-padding:0;"));
                add.setOnAction(e -> {
                    try { friendService.addFriend(currentUser.getId(), u.getId()); loadSidebar(); loadFeed(); }
                    catch (SQLException ex) { toast("Erreur : " + ex.getMessage()); }
                });
                row.getChildren().addAll(avl, info, add);
                suggestionsBox.getChildren().add(row);
            }
        } catch (SQLException e) {
            suggestionsBox.getChildren().add(emptyLabel("Erreur : " + e.getMessage()));
        }
    }

    private void loadAmis() {
        if (amisBox == null || currentUser == null) return;
        amisBox.getChildren().clear();
        try {
            List<User> friends = friendService.findFriends(currentUser.getId());
            if (friends.isEmpty()) {
                Label lbl = emptyLabel("Aucun ami pour le moment.");
                lbl.setPadding(new Insets(8, 14, 8, 14));
                amisBox.getChildren().add(lbl);
                return;
            }
            java.util.Map<Integer,Boolean> onlineMap = friendService.getOnlineStatus(friends);
            for (User u : friends) {
                boolean online = onlineMap.getOrDefault(u.getId(), false);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 14, 8, 14));
                row.setOnMouseEntered(ev -> row.setStyle("-fx-background-color:rgba(34,197,94,0.06);"));
                row.setOnMouseExited(ev -> row.setStyle(""));

                // Avatar + point statut
                StackPane sp = new StackPane();
                Label avl = av(u.getName(), 38);
                Label dot = new Label();
                dot.setMinSize(10, 10); dot.setMaxSize(10, 10);
                dot.setStyle(online
                    ? "-fx-background-color:#22c55e;-fx-background-radius:5;" +
                      "-fx-border-color:#0a0a0f;-fx-border-width:1.5;-fx-border-radius:5;"
                    : "-fx-background-color:#475569;-fx-background-radius:5;" +
                      "-fx-border-color:#0a0a0f;-fx-border-width:1.5;-fx-border-radius:5;");
                StackPane.setAlignment(dot, Pos.BOTTOM_RIGHT);
                sp.getChildren().addAll(avl, dot);

                VBox info = new VBox(2); HBox.setHgrow(info, Priority.ALWAYS);
                Label nm = new Label(u.getName());
                nm.setStyle("-fx-font-weight:bold;-fx-text-fill:#e2e8f0;-fx-font-size:13;");
                Label st = new Label(online ? "● En ligne" : "● Hors ligne");
                st.setStyle(online
                    ? "-fx-text-fill:#22c55e;-fx-font-size:11;-fx-font-weight:bold;"
                    : "-fx-text-fill:#475569;-fx-font-size:11;");
                info.getChildren().addAll(nm, st);
                row.getChildren().addAll(sp, info);
                amisBox.getChildren().add(row);
            }
        } catch (SQLException e) {
            amisBox.getChildren().add(emptyLabel("Erreur : " + e.getMessage()));
        }
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────
    private void openEditPost(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/interfaces/User/UserPostFormView.fxml"));
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setTitle(post == null ? "Nouveau Post" : "Modifier ma publication");
            s.setScene(new Scene(loader.load(), 550, 530));
            ((UserPostFormController) loader.getController()).init(post, currentUser);
            s.showAndWait(); loadFeed();
        } catch (IOException e) { toast("Erreur : " + e.getMessage()); }
    }

    private void openEditComment(Commentaire c) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gamilha/interfaces/User/UserCommentaireFormView.fxml"));
            Stage s = new Stage(); s.initModality(Modality.APPLICATION_MODAL);
            s.setScene(new Scene(loader.load(), 480, 270));
            ((UserCommentaireFormController) loader.getController()).init(c);
            s.showAndWait(); loadFeed();
        } catch (IOException e) { toast("Erreur : " + e.getMessage()); }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────
    private Label av(String name, double sz) {
        String init = name != null && name.length() >= 2 ? name.substring(0,2).toUpperCase() : "??";
        Label l = new Label(init);
        l.setMinSize(sz,sz); l.setMaxSize(sz,sz); l.setAlignment(Pos.CENTER);
        l.setStyle("-fx-background-color:#5b21b6;-fx-text-fill:white;-fx-font-weight:bold;" +
                   "-fx-font-size:"+(sz*0.35)+";-fx-background-radius:"+(sz/2)+";");
        return l;
    }

    private Button menuBtn() {
        Button b = new Button("⋮");
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:#1a1e2e;-fx-text-fill:#e2e8f0;-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;-fx-background-radius:6;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-size:17;-fx-cursor:hand;-fx-padding:0 6;"));
        return b;
    }

    private Button iconBtn(String icon, String color) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-size:15;-fx-cursor:hand;-fx-padding:2 6;");
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color:rgba(59,130,246,0.1);-fx-text-fill:"+color+";-fx-font-size:15;-fx-cursor:hand;-fx-padding:2 6;-fx-background-radius:8;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-size:15;-fx-cursor:hand;-fx-padding:2 6;"));
        return b;
    }

    private Image localImg(String fn) {
        for (String p : new String[]{AdminPostController.UPLOADS+fn, AdminPostController.UPLOADS+"images/"+fn}) {
            File f = new File(p); if (f.exists()) return new Image(f.toURI().toString());
        }
        return null;
    }

    private Hyperlink linkLabel(String url) {
        Hyperlink lk = new Hyperlink("🔗 " + url); lk.setStyle("-fx-text-fill:#60a5fa;-fx-font-size:12;");
        lk.setOnAction(e -> { try { java.awt.Desktop.getDesktop().browse(new java.net.URI(url)); } catch (Exception ignored) {} });
        return lk;
    }

    private Separator sep() {
        Separator s = new Separator(); s.setStyle("-fx-background-color:#1a1e2e;");
        VBox.setMargin(s, new Insets(4, 16, 4, 16)); return s;
    }

    private Label emptyLabel(String msg) {
        Label l = new Label(msg); l.setStyle("-fx-text-fill:#475569;-fx-font-size:12;"); return l;
    }

    private boolean isOwner(User u) { return currentUser!=null && u!=null && u.getId()==currentUser.getId(); }
    private void toast(String msg) { Alert a = new Alert(Alert.AlertType.WARNING); a.setHeaderText(null); a.setContentText(msg); a.showAndWait(); }

    @FXML void onNewPost() { openEditPost(null); }
    @FXML void onRefresh() { loadFeed(); loadSidebar(); }
}
