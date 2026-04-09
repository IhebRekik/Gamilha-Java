package com.gamilha.controllers.coaching;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.entity.User;
import com.gamilha.services.CoachingVideoService;
import com.gamilha.services.PlaylistService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  CONTROLLER UNIQUE — PLAYLIST  (affichage en CARDS)
 *  Pages gérées :
 *    • PlaylistList.fxml  → grille de cards modernes
 *    • PlaylistForm.fxml  → formulaire + import image locale
 *    • PlaylistShow.fxml  → détails + mini-cards des vidéos
 * ══════════════════════════════════════════════════════════════════════════
 */
public class PlaylistController {

    // ─── État statique partagé ────────────────────────────────────────────
    private static Playlist selectedPlaylist = null;
    private static boolean  editMode         = false;
    private static final String BASE = "/com/gamilha/interfaces/coaching/";

    // ════════════════════════════════════════════════════════════════════════
    //  CHAMPS FXML — Page LISTE
    // ════════════════════════════════════════════════════════════════════════
    @FXML private TextField        searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private FlowPane         cardsPane;
    @FXML private Button           btnAdd;

    // ════════════════════════════════════════════════════════════════════════
    //  CHAMPS FXML — Page FORMULAIRE
    // ════════════════════════════════════════════════════════════════════════
    @FXML private Label            lblFormTitle;
    @FXML private TextField        txtTitle;
    @FXML private TextArea         txtDescription;
    @FXML private ComboBox<String> cmbNiveau;
    @FXML private TextField        txtCategorie;
    @FXML private ImageView        imgPreview;
    @FXML private Label            lblImagePath;
    @FXML private Button           btnChooseImage;
    @FXML private Label            lblError;
    @FXML private Button           btnSave;
    @FXML private Button           btnCancel;

    // ════════════════════════════════════════════════════════════════════════
    //  CHAMPS FXML — Page DÉTAILS (Show)
    // ════════════════════════════════════════════════════════════════════════
    @FXML private Label            lblTitre;
    @FXML private ImageView        imgPlaylist;
    @FXML private Label            lblDescShow;
    @FXML private Label            lblNiveauShow;
    @FXML private Label            lblCategorieShow;
    @FXML private Label            lblDate;
    @FXML private FlowPane         videosPane;
    @FXML private Button           btnRetour;
    @FXML private Button           btnModifier;
    @FXML private Button           btnSupprimer;

    // ─── Services ─────────────────────────────────────────────────────────
    private final PlaylistService      playlistService = new PlaylistService();
    private final CoachingVideoService videoService    = new CoachingVideoService();
    private ObservableList<Playlist>   masterData      = FXCollections.observableArrayList();

    // ─── État formulaire (par instance) ───────────────────────────────────
    private String selectedImagePath = null;

    // ════════════════════════════════════════════════════════════════════════
    //  DÉTECTION DE PAGE
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if      (cardsPane != null) initListPage();
        else if (txtTitle  != null) initFormPage();
        else if (lblTitre  != null) initShowPage();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE : LISTE — Grille de cards
    // ════════════════════════════════════════════════════════════════════════
    private void initListPage() {
        masterData.addAll(playlistService.afficherPlaylists());

        filterNiveau.getItems().add("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream().map(Playlist::getNiveau)
                        .filter(n -> n != null && !n.isEmpty()).distinct().toList()
        );
        filterNiveau.setValue("Tous");

        if (btnAdd != null) { btnAdd.setVisible(!isAdmin()); btnAdd.setManaged(!isAdmin()); }

        buildCards(masterData);

        searchInput.textProperty().addListener((obs, o, n) -> filterList());
        filterNiveau.valueProperty().addListener((obs, o, n) -> filterList());
    }

