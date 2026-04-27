package com.gamilha.controllers.coaching;

import com.gamilha.MainFX;
import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.entity.SubtitleCue;
import com.gamilha.entity.User;
import com.gamilha.services.CoachingVideoService;
import com.gamilha.services.FavoriteVideoService;
import com.gamilha.services.NotificationService;
import com.gamilha.services.PlaylistService;
import com.gamilha.services.PlaylistStatisticsService;
import com.gamilha.services.SubtitleApiService;
import com.gamilha.utils.SessionContext;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CoachingVideoController {

    private static final String BASE = "/com/gamilha/interfaces/coaching/";
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String LOG_PREFIX = "[SUBTITLE][CTRL]";
    private static CoachingVideo selectedVideo;
    private static Playlist playlistFilter;
    private static boolean editMode;

    // ── Liste ──────────────────────────────────────────────────────────────
    @FXML private TextField searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterPremium;
    @FXML private Label lblPlaylistBadge;
    @FXML private FlowPane cardsPane;
    @FXML private Button btnAdd;

    // ── Formulaire ─────────────────────────────────────────────────────────
    @FXML private Label lblFormTitle;
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cbNiveau;       // ← ComboBox (plus TextField)
    @FXML private TextField txtDuration;
    @FXML private CheckBox cbPremium;
    @FXML private ComboBox<String> cbPlaylist;
    @FXML private VBox localSection;
    @FXML private VBox urlSection;
    @FXML private TextField txtUrl;
    @FXML private Button btnChooseVideo;
    @FXML private Label lblVideoPath;
    @FXML private Button btnToggleLocal;
    @FXML private Button btnToggleUrl;
    @FXML private Label lblErrorVideo;
    @FXML private Button btnSaveVideo;
    @FXML private Button btnCancelVideo;

    // ── Lecteur / Show ─────────────────────────────────────────────────────
    @FXML private Label lblTitreShow;
    @FXML private Label lblDescVideo;
    @FXML private Label lblNiveauVideo;
    @FXML private Label lblDureeVideo;
    @FXML private Label lblPremiumVideo;
    @FXML private Label lblPlaylistVideo;
    @FXML private StackPane videoContainer;
    @FXML private Button btnRetourVideo;
    @FXML private Button btnModifierVideo;
    @FXML private Button btnSupprimerVideo;
    @FXML private Button btnFavoriteVideo;
    @FXML private ComboBox<String> cbSubtitleLanguage;

    // ── Sous-titres automatiques — affichés SOUS la vidéo ──────────────────
    @FXML private Label lblSubtitleDisplay;   // zone de texte sous la vidéo
    @FXML private Label lblSubtitleStatus;    // info de chargement

    // ── Services ───────────────────────────────────────────────────────────
    private final CoachingVideoService videoService           = new CoachingVideoService();
    private final PlaylistService playlistService             = new PlaylistService();
    private final FavoriteVideoService favoriteService        = new FavoriteVideoService();
    private final NotificationService notificationService     = new NotificationService();
    private final PlaylistStatisticsService statisticsService = new PlaylistStatisticsService();
    private final SubtitleApiService subtitleApiService       = new SubtitleApiService();

    // ── État ────────────────────────────────────────────────────────────────
    private final ObservableList<CoachingVideo> masterData = FXCollections.observableArrayList();
    private List<Playlist> allPlaylists = new ArrayList<>();
    private String selectedVideoPath;
    private boolean useLocalFile;

    // ── Lecteur ─────────────────────────────────────────────────────────────
    private MediaPlayer activePlayer;
    private MediaPlayerFactory vlcFactory;
    private EmbeddedMediaPlayer vlcPlayer;

    // ── Sous-titres auto ─────────────────────────────────────────────────────
    private List<SubtitleCue> autoSubtitles = new ArrayList<>();
    private List<SubtitleCue> sourceSubtitles = new ArrayList<>();
    private String sourceSubtitleLanguage = "";
    private final Map<String, List<SubtitleCue>> subtitleCache = new HashMap<>();
    private final Map<String, String> subtitleCodesByLabel = new LinkedHashMap<>();
    private final Map<String, String> subtitleNamesByCode = new LinkedHashMap<>();
    private boolean subtitleGenerationInProgress;
    private boolean updatingSubtitleLanguageChoices;
    private String subtitleRunId = "none";
    private long subtitleFlowStartMs;
    private long lastSubtitleTickLogMs;
    private String lastSubtitleText = "";

    // ════════════════════════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════════════════════════

    public static void setPlaylistFilter(Playlist playlist) { playlistFilter = playlist; }
    public static void setSelectedVideoStatic(CoachingVideo video) { selectedVideo = video; }

    @FXML
    public void initialize() {
        if (cardsPane != null)       initListPage();
        else if (txtTitre != null)   initFormPage();
        else if (lblTitreShow != null) initShowPage();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE LISTE
    // ════════════════════════════════════════════════════════════════════════

    private void initListPage() {
        chargerDonnees();

        filterNiveau.getItems().setAll("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream()
                        .map(CoachingVideo::getNiveau)
                        .filter(v -> v != null && !v.isBlank())
                        .distinct().toList()
        );
        filterNiveau.setValue("Tous");

        filterPremium.getItems().setAll("Tous", "Premium", "Gratuit");
        filterPremium.setValue("Tous");

        if (lblPlaylistBadge != null && playlistFilter != null) {
            lblPlaylistBadge.setText("Playlist : " + playlistFilter.getTitle());
            lblPlaylistBadge.setVisible(true);
            lblPlaylistBadge.setManaged(true);
        }

        if (btnAdd != null) {
            btnAdd.setVisible(!isAdmin());
            btnAdd.setManaged(!isAdmin());
        }

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

    private void buildCards(ObservableList<CoachingVideo> data) {
        cardsPane.getChildren().clear();
        if (data.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("Videos");
            icon.setStyle("-fx-text-fill:#38bdf8;-fx-font-size:22;-fx-font-weight:bold;");
            Label msg  = new Label("Aucune video trouvee.");
            msg.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:14;");
            empty.getChildren().addAll(icon, msg);
            cardsPane.getChildren().add(empty);
            return;
        }
        for (CoachingVideo video : data) cardsPane.getChildren().add(createVideoCard(video));
    }

    private VBox createVideoCard(CoachingVideo video) {
        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setStyle("-fx-background-color:#1e293b;-fx-background-radius:14;"
                + "-fx-border-color:#334155;-fx-border-radius:14;");

        StackPane thumb = new StackPane();
        thumb.setPrefHeight(170);
        thumb.setStyle("-fx-background-color:linear-gradient(to bottom right,#0f172a,#1d4ed8);"
                + "-fx-background-radius:14 14 0 0;");

        Label fallbackIcon = new Label("VIDEO");
        fallbackIcon.setStyle("-fx-text-fill:rgba(255,255,255,0.2);-fx-font-size:20;-fx-font-weight:bold;");
        thumb.getChildren().add(fallbackIcon);

        String url     = video.getUrl() != null ? video.getUrl() : "";
        boolean youtube = isYoutubeUrl(url);
        boolean local   = !url.isBlank() && !url.startsWith("http");

        if (youtube) {
            String vid = extractYoutubeId(url);
            if (vid != null) {
                try {
                    ImageView iv = new ImageView(new Image(
                            "https://img.youtube.com/vi/" + vid + "/mqdefault.jpg",
                            300, 170, false, true, true));
                    iv.setFitWidth(300); iv.setFitHeight(170); iv.setPreserveRatio(false);
                    thumb.getChildren().add(iv);
                } catch (Exception ignored) {}
            }
        } else if (local) {
            Label lb = new Label("LOCAL");
            lb.setStyle("-fx-text-fill:white;-fx-font-size:24;-fx-font-weight:bold;");
            thumb.getChildren().add(lb);
        }

        Label playIcon = new Label("▶");
        playIcon.setStyle("-fx-text-fill:white;-fx-font-size:28;");
        thumb.getChildren().add(playIcon);

        Label durationBadge = new Label(video.getDurationFormatted());
        durationBadge.setStyle("-fx-background-color:rgba(0,0,0,0.75);-fx-text-fill:white;"
                + "-fx-padding:3 8;-fx-background-radius:6;");
        StackPane.setAlignment(durationBadge, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(durationBadge, new Insets(0, 8, 8, 0));
        thumb.getChildren().add(durationBadge);

        Button favoriteButton = createFavoriteButton(video);
        StackPane.setAlignment(favoriteButton, Pos.TOP_LEFT);
        StackPane.setMargin(favoriteButton, new Insets(8, 0, 0, 8));
        thumb.getChildren().add(favoriteButton);

        if (video.isPremium()) {
            Label premiumBadge = new Label("Premium");
            premiumBadge.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;"
                    + "-fx-padding:3 8;-fx-background-radius:6;");
            StackPane.setAlignment(premiumBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(premiumBadge, new Insets(8, 8, 0, 0));
            thumb.getChildren().add(premiumBadge);
        }

        VBox content = new VBox(8);
        content.setPadding(new Insets(14));
        VBox.setVgrow(content, Priority.ALWAYS);

        Label title = new Label(video.getTitre());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill:white;-fx-font-size:15;-fx-font-weight:bold;");

        String descText = video.getDescription() != null ? video.getDescription() : "";
        if (descText.length() > 90) descText = descText.substring(0, 90) + "...";
        Label description = new Label(descText);
        description.setWrapText(true);
        description.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");

        HBox meta = new HBox(6);
        if (video.getNiveau() != null && !video.getNiveau().isBlank()) {
            Label level = new Label(video.getNiveau());
            level.setStyle("-fx-background-color:" + niveauColor(video.getNiveau())
                    + ";-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:20;");
            meta.getChildren().add(level);
        }
        if (video.getPlaylist() != null && video.getPlaylist().getTitle() != null) {
            Label playlist = new Label(video.getPlaylist().getTitle());
            playlist.setStyle("-fx-background-color:#0c4a6e;-fx-text-fill:#38bdf8;"
                    + "-fx-padding:3 8;-fx-background-radius:6;");
            meta.getChildren().add(playlist);
        }
        content.getChildren().addAll(title, description, meta);

        Pane separator = new Pane();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color:#334155;");

        HBox actions = new HBox(8);
        actions.setPadding(new Insets(10, 14, 14, 14));
        actions.setAlignment(Pos.CENTER_LEFT);

        Button voir = styledBtn("Voir", "#0284c7");
        voir.setOnAction(e -> { selectedVideo = video; navigateTo("VideoShow.fxml"); });
        actions.getChildren().add(voir);

        if (!isAdmin()) {
            Button modifier  = styledBtn("Modifier",  "#d97706");
            modifier.setOnAction(e -> { selectedVideo = video; editMode = true; navigateTo("VideoForm.fxml"); });
            actions.getChildren().add(modifier);
        }

        card.getChildren().addAll(thumb, content, separator, actions);
        return card;
    }

    @FXML private void handleAdd() { editMode = false; selectedVideo = null; navigateTo("VideoForm.fxml"); }
    @FXML private void handleRetourPlaylist() { playlistFilter = null; navigateTo("PlaylistList.fxml"); }

    private void filterList() {
        String search  = searchInput.getText() == null ? "" : searchInput.getText().trim().toLowerCase();
        String niveau  = filterNiveau.getValue();
        String premium = filterPremium.getValue();

        ObservableList<CoachingVideo> filtered = masterData.stream()
                .filter(v -> v.getTitre().toLowerCase().contains(search)
                        || (v.getDescription() != null && v.getDescription().toLowerCase().contains(search)))
                .filter(v -> niveau  == null || "Tous".equals(niveau)  || niveau.equals(v.getNiveau()))
                .filter(v -> premium == null || "Tous".equals(premium)
                        || ("Premium".equals(premium) && v.isPremium())
                        || ("Gratuit".equals(premium) && !v.isPremium()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        buildCards(filtered);
    }

    private void confirmDelete(CoachingVideo video) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + video.getTitre() + "\" ?", ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                videoService.supprimerVideo(video.getId());
                masterData.remove(video);
                buildCards(masterData);
            }
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE FORMULAIRE
    // ════════════════════════════════════════════════════════════════════════

    private void initFormPage() {
        allPlaylists = playlistService.afficherPlaylists();

        // Niveau — ComboBox avec valeurs fixes
        if (cbNiveau != null) {
            cbNiveau.getItems().setAll("Debutant", "Intermediaire", "Avance", "Expert");
            cbNiveau.setValue("Debutant");
        }

        // Playlists
        cbPlaylist.getItems().clear();
        cbPlaylist.getItems().add("-- Aucune --");
        allPlaylists.forEach(p -> cbPlaylist.getItems().add(p.getTitle()));
        cbPlaylist.setValue("-- Aucune --");

        if (editMode && selectedVideo != null) {
            if (lblFormTitle != null) lblFormTitle.setText("Modifier la video");
            txtTitre.setText(selectedVideo.getTitre());
            txtDescription.setText(selectedVideo.getDescription() != null ? selectedVideo.getDescription() : "");
            if (cbNiveau != null && selectedVideo.getNiveau() != null) {
                cbNiveau.setValue(selectedVideo.getNiveau());
            }
            txtDuration.setText(String.valueOf(selectedVideo.getDuration()));
            cbPremium.setSelected(selectedVideo.isPremium());
            if (selectedVideo.getPlaylist() != null) cbPlaylist.setValue(selectedVideo.getPlaylist().getTitle());

            String existingUrl = selectedVideo.getUrl() != null ? selectedVideo.getUrl() : "";
            if (!existingUrl.isBlank() && !existingUrl.startsWith("http")) {
                useLocalFile = true;
                selectedVideoPath = existingUrl;
            } else {
                useLocalFile = false;
                txtUrl.setText(existingUrl);
            }
        } else {
            if (lblFormTitle != null) lblFormTitle.setText("Nouvelle video");
            useLocalFile = false;
            if (playlistFilter != null) cbPlaylist.setValue(playlistFilter.getTitle());
        }

        appliquerToggle();

        if (selectedVideoPath != null && lblVideoPath != null)
            lblVideoPath.setText("OK " + new File(selectedVideoPath).getName());

        if (btnToggleLocal != null) btnToggleLocal.setOnAction(e -> { useLocalFile = true;  appliquerToggle(); });
        if (btnToggleUrl   != null) btnToggleUrl.setOnAction(e   -> { useLocalFile = false; appliquerToggle(); });
        if (btnSaveVideo   != null) btnSaveVideo.setOnAction(e   -> handleSave());
        if (btnCancelVideo != null) btnCancelVideo.setOnAction(e -> {
            editMode = false;
            selectedVideoPath = null;
            navigateTo("VideoList.fxml");
        });
    }

    private void appliquerToggle() {
        if (localSection != null) { localSection.setVisible(useLocalFile);  localSection.setManaged(useLocalFile); }
        if (urlSection   != null) { urlSection.setVisible(!useLocalFile);   urlSection.setManaged(!useLocalFile); }

        String activeStyle   = "-fx-background-color:#0284c7;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-padding:8 20;";
        String inactiveStyle = "-fx-background-color:#1e293b;-fx-text-fill:#94a3b8;-fx-background-radius:8;-fx-padding:8 20;";
        if (btnToggleLocal != null) btnToggleLocal.setStyle(useLocalFile  ? activeStyle : inactiveStyle);
        if (btnToggleUrl   != null) btnToggleUrl.setStyle(!useLocalFile   ? activeStyle : inactiveStyle);
    }

    @FXML
    private void handleChooseVideo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une video");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Videos", "*.mp4","*.avi","*.mkv","*.mov","*.wmv","*.flv","*.webm"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );
        File file = chooser.showOpenDialog(btnChooseVideo.getScene().getWindow());
        if (file != null) {
            selectedVideoPath = file.getAbsolutePath();
            if (lblVideoPath != null) lblVideoPath.setText("OK " + file.getName());
        }
    }

    private void handleSave() {
        lblErrorVideo.setText("");

        String title = txtTitre.getText().trim();
        if (title.isEmpty()) { lblErrorVideo.setText("Le titre est obligatoire."); return; }

        String finalUrl = useLocalFile
                ? (selectedVideoPath != null ? selectedVideoPath : "")
                : txtUrl.getText().trim();
        if (finalUrl.isBlank()) { lblErrorVideo.setText("La source video est obligatoire."); return; }

        int duration = 0;
        try {
            if (!txtDuration.getText().trim().isBlank())
                duration = Integer.parseInt(txtDuration.getText().trim());
        } catch (NumberFormatException ex) {
            lblErrorVideo.setText("La duree doit etre un entier."); return;
        }

        // Niveau depuis la ComboBox
        String niveau = (cbNiveau != null && cbNiveau.getValue() != null)
                ? cbNiveau.getValue() : "";

        Playlist playlist   = getSelectedPlaylist();
        boolean creating    = !(editMode && selectedVideo != null);
        CoachingVideo video = creating ? new CoachingVideo() : selectedVideo;
        video.setTitre(title);
        video.setDescription(txtDescription.getText().trim());
        video.setUrl(finalUrl);
        video.setNiveau(niveau);
        video.setPremium(cbPremium.isSelected());
        video.setDuration(duration);
        video.setPlaylist(playlist);

        if (creating) videoService.ajouterVideo(video);
        else          videoService.modifierVideo(video);

        if (creating) notificationService.notifyVideoAdded(playlist, video);

        MainFX.refreshNavigationBadges();
        showAlert(Alert.AlertType.INFORMATION, "Succes",
                creating ? "Video ajoutee avec succes !" : "Video modifiee avec succes !");

        editMode          = false;
        selectedVideo     = null;
        selectedVideoPath = null;
        navigateTo("VideoList.fxml");
    }

    private Playlist getSelectedPlaylist() {
        if (cbPlaylist == null || cbPlaylist.getValue() == null || "-- Aucune --".equals(cbPlaylist.getValue()))
            return null;
        return allPlaylists.stream()
                .filter(p -> p.getTitle().equals(cbPlaylist.getValue()))
                .findFirst().orElse(null);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAGE SHOW (Lecteur)
    // ════════════════════════════════════════════════════════════════════════

    private void initShowPage() {
        if (selectedVideo == null) { navigateTo("VideoList.fxml"); return; }

        logInfo("initShowPage video id=" + selectedVideo.getId()
                + ", title=" + selectedVideo.getTitre()
                + ", url=" + selectedVideo.getUrl()
                + ", userId=" + getCurrentUserId()
                + ", isAdmin=" + isAdmin());

        lblTitreShow.setText(selectedVideo.getTitre());
        lblDescVideo.setText(selectedVideo.getDescription() != null && !selectedVideo.getDescription().isBlank()
                ? selectedVideo.getDescription() : "-");
        lblNiveauVideo.setText(selectedVideo.getNiveau() != null && !selectedVideo.getNiveau().isBlank()
                ? selectedVideo.getNiveau() : "-");
        lblDureeVideo.setText(selectedVideo.getDurationFormatted());
        lblPremiumVideo.setText(selectedVideo.isPremium() ? "Premium" : "Gratuit");
        lblPlaylistVideo.setText(selectedVideo.getPlaylist() != null ? selectedVideo.getPlaylist().getTitle() : "-");

        statisticsService.recordView(selectedVideo, getCurrentUserId());

        String videoUrl = selectedVideo.getUrl() != null ? selectedVideo.getUrl() : "";
        buildVideoPlayer(videoUrl);
        lancerSousTitresAuto(videoUrl);

        configureFavoriteShowButton();

        boolean admin = isAdmin();
        btnModifierVideo.setVisible(!admin); btnModifierVideo.setManaged(!admin);
        btnSupprimerVideo.setVisible(!admin); btnSupprimerVideo.setManaged(!admin);

        btnRetourVideo.setOnAction(e -> { stopPlayer(); navigateTo("VideoList.fxml"); });
        btnModifierVideo.setOnAction(e -> { stopPlayer(); editMode = true; navigateTo("VideoForm.fxml"); });
        btnSupprimerVideo.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer \"" + selectedVideo.getTitre() + "\" ?", ButtonType.OK, ButtonType.CANCEL);
            alert.setHeaderText(null);
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    stopPlayer();
                    videoService.supprimerVideo(selectedVideo.getId());
                    selectedVideo = null;
                    navigateTo("VideoList.fxml");
                }
            });
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SOUS-TITRES AUTOMATIQUES — API
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Lance la récupération des sous-titres en arrière-plan.
     * - YouTube : appel à l'API timedtext (gratuit, sans clé)
     * - Fichier local : message informatif (extensible à Whisper API, AssemblyAI, etc.)
     */
    private void lancerSousTitresAuto(String videoUrl) {
        lancerSousTitresAutoV2(videoUrl);
    }

    private void configureSubtitleLanguageComboUi() {
        if (cbSubtitleLanguage == null) {
            return;
        }

        cbSubtitleLanguage.setStyle(
                "-fx-background-color:#0f172a;"
                        + "-fx-text-fill:#e2e8f0;"
                        + "-fx-prompt-text-fill:#64748b;"
                        + "-fx-mark-color:#38bdf8;"
        );

        cbSubtitleLanguage.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.web("#e2e8f0"));
                setStyle("-fx-background-color:#0f172a;");
            }
        });

        cbSubtitleLanguage.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setTextFill(Color.web("#e2e8f0"));
                setStyle(empty ? "-fx-background-color:#0f172a;" : "-fx-background-color:#111827;");
            }
        });
    }

    /** Affiche ou cache la zone de sous-titres sous la vidéo */
    private void afficherSousTitre(String text) {
        if (lblSubtitleDisplay == null) return;
        boolean visible = text != null && !text.isBlank();
        lblSubtitleDisplay.setText(visible ? text : "");
        lblSubtitleDisplay.setVisible(visible);
        lblSubtitleDisplay.setManaged(visible);

        String normalized = text == null ? "" : text.trim();
        if (!normalized.equals(lastSubtitleText)) {
            lastSubtitleText = normalized;
            if (!normalized.isEmpty()) {
                logInfo("subtitle display update: text=" + shorten(normalized, 120));
            }
        }
    }

    private void afficherStatutSousTitres(String msg) {
        if (lblSubtitleStatus == null) return;
        lblSubtitleStatus.setText(msg);
        lblSubtitleStatus.setVisible(true);
        lblSubtitleStatus.setManaged(true);
        logInfo("subtitle status: " + msg);
    }

    private void masquerStatutSousTitres() {
        if (lblSubtitleStatus == null) return;
        lblSubtitleStatus.setVisible(false);
        lblSubtitleStatus.setManaged(false);
        logInfo("subtitle status cleared");
    }

    /** Appelé à chaque tick du player pour mettre à jour les sous-titres */
    private void updateSubtitleDisplay(long positionMillis) {
        if (autoSubtitles == null || autoSubtitles.isEmpty()) {
            afficherSousTitre("");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastSubtitleTickLogMs > 5000) {
            lastSubtitleTickLogMs = now;
            logInfo("subtitle tick: positionMs=" + positionMillis + ", activeCues=" + autoSubtitles.size());
        }

        String text = subtitleApiService.getSubtitleAt(autoSubtitles, positionMillis);
        afficherSousTitre(text);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  LECTEUR VIDÉO
    // ════════════════════════════════════════════════════════════════════════

    private void buildVideoPlayer(String url) {
        buildIntegratedVideoPlayer(url);
    }

    private void buildYoutubePreview(String url) {
        String videoId = extractYoutubeId(url);
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);

        StackPane preview = new StackPane();
        preview.setMaxWidth(760); preview.setMaxHeight(420);
        preview.setStyle("-fx-background-color:#0f172a;-fx-background-radius:12;");

        if (videoId != null) {
            try {
                ImageView iv = new ImageView(new Image(
                        "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg",
                        760, 420, false, true, true));
                iv.setFitWidth(760); iv.setFitHeight(420); iv.setPreserveRatio(false);
                preview.getChildren().add(iv);
            } catch (Exception ignored) {}
        }
        Label play = new Label("Ouvrir dans YouTube");
        play.setStyle("-fx-text-fill:white;-fx-font-size:22;-fx-font-weight:bold;");
        preview.getChildren().add(play);

        Button openButton = new Button("Lancer dans le navigateur");
        openButton.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-background-radius:8;-fx-padding:10 18;");
        openButton.setOnAction(e -> openUrlInBrowser(url));

        content.getChildren().addAll(preview, openButton);
        videoContainer.getChildren().add(content);
    }

    private void buildVlcPlayerInContainer(String filePath, StackPane container) {
        String vlcHome = findVlcPath();
        if (vlcHome == null) { buildJfxPlayerInContainer(new File(filePath), container); return; }

        try {
            System.setProperty("jna.library.path", vlcHome);
            vlcFactory = new MediaPlayerFactory("--quiet", "--no-video-title-show", "--no-osd");
            vlcPlayer  = vlcFactory.mediaPlayers().newEmbeddedMediaPlayer();

            Canvas canvas = new Canvas(960, 520);
            WritableImage[] imageRef = {new WritableImage(960, 520)};

            BufferFormatCallback formatCallback = new BufferFormatCallback() {
                @Override
                public BufferFormat getBufferFormat(int sw, int sh) {
                    int w = Math.max(sw, 1), h = Math.max(sh, 1);
                    Platform.runLater(() -> imageRef[0] = new WritableImage(w, h));
                    return new RV32BufferFormat(w, h);
                }
                @Override public void allocatedBuffers(ByteBuffer[] b) {}
            };

            RenderCallback renderCallback = (mp, nativeBuffers, bufferFormat) -> {
                ByteBuffer buf = nativeBuffers[0];
                byte[] bytes = new byte[bufferFormat.getWidth() * bufferFormat.getHeight() * 4];
                buf.get(bytes); buf.rewind();
                Platform.runLater(() -> {
                    WritableImage img = imageRef[0];
                    if (img != null) {
                        img.getPixelWriter().setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(),
                                PixelFormat.getByteBgraInstance(), bytes, 0, bufferFormat.getWidth() * 4);
                        canvas.getGraphicsContext2D().drawImage(img, 0, 0, canvas.getWidth(), canvas.getHeight());
                    }
                });
            };

            vlcPlayer.videoSurface().set(
                    vlcFactory.videoSurfaces().newVideoSurface(formatCallback, renderCallback, true));
            vlcPlayer.media().play(filePath);

            Slider seekBar = new Slider(0, 1, 0);
            seekBar.setPrefWidth(Double.MAX_VALUE);
            HBox.setHgrow(seekBar, Priority.ALWAYS);

            Button playPause = new Button("Pause");
            playPause.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;-fx-background-radius:8;");
            playPause.setOnAction(e -> {
                if (vlcPlayer.status().isPlaying()) { vlcPlayer.controls().pause(); playPause.setText("Lecture"); }
                else { vlcPlayer.controls().play(); playPause.setText("Pause"); }
            });

            Label timeLabel = new Label("0:00");
            timeLabel.setStyle("-fx-text-fill:#94a3b8;");

            Slider volume = new Slider(0, 100, 80);
            volume.setPrefWidth(100);
            vlcPlayer.audio().setVolume(80);
            volume.valueProperty().addListener((obs, o, n) -> vlcPlayer.audio().setVolume(n.intValue()));

            Thread updater = new Thread(() -> {
                while (vlcPlayer != null) {
                    try { Thread.sleep(400); } catch (InterruptedException e) { break; }
                    long time     = vlcPlayer.status().time();
                    float position = vlcPlayer.status().position();
                    Platform.runLater(() -> {
                        if (!seekBar.isValueChanging()) seekBar.setValue(position);
                        int s = (int)(time / 1000);
                        timeLabel.setText(String.format("%d:%02d", s / 60, s % 60));
                        updateSubtitleDisplay(time);  // ← mise à jour sous-titres
                    });
                }
            }, "vlc-updater");
            updater.setDaemon(true);
            updater.start();

            seekBar.setOnMousePressed(e  -> vlcPlayer.controls().setPosition((float) seekBar.getValue()));
            seekBar.setOnMouseDragged(e  -> vlcPlayer.controls().setPosition((float) seekBar.getValue()));

            Button systemButton = new Button("Lecteur systeme");
            systemButton.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#cbd5e1;-fx-background-radius:8;");
            systemButton.setOnAction(e -> openWithSystem(new File(filePath)));

            // Row 1 : barre de progression + temps
            HBox.setHgrow(seekBar, Priority.ALWAYS);
            HBox progressRow = new HBox(8, seekBar, timeLabel);
            progressRow.setAlignment(Pos.CENTER_LEFT);

            // Row 2 : lecture/pause à gauche | volume à droite | lecteur système
            Label volIcon = new Label("🔊");
            volIcon.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");
            Pane spacerCtrl = new Pane();
            HBox.setHgrow(spacerCtrl, Priority.ALWAYS);
            HBox buttonRow = new HBox(10, playPause, spacerCtrl, volIcon, volume, systemButton);
            buttonRow.setAlignment(Pos.CENTER_LEFT);

            VBox controls = new VBox(4, progressRow, buttonRow);
            controls.setPadding(new Insets(8, 12, 8, 12));
            controls.setStyle("-fx-background-color:#0f172a;");

            VBox wrapper = new VBox(canvas, controls);
            wrapper.setFillWidth(true);
            container.getChildren().add(wrapper);

        } catch (Exception ex) {
            buildJfxPlayerInContainer(new File(filePath), container);
        }
    }

    private void buildJfxPlayerInContainer(File file, StackPane container) {
        try {
            Media media      = new Media(file.toURI().toString());
            activePlayer     = new MediaPlayer(media);
            activePlayer.setAutoPlay(true);

            MediaView mediaView = new MediaView(activePlayer);
            mediaView.setPreserveRatio(true);

            StackPane mediaPane = new StackPane(mediaView);
            mediaPane.setStyle("-fx-background-color:#000;");
            VBox.setVgrow(mediaPane, Priority.ALWAYS);
            mediaView.fitWidthProperty().bind(mediaPane.widthProperty());
            mediaView.fitHeightProperty().bind(mediaPane.heightProperty());

            Slider seekBar = new Slider(0, 1, 0);
            seekBar.setPrefWidth(Double.MAX_VALUE);
            HBox.setHgrow(seekBar, Priority.ALWAYS);

            activePlayer.currentTimeProperty().addListener((obs, o, n) -> {
                if (!seekBar.isValueChanging() && activePlayer.getTotalDuration() != null
                        && !activePlayer.getTotalDuration().isUnknown()
                        && activePlayer.getTotalDuration().toMillis() > 0)
                    seekBar.setValue(n.toMillis() / activePlayer.getTotalDuration().toMillis());
                updateSubtitleDisplay((long) n.toMillis());  // ← mise à jour sous-titres
            });

            seekBar.setOnMousePressed(e -> { if (activePlayer.getTotalDuration() != null)
                activePlayer.seek(activePlayer.getTotalDuration().multiply(seekBar.getValue())); });
            seekBar.setOnMouseDragged(e -> { if (activePlayer.getTotalDuration() != null)
                activePlayer.seek(activePlayer.getTotalDuration().multiply(seekBar.getValue())); });

            Button playPause = new Button("Pause");
            playPause.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;-fx-background-radius:8;");
            playPause.setOnAction(e -> {
                if (activePlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    activePlayer.pause(); playPause.setText("Lecture");
                } else { activePlayer.play(); playPause.setText("Pause"); }
            });

            Slider volume = new Slider(0, 1, 0.8);
            volume.setPrefWidth(100);
            activePlayer.setVolume(0.8);
            volume.valueProperty().addListener((obs, o, n) -> activePlayer.setVolume(n.doubleValue()));

            Label timeLabel = new Label("0:00");
            timeLabel.setStyle("-fx-text-fill:#94a3b8;");
            activePlayer.currentTimeProperty().addListener((obs, o, n) -> {
                int s = (int) n.toSeconds();
                timeLabel.setText(String.format("%d:%02d", s / 60, s % 60));
            });

            Button systemButton = new Button("Lecteur systeme");
            systemButton.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#cbd5e1;-fx-background-radius:8;");
            systemButton.setOnAction(e -> openWithSystem(file));

            // Row 1 : barre de progression + temps
            HBox.setHgrow(seekBar, Priority.ALWAYS);
            HBox progressRow = new HBox(8, seekBar, timeLabel);
            progressRow.setAlignment(Pos.CENTER_LEFT);

            // Row 2 : lecture/pause à gauche | volume à droite | lecteur système
            Label volIcon = new Label("🔊");
            volIcon.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");
            Pane spacerCtrl = new Pane();
            HBox.setHgrow(spacerCtrl, Priority.ALWAYS);
            HBox buttonRow = new HBox(10, playPause, spacerCtrl, volIcon, volume, systemButton);
            buttonRow.setAlignment(Pos.CENTER_LEFT);

            VBox controls = new VBox(4, progressRow, buttonRow);
            controls.setPadding(new Insets(8, 12, 8, 12));
            controls.setStyle("-fx-background-color:#0f172a;");

            VBox wrapper = new VBox(mediaPane, controls);
            wrapper.setFillWidth(true);
            container.getChildren().add(wrapper);

            activePlayer.setOnError(() -> buildFallbackPlayer(container, file));
            media.setOnError(() -> buildFallbackPlayer(container, file));

        } catch (Exception ex) {
            buildFallbackPlayer(container, file);
        }
    }

    private void buildFallbackPlayer(StackPane container, File file) {
        stopPlayer();
        container.getChildren().clear();
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));
        Label title = new Label("Lecture integree indisponible");
        title.setStyle("-fx-text-fill:white;-fx-font-size:18;-fx-font-weight:bold;");
        Label message = new Label("Cette video ne peut pas etre decodee par le lecteur integre.");
        message.setWrapText(true);
        message.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");
        Button openButton = new Button("Ouvrir avec le lecteur systeme");
        openButton.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;-fx-background-radius:8;");
        openButton.setOnAction(e -> openWithSystem(file));
        box.getChildren().addAll(title, message, openButton);
        container.getChildren().add(box);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FAVORIS
    // ════════════════════════════════════════════════════════════════════════

    private void lancerSousTitresAutoV2(String videoUrl) {
        subtitleRunId = UUID.randomUUID().toString().substring(0, 8);
        subtitleFlowStartMs = System.currentTimeMillis();
        lastSubtitleTickLogMs = 0L;
        lastSubtitleText = "";
        logInfo("subtitle flow start: runId=" + subtitleRunId + ", url=" + videoUrl);

        autoSubtitles = new ArrayList<>();
        sourceSubtitles = new ArrayList<>();
        sourceSubtitleLanguage = "";
        subtitleCache.clear();
        subtitleCodesByLabel.clear();
        subtitleNamesByCode.clear();
        subtitleGenerationInProgress = false;
        afficherSousTitre("");

        configureSubtitleLanguageComboUi();

        List<SubtitleApiService.LanguageOption> fallbackLanguages = subtitleApiService.getFallbackTranslationLanguages();
        populateSubtitleLanguages("auto", fallbackLanguages);
        logInfo("subtitle languages preloaded from fallback list: count=" + fallbackLanguages.size());

        if (cbSubtitleLanguage != null) {
            cbSubtitleLanguage.setOnAction(e -> handleSubtitleLanguageSelection());
            cbSubtitleLanguage.setDisable(false);
        }

        if (videoUrl == null || videoUrl.isBlank()) {
            logWarn("subtitle flow aborted: empty video url");
            return;
        }

        subtitleGenerationInProgress = true;
        logInfo("subtitle generation flag=true");

        if (isYoutubeUrl(videoUrl)) {
            afficherStatutSousTitres("Chargement des sous-titres YouTube...");
            String videoId = extractYoutubeId(videoUrl);
            if (videoId == null) {
                afficherStatutSousTitres("Impossible d'extraire l'identifiant YouTube.");
                subtitleGenerationInProgress = false;
                logWarn("subtitle flow YouTube failed: unable to extract videoId");
                return;
            }
            logInfo("subtitle flow YouTube branch: videoId=" + videoId);

            Thread thread = new Thread(() -> {
                List<SubtitleCue> cues = subtitleApiService.fetchYoutubeCaptions(videoId);
                List<SubtitleApiService.LanguageOption> languages = subtitleApiService.fetchTranslationLanguages();
                Platform.runLater(() -> {
                    sourceSubtitles = cues;
                    autoSubtitles = cues;
                    if (cues.isEmpty()) {
                        afficherStatutSousTitres("Aucun sous-titre automatique disponible pour cette video.");
                        logWarn("subtitle flow YouTube: 0 cues returned");
                    } else {
                        sourceSubtitleLanguage = "auto";
                        subtitleCache.put("auto", cues);
                        populateSubtitleLanguages("auto", languages);
                        masquerStatutSousTitres();
                        logInfo("subtitle flow YouTube success: cues=" + cues.size()
                                + ", languages=" + languages.size()
                                + ", elapsedMs=" + (System.currentTimeMillis() - subtitleFlowStartMs));
                    }
                    subtitleGenerationInProgress = false;
                    logInfo("subtitle generation flag=false");
                });
            }, "youtube-subtitle-loader");
            thread.setDaemon(true);
            thread.start();
            return;
        }

        File mediaFile = new File(videoUrl);
        if (!mediaFile.exists()) {
            afficherStatutSousTitres("Le fichier video local est introuvable.");
            subtitleGenerationInProgress = false;
            logWarn("subtitle flow local failed: media file not found " + mediaFile.getAbsolutePath());
            return;
        }
        logInfo("subtitle flow local branch: file=" + mediaFile.getAbsolutePath() + ", bytes=" + mediaFile.length());

        if (!subtitleApiService.hasAssemblyAiApiKey()) {
            afficherStatutSousTitres("Sous-titres API inactifs. Ajoutez ASSEMBLYAI_API_KEY pour activer la transcription automatique.");
            subtitleGenerationInProgress = false;
            logWarn("subtitle flow local failed: AssemblyAI API key missing");
            return;
        }

        afficherStatutSousTitres("Generation des sous-titres automatiques via API...");
        Thread thread = new Thread(() -> {
            try {
                SubtitleApiService.GeneratedSubtitleTrack track = subtitleApiService.generateLocalSubtitles(mediaFile);
                List<SubtitleApiService.LanguageOption> languages;
                try {
                    languages = subtitleApiService.fetchTranslationLanguages();
                } catch (Exception ignored) {
                    languages = new ArrayList<>();
                    logWarn("subtitle flow local: translation language fetch failed, using empty list");
                }
                List<SubtitleApiService.LanguageOption> availableLanguages = languages;

                Platform.runLater(() -> applyLocalSubtitleTrack(track, availableLanguages));
            } catch (Exception ex) {
                Platform.runLater(() -> afficherStatutSousTitres(
                        "Sous-titrage API indisponible: " + readableMessage(ex)));
                subtitleGenerationInProgress = false;
                logError("subtitle flow local failed: " + ex.getMessage(), ex);
            }
        }, "local-subtitle-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyLocalSubtitleTrack(SubtitleApiService.GeneratedSubtitleTrack track,
                                         List<SubtitleApiService.LanguageOption> languages) {
        subtitleGenerationInProgress = false;
        logInfo("subtitle generation flag=false");
        sourceSubtitles = track.getCues() != null ? track.getCues() : new ArrayList<>();
        autoSubtitles = sourceSubtitles;
        sourceSubtitleLanguage = subtitleApiService.normalizeLanguageCode(track.getDetectedLanguage());
        if (sourceSubtitleLanguage.isBlank()) {
            sourceSubtitleLanguage = "auto";
        }

        subtitleCache.put(sourceSubtitleLanguage, sourceSubtitles);
        populateSubtitleLanguages(sourceSubtitleLanguage, languages);
        logInfo("applyLocalSubtitleTrack: transcriptId=" + track.getTranscriptId()
                + ", detectedLanguage=" + sourceSubtitleLanguage
                + ", cues=" + sourceSubtitles.size()
                + ", languageChoices=" + subtitleCodesByLabel.size()
                + ", elapsedMs=" + (System.currentTimeMillis() - subtitleFlowStartMs));

        if (sourceSubtitles.isEmpty()) {
            afficherStatutSousTitres("L'API n'a retourne aucun sous-titre pour cette video.");
            logWarn("applyLocalSubtitleTrack: no cues returned");
            return;
        }

        masquerStatutSousTitres();
        updateSubtitleDisplay(currentPlaybackPositionMillis());
    }

    private void populateSubtitleLanguages(String sourceLanguage,
                                           List<SubtitleApiService.LanguageOption> languages) {
        if (cbSubtitleLanguage == null) {
            return;
        }

        subtitleCodesByLabel.clear();
        subtitleNamesByCode.clear();

        String sourceName = resolveLanguageName(sourceLanguage, languages);
        registerSubtitleLanguage(sourceLanguage, "Original - " + sourceName);

        languages.stream()
                .sorted(Comparator.comparing(SubtitleApiService.LanguageOption::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(language -> {
                    String code = subtitleApiService.normalizeLanguageCode(language.getCode());
                    if (!code.isBlank() && !code.equals(sourceLanguage)) {
                        registerSubtitleLanguage(code, language.getName() + " (" + code + ")");
                    }
                });

        updatingSubtitleLanguageChoices = true;
        try {
            cbSubtitleLanguage.getItems().setAll(subtitleCodesByLabel.keySet());
            cbSubtitleLanguage.setDisable(subtitleCodesByLabel.isEmpty());
            if (!subtitleCodesByLabel.isEmpty()) {
                cbSubtitleLanguage.setValue(subtitleCodesByLabel.keySet().iterator().next());
            } else {
                cbSubtitleLanguage.setValue(null);
            }
            logInfo("populateSubtitleLanguages: source=" + sourceLanguage
                    + ", requestedLangs=" + languages.size()
                    + ", comboItems=" + subtitleCodesByLabel.size()
                    + ", selected=" + cbSubtitleLanguage.getValue());
        } finally {
            updatingSubtitleLanguageChoices = false;
        }
    }

    private void registerSubtitleLanguage(String code, String label) {
        String normalizedCode = subtitleApiService.normalizeLanguageCode(code);
        if (normalizedCode.isBlank() || subtitleNamesByCode.containsKey(normalizedCode)) {
            return;
        }
        subtitleCodesByLabel.put(label, normalizedCode);
        subtitleNamesByCode.put(normalizedCode, label);
    }

    private String resolveLanguageName(String code, List<SubtitleApiService.LanguageOption> languages) {
        String normalizedCode = subtitleApiService.normalizeLanguageCode(code);
        for (SubtitleApiService.LanguageOption language : languages) {
            if (subtitleApiService.normalizeLanguageCode(language.getCode()).equals(normalizedCode)) {
                return language.getName() + " (" + normalizedCode + ")";
            }
        }
        return normalizedCode.isBlank() || "auto".equals(normalizedCode)
                ? "Langue detectee"
                : normalizedCode.toUpperCase();
    }

    private void handleSubtitleLanguageSelection() {
        if (cbSubtitleLanguage == null || updatingSubtitleLanguageChoices) {
            return;
        }

        String selectedLabel = cbSubtitleLanguage.getValue();
        if (selectedLabel == null || selectedLabel.isBlank()) {
            return;
        }
        logInfo("handleSubtitleLanguageSelection: selectedLabel=" + selectedLabel);

        String targetLanguage = subtitleCodesByLabel.get(selectedLabel);
        if (targetLanguage == null || targetLanguage.isBlank()) {
            logWarn("handleSubtitleLanguageSelection: no code for label=" + selectedLabel);
            return;
        }
        logInfo("handleSubtitleLanguageSelection: targetLanguage=" + targetLanguage
                + ", sourceLanguage=" + sourceSubtitleLanguage
                + ", sourceCues=" + sourceSubtitles.size());

        List<SubtitleCue> cached = subtitleCache.get(targetLanguage);
        if (cached != null) {
            autoSubtitles = cached;
            masquerStatutSousTitres();
            updateSubtitleDisplay(currentPlaybackPositionMillis());
            logInfo("handleSubtitleLanguageSelection: cache hit, cues=" + cached.size());
            return;
        }

        if (sourceSubtitles.isEmpty()) {
            if (subtitleGenerationInProgress) {
                afficherStatutSousTitres("Generation des sous-titres en cours... Merci de patienter.");
            } else {
                afficherStatutSousTitres("Aucun sous-titre source disponible pour la traduction.");
            }
            return;
        }

        afficherStatutSousTitres("Traduction des sous-titres vers " + selectedLabel + "...");
        Thread thread = new Thread(() -> {
            try {
                long startedAt = System.currentTimeMillis();
                List<SubtitleCue> translated = subtitleApiService.translateSubtitles(
                        sourceSubtitles,
                        sourceSubtitleLanguage,
                        targetLanguage
                );

                Platform.runLater(() -> {
                    subtitleCache.put(targetLanguage, translated);
                    if (selectedLabel.equals(cbSubtitleLanguage.getValue())) {
                        autoSubtitles = translated;
                        masquerStatutSousTitres();
                        updateSubtitleDisplay(currentPlaybackPositionMillis());
                    }
                    logInfo("handleSubtitleLanguageSelection: translation success target=" + targetLanguage
                            + ", cues=" + translated.size()
                            + ", elapsedMs=" + (System.currentTimeMillis() - startedAt));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> afficherStatutSousTitres(
                        "Traduction indisponible: " + readableMessage(ex)));
                logError("handleSubtitleLanguageSelection translation failed: " + ex.getMessage(), ex);
            }
        }, "subtitle-translation-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void buildIntegratedVideoPlayer(String url) {
        stopPlayer();
        videoContainer.getChildren().clear();
        videoContainer.setAlignment(Pos.CENTER);
        videoContainer.setStyle("-fx-background-color:#000;-fx-background-radius:10;");
        logInfo("buildIntegratedVideoPlayer start: url=" + url);

        if (url == null || url.isBlank()) {
            videoContainer.getChildren().add(errorLabel("Aucune source video disponible."));
            logWarn("buildIntegratedVideoPlayer aborted: empty url");
            return;
        }
        if (isYoutubeUrl(url)) {
            logInfo("buildIntegratedVideoPlayer detected YouTube URL");
            buildYoutubePreview(url);
            return;
        }

        File file = new File(url);
        if (!file.exists()) {
            videoContainer.getChildren().add(errorLabel("Fichier introuvable : " + url));
            logWarn("buildIntegratedVideoPlayer file not found: " + url);
            return;
        }

        if (tryBuildJfxPlayerInContainer(file, videoContainer)) {
            logInfo("buildIntegratedVideoPlayer using JavaFX native media player");
            return;
        }
        if (tryBuildVlcPlayerInContainer(file.getAbsolutePath(), videoContainer)) {
            logInfo("buildIntegratedVideoPlayer using VLCJ fallback player");
            return;
        }

        logWarn("buildIntegratedVideoPlayer no embedded player available for file=" + file.getAbsolutePath());
        buildEmbeddedPlaybackError(videoContainer, file);
    }

    private boolean tryBuildVlcPlayerInContainer(String filePath, StackPane container) {
        String vlcHome = findVlcPath();
        if (vlcHome == null) {
            logWarn("tryBuildVlcPlayerInContainer: VLC not found on machine.");
            return false;
        }

        try {
            logInfo("tryBuildVlcPlayerInContainer start: vlcHome=" + vlcHome + ", filePath=" + filePath);
            System.setProperty("jna.library.path", vlcHome);
            vlcFactory = new MediaPlayerFactory("--quiet", "--no-video-title-show", "--no-osd");
            vlcPlayer = vlcFactory.mediaPlayers().newEmbeddedMediaPlayer();

            Canvas canvas = new Canvas(960, 520);
            WritableImage[] imageRef = {new WritableImage(960, 520)};

            BufferFormatCallback formatCallback = new BufferFormatCallback() {
                @Override
                public BufferFormat getBufferFormat(int sw, int sh) {
                    int w = Math.max(sw, 1);
                    int h = Math.max(sh, 1);
                    Platform.runLater(() -> imageRef[0] = new WritableImage(w, h));
                    return new RV32BufferFormat(w, h);
                }

                @Override
                public void allocatedBuffers(ByteBuffer[] buffers) {
                }
            };

            RenderCallback renderCallback = (mp, nativeBuffers, bufferFormat) -> {
                ByteBuffer buffer = nativeBuffers[0];
                byte[] bytes = new byte[bufferFormat.getWidth() * bufferFormat.getHeight() * 4];
                buffer.get(bytes);
                buffer.rewind();
                Platform.runLater(() -> {
                    WritableImage image = imageRef[0];
                    if (image != null) {
                        image.getPixelWriter().setPixels(
                                0,
                                0,
                                bufferFormat.getWidth(),
                                bufferFormat.getHeight(),
                                PixelFormat.getByteBgraInstance(),
                                bytes,
                                0,
                                bufferFormat.getWidth() * 4
                        );
                        canvas.getGraphicsContext2D().drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight());
                    }
                });
            };

            vlcPlayer.videoSurface().set(
                    vlcFactory.videoSurfaces().newVideoSurface(formatCallback, renderCallback, true));
            vlcPlayer.media().play(filePath);

            Slider seekBar = new Slider(0, 1, 0);
            seekBar.setMinWidth(260);
            seekBar.setPrefWidth(Double.MAX_VALUE);
            HBox.setHgrow(seekBar, Priority.ALWAYS);

            Button playPause = new Button("Pause");
            playPause.setMinWidth(96);
            playPause.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;-fx-background-radius:8;-fx-font-weight:bold;");
            playPause.setOnAction(e -> {
                if (vlcPlayer.status().isPlaying()) {
                    vlcPlayer.controls().pause();
                    playPause.setText("Lecture");
                } else {
                    vlcPlayer.controls().play();
                    playPause.setText("Pause");
                }
            });

            Label timeLabel = new Label("0:00");
            timeLabel.setStyle("-fx-text-fill:#94a3b8;");

            Slider volume = new Slider(0, 100, 80);
            volume.setMinWidth(110);
            volume.setPrefWidth(110);
            vlcPlayer.audio().setVolume(80);
            volume.valueProperty().addListener((obs, oldValue, newValue) ->
                    vlcPlayer.audio().setVolume(newValue.intValue()));

            Thread updater = new Thread(() -> {
                while (vlcPlayer != null) {
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ex) {
                        break;
                    }

                    long time = vlcPlayer.status().time();
                    float position = vlcPlayer.status().position();
                    Platform.runLater(() -> {
                        if (!seekBar.isValueChanging()) {
                            seekBar.setValue(position);
                        }
                        int seconds = (int) (time / 1000);
                        timeLabel.setText(String.format("%d:%02d", seconds / 60, seconds % 60));
                        updateSubtitleDisplay(time);
                    });
                }
            }, "vlc-updater-v2");
            updater.setDaemon(true);
            updater.start();

            seekBar.setOnMousePressed(e -> vlcPlayer.controls().setPosition((float) seekBar.getValue()));
            seekBar.setOnMouseDragged(e -> vlcPlayer.controls().setPosition((float) seekBar.getValue()));

            // Row 1 : barre de progression + temps
            HBox.setHgrow(seekBar, Priority.ALWAYS);
            HBox progressRow = new HBox(8, seekBar, timeLabel);
            progressRow.setAlignment(Pos.CENTER_LEFT);

            // Row 2 : lecture/pause à gauche | volume à droite
            Label volIcon = new Label("🔊");
            volIcon.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");
            Pane spacerCtrl = new Pane();
            HBox.setHgrow(spacerCtrl, Priority.ALWAYS);
            HBox buttonRow = new HBox(10, playPause, spacerCtrl, volIcon, volume);
            buttonRow.setAlignment(Pos.CENTER_LEFT);

            VBox controls = new VBox(4, progressRow, buttonRow);
            controls.setPadding(new Insets(8, 12, 8, 12));
            controls.setStyle("-fx-background-color:#0f172a;");

            StackPane mediaPane = new StackPane(canvas);
            mediaPane.setStyle("-fx-background-color:#000;");
            mediaPane.setPrefHeight(460);
            VBox.setVgrow(mediaPane, Priority.ALWAYS);
            canvas.widthProperty().bind(mediaPane.widthProperty());
            canvas.heightProperty().bind(mediaPane.heightProperty());

            VBox wrapper = new VBox(mediaPane, controls);
            wrapper.setFillWidth(true);
            wrapper.setMaxWidth(1120);
            StackPane.setAlignment(wrapper, Pos.CENTER);
            container.getChildren().add(wrapper);
            logInfo("tryBuildVlcPlayerInContainer success");
            return true;
        } catch (Exception ex) {
            logError("tryBuildVlcPlayerInContainer failed: " + ex.getMessage(), ex);
            return false;
        }
    }

    private boolean tryBuildJfxPlayerInContainer(File file, StackPane container) {
        try {
            logInfo("tryBuildJfxPlayerInContainer start: file=" + file.getAbsolutePath());
            Media media = new Media(file.toURI().toString());
            activePlayer = new MediaPlayer(media);
            activePlayer.setAutoPlay(true);

            MediaView mediaView = new MediaView(activePlayer);
            mediaView.setPreserveRatio(true);

            StackPane mediaPane = new StackPane(mediaView);
            mediaPane.setStyle("-fx-background-color:#000;");
            VBox.setVgrow(mediaPane, Priority.ALWAYS);
            mediaView.fitWidthProperty().bind(mediaPane.widthProperty());
            mediaView.fitHeightProperty().bind(mediaPane.heightProperty());

            Slider seekBar = new Slider(0, 1, 0);
            seekBar.setMinWidth(260);
            seekBar.setPrefWidth(Double.MAX_VALUE);
            HBox.setHgrow(seekBar, Priority.ALWAYS);

            activePlayer.currentTimeProperty().addListener((obs, oldValue, newValue) -> {
                if (!seekBar.isValueChanging()
                        && activePlayer.getTotalDuration() != null
                        && !activePlayer.getTotalDuration().isUnknown()
                        && activePlayer.getTotalDuration().toMillis() > 0) {
                    seekBar.setValue(newValue.toMillis() / activePlayer.getTotalDuration().toMillis());
                }
                updateSubtitleDisplay((long) newValue.toMillis());
            });

            seekBar.setOnMousePressed(e -> {
                if (activePlayer.getTotalDuration() != null) {
                    activePlayer.seek(activePlayer.getTotalDuration().multiply(seekBar.getValue()));
                }
            });
            seekBar.setOnMouseDragged(e -> {
                if (activePlayer.getTotalDuration() != null) {
                    activePlayer.seek(activePlayer.getTotalDuration().multiply(seekBar.getValue()));
                }
            });

            Button playPause = new Button("Pause");
            playPause.setMinWidth(96);
            playPause.setStyle("-fx-background-color:#0284c7;-fx-text-fill:white;-fx-background-radius:8;-fx-font-weight:bold;");
            playPause.setOnAction(e -> {
                if (activePlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    activePlayer.pause();
                    playPause.setText("Lecture");
                } else {
                    activePlayer.play();
                    playPause.setText("Pause");
                }
            });

            Slider volume = new Slider(0, 1, 0.8);
            volume.setMinWidth(110);
            volume.setPrefWidth(110);
            activePlayer.setVolume(0.8);
            volume.valueProperty().addListener((obs, oldValue, newValue) ->
                    activePlayer.setVolume(newValue.doubleValue()));

            Label timeLabel = new Label("0:00");
            timeLabel.setStyle("-fx-text-fill:#94a3b8;");
            activePlayer.currentTimeProperty().addListener((obs, oldValue, newValue) -> {
                int seconds = (int) newValue.toSeconds();
                timeLabel.setText(String.format("%d:%02d", seconds / 60, seconds % 60));
            });

            // Row 1 : barre de progression + temps
            HBox.setHgrow(seekBar, Priority.ALWAYS);
            HBox progressRow = new HBox(8, seekBar, timeLabel);
            progressRow.setAlignment(Pos.CENTER_LEFT);

            // Row 2 : lecture/pause à gauche | volume à droite
            Label volIcon = new Label("🔊");
            volIcon.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:13;");
            Pane spacerCtrl = new Pane();
            HBox.setHgrow(spacerCtrl, Priority.ALWAYS);
            HBox buttonRow = new HBox(10, playPause, spacerCtrl, volIcon, volume);
            buttonRow.setAlignment(Pos.CENTER_LEFT);

            VBox controls = new VBox(4, progressRow, buttonRow);
            controls.setPadding(new Insets(8, 12, 8, 12));
            controls.setStyle("-fx-background-color:#0f172a;");

            VBox wrapper = new VBox(mediaPane, controls);
            wrapper.setFillWidth(true);
            wrapper.setMaxWidth(1120);
            StackPane.setAlignment(wrapper, Pos.CENTER);
            container.getChildren().add(wrapper);
            activePlayer.setOnError(() -> buildEmbeddedPlaybackError(container, file));
            media.setOnError(() -> buildEmbeddedPlaybackError(container, file));
            logInfo("tryBuildJfxPlayerInContainer success");
            return true;
        } catch (Exception ex) {
            logError("tryBuildJfxPlayerInContainer failed: " + ex.getMessage(), ex);
            return false;
        }
    }

    private void buildEmbeddedPlaybackError(StackPane container, File file) {
        stopPlayer();
        container.getChildren().clear();

        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(24));

        Label title = new Label("Lecture integree indisponible");
        title.setStyle("-fx-text-fill:white;-fx-font-size:18;-fx-font-weight:bold;");

        Label message = new Label(
                "Cette video ne peut pas etre lue avec le decodeur integre de cette machine.");
        message.setWrapText(true);
        message.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");

        Label path = new Label(file.getAbsolutePath());
        path.setWrapText(true);
        path.setStyle("-fx-text-fill:#475569;-fx-font-size:11;");

        box.getChildren().addAll(title, message, path);
        container.getChildren().add(box);
    }

    private long currentPlaybackPositionMillis() {
        try {
            if (activePlayer != null && activePlayer.getCurrentTime() != null) {
                return (long) activePlayer.getCurrentTime().toMillis();
            }
        } catch (Exception ignored) {
        }

        try {
            if (vlcPlayer != null) {
                return vlcPlayer.status().time();
            }
        } catch (Exception ignored) {
        }

        return 0L;
    }

    private String readableMessage(Exception ex) {
        String message = ex.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            return ex.getCause().getMessage();
        }
        return "Erreur reseau ou configuration API manquante.";
    }

    private void configureFavoriteShowButton() {
        if (btnFavoriteVideo == null || selectedVideo == null) return;
        refreshFavoriteButton(btnFavoriteVideo, selectedVideo);
        btnFavoriteVideo.setOnAction(e -> toggleFavorite(selectedVideo, btnFavoriteVideo));
    }

    private Button createFavoriteButton(CoachingVideo video) {
        Button button = new Button();
        button.setFocusTraversable(false);
        refreshFavoriteButton(button, video);
        button.setOnAction(e -> { e.consume(); toggleFavorite(video, button); });
        return button;
    }

    private void refreshFavoriteButton(Button button, CoachingVideo video) {
        boolean fav = favoriteService.isFavorite(getCurrentUserId(), video.getId());
        button.setText(fav ? "❤" : "♡");
        button.setStyle("-fx-background-color:" + (fav ? "#be185d" : "rgba(15,23,42,0.75)") + ";"
                + "-fx-text-fill:white;-fx-background-radius:20;-fx-padding:6 10;-fx-font-size:14;");
    }

    private void toggleFavorite(CoachingVideo video, Button button) {
        favoriteService.toggleFavorite(getCurrentUserId(), video);
        refreshFavoriteButton(button, video);
        MainFX.refreshNavigationBadges();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    private String findVlcPath() {
        String envPath = System.getenv("VLC_HOME");
        if (envPath != null && !envPath.isBlank() && new File(envPath, "libvlc.dll").exists()) {
            return envPath;
        }

        for (String path : new String[]{
                "C:\\Program Files\\VideoLAN\\VLC",
                "C:\\Program Files (x86)\\VideoLAN\\VLC",
                "C:\\Users\\m50057189\\AppData\\Local\\Programs\\VideoLAN\\VLC"}) {
            if (new File(path, "libvlc.dll").exists()) return path;
        }

        try {
            Process process = new ProcessBuilder("where", "vlc").redirectErrorStream(true).start();
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String executable = reader.readLine();
                if (executable != null && !executable.isBlank()) {
                    File parent = new File(executable).getParentFile();
                    if (parent != null && new File(parent, "libvlc.dll").exists()) {
                        return parent.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void openWithSystem(File file) {
        new Thread(() -> {
            try {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath())
                        .redirectErrorStream(true).start();
            } catch (Exception ex) {
                try { if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file); }
                catch (Exception ignored) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible d'ouvrir le lecteur systeme."));
                }
            }
        }, "open-system-player").start();
    }

    private void openUrlInBrowser(String url) {
        new Thread(() -> {
            try {
                new ProcessBuilder("cmd", "/c", "start", "", url)
                        .redirectErrorStream(true).start();
            } catch (Exception ex1) {
                try {
                    if (Desktop.isDesktopSupported()
                            && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
                        Desktop.getDesktop().browse(new URI(url));
                } catch (Exception ex2) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible d'ouvrir le navigateur."));
                }
            }
        }, "open-browser").start();
    }

    private Integer getCurrentUserId() {
        User user = SessionContext.getCurrentUser();
        return user != null ? user.getId() : null;
    }

    private boolean isYoutubeUrl(String url) {
        return url.contains("youtube") || url.contains("youtu.be");
    }

    private String niveauColor(String niveau) {
        if (niveau == null) return "#475569";
        return switch (niveau.toLowerCase()) {
            case "debutant"      -> "#166534";
            case "intermediaire" -> "#1d4ed8";
            case "avance"        -> "#c2410c";
            case "expert"        -> "#7c3aed";
            default              -> "#475569";
        };
    }

    private String extractYoutubeId(String url) {
        Pattern p = Pattern.compile(
                "(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|v/))([^&?\\s]+)");
        Matcher m = p.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private void stopPlayer() {
        if (activePlayer != null) {
            try { activePlayer.stop(); activePlayer.dispose(); } catch (Exception ignored) {}
            activePlayer = null;
        }
        if (vlcPlayer != null) {
            try { vlcPlayer.controls().stop(); vlcPlayer.release(); } catch (Exception ignored) {}
            vlcPlayer = null;
        }
        if (vlcFactory != null) {
            try { vlcFactory.release(); } catch (Exception ignored) {}
            vlcFactory = null;
        }
    }

    private void navigateTo(String fxmlFile) {
        try {
            Node anchor = cardsPane != null ? cardsPane
                    : (txtTitre != null ? txtTitre : lblTitreShow);
            if (anchor == null) { MainFX.navigateFromShell(fxmlFile); return; }
            Parent view = FXMLLoader.load(getClass().getResource(BASE + fxmlFile));
            BorderPane root = (BorderPane) anchor.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            if (contentArea != null) contentArea.setCenter(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private boolean isAdmin() {
        User user = SessionContext.getCurrentUser();
        return user != null && user.getRoles() != null
                && user.getRoles().stream().anyMatch(r -> r.toUpperCase().contains("ADMIN"));
    }

    private Button styledBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setMinWidth(104);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                + "-fx-background-radius:8;-fx-font-weight:bold;-fx-font-size:12;");
        return btn;
    }

    private Label errorLabel(String message) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill:#f87171;-fx-font-size:14;");
        return label;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logInfo(String message) {
        System.out.println(LOG_PREFIX + "[" + LocalDateTime.now().format(LOG_TIME) + "][run=" + subtitleRunId + "] " + message);
    }

    private void logWarn(String message) {
        System.out.println(LOG_PREFIX + "[" + LocalDateTime.now().format(LOG_TIME) + "][run=" + subtitleRunId + "][WARN] " + message);
    }

    private void logError(String message, Throwable throwable) {
        System.out.println(LOG_PREFIX + "[" + LocalDateTime.now().format(LOG_TIME) + "][run=" + subtitleRunId + "][ERROR] " + message);
        if (throwable != null) {
            throwable.printStackTrace(System.out);
        }
    }

    private String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\n', ' ').replace('\r', ' ');
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max) + "...";
    }
}
