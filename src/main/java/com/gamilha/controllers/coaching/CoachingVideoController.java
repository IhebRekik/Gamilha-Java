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
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ══════════════════════════════════════════════════════════════════════════
 *  CONTROLLER UNIQUE — COACHING VIDEO  (affichage en CARDS)
 *  Pages gérées :
 *    • VideoList.fxml  → grille de cards modernes
 *    • VideoForm.fxml  → formulaire + import fichier local OU URL YouTube
 *    • VideoShow.fxml  → détails + lecteur vidéo intégré
 * ══════════════════════════════════════════════════════════════════════════
 */
public class CoachingVideoController {

    // ─── État statique partagé ────────────────────────────────────────────
    private static CoachingVideo selectedVideo  = null;
    private static Playlist      playlistFilter = null;
    private static boolean       editMode       = false;
    private static final String  BASE = "/com/gamilha/interfaces/coaching/";

    // ════════════════════════════════════════════════════════════════════════
    //  CHAMPS FXML — Page LISTE
    // ════════════════════════════════════════════════════════════════════════
    @FXML private TextField        searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterPremium;
    @FXML private Label            lblPlaylistBadge;
    @FXML private FlowPane         cardsPane;
    @FXML private Button           btnAdd;

    // ════════════════════════════════════════════════════════════════════════
    //  CHAMPS FXML — Page FORMULAIRE
    // ════════════════════════════════════════════════════════════════════════
    @FXML private Label            lblFormTitle;
    @FXML private TextField        txtTitre;
    @FXML private TextArea         txtDescription;
    @FXML private TextField        txtNiveau;
    @FXML private TextField        txtDuration;
    @FXML private CheckBox         cbPremium;
    @FXML private ComboBox<String> cbPlaylist;

    // Source vidéo — toggle
    @FXML private VBox             localSection;   // section fichier local
    @FXML private VBox             urlSection;     // section URL YouTube
    @FXML private TextField        txtUrl;
    @FXML private Button           btnChooseVideo;
    @FXML private Label            lblVideoPath;
    @FXML private Button           btnToggleLocal;
    @FXML private Button           btnToggleUrl;

    @FXML private Label            lblErrorVideo;
    @FXML private Button           btnSaveVideo;
    @FXML private Button           btnCancelVideo;

    // ════════════════════════════════════════════════════════════════════════
    //  CHAMPS FXML — Page DÉTAILS (Show)
    // ════════════════════════════════════════════════════════════════════════
    @FXML private Label            lblTitreShow;
    @FXML private Label            lblDescVideo;
    @FXML private Label            lblNiveauVideo;
    @FXML private Label            lblDureeVideo;
    @FXML private Label            lblPremiumVideo;
    @FXML private Label            lblPlaylistVideo;
    @FXML private StackPane        videoContainer;
    @FXML private Button           btnRetourVideo;
    @FXML private Button           btnModifierVideo;
    @FXML private Button           btnSupprimerVideo;

    // ─── Services ─────────────────────────────────────────────────────────
    private final CoachingVideoService videoService    = new CoachingVideoService();
    private final PlaylistService      playlistService = new PlaylistService();
    private ObservableList<CoachingVideo> masterData   = FXCollections.observableArrayList();
    private List<Playlist>             allPlaylists;

    // ─── État formulaire (par instance) ───────────────────────────────────
    private String  selectedVideoPath = null;
    private boolean useLocalFile      = false;
    private MediaPlayer activePlayer  = null;

    // ─── VLCJ — lecteur universel (H.264 + H.265 + tous codecs) ──────────
    private MediaPlayerFactory  vlcFactory   = null;
    private EmbeddedMediaPlayer vlcPlayer    = null;

    // ─── Cache thumbs locaux ──────────────────────────────────────────────
    private static final File THUMB_CACHE_DIR =
            new File(System.getProperty("java.io.tmpdir"), "gamilha-thumbs");

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTHODES STATIQUES (appelées depuis d'autres controllers)
    // ════════════════════════════════════════════════════════════════════════
    public static void setPlaylistFilter(Playlist p)      { playlistFilter = p; }
    public static void setSelectedVideoStatic(CoachingVideo v) { selectedVideo = v; }

    // ════════════════════════════════════════════════════════════════════════
    //  DÉTECTION DE PAGE
    // ════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        if      (cardsPane    != null) initListPage();
        else if (txtTitre     != null) initFormPage();
        else if (lblTitreShow != null) initShowPage();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE : LISTE — Grille de cards
    // ════════════════════════════════════════════════════════════════════════
    private void initListPage() {
        chargerDonnees();

        filterNiveau.getItems().add("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream().map(CoachingVideo::getNiveau)
                        .filter(n -> n != null && !n.isEmpty()).distinct().toList()
        );
        filterNiveau.setValue("Tous");

        filterPremium.getItems().addAll("Tous", "🔒 Premium", "❌ Gratuit");
        filterPremium.setValue("Tous");

        if (lblPlaylistBadge != null && playlistFilter != null) {
            lblPlaylistBadge.setText("📂 " + playlistFilter.getTitle());
            lblPlaylistBadge.setVisible(true);
            lblPlaylistBadge.setManaged(true);
        }

        if (btnAdd != null) { btnAdd.setVisible(!isAdmin()); btnAdd.setManaged(!isAdmin()); }

        buildCards(masterData);

        searchInput.textProperty().addListener((obs, o, n) -> filterList());
        filterNiveau.valueProperty().addListener((obs, o, n) -> filterList());
        filterPremium.valueProperty().addListener((obs, o, n) -> filterList());
    }

    private void chargerDonnees() {
        masterData.clear();
        if (playlistFilter != null)
            masterData.addAll(videoService.afficherVideosByPlaylist(playlistFilter.getId()));
        else
            masterData.addAll(videoService.afficherVideos());
    }

    // ── Construction des cards vidéo ─────────────────────────────────────
    private void buildCards(ObservableList<CoachingVideo> data) {
        cardsPane.getChildren().clear();

        for (CoachingVideo v : data) {
            cardsPane.getChildren().add(createVideoCard(v));
        }

        if (data.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label ico = new Label("🎬");
            ico.setStyle("-fx-font-size:52;");
            Label msg = new Label("Aucune vidéo trouvée.");
            msg.setStyle("-fx-text-fill:#475569;-fx-font-size:16;");
            empty.getChildren().addAll(ico, msg);
            cardsPane.getChildren().add(empty);
        }
    }

    private VBox createVideoCard(CoachingVideo v) {
        VBox card = new VBox(0);
        card.setPrefWidth(295);
        card.setMaxWidth(295);
        card.setStyle(
                "-fx-background-color:#1e293b;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.55),12,0,0,5);"
        );
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color:#243350;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.25),16,0,0,4);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color:#1e293b;-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.55),12,0,0,5);"
        ));