    // ── Construction des cards ────────────────────────────────────────────
    private void buildCards(ObservableList<Playlist> data) {
        cardsPane.getChildren().clear();

        for (Playlist p : data) {
            cardsPane.getChildren().add(createPlaylistCard(p));
        }

        if (data.isEmpty()) {
            VBox emptyBox = new VBox(12);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(60));
            Label ico = new Label("🎓");
            ico.setStyle("-fx-font-size:52;");
            Label msg = new Label("Aucune playlist trouvée.");
            msg.setStyle("-fx-text-fill:#475569;-fx-font-size:16;");
            emptyBox.getChildren().addAll(ico, msg);
            cardsPane.getChildren().add(emptyBox);
        }
    }

    private VBox createPlaylistCard(Playlist p) {
        VBox card = new VBox(0);
        card.setPrefWidth(285);
        card.setMaxWidth(285);
        card.setStyle(
                "-fx-background-color:#1e293b;" +
                "-fx-background-radius:14;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.55),12,0,0,5);"
        );

        // ── Thumbnail ─────────────────────────────────────────────────────
        StackPane imgSection = new StackPane();
        imgSection.setPrefHeight(165);
        imgSection.setMinHeight(165);
        imgSection.setMaxHeight(165);
        imgSection.setStyle(
                "-fx-background-color:linear-gradient(to bottom right,#4c1d95,#1e3a8a);" +
                "-fx-background-radius:14 14 0 0;"
        );

        // Default icon (behind any image)
        Label iconDefault = new Label("🎓");
        iconDefault.setStyle("-fx-font-size:54;-fx-text-fill:rgba(255,255,255,0.18);");
        StackPane.setAlignment(iconDefault, Pos.CENTER);
        imgSection.getChildren().add(iconDefault);

        // Charger l'image si disponible
        if (p.getImage() != null && !p.getImage().isEmpty()) {
            try {
                String imgUrl = p.getImage().startsWith("http")
                        ? p.getImage()
                        : new File(p.getImage()).toURI().toString();
                Image img = new Image(imgUrl, 285, 165, false, true, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(285);
                iv.setFitHeight(165);
                iv.setPreserveRatio(false);
                imgSection.getChildren().add(iv);
            } catch (Exception ignored) {}
        }

        // Badge niveau
        if (p.getNiveau() != null && !p.getNiveau().isEmpty()) {
            Label lvl = new Label(p.getNiveau());
            lvl.setStyle(
                    "-fx-background-color:" + niveauColor(p.getNiveau()) + ";" +
                    "-fx-text-fill:white;-fx-padding:4 12;-fx-background-radius:20;" +
                    "-fx-font-size:10;-fx-font-weight:bold;"
            );
            StackPane.setAlignment(lvl, Pos.TOP_LEFT);
            StackPane.setMargin(lvl, new Insets(10, 0, 0, 10));
            imgSection.getChildren().add(lvl);
        }

        // ── Contenu ───────────────────────────────────────────────────────
        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 16, 10, 16));
        VBox.setVgrow(content, Priority.ALWAYS);

        Label titleLbl = new Label(p.getTitle() != null ? p.getTitle() : "");
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:15;-fx-font-weight:bold;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(253);

        String descTxt = p.getDescription() != null ? p.getDescription() : "Aucune description.";
        if (descTxt.length() > 82) descTxt = descTxt.substring(0, 82) + "…";
        Label descLbl = new Label(descTxt);
        descLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(253);

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);
        if (p.getCategorie() != null && !p.getCategorie().isEmpty()) {
            Label cat = new Label("📁 " + p.getCategorie());
            cat.setStyle(
                    "-fx-text-fill:#38bdf8;-fx-font-size:10;" +
                    "-fx-background-color:#0c4a6e;-fx-background-radius:4;-fx-padding:3 8;"
            );
            meta.getChildren().add(cat);
        }

        content.getChildren().addAll(titleLbl, descLbl, meta);

        // ── Séparateur ────────────────────────────────────────────────────
        Pane sep = new Pane();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#334155;");

        // ── Boutons d'action ──────────────────────────────────────────────
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(10, 14, 14, 14));
        actions.setAlignment(Pos.CENTER_LEFT);

        Button voir = styledBtn("👁 Voir", "#2563eb");
        voir.setOnAction(e -> { selectedPlaylist = p; navigateTo("PlaylistShow.fxml"); });
        actions.getChildren().add(voir);

        if (!isAdmin()) {
            Button modifier  = styledBtn("✏️",       "#d97706");
            Button supprimer = styledBtn("🗑",        "#dc2626");
            Button videos    = styledBtn("🎬 Vidéos", "#7c3aed");

            modifier.setOnAction(e  -> { selectedPlaylist = p; editMode = true; navigateTo("PlaylistForm.fxml"); });
            supprimer.setOnAction(e -> confirmDelete(p));
            videos.setOnAction(e    -> {
                selectedPlaylist = p;
                CoachingVideoController.setPlaylistFilter(p);
                navigateTo("VideoList.fxml");
            });
            actions.getChildren().addAll(modifier, supprimer, videos);
        }

        card.getChildren().addAll(imgSection, content, sep, actions);
        return card;
    }

    @FXML private void handleAdd() {
        editMode = false; selectedPlaylist = null;
        navigateTo("PlaylistForm.fxml");
    }

    private void filterList() {
        String search = searchInput.getText() == null ? "" : searchInput.getText().toLowerCase();
        String niveau = filterNiveau.getValue();
        ObservableList<Playlist> filtered = masterData.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(search)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(search)))
                .filter(p -> niveau == null || "Tous".equals(niveau) || niveau.equals(p.getNiveau()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        buildCards(filtered);
    }

    private void confirmDelete(Playlist p) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer « " + p.getTitle() + " » ?", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Supprimer Playlist");
        a.setHeaderText(null);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                playlistService.supprimerPlaylist(p.getId());
                masterData.remove(p);
                buildCards(masterData);
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE : FORMULAIRE — avec import d'image locale
    // ════════════════════════════════════════════════════════════════════════
    private void initFormPage() {
        cmbNiveau.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert");

        if (editMode && selectedPlaylist != null) {
            lblFormTitle.setText("✏️ Modifier la Playlist");
            txtTitle.setText(selectedPlaylist.getTitle());
            if (txtDescription != null) txtDescription.setText(
                    selectedPlaylist.getDescription() != null ? selectedPlaylist.getDescription() : "");
            cmbNiveau.setValue(selectedPlaylist.getNiveau() != null ? selectedPlaylist.getNiveau() : "Débutant");
            if (txtCategorie != null) txtCategorie.setText(
                    selectedPlaylist.getCategorie() != null ? selectedPlaylist.getCategorie() : "");

            // Pré-charger image existante
            selectedImagePath = selectedPlaylist.getImage();
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                try {
                    String imgUrl = selectedImagePath.startsWith("http")
                            ? selectedImagePath
                            : new File(selectedImagePath).toURI().toString();
                    if (imgPreview != null) {
                        imgPreview.setImage(new Image(imgUrl, 240, 130, false, true));
                        imgPreview.setVisible(true);
                        imgPreview.setManaged(true);
                    }
                    if (lblImagePath != null) {
                        String name = selectedImagePath.contains(File.separator)
                                ? selectedImagePath.substring(selectedImagePath.lastIndexOf(File.separator) + 1)
                                : selectedImagePath;
                        lblImagePath.setText(name);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            lblFormTitle.setText("➕ Nouvelle Playlist");
            cmbNiveau.setValue("Débutant");
        }

        btnSave.setOnAction(e   -> handleSave());
        btnCancel.setOnAction(e -> { editMode = false; selectedImagePath = null; navigateTo("PlaylistList.fxml"); });
    }

    /** Ouvre FileChooser pour sélectionner une image locale */
    @FXML
    private void handleChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp","*.bmp")
        );
        File file = fc.showOpenDialog(btnChooseImage.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            if (lblImagePath != null) lblImagePath.setText("✅ " + file.getName());
            if (imgPreview != null) {
                imgPreview.setImage(new Image(file.toURI().toString(), 240, 130, false, true));
                imgPreview.setVisible(true);
                imgPreview.setManaged(true);
            }
        }
    }

    private void handleSave() {
        if (lblError != null) lblError.setText("");
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) {
            if (lblError != null) lblError.setText("⚠️ Le titre est obligatoire.");
            return;
        }

        String desc  = txtDescription != null ? txtDescription.getText().trim() : "";
        String niv   = cmbNiveau.getValue();
        String cat   = txtCategorie != null ? txtCategorie.getText().trim() : "";

        if (editMode && selectedPlaylist != null) {
            selectedPlaylist.setTitle(title);
            selectedPlaylist.setDescription(desc);
            selectedPlaylist.setNiveau(niv);
            selectedPlaylist.setCategorie(cat);
            selectedPlaylist.setImage(selectedImagePath);
            playlistService.modifierPlaylist(selectedPlaylist);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Playlist modifiée avec succès !");
        } else {
            Playlist p = new Playlist(title, desc, niv, cat, selectedImagePath, LocalDateTime.now());
            playlistService.ajouterPlaylist(p);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Playlist ajoutée avec succès !");
        }

        editMode = false; selectedPlaylist = null; selectedImagePath = null;
        navigateTo("PlaylistList.fxml");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE : DÉTAILS — avec image + mini-cards vidéos
    // ════════════════════════════════════════════════════════════════════════
    private void initShowPage() {
        if (selectedPlaylist == null) { navigateTo("PlaylistList.fxml"); return; }

        lblTitre.setText(selectedPlaylist.getTitle());
        if (lblDescShow     != null) lblDescShow.setText(selectedPlaylist.getDescription() != null ? selectedPlaylist.getDescription() : "-");
        if (lblNiveauShow   != null) lblNiveauShow.setText(selectedPlaylist.getNiveau() != null ? selectedPlaylist.getNiveau() : "-");
        if (lblCategorieShow!= null) lblCategorieShow.setText(selectedPlaylist.getCategorie() != null ? selectedPlaylist.getCategorie() : "-");
        if (lblDate         != null) lblDate.setText(selectedPlaylist.getCreatedAt() != null ? selectedPlaylist.getCreatedAt().toLocalDate().toString() : "-");

        // Image de la playlist
        if (imgPlaylist != null && selectedPlaylist.getImage() != null && !selectedPlaylist.getImage().isEmpty()) {
            try {
                String imgUrl = selectedPlaylist.getImage().startsWith("http")
                        ? selectedPlaylist.getImage()
                        : new File(selectedPlaylist.getImage()).toURI().toString();
                imgPlaylist.setImage(new Image(imgUrl, 900, 260, false, true));
                imgPlaylist.setVisible(true);
                imgPlaylist.setManaged(true);
            } catch (Exception ignored) {}
        }

        boolean admin = isAdmin();
        if (btnModifier  != null) { btnModifier.setVisible(!admin);  btnModifier.setManaged(!admin);  }
        if (btnSupprimer != null) { btnSupprimer.setVisible(!admin); btnSupprimer.setManaged(!admin); }

        if (btnRetour    != null) btnRetour.setOnAction(e -> navigateTo("PlaylistList.fxml"));
        if (btnModifier  != null) btnModifier.setOnAction(e -> { editMode = true; navigateTo("PlaylistForm.fxml"); });
        if (btnSupprimer != null) btnSupprimer.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer « " + selectedPlaylist.getTitle() + " » ?", ButtonType.OK, ButtonType.CANCEL);
            a.setTitle("Supprimer Playlist"); a.setHeaderText(null);
            a.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    playlistService.supprimerPlaylist(selectedPlaylist.getId());
                    selectedPlaylist = null;
                    navigateTo("PlaylistList.fxml");
                }
            });
        });

        chargerVideos();
    }

    /** Construit les mini-cards des vidéos de la playlist */
    private void chargerVideos() {
        List<CoachingVideo> videos = videoService.afficherVideosByPlaylist(selectedPlaylist.getId());
        videosPane.getChildren().clear();

        for (CoachingVideo v : videos) {
            videosPane.getChildren().add(createVideoMiniCard(v));
        }

        if (videos.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label ico = new Label("🎬");
            ico.setStyle("-fx-font-size:40;");
            Label msg = new Label("Aucune vidéo dans cette playlist.");
            msg.setStyle("-fx-text-fill:#475569;-fx-font-size:14;");
            empty.getChildren().addAll(ico, msg);
            videosPane.getChildren().add(empty);
        }
    }

    private VBox createVideoMiniCard(CoachingVideo v) {
        VBox card = new VBox(0);
        card.setPrefWidth(245);
        card.setMaxWidth(245);
        card.setStyle(
                "-fx-background-color:#0f172a;-fx-background-radius:10;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.45),8,0,0,3);"
        );
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#1e293b;-fx-background-radius:10;" +
                "-fx-effect:dropshadow(gaussian,rgba(124,58,237,0.35),14,0,0,4);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#0f172a;-fx-background-radius:10;" +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.45),8,0,0,3);"
        ));

        // ── Thumbnail ─────────────────────────────────────────────────────
        StackPane thumb = new StackPane();
        thumb.setPrefHeight(135);
        thumb.setStyle("-fx-background-color:#1a1a2e;-fx-background-radius:10 10 0 0;");

        // Miniature YouTube
        String url = v.getUrl();
        if (url != null && (url.contains("youtube") || url.contains("youtu.be"))) {
            String ytId = extractYoutubeId(url);
            if (ytId != null) {
                try {
                    Image ytThumb = new Image(
                            "https://img.youtube.com/vi/" + ytId + "/mqdefault.jpg",
                            245, 135, false, true, true
                    );
                    ImageView iv = new ImageView(ytThumb);
                    iv.setFitWidth(245); iv.setFitHeight(135); iv.setPreserveRatio(false);
                    thumb.getChildren().add(iv);
                } catch (Exception ignored) {}
            }
        } else if (url != null && !url.isEmpty() && !url.startsWith("http")) {
            // Fichier local → icône vidéo
            Label fileIcon = new Label("🎞️");
            fileIcon.setStyle("-fx-font-size:36;");
            thumb.getChildren().add(fileIcon);
        }

        // Icône play overlay
        Label play = new Label("▶");
        play.setStyle("-fx-font-size:26;-fx-text-fill:rgba(255,255,255,0.75);-fx-effect:dropshadow(gaussian,black,5,0,0,0);");
        thumb.getChildren().add(play);

        // Badge durée
        Label dur = new Label(v.getDurationFormatted());
        dur.setStyle("-fx-background-color:rgba(0,0,0,0.82);-fx-text-fill:white;-fx-padding:2 7;-fx-background-radius:4;-fx-font-size:10;");
        StackPane.setAlignment(dur, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(dur, new Insets(0, 6, 6, 0));
        thumb.getChildren().add(dur);

        // Badge premium
        if (v.isPremium()) {
            Label prem = new Label("🔒 Premium");
            prem.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-padding:2 7;-fx-background-radius:4;-fx-font-size:10;");
            StackPane.setAlignment(prem, Pos.TOP_RIGHT);
            StackPane.setMargin(prem, new Insets(6, 6, 0, 0));
            thumb.getChildren().add(prem);
        }

        // ── Contenu ───────────────────────────────────────────────────────
        VBox content = new VBox(6);
        content.setPadding(new Insets(10, 12, 10, 12));

        Label titleLbl = new Label(v.getTitre());
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(221);

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        if (v.getNiveau() != null && !v.getNiveau().isEmpty()) {
            Label lvl = new Label(v.getNiveau());
            lvl.setStyle("-fx-background-color:" + niveauColor(v.getNiveau()) + ";-fx-text-fill:white;-fx-padding:2 7;-fx-background-radius:4;-fx-font-size:10;");
            badges.getChildren().add(lvl);
        }
        content.getChildren().addAll(titleLbl, badges);

        // ── Bouton voir ───────────────────────────────────────────────────
        Button voir = new Button("▶ Voir la vidéo");
        voir.setMaxWidth(Double.MAX_VALUE);
        voir.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#38bdf8;-fx-font-size:12;-fx-font-weight:bold;" +
                "-fx-padding:8;-fx-background-radius:0 0 10 10;-fx-cursor:hand;");
        voir.setOnMouseEntered(e -> voir.setStyle("-fx-background-color:#1e40af;-fx-text-fill:white;-fx-font-size:12;-fx-font-weight:bold;-fx-padding:8;-fx-background-radius:0 0 10 10;-fx-cursor:hand;"));
        voir.setOnMouseExited(e  -> voir.setStyle("-fx-background-color:#1e3a5f;-fx-text-fill:#38bdf8;-fx-font-size:12;-fx-font-weight:bold;-fx-padding:8;-fx-background-radius:0 0 10 10;-fx-cursor:hand;"));
        voir.setOnAction(e -> {
            CoachingVideoController.setSelectedVideoStatic(v);
            navigateTo("VideoShow.fxml");
        });

        card.getChildren().addAll(thumb, content, voir);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════
    private String niveauColor(String n) {
        if (n == null) return "#374151";
        return switch (n.toLowerCase()) {
            case "débutant"      -> "#166534";
            case "intermédiaire" -> "#1e40af";
            case "avancé"        -> "#9a3412";
            case "expert"        -> "#581c87";
            default              -> "#374151";
        };
    }

    private String extractYoutubeId(String url) {
        Pattern p = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|v/))([^&?\\s]+)");
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private void navigateTo(String fxmlFile) {
        try {
            Node anchor = (cardsPane != null) ? cardsPane
                        : (txtTitle  != null) ? txtTitle
                        : lblTitre;
            if (anchor == null) return;
            Parent view       = FXMLLoader.load(getClass().getResource(BASE + fxmlFile));
            BorderPane root   = (BorderPane) anchor.getScene().getRoot();
            BorderPane cArea  = (BorderPane) root.lookup("#contentArea");
            if (cArea != null) cArea.setCenter(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean isAdmin() {
        User u = SessionContext.getCurrentUser();
        return u != null && u.getRoles() != null
                && u.getRoles().stream().anyMatch(r -> r.toUpperCase().contains("ADMIN"));
    }

    private Button styledBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;" +
                "-fx-background-radius:6;-fx-padding:5 11;-fx-font-size:12;");
        return btn;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