        // ── Thumbnail ─────────────────────────────────────────────────────
        StackPane thumb = new StackPane();
        thumb.setPrefHeight(165);
        thumb.setMinHeight(165);
        thumb.setStyle("-fx-background-color:linear-gradient(to bottom right,#0c1a3a,#1a1a3e);-fx-background-radius:14 14 0 0;");

        // Icône par défaut (derrière)
        Label defIcon = new Label("🎬");
        defIcon.setStyle("-fx-font-size:48;-fx-text-fill:rgba(255,255,255,0.15);");
        StackPane.setAlignment(defIcon, Pos.CENTER);
        thumb.getChildren().add(defIcon);

        String url = v.getUrl();
        boolean isYoutube = url != null && (url.contains("youtube") || url.contains("youtu.be"));
        boolean isLocal   = url != null && !url.isEmpty() && !url.startsWith("http");

        if (isYoutube) {
            String ytId = extractYoutubeId(url);
            if (ytId != null) {
                try {
                    Image ytThumb = new Image(
                            "https://img.youtube.com/vi/" + ytId + "/mqdefault.jpg",
                            295, 165, false, true, true
                    );
                    ImageView iv = new ImageView(ytThumb);
                    iv.setFitWidth(295); iv.setFitHeight(165); iv.setPreserveRatio(false);
                    thumb.getChildren().add(iv);
                } catch (Exception ignored) {}
            }
            // Badge YouTube
            Label ytBadge = new Label("▶ YouTube");
            ytBadge.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:4;-fx-font-size:10;-fx-font-weight:bold;");
            StackPane.setAlignment(ytBadge, Pos.BOTTOM_LEFT);
            StackPane.setMargin(ytBadge, new Insets(0, 0, 6, 6));
            thumb.getChildren().add(ytBadge);

        } else if (isLocal) {
            // ── Thumbnail local (générée en arrière-plan via FFmpeg) ──────
            ImageView localThumb = new ImageView();
            localThumb.setFitWidth(295);
            localThumb.setFitHeight(165);
            localThumb.setPreserveRatio(false);
            localThumb.setVisible(false);
            StackPane.setAlignment(localThumb, Pos.CENTER);
            thumb.getChildren().add(1, localThumb); // après defIcon, avant badges

            generateLocalThumbnail(new File(url), localThumb);

            Label localBadge = new Label("📁 Fichier local");
            localBadge.setStyle("-fx-background-color:#0369a1;-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:4;-fx-font-size:10;-fx-font-weight:bold;");
            StackPane.setAlignment(localBadge, Pos.BOTTOM_LEFT);
            StackPane.setMargin(localBadge, new Insets(0, 0, 6, 6));
            thumb.getChildren().add(localBadge);
        }

        // Icône play
        Label play = new Label("▶");
        play.setStyle("-fx-font-size:28;-fx-text-fill:rgba(255,255,255,0.8);-fx-effect:dropshadow(gaussian,black,6,0,0,0);");
        thumb.getChildren().add(play);

        // Badge durée
        Label dur = new Label(v.getDurationFormatted());
        dur.setStyle("-fx-background-color:rgba(0,0,0,0.85);-fx-text-fill:white;-fx-padding:2 7;-fx-background-radius:4;-fx-font-size:10;");
        StackPane.setAlignment(dur, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(dur, new Insets(0, 6, 6, 0));
        thumb.getChildren().add(dur);

        // Badge premium
        if (v.isPremium()) {
            Label prem = new Label("🔒 Premium");
            prem.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:4;-fx-font-size:10;-fx-font-weight:bold;");
            StackPane.setAlignment(prem, Pos.TOP_RIGHT);
            StackPane.setMargin(prem, new Insets(8, 8, 0, 0));
            thumb.getChildren().add(prem);
        }

        // ── Contenu ───────────────────────────────────────────────────────
        VBox content = new VBox(8);
        content.setPadding(new Insets(14, 16, 10, 16));
        VBox.setVgrow(content, Priority.ALWAYS);

        Label titleLbl = new Label(v.getTitre());
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:bold;");
        titleLbl.setWrapText(true);
        titleLbl.setMaxWidth(263);

        String descTxt = v.getDescription() != null ? v.getDescription() : "";
        if (descTxt.length() > 72) descTxt = descTxt.substring(0, 72) + "…";
        Label descLbl = new Label(descTxt);
        descLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");
        descLbl.setWrapText(true);
        descLbl.setMaxWidth(263);

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);
        if (v.getNiveau() != null && !v.getNiveau().isEmpty()) {
            Label lvl = new Label(v.getNiveau());
            lvl.setStyle("-fx-background-color:" + niveauColor(v.getNiveau()) + ";-fx-text-fill:white;-fx-padding:3 9;-fx-background-radius:20;-fx-font-size:10;-fx-font-weight:bold;");
            badges.getChildren().add(lvl);
        }
        if (v.getPlaylist() != null && v.getPlaylist().getTitle() != null) {
            Label pl = new Label("📂 " + v.getPlaylist().getTitle());
            pl.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:10;-fx-background-color:#0c4a6e;-fx-background-radius:4;-fx-padding:3 8;");
            badges.getChildren().add(pl);
        }

        content.getChildren().addAll(titleLbl, descLbl, badges);

        // ── Séparateur ────────────────────────────────────────────────────
        Pane sep = new Pane();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#334155;");

        // ── Boutons d'action ──────────────────────────────────────────────
        HBox actions = new HBox(6);
        actions.setPadding(new Insets(10, 14, 14, 14));
        actions.setAlignment(Pos.CENTER_LEFT);

        Button voir = styledBtn("▶ Voir", "#0284c7");
        voir.setOnAction(e -> { selectedVideo = v; navigateTo("VideoShow.fxml"); });
        actions.getChildren().add(voir);

        if (!isAdmin()) {
            Button modifier  = styledBtn("✏️",  "#d97706");
            Button supprimer = styledBtn("🗑",   "#dc2626");
            modifier.setOnAction(e  -> { selectedVideo = v; editMode = true; navigateTo("VideoForm.fxml"); });
            supprimer.setOnAction(e -> confirmDelete(v));
            actions.getChildren().addAll(modifier, supprimer);
        }

        card.getChildren().addAll(thumb, content, sep, actions);
        return card;
    }

    @FXML private void handleAdd() {
        editMode = false; selectedVideo = null;
        navigateTo("VideoForm.fxml");
    }

    @FXML private void handleRetourPlaylist() {
        playlistFilter = null;
        navigateTo("PlaylistList.fxml");
    }

    private void filterList() {
        String search  = searchInput.getText() == null ? "" : searchInput.getText().toLowerCase();
        String niveau  = filterNiveau.getValue();
        String premium = filterPremium.getValue();

        ObservableList<CoachingVideo> filtered = masterData.stream()
                .filter(v -> v.getTitre().toLowerCase().contains(search)
                        || (v.getDescription() != null && v.getDescription().toLowerCase().contains(search)))
                .filter(v -> niveau == null || "Tous".equals(niveau) || niveau.equals(v.getNiveau()))
                .filter(v -> {
                    if ("🔒 Premium".equals(premium)) return v.isPremium();
                    if ("❌ Gratuit".equals(premium))  return !v.isPremium();
                    return true;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        buildCards(filtered);
    }

    private void confirmDelete(CoachingVideo v) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer « " + v.getTitre() + " » ?", ButtonType.OK, ButtonType.CANCEL);
        a.setTitle("Supprimer Vidéo"); a.setHeaderText(null);
        a.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                videoService.supprimerVideo(v.getId());
                masterData.remove(v);
                buildCards(masterData);
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE : FORMULAIRE — toggle Local / YouTube + FileChooser
    // ════════════════════════════════════════════════════════════════════════
    private void initFormPage() {
        allPlaylists = playlistService.afficherPlaylists();

        // Remplir ComboBox playlists
        if (cbPlaylist != null) {
            cbPlaylist.getItems().clear();
            cbPlaylist.getItems().add("-- Aucune --");
            allPlaylists.forEach(p -> cbPlaylist.getItems().add(p.getTitle()));
            cbPlaylist.setValue("-- Aucune --");
        }

        if (editMode && selectedVideo != null) {
            if (lblFormTitle   != null) lblFormTitle.setText("✏️ Modifier la Vidéo");
            txtTitre.setText(selectedVideo.getTitre());
            if (txtDescription != null) txtDescription.setText(selectedVideo.getDescription() != null ? selectedVideo.getDescription() : "");
            if (txtNiveau      != null) txtNiveau.setText(selectedVideo.getNiveau() != null ? selectedVideo.getNiveau() : "");
            if (txtDuration    != null) txtDuration.setText(String.valueOf(selectedVideo.getDuration()));
            if (cbPremium      != null) cbPremium.setSelected(selectedVideo.isPremium());
            if (cbPlaylist     != null && selectedVideo.getPlaylist() != null)
                cbPlaylist.setValue(selectedVideo.getPlaylist().getTitle());

            // Détecter le type de source
            String existingUrl = selectedVideo.getUrl();
            if (existingUrl != null && !existingUrl.startsWith("http") && !existingUrl.isEmpty()) {
                // Fichier local
                selectedVideoPath = existingUrl;
                useLocalFile = true;
            } else {
                if (txtUrl != null && existingUrl != null) txtUrl.setText(existingUrl);
                useLocalFile = false;
            }
        } else {
            if (lblFormTitle != null) lblFormTitle.setText("➕ Nouvelle Vidéo");
            if (cbPlaylist   != null && playlistFilter != null)
                cbPlaylist.setValue(playlistFilter.getTitle());
            useLocalFile = false;
        }

        // Appliquer l'état des sections
        appliquerToggle();

        // Handlers boutons toggle
        if (btnToggleLocal != null) btnToggleLocal.setOnAction(e -> { useLocalFile = true;  appliquerToggle(); });
        if (btnToggleUrl   != null) btnToggleUrl.setOnAction(e   -> { useLocalFile = false; appliquerToggle(); });

        // Pré-remplir label fichier local si existant
        if (useLocalFile && selectedVideoPath != null && lblVideoPath != null) {
            String name = selectedVideoPath.contains(File.separator)
                    ? selectedVideoPath.substring(selectedVideoPath.lastIndexOf(File.separator) + 1)
                    : selectedVideoPath;
            lblVideoPath.setText("✅ " + name);
        }

        if (btnSaveVideo   != null) btnSaveVideo.setOnAction(e -> handleSave());
        if (btnCancelVideo != null) btnCancelVideo.setOnAction(e -> { editMode = false; selectedVideoPath = null; navigateTo("VideoList.fxml"); });
    }

    /** Affiche/masque les sections selon le mode sélectionné */
    private void appliquerToggle() {
        if (localSection != null) { localSection.setVisible(useLocalFile);  localSection.setManaged(useLocalFile);  }
        if (urlSection   != null) { urlSection.setVisible(!useLocalFile);   urlSection.setManaged(!useLocalFile);   }

        String activeStyle   = "-fx-background-color:#0284c7;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 20;";
        String inactiveStyle = "-fx-background-color:#1e293b;-fx-text-fill:#94a3b8;-fx-background-radius:8;-fx-padding:8 20;";

        if (btnToggleLocal != null) btnToggleLocal.setStyle(useLocalFile  ? activeStyle : inactiveStyle);
        if (btnToggleUrl   != null) btnToggleUrl.setStyle(!useLocalFile   ? activeStyle : inactiveStyle);
    }

    /** Ouvre FileChooser pour sélectionner un fichier vidéo local */
    @FXML
    private void handleChooseVideo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une vidéo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Vidéos", "*.mp4","*.avi","*.mkv","*.mov","*.wmv","*.flv","*.webm"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        File file = fc.showOpenDialog(btnChooseVideo.getScene().getWindow());
        if (file != null) {
            selectedVideoPath = file.getAbsolutePath();
            if (lblVideoPath != null) lblVideoPath.setText("✅ " + file.getName());
        }
    }

    private void handleSave() {
        if (lblErrorVideo != null) lblErrorVideo.setText("");

        String titre = txtTitre.getText().trim();
        if (titre.isEmpty()) {
            if (lblErrorVideo != null) lblErrorVideo.setText("⚠️ Le titre est obligatoire.");
            return;
        }

        // Résoudre la source vidéo
        String finalUrl;
        if (useLocalFile) {
            finalUrl = selectedVideoPath != null ? selectedVideoPath : "";
        } else {
            finalUrl = txtUrl != null ? txtUrl.getText().trim() : "";
        }

        int durationSec = 0;
        try {
            if (txtDuration != null && !txtDuration.getText().trim().isEmpty())
                durationSec = Integer.parseInt(txtDuration.getText().trim());
        } catch (NumberFormatException ex) {
            if (lblErrorVideo != null) lblErrorVideo.setText("⚠️ La durée doit être un entier (secondes).");
            return;
        }

        Playlist playlist = getSelectedPlaylist();

        if (editMode && selectedVideo != null) {
            selectedVideo.setTitre(titre);
            selectedVideo.setDescription(txtDescription != null ? txtDescription.getText().trim() : "");
            selectedVideo.setUrl(finalUrl);
            selectedVideo.setNiveau(txtNiveau != null ? txtNiveau.getText().trim() : "");
            selectedVideo.setPremium(cbPremium != null && cbPremium.isSelected());
            selectedVideo.setDuration(durationSec);
            selectedVideo.setPlaylist(playlist);
            videoService.modifierVideo(selectedVideo);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Vidéo modifiée avec succès !");
        } else {
            CoachingVideo v = new CoachingVideo(
                    titre,
                    txtDescription != null ? txtDescription.getText().trim() : "",
                    finalUrl,
                    txtNiveau != null ? txtNiveau.getText().trim() : "",
                    cbPremium != null && cbPremium.isSelected(),
                    durationSec,
                    playlist
            );
            videoService.ajouterVideo(v);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Vidéo ajoutée avec succès !");
        }

        editMode = false; selectedVideo = null; selectedVideoPath = null;
        navigateTo("VideoList.fxml");
    }

    private Playlist getSelectedPlaylist() {
        if (cbPlaylist == null || cbPlaylist.getValue() == null
                || "-- Aucune --".equals(cbPlaylist.getValue())) return null;
        String sel = cbPlaylist.getValue();
        return allPlaylists.stream().filter(p -> p.getTitle().equals(sel)).findFirst().orElse(null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE : DÉTAILS — avec lecteur vidéo intégré
    // ════════════════════════════════════════════════════════════════════════
    private void initShowPage() {
        if (selectedVideo == null) { navigateTo("VideoList.fxml"); return; }

        lblTitreShow.setText(selectedVideo.getTitre());
        if (lblDescVideo     != null) lblDescVideo.setText(selectedVideo.getDescription() != null ? selectedVideo.getDescription() : "-");
        if (lblNiveauVideo   != null) lblNiveauVideo.setText(selectedVideo.getNiveau() != null ? selectedVideo.getNiveau() : "-");
        if (lblDureeVideo    != null) lblDureeVideo.setText(selectedVideo.getDurationFormatted());
        if (lblPremiumVideo  != null) lblPremiumVideo.setText(selectedVideo.isPremium() ? "🔒 Premium" : "❌ Gratuit");
        if (lblPlaylistVideo != null) lblPlaylistVideo.setText(selectedVideo.getPlaylist() != null ? selectedVideo.getPlaylist().getTitle() : "-");

        // ── Lecteur vidéo ─────────────────────────────────────────────────
        if (videoContainer != null && selectedVideo.getUrl() != null && !selectedVideo.getUrl().isEmpty()) {
            buildVideoPlayer(selectedVideo.getUrl());
        }

        // Rôle → boutons
        boolean admin = isAdmin();
        if (btnModifierVideo  != null) { btnModifierVideo.setVisible(!admin);  btnModifierVideo.setManaged(!admin);  }
        if (btnSupprimerVideo != null) { btnSupprimerVideo.setVisible(!admin); btnSupprimerVideo.setManaged(!admin); }

        if (btnRetourVideo    != null) btnRetourVideo.setOnAction(e -> { stopPlayer(); navigateTo("VideoList.fxml"); });
        if (btnModifierVideo  != null) btnModifierVideo.setOnAction(e -> { stopPlayer(); editMode = true; navigateTo("VideoForm.fxml"); });
        if (btnSupprimerVideo != null) btnSupprimerVideo.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer « " + selectedVideo.getTitre() + " » ?", ButtonType.OK, ButtonType.CANCEL);
            a.setTitle("Supprimer Vidéo"); a.setHeaderText(null);
            a.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    stopPlayer();
                    videoService.supprimerVideo(selectedVideo.getId());
                    selectedVideo = null;
                    navigateTo("VideoList.fxml");
                }
            });
        });
    }

    /** Construit le lecteur approprié selon le type de source */
    private void buildVideoPlayer(String url) {
        videoContainer.getChildren().clear();
        videoContainer.setStyle("-fx-background-color:#000;-fx-background-radius:10;");

        boolean isYoutube = url.contains("youtube") || url.contains("youtu.be");

        // ── Vidéo locale : demander le mode de lecture ──────────────────────
        if (!isYoutube && !url.startsWith("http")) {
            File f = new File(url);
            if (!f.exists()) {
                videoContainer.getChildren().add(errorLabel("❌ Fichier introuvable : " + url));
                return;
            }
            showLaunchChoiceScreen(f, url);
            return;
        }

        if (isYoutube) {
            // ── YouTube → thumbnail + bouton "Ouvrir dans YouTube" ─────────
            String videoId = extractYoutubeId(url);

            // Fond gradient derrière la vignette
            videoContainer.setStyle("-fx-background-color:linear-gradient(to bottom,#0c1220,#000);-fx-background-radius:10;");

            VBox ytBox = new VBox(18);
            ytBox.setAlignment(Pos.CENTER);
            ytBox.setMaxWidth(Double.MAX_VALUE);
            ytBox.setMaxHeight(Double.MAX_VALUE);

            if (videoId != null) {
                // Thumbnail YouTube (chargée en arrière-plan)
                StackPane thumbStack = new StackPane();
                thumbStack.setMaxWidth(640);
                thumbStack.setMaxHeight(360);
                thumbStack.setStyle("-fx-background-color:#000;-fx-background-radius:8;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.8),20,0,0,6);");

                // Image de fond par défaut
                Label defIco = new Label("🎬");
                defIco.setStyle("-fx-font-size:64;-fx-text-fill:rgba(255,255,255,0.2);");
                thumbStack.getChildren().add(defIco);

                try {
                    Image thumb = new Image(
                            "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg",
                            640, 360, false, true, true
                    );
                    ImageView thumbIv = new ImageView(thumb);
                    thumbIv.setFitWidth(640);
                    thumbIv.setFitHeight(360);
                    thumbIv.setPreserveRatio(false);
                    thumbIv.setStyle("-fx-background-radius:8;");
                    thumbStack.getChildren().add(thumbIv);
                } catch (Exception ignored) {}

                // Icône play par-dessus
                Label playOverlay = new Label("▶");
                playOverlay.setStyle("-fx-font-size:64;-fx-text-fill:rgba(255,255,255,0.85);" +
                        "-fx-effect:dropshadow(gaussian,black,12,0.4,0,0);");
                thumbStack.getChildren().add(playOverlay);

                ytBox.getChildren().add(thumbStack);
            }

            // Titre "YouTube"
            Label ytLabel = new Label("🎬  Vidéo YouTube");
            ytLabel.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");

            // Bouton principal
            Button openBtn = new Button("▶  Ouvrir dans YouTube");
            openBtn.setStyle(
                    "-fx-background-color:#dc2626;" +
                            "-fx-text-fill:white;" +
                            "-fx-font-size:15;" +
                            "-fx-font-weight:bold;" +
                            "-fx-background-radius:9;" +
                            "-fx-padding:12 30;" +
                            "-fx-effect:dropshadow(gaussian,rgba(220,38,38,0.55),14,0,0,4);"
            );
            final String finalUrl = url;
            openBtn.setOnAction(ev -> openUrlInBrowser(finalUrl));

            ytBox.getChildren().addAll(ytLabel, openBtn);
            videoContainer.getChildren().add(ytBox);

        } else {
            videoContainer.getChildren().add(errorLabel("⚠️ Aucun lecteur disponible pour cette URL."));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ÉCRAN DE CHOIX — Lancer dans l'app  OU  avec lecteur externe
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Affiche un écran de choix stylisé directement dans videoContainer.
     * L'utilisateur choisit entre lecteur intégré et lecteur externe.
     */
    private void showLaunchChoiceScreen(File f, String url) {
        videoContainer.getChildren().clear();
        videoContainer.setStyle(
            "-fx-background-color:linear-gradient(to bottom,#0c1a2e,#0a0f1a);" +
            "-fx-background-radius:10;"
        );

        // ── Icône / titre ────────────────────────────────────────────────
        Label ico = new Label("🎬");
        ico.setStyle("-fx-font-size:52;");

        String fname = f.getName();
        Label nameLabel = new Label("📁  " + fname);
        nameLabel.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");

        Label question = new Label("Comment voulez-vous lire cette vidéo ?");
        question.setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:15;-fx-font-weight:bold;");

        // ── Bouton : Lire dans l'application ────────────────────────────
        VBox btnInApp = buildChoiceButton(
            "▶  Lire dans l'application",
            "Lecteur intégré  •  Recommandé pour PC performant",
            "#0284c7",
            "#0c1a2e",
            "rgba(2,132,199,0.25)"
        );
        btnInApp.setOnMouseClicked(e -> {
            videoContainer.setStyle("-fx-background-color:#000;-fx-background-radius:10;");
            videoContainer.getChildren().clear();
            buildJfxPlayerInContainer(f, url, videoContainer);
        });

        // ── Bouton : Ouvrir avec lecteur externe ─────────────────────────
        VBox btnExternal = buildChoiceButton(
            "📂  Ouvrir avec lecteur externe",
            "VLC, Windows Media Player…  •  Recommandé pour PC modeste",
            "#7c3aed",
            "#1a0c2e",
            "rgba(124,58,237,0.25)"
        );
        btnExternal.setOnMouseClicked(e -> openWithSystem(f));

        // ── Layout ───────────────────────────────────────────────────────
        HBox buttons = new HBox(20, btnInApp, btnExternal);
        buttons.setAlignment(Pos.CENTER);
        buttons.setMaxWidth(Double.MAX_VALUE);

        VBox box = new VBox(16, ico, nameLabel, question, buttons);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        box.setMaxWidth(Double.MAX_VALUE);

        videoContainer.getChildren().add(box);
    }

    /** Construit un bouton de choix stylisé (carte cliquable). */
    private VBox buildChoiceButton(String title, String subtitle,
                                   String borderColor, String bgColor,
                                   String hoverBg) {
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:white;-fx-font-size:14;-fx-font-weight:bold;");

        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");
        subLbl.setWrapText(true);

        VBox card = new VBox(6, titleLbl, subLbl);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setPadding(new Insets(18, 20, 18, 20));
        card.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-background-radius:12;" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-radius:12;-fx-border-width:2;" +
            "-fx-cursor:hand;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),10,0,0,4);"
        );
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color:" + hoverBg + ";" +
            "-fx-background-radius:12;" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-radius:12;-fx-border-width:2;" +
            "-fx-cursor:hand;" +
            "-fx-effect:dropshadow(gaussian," + borderColor + ",16,0.3,0,0);"
        ));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-background-radius:12;" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-radius:12;-fx-border-width:2;" +
            "-fx-cursor:hand;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),10,0,0,4);"
        ));
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  DÉTECTION VLC / FFMPEG
    // ════════════════════════════════════════════════════════════════════════

    /** Retourne le répertoire d'installation de VLC, ou null si non trouvé. */
    private static String findVlcPath() {
        String[] candidates = {
                "C:\\Program Files\\VideoLAN\\VLC",
                "C:\\Program Files (x86)\\VideoLAN\\VLC",
                System.getenv("USERPROFILE") != null
                        ? System.getenv("USERPROFILE") + "\\AppData\\Local\\VideoLAN\\VLC" : ""
        };
        for (String c : candidates) {
            if (!c.isEmpty() && new File(c, "libvlc.dll").exists()) return c;
        }
        // Vérifier PATH système
        try {
            ProcessBuilder pb = new ProcessBuilder("where", "vlc");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            if (p.waitFor(2, TimeUnit.SECONDS) && p.exitValue() == 0) {
                String line = new String(p.getInputStream().readAllBytes()).trim().split("\\r?\\n")[0];
                File vlcExe = new File(line);
                if (vlcExe.exists()) return vlcExe.getParent();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** Retourne le chemin vers ffmpeg.exe, ou null si non disponible. */
    private static String findFFmpeg() {
        String[] candidates = {
                "C:\\ffmpeg\\bin\\ffmpeg.exe",
                "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe",
                "C:\\Program Files (x86)\\ffmpeg\\bin\\ffmpeg.exe",
                System.getenv("USERPROFILE") != null
                        ? System.getenv("USERPROFILE") + "\\ffmpeg\\bin\\ffmpeg.exe" : ""
        };
        for (String c : candidates) {
            if (!c.isEmpty() && new File(c).exists()) return c;
        }
        // Vérifier PATH
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            if (p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0) return "ffmpeg";
        } catch (Exception ignored) {}
        return null;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  THUMBNAIL LOCAL (FFmpeg async)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Génère la thumbnail d'une vidéo locale en arrière-plan via FFmpeg,
     * puis met à jour l'ImageView sur le thread JavaFX.
     */
    private void generateLocalThumbnail(File videoFile, ImageView target) {
        Thread t = new Thread(() -> {
            try {
                THUMB_CACHE_DIR.mkdirs();
                String hash  = Integer.toHexString(videoFile.getAbsolutePath().hashCode());
                File cache   = new File(THUMB_CACHE_DIR, hash + ".jpg");

                if (!cache.exists() || cache.length() == 0) {
                    String ffmpeg = findFFmpeg();
                    if (ffmpeg == null) return; // FFmpeg absent → pas de thumbnail

                    ProcessBuilder pb = new ProcessBuilder(
                            ffmpeg, "-y",
                            "-ss", "00:00:02",
                            "-i",  videoFile.getAbsolutePath(),
                            "-vframes", "1",
                            "-vf", "scale=295:165:force_original_aspect_ratio=decrease,pad=295:165:(ow-iw)/2:(oh-ih)/2:color=black",
                            "-q:v", "3",
                            cache.getAbsolutePath()
                    );
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    boolean done = p.waitFor(20, TimeUnit.SECONDS);
                    if (!done) { p.destroyForcibly(); return; }
                }

                if (cache.exists() && cache.length() > 0) {
                    Image img = new Image(cache.toURI().toString(), 295, 165, false, true);
                    Platform.runLater(() -> {
                        target.setImage(img);
                        target.setVisible(true);
                    });
                }
            } catch (Exception e) {
                System.err.println("[THUMB] " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LECTEUR VLCJ (tous codecs : H.264, H.265/HEVC, AVI, MKV…)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Construit un lecteur VLC intégré dans videoContainer.
     * Le rendu se fait via un Canvas JavaFX + callback pixel VLCJ.
     */
    private void buildVlcPlayerInContainer(String filePath, StackPane container) {
        try {
            String vlcHome = findVlcPath();
            if (vlcHome == null) { buildJfxPlayerInContainer(new File(filePath), filePath, container); return; }

            // Indiquer à JNA où trouver libvlc.dll
            System.setProperty("jna.library.path", vlcHome);

            // Canvas qui recevra chaque frame décodée par VLC
            // ⚠️ Taille bornée à 1280×720 max pour éviter le dépassement
            //    de texture GPU (NPE NGCanvas$RenderBuf sur cartes faibles)
            Canvas canvas = new Canvas(720, 405);
            canvas.setStyle("-fx-background-color:#000;");
            

            // Lier la taille au container, mais JAMAIS au-delà de 1280×720
            canvas.widthProperty().bind(
                javafx.beans.binding.Bindings.min(container.widthProperty(), 1280)
            );
            canvas.heightProperty().bind(
                javafx.beans.binding.Bindings.min(
                    container.heightProperty().subtract(60), 720
                )
            );

            WritableImage[] imgRef = {new WritableImage(1280, 720)};

            vlcFactory = new MediaPlayerFactory(vlcHome,
                    "--no-video-title-show", "--quiet", "--no-osd",
                    "--no-snapshot-preview");
            vlcPlayer  = vlcFactory.mediaPlayers().newEmbeddedMediaPlayer();

            // ── Buffer format : BGRA 32 bits ──────────────────────────────
            BufferFormatCallback bfc = new BufferFormatCallback() {
                @Override
                public BufferFormat getBufferFormat(int sw, int sh) {
                    // Guard : dimensions valides avant de créer la WritableImage
                    if (sw > 0 && sh > 0)
                        Platform.runLater(() -> imgRef[0] = new WritableImage(sw, sh));
                    return new RV32BufferFormat(sw, sh);
                }
                @Override public void allocatedBuffers(ByteBuffer[] buffers) {}
            };

            // ── Callback de rendu — copie les pixels dans la WritableImage ─
            RenderCallback rc = (mp, nativeBuffers, fmt) -> {
                ByteBuffer buf = nativeBuffers[0];
                final byte[] bytes = new byte[fmt.getWidth() * fmt.getHeight() * 4];
                buf.get(bytes);
                buf.rewind();
                Platform.runLater(() -> {
                    WritableImage img = imgRef[0];
                    double cw = canvas.getWidth();
                    double ch = canvas.getHeight();
                    // Guard : canvas doit avoir des dimensions valides
                    //         ET la WritableImage doit correspondre au format reçu
                    if (img != null && cw > 1 && ch > 1
                            && (int) img.getWidth() == fmt.getWidth()) {
                        img.getPixelWriter().setPixels(
                                0, 0, fmt.getWidth(), fmt.getHeight(),
                                PixelFormat.getByteBgraInstance(),
                                bytes, 0, fmt.getWidth() * 4
                        );
                        canvas.getGraphicsContext2D()
                                .drawImage(img, 0, 0, cw, ch);
                    }
                });
            };

            vlcPlayer.videoSurface().set(
                    vlcFactory.videoSurfaces().newVideoSurface(bfc, rc, true)
            );
            vlcPlayer.media().play(filePath);

            // ── Contrôles VLC ─────────────────────────────────────────────
            final EmbeddedMediaPlayer player = vlcPlayer;

            Slider seekBar = new Slider(0, 1, 0);
            seekBar.setPrefWidth(Double.MAX_VALUE);
            HBox.setHgrow(seekBar, Priority.ALWAYS);
            seekBar.setStyle("-fx-accent:#38bdf8;");

            // Mise à jour de la seek bar (depuis thread VLC → FX thread)
            new Thread(() -> {
                while (player.status().isPlayable() || player.media().info() == null) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { break; }
                    float pos = player.status().position();
                    Platform.runLater(() -> {
                        if (!seekBar.isValueChanging()) seekBar.setValue(pos);
                    });
                }
            }, "vlc-seek-updater").start();

            seekBar.setOnMousePressed(e  -> player.controls().setPosition((float) seekBar.getValue()));
            seekBar.setOnMouseDragged(e  -> player.controls().setPosition((float) seekBar.getValue()));

            Button playPause = new Button("⏸ Pause");
            playPause.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;" +
                    "-fx-font-size:12;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 16;");
            playPause.setPrefWidth(110);
            playPause.setOnAction(e -> {
                if (player.status().isPlaying()) { player.controls().pause(); playPause.setText("▶ Lecture"); }
                else                             { player.controls().play();  playPause.setText("⏸ Pause"); }
            });

            Label volLbl = new Label("🔊");
            volLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");
            Slider volSlider = new Slider(0, 100, 80);
            volSlider.setPrefWidth(90);
            volSlider.setStyle("-fx-accent:#38bdf8;");
            player.audio().setVolume(80);
            volSlider.valueProperty().addListener((obs, o, n) -> player.audio().setVolume(n.intValue()));

            Label timeLbl = new Label("0:00");
            timeLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");
            new Thread(() -> {
                while (true) {
                    try { Thread.sleep(500); } catch (InterruptedException ie) { break; }
                    long ms = player.status().time();
                    Platform.runLater(() -> {
                        int s = (int)(ms / 1000);
                        timeLbl.setText(String.format("%d:%02d", s / 60, s % 60));
                    });
                }
            }, "vlc-time-updater").start();

            HBox seekRow = new HBox(seekBar);
            seekRow.setPadding(new Insets(6, 12, 0, 12));

            HBox ctrlRow = new HBox(12, playPause, volLbl, volSlider, timeLbl);
            ctrlRow.setAlignment(Pos.CENTER_LEFT);
            ctrlRow.setPadding(new Insets(6, 12, 8, 12));

            VBox controls = new VBox(0, seekRow, ctrlRow);
            controls.setStyle("-fx-background-color:rgba(10,20,35,0.95);");
            controls.setPrefHeight(60);

            VBox playerBox = new VBox(canvas, controls);
            playerBox.setMaxWidth(Double.MAX_VALUE);
            playerBox.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(canvas, Priority.ALWAYS);

            container.getChildren().add(playerBox);

            // Bouton discret "Ouvrir avec lecteur système"
            Button sysBtn = new Button("📂 Ouvrir avec le lecteur système");
            sysBtn.setStyle("-fx-background-color:rgba(30,41,59,0.85);-fx-text-fill:#64748b;" +
                    "-fx-font-size:10;-fx-background-radius:5;-fx-padding:4 12;-fx-cursor:hand;");
            sysBtn.setOnAction(ev -> openWithSystem(new File(filePath)));
            StackPane.setAlignment(sysBtn, Pos.TOP_RIGHT);
            StackPane.setMargin(sysBtn, new Insets(6));
            container.getChildren().add(sysBtn);

        } catch (Exception ex) {
            System.err.println("[VLC] Erreur initialisation : " + ex.getMessage());
            buildJfxPlayerInContainer(new File(filePath), filePath, container);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LECTEUR JavaFX MediaPlayer (H.264/MP4 natif — aucune dépendance externe)
    // ════════════════════════════════════════════════════════════════════════

    private void buildJfxPlayerInContainer(File f, String url, StackPane container) {
        try {
            Media media  = new Media(f.toURI().toString());
            activePlayer = new MediaPlayer(media);

            boolean[] failed = {false};
            Runnable onFail = () -> {
                if (failed[0]) return;
                failed[0] = true;
                stopPlayer();
                Platform.runLater(() -> showFallbackPlayer(container, f, url));
            };
            activePlayer.setOnError(() -> onFail.run());
            media.setOnError(onFail::run);

            activePlayer.setAutoPlay(true);

            MediaView mv = new MediaView(activePlayer);
            mv.setPreserveRatio(true);
            mv.setOnError(e -> onFail.run());

            StackPane mediaPane = new StackPane(mv);
            mediaPane.setStyle("-fx-background-color:#000;");
            VBox.setVgrow(mediaPane, Priority.ALWAYS);
            mv.fitWidthProperty().bind(mediaPane.widthProperty());
            mv.fitHeightProperty().bind(mediaPane.heightProperty());

            Slider seekBar = new Slider(0, 1, 0);
            seekBar.setPrefWidth(Double.MAX_VALUE);
            HBox.setHgrow(seekBar, Priority.ALWAYS);
            seekBar.setStyle("-fx-accent:#38bdf8;");
            activePlayer.currentTimeProperty().addListener((obs, o, n) -> {
                if (!seekBar.isValueChanging() && activePlayer.getTotalDuration() != null
                        && !activePlayer.getTotalDuration().isUnknown())
                    seekBar.setValue(n.toSeconds() / activePlayer.getTotalDuration().toSeconds());
            });
            seekBar.setOnMousePressed(e -> { if (activePlayer.getTotalDuration() != null) activePlayer.seek(activePlayer.getTotalDuration().multiply(seekBar.getValue())); });
            seekBar.setOnMouseDragged(e -> { if (activePlayer.getTotalDuration() != null) activePlayer.seek(activePlayer.getTotalDuration().multiply(seekBar.getValue())); });

            Button playPause = new Button("⏸ Pause");
            playPause.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;-fx-font-size:12;-fx-font-weight:bold;-fx-background-radius:6;-fx-padding:6 16;");
            playPause.setPrefWidth(110);
            playPause.setOnAction(e -> {
                if (activePlayer.getStatus() == MediaPlayer.Status.PLAYING) { activePlayer.pause(); playPause.setText("▶ Lecture"); }
                else { activePlayer.play(); playPause.setText("⏸ Pause"); }
            });

            Label volLbl = new Label("🔊");
            volLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");
            Slider volSlider = new Slider(0, 1, 0.8);
            volSlider.setPrefWidth(90); volSlider.setStyle("-fx-accent:#38bdf8;");
            activePlayer.setVolume(0.8);
            volSlider.valueProperty().addListener((obs, o, n) -> activePlayer.setVolume(n.doubleValue()));

            Label timeLbl = new Label("0:00");
            timeLbl.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11;");
            activePlayer.currentTimeProperty().addListener((obs, o, n) -> {
                int s = (int) n.toSeconds();
                timeLbl.setText(String.format("%d:%02d", s / 60, s % 60));
            });

            HBox seekRow = new HBox(seekBar);
            seekRow.setPadding(new Insets(6, 12, 0, 12));
            HBox ctrlRow = new HBox(12, playPause, volLbl, volSlider, timeLbl);
            ctrlRow.setAlignment(Pos.CENTER_LEFT);
            ctrlRow.setPadding(new Insets(6, 12, 8, 12));
            VBox controls = new VBox(0, seekRow, ctrlRow);
            controls.setStyle("-fx-background-color:rgba(10,20,35,0.95);");

            VBox playerBox = new VBox(mediaPane, controls);
            playerBox.setMaxWidth(Double.MAX_VALUE);
            playerBox.setMaxHeight(Double.MAX_VALUE);
            VBox.setVgrow(mediaPane, Priority.ALWAYS);
            container.getChildren().add(playerBox);

        } catch (Exception ex) {
            showFallbackPlayer(container, f, url);
        }
    }

    /**
     * Affiché quand JavaFX MediaPlayer ne peut pas décoder le codec.
     * Tout reste DANS l'application — aucun lecteur externe n'est ouvert.
     */
    private void showFallbackPlayer(StackPane container, File f, String originalUrl) {
        container.getChildren().clear();
        container.setStyle(
                "-fx-background-color:linear-gradient(to bottom,#0c1a2e,#0a0f1a);" +
                "-fx-background-radius:10;"
        );

        String ext = f.getName().contains(".")
                ? f.getName().substring(f.getName().lastIndexOf('.') + 1).toUpperCase() : "?";

        Label ico  = new Label("🎬");
        ico.setStyle("-fx-font-size:48;-fx-text-fill:rgba(255,255,255,0.15);");

        Label titre = new Label("Codec non supporté");
        titre.setStyle("-fx-text-fill:#f1f5f9;-fx-font-size:16;-fx-font-weight:bold;");

        Label sub = new Label(
                "Le lecteur intégré ne peut pas lire ce fichier " + ext + ".\n" +
                "Installez VLC sur votre PC pour activer la lecture de tous les formats."
        );
        sub.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;-fx-text-alignment:center;");
        sub.setWrapText(true); sub.setMaxWidth(400); sub.setAlignment(Pos.CENTER);

        Label fname = new Label("📁  " + f.getName());
        fname.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:11;" +
                "-fx-background-color:#0c2a3e;-fx-background-radius:4;-fx-padding:4 10;");

        VBox box = new VBox(14, ico, titre, sub, fname);
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setMaxHeight(Double.MAX_VALUE);
        container.getChildren().add(box);
    }

    /** Ouvre un fichier avec l'application par défaut du système (VLC, Windows Media Player…) */
    /**
     * Ouvre un fichier avec l'application par défaut du système (en thread séparé
     * pour ne pas bloquer le thread JavaFX).
     */
    private void openWithSystem(File f) {
        new Thread(() -> {
            try {
                // cmd /c start "" "chemin" est la méthode la plus fiable sous Windows
                new ProcessBuilder("cmd", "/c", "start", "", f.getAbsolutePath())
                        .redirectErrorStream(true).start();
            } catch (Exception ex) {
                // Tentative via Desktop en dernier recours
                try {
                    if (Desktop.isDesktopSupported())
                        Desktop.getDesktop().open(f);
                } catch (Exception ex2) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Erreur",
                                    "Impossible d'ouvrir : " + ex2.getMessage()));
                }
            }
        }, "open-system").start();
    }

    /**
     * Ouvre une URL dans le navigateur par défaut.
     * Utilise cmd /c start (Windows) comme méthode principale — plus fiable que
     * Desktop.browse() depuis un processus JavaFX lancé par Maven.
     */
    private void openUrlInBrowser(String url) {
        new Thread(() -> {
            try {
                // Méthode 1 : cmd /c start (Windows — la plus fiable)
                new ProcessBuilder("cmd", "/c", "start", "", url)
                        .redirectErrorStream(true).start();
            } catch (Exception ex1) {
                try {
                    // Méthode 2 : Desktop.browse (autres OS)
                    if (Desktop.isDesktopSupported() &&
                            Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(new URI(url));
                    } else {
                        // Méthode 3 : xdg-open (Linux)
                        new ProcessBuilder("xdg-open", url).start();
                    }
                } catch (Exception ex2) {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Erreur",
                                    "Impossible d'ouvrir le navigateur."));
                }
            }
        }, "open-browser").start();
    }

    private Label errorLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill:#f87171;-fx-font-size:14;-fx-wrap-text:true;");
        return l;
    }

    private void stopPlayer() {
        // Arrêt JavaFX MediaPlayer
        if (activePlayer != null) {
            try { activePlayer.stop();    } catch (Exception ignored) {}
            try { activePlayer.dispose(); } catch (Exception ignored) {}
            activePlayer = null;
        }
        // Arrêt VLCJ
        if (vlcPlayer != null) {
            try { vlcPlayer.controls().stop(); } catch (Exception ignored) {}
            try { vlcPlayer.release();         } catch (Exception ignored) {}
            vlcPlayer = null;
        }
        if (vlcFactory != null) {
            try { vlcFactory.release(); } catch (Exception ignored) {}
            vlcFactory = null;
        }
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
            Node anchor = (cardsPane    != null) ? cardsPane
                    : (txtTitre     != null) ? txtTitre
                    : lblTitreShow;
            if (anchor == null) return;
            Parent view      = FXMLLoader.load(getClass().getResource(BASE + fxmlFile));
            BorderPane root  = (BorderPane) anchor.getScene().getRoot();
            BorderPane cArea = (BorderPane) root.lookup("#contentArea");
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
                "-fx-background-radius:6;-fx-padding:5 12;-fx-font-size:12;");
        return btn;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}