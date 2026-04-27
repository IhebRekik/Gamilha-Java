package com.gamilha.controllers.coaching;

import com.gamilha.MainFX;
import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.entity.User;
import com.gamilha.services.CoachingVideoService;
import com.gamilha.services.FavoriteVideoService;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlaylistController {

    private static final String BASE = "/com/gamilha/interfaces/coaching/";
    private static Playlist selectedPlaylist;
    private static boolean editMode;

    @FXML private TextField searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private FlowPane cardsPane;
    @FXML private Button btnAdd;

    @FXML private Label lblFormTitle;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbNiveau;
    @FXML private TextField txtCategorie;
    @FXML private ImageView imgPreview;
    @FXML private Label lblImagePath;
    @FXML private Button btnChooseImage;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    @FXML private Label lblTitre;
    @FXML private ImageView imgPlaylist;
    @FXML private Label lblDescShow;
    @FXML private Label lblNiveauShow;
    @FXML private Label lblCategorieShow;
    @FXML private Label lblDate;
    @FXML private Label lblVideosCount;
    @FXML private FlowPane videosPane;
    @FXML private Button btnRetour;
    @FXML private Button btnVoirToutesVideos;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private PlaylistStatisticsController statsPanelController;

    private final PlaylistService playlistService = new PlaylistService();
    private final CoachingVideoService videoService = new CoachingVideoService();
    private final FavoriteVideoService favoriteService = new FavoriteVideoService();
    private final ObservableList<Playlist> masterData = FXCollections.observableArrayList();

    private String selectedImagePath;

    @FXML
    public void initialize() {
        if (cardsPane != null) {
            initListPage();
        } else if (txtTitle != null) {
            initFormPage();
        } else if (lblTitre != null) {
            initShowPage();
        }
    }

    private void initListPage() {
        masterData.setAll(playlistService.afficherPlaylists());

        filterNiveau.getItems().setAll("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream()
                        .map(Playlist::getNiveau)
                        .filter(value -> value != null && !value.isBlank())
                        .distinct()
                        .toList()
        );
        filterNiveau.setValue("Tous");

        if (btnAdd != null) {
            btnAdd.setVisible(!isAdmin());
            btnAdd.setManaged(!isAdmin());
        }

        buildCards(masterData);
        searchInput.textProperty().addListener((obs, oldValue, newValue) -> filterList());
        filterNiveau.valueProperty().addListener((obs, oldValue, newValue) -> filterList());
    }

    private void buildCards(ObservableList<Playlist> data) {
        cardsPane.getChildren().clear();
        if (data.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("PLAYLISTS");
            icon.setStyle("-fx-text-fill:#c084fc;-fx-font-size:22;-fx-font-weight:bold;");
            Label msg = new Label("Aucune playlist trouvee.");
            msg.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:14;");
            empty.getChildren().addAll(icon, msg);
            cardsPane.getChildren().add(empty);
            return;
        }

        for (Playlist playlist : data) {
            cardsPane.getChildren().add(createPlaylistCard(playlist));
        }
    }

    private VBox createPlaylistCard(Playlist playlist) {
        VBox card = new VBox(0);
        card.setPrefWidth(290);
        card.setStyle(
                "-fx-background-color:#1e293b;-fx-background-radius:14;"
                        + "-fx-border-color:#334155;-fx-border-radius:14;"
        );

        StackPane header = new StackPane();
        header.setPrefHeight(170);
        header.setStyle("-fx-background-color:linear-gradient(to bottom right,#4c1d95,#1d4ed8);-fx-background-radius:14 14 0 0;");

        Label fallback = new Label("PLAYLIST");
        fallback.setStyle("-fx-text-fill:rgba(255,255,255,0.2);-fx-font-size:20;-fx-font-weight:bold;");
        header.getChildren().add(fallback);

        if (playlist.getImage() != null && !playlist.getImage().isBlank()) {
            try {
                String imageUrl = playlist.getImage().startsWith("http")
                        ? playlist.getImage()
                        : new File(playlist.getImage()).toURI().toString();
                ImageView imageView = new ImageView(new Image(imageUrl, 290, 170, false, true, true));
                imageView.setFitWidth(290);
                imageView.setFitHeight(170);
                imageView.setPreserveRatio(false);
                header.getChildren().add(imageView);
            } catch (Exception ignored) {
            }
        }

        if (playlist.getNiveau() != null && !playlist.getNiveau().isBlank()) {
            Label badge = new Label(playlist.getNiveau());
            badge.setStyle("-fx-background-color:" + niveauColor(playlist.getNiveau()) + ";-fx-text-fill:white;-fx-padding:4 10;-fx-background-radius:20;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
            header.getChildren().add(badge);
        }

        VBox content = new VBox(8);
        content.setPadding(new Insets(14));

        Label title = new Label(playlist.getTitle());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill:white;-fx-font-size:15;-fx-font-weight:bold;");

        String descriptionText = playlist.getDescription() != null ? playlist.getDescription() : "";
        if (descriptionText.length() > 90) {
            descriptionText = descriptionText.substring(0, 90) + "...";
        }
        Label description = new Label(descriptionText);
        description.setWrapText(true);
        description.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:12;");

        HBox meta = new HBox(8);
        if (playlist.getCategorie() != null && !playlist.getCategorie().isBlank()) {
            Label category = new Label(playlist.getCategorie());
            category.setStyle("-fx-background-color:#0c4a6e;-fx-text-fill:#38bdf8;-fx-padding:3 8;-fx-background-radius:6;");
            meta.getChildren().add(category);
        }

        content.getChildren().addAll(title, description, meta);

        Pane separator = new Pane();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color:#334155;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10, 14, 14, 14));

        Button ouvrir = styledBtn("Ouvrir", "#2563eb");
        ouvrir.setOnAction(e -> {
            selectedPlaylist = playlist;
            navigateTo("PlaylistShow.fxml");
        });
        HBox.setHgrow(ouvrir, Priority.ALWAYS);

        Button videos = styledBtn("Videos", "#7c3aed");
        videos.setOnAction(e -> {
            selectedPlaylist = playlist;
            CoachingVideoController.setPlaylistFilter(playlist);
            navigateTo("VideoList.fxml");
        });
        HBox.setHgrow(videos, Priority.ALWAYS);

        actions.getChildren().addAll(ouvrir, videos);

        card.getChildren().addAll(header, content, separator, actions);
        return card;
    }

    @FXML
    private void handleAdd() {
        editMode = false;
        selectedPlaylist = null;
        navigateTo("PlaylistForm.fxml");
    }

    private void filterList() {
        String search = searchInput.getText() == null ? "" : searchInput.getText().trim().toLowerCase();
        String niveau = filterNiveau.getValue();

        ObservableList<Playlist> filtered = masterData.stream()
                .filter(playlist -> playlist.getTitle().toLowerCase().contains(search)
                        || (playlist.getDescription() != null && playlist.getDescription().toLowerCase().contains(search)))
                .filter(playlist -> niveau == null || "Tous".equals(niveau) || niveau.equals(playlist.getNiveau()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
        buildCards(filtered);
    }

    private void confirmDelete(Playlist playlist) {
        Alert alert = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Supprimer \"" + playlist.getTitle() + "\" ?",
                ButtonType.OK,
                ButtonType.CANCEL
        );
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                playlistService.supprimerPlaylist(playlist.getId());
                masterData.remove(playlist);
                buildCards(masterData);
            }
        });
    }

    private void initFormPage() {
        cmbNiveau.getItems().setAll("Debutant", "Intermediaire", "Avance", "Expert");

        if (editMode && selectedPlaylist != null) {
            lblFormTitle.setText("Modifier la playlist");
            txtTitle.setText(selectedPlaylist.getTitle());
            txtDescription.setText(selectedPlaylist.getDescription() != null ? selectedPlaylist.getDescription() : "");
            cmbNiveau.setValue(selectedPlaylist.getNiveau() != null ? selectedPlaylist.getNiveau() : "Debutant");
            txtCategorie.setText(selectedPlaylist.getCategorie() != null ? selectedPlaylist.getCategorie() : "");

            selectedImagePath = selectedPlaylist.getImage();
            if (selectedImagePath != null && !selectedImagePath.isBlank()) {
                try {
                    String imageUrl = selectedImagePath.startsWith("http")
                            ? selectedImagePath
                            : new File(selectedImagePath).toURI().toString();
                    imgPreview.setImage(new Image(imageUrl, 240, 130, false, true));
                    imgPreview.setVisible(true);
                    imgPreview.setManaged(true);
                    lblImagePath.setText(new File(selectedImagePath).getName());
                } catch (Exception ignored) {
                }
            }
        } else {
            lblFormTitle.setText("Nouvelle playlist");
            cmbNiveau.setValue("Debutant");
        }

        btnSave.setOnAction(e -> handleSave());
        btnCancel.setOnAction(e -> {
            editMode = false;
            selectedImagePath = null;
            navigateTo("PlaylistList.fxml");
        });
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp")
        );
        File file = chooser.showOpenDialog(btnChooseImage.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            lblImagePath.setText("OK " + file.getName());
            imgPreview.setImage(new Image(file.toURI().toString(), 240, 130, false, true));
            imgPreview.setVisible(true);
            imgPreview.setManaged(true);
        }
    }

    private void handleSave() {
        lblError.setText("");
        String title = txtTitle.getText().trim();
        if (title.isEmpty()) {
            lblError.setText("Le titre est obligatoire.");
            return;
        }

        String description = txtDescription.getText().trim();
        String level = cmbNiveau.getValue();
        String category = txtCategorie.getText().trim();

        if (editMode && selectedPlaylist != null) {
            selectedPlaylist.setTitle(title);
            selectedPlaylist.setDescription(description);
            selectedPlaylist.setNiveau(level);
            selectedPlaylist.setCategorie(category);
            selectedPlaylist.setImage(selectedImagePath);
            playlistService.modifierPlaylist(selectedPlaylist);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Playlist modifiee avec succes !");
        } else {
            Playlist playlist = new Playlist(title, description, level, category, selectedImagePath, LocalDateTime.now());
            playlistService.ajouterPlaylist(playlist);
            showAlert(Alert.AlertType.INFORMATION, "Succes", "Playlist ajoutee avec succes !");
        }

        editMode = false;
        selectedPlaylist = null;
        selectedImagePath = null;
        navigateTo("PlaylistList.fxml");
    }

    private void initShowPage() {
        if (selectedPlaylist == null) {
            navigateTo("PlaylistList.fxml");
            return;
        }

        lblTitre.setText(selectedPlaylist.getTitle());
        lblDescShow.setText(selectedPlaylist.getDescription() != null && !selectedPlaylist.getDescription().isBlank()
                ? selectedPlaylist.getDescription()
                : "-");
        lblNiveauShow.setText(selectedPlaylist.getNiveau() != null ? selectedPlaylist.getNiveau() : "-");
        lblCategorieShow.setText(selectedPlaylist.getCategorie() != null ? selectedPlaylist.getCategorie() : "-");
        lblDate.setText(selectedPlaylist.getCreatedAt() != null ? selectedPlaylist.getCreatedAt().toLocalDate().toString() : "-");

        if (imgPlaylist != null && selectedPlaylist.getImage() != null && !selectedPlaylist.getImage().isBlank()) {
            try {
                String imageUrl = selectedPlaylist.getImage().startsWith("http")
                        ? selectedPlaylist.getImage()
                        : new File(selectedPlaylist.getImage()).toURI().toString();
                imgPlaylist.setImage(new Image(imageUrl, 1100, 240, false, true));
                imgPlaylist.setVisible(true);
                imgPlaylist.setManaged(true);
            } catch (Exception ignored) {
            }
        }

        if (statsPanelController != null) {
            statsPanelController.loadStatistics(selectedPlaylist);
        }

        boolean admin = isAdmin();
        btnModifier.setVisible(!admin);
        btnModifier.setManaged(!admin);
        btnSupprimer.setVisible(!admin);
        btnSupprimer.setManaged(!admin);

        btnRetour.setOnAction(e -> navigateTo("PlaylistList.fxml"));
        if (btnVoirToutesVideos != null) {
            btnVoirToutesVideos.setOnAction(e -> {
                CoachingVideoController.setPlaylistFilter(selectedPlaylist);
                navigateTo("VideoList.fxml");
            });
        }
        btnModifier.setOnAction(e -> {
            editMode = true;
            navigateTo("PlaylistForm.fxml");
        });
        btnSupprimer.setOnAction(e -> {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Supprimer \"" + selectedPlaylist.getTitle() + "\" ?",
                    ButtonType.OK,
                    ButtonType.CANCEL
            );
            alert.setHeaderText(null);
            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    playlistService.supprimerPlaylist(selectedPlaylist.getId());
                    selectedPlaylist = null;
                    navigateTo("PlaylistList.fxml");
                }
            });
        });

        chargerVideos();
    }

    private void chargerVideos() {
        List<CoachingVideo> videos = videoService.afficherVideosByPlaylist(selectedPlaylist.getId());
        videosPane.getChildren().clear();
        if (lblVideosCount != null) {
            lblVideosCount.setText(videos.size() + " video(s)");
        }

        if (videos.isEmpty()) {
            VBox empty = new VBox(10);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(40));
            Label msg = new Label("Aucune video dans cette playlist.");
            msg.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:14;");
            empty.getChildren().add(msg);
            videosPane.getChildren().add(empty);
            return;
        }

        for (CoachingVideo video : videos) {
            videosPane.getChildren().add(createVideoMiniCard(video));
        }
    }

    private VBox createVideoMiniCard(CoachingVideo video) {
        VBox card = new VBox(0);
        card.setPrefWidth(250);
        card.setStyle(
                "-fx-background-color:#0f172a;-fx-background-radius:12;"
                        + "-fx-border-color:#334155;-fx-border-radius:12;"
        );

        StackPane thumb = new StackPane();
        thumb.setPrefHeight(140);
        thumb.setStyle("-fx-background-color:#111827;-fx-background-radius:12 12 0 0;");

        String url = video.getUrl() != null ? video.getUrl() : "";
        if (url.contains("youtube") || url.contains("youtu.be")) {
            String videoId = extractYoutubeId(url);
            if (videoId != null) {
                try {
                    ImageView imageView = new ImageView(new Image(
                            "https://img.youtube.com/vi/" + videoId + "/mqdefault.jpg",
                            250, 140, false, true, true
                    ));
                    imageView.setFitWidth(250);
                    imageView.setFitHeight(140);
                    imageView.setPreserveRatio(false);
                    thumb.getChildren().add(imageView);
                } catch (Exception ignored) {
                }
            }
        } else {
            Label local = new Label("LOCAL");
            local.setStyle("-fx-text-fill:white;-fx-font-size:22;-fx-font-weight:bold;");
            thumb.getChildren().add(local);
        }

        Label play = new Label("â–¶");
        play.setStyle("-fx-text-fill:white;-fx-font-size:24;");
        thumb.getChildren().add(play);

        Label duration = new Label(video.getDurationFormatted());
        duration.setStyle("-fx-background-color:rgba(0,0,0,0.75);-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:6;");
        StackPane.setAlignment(duration, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(duration, new Insets(0, 8, 8, 0));
        thumb.getChildren().add(duration);

        Button favoriteButton = createFavoriteButton(video);
        StackPane.setAlignment(favoriteButton, Pos.TOP_LEFT);
        StackPane.setMargin(favoriteButton, new Insets(8, 0, 0, 8));
        thumb.getChildren().add(favoriteButton);

        if (video.isPremium()) {
            Label premium = new Label("Premium");
            premium.setStyle("-fx-background-color:#7c3aed;-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:6;");
            StackPane.setAlignment(premium, Pos.TOP_RIGHT);
            StackPane.setMargin(premium, new Insets(8, 8, 0, 0));
            thumb.getChildren().add(premium);
        }

        VBox content = new VBox(6);
        content.setPadding(new Insets(12));

        Label title = new Label(video.getTitre());
        title.setWrapText(true);
        title.setStyle("-fx-text-fill:white;-fx-font-size:13;-fx-font-weight:bold;");

        if (video.getNiveau() != null && !video.getNiveau().isBlank()) {
            Label level = new Label(video.getNiveau());
            level.setStyle("-fx-background-color:" + niveauColor(video.getNiveau()) + ";-fx-text-fill:white;-fx-padding:3 8;-fx-background-radius:6;");
            content.getChildren().addAll(title, level);
        } else {
            content.getChildren().add(title);
        }

        Button voir = new Button("Voir la video");
        voir.setMaxWidth(Double.MAX_VALUE);
        voir.setStyle("-fx-background-color:#1e40af;-fx-text-fill:white;-fx-background-radius:0 0 12 12;");
        voir.setOnAction(e -> {
            CoachingVideoController.setSelectedVideoStatic(video);
            navigateTo("VideoShow.fxml");
        });

        card.getChildren().addAll(thumb, content, voir);
        return card;
    }

    private Button createFavoriteButton(CoachingVideo video) {
        Button button = new Button();
        button.setFocusTraversable(false);
        refreshFavoriteButton(button, video);
        button.setOnAction(e -> {
            e.consume();
            favoriteService.toggleFavorite(getCurrentUserId(), video);
            refreshFavoriteButton(button, video);
            MainFX.refreshNavigationBadges();
        });
        return button;
    }

    private void refreshFavoriteButton(Button button, CoachingVideo video) {
        boolean favorite = favoriteService.isFavorite(getCurrentUserId(), video.getId());
        button.setText(favorite ? "❤" : "♡");
        button.setStyle(
                "-fx-background-color:" + (favorite ? "#be185d" : "rgba(15,23,42,0.8)") + ";"
                        + "-fx-text-fill:white;-fx-background-radius:20;-fx-padding:6 10;"
        );
    }

    private Integer getCurrentUserId() {
        User user = SessionContext.getCurrentUser();
        return user != null ? user.getId() : null;
    }

    private String niveauColor(String niveau) {
        if (niveau == null) {
            return "#475569";
        }
        return switch (niveau.toLowerCase()) {
            case "debutant" -> "#166534";
            case "intermediaire" -> "#1d4ed8";
            case "avance" -> "#c2410c";
            case "expert" -> "#7c3aed";
            default -> "#475569";
        };
    }

    private String extractYoutubeId(String url) {
        Pattern pattern = Pattern.compile("(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|v/))([^&?\\s]+)");
        Matcher matcher = pattern.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private void navigateTo(String fxmlFile) {
        try {
            Node anchor = cardsPane != null
                    ? cardsPane
                    : (txtTitle != null ? txtTitle : lblTitre);
            if (anchor == null) {
                MainFX.navigateFromShell(fxmlFile);
                return;
            }
            Parent view = FXMLLoader.load(getClass().getResource(BASE + fxmlFile));
            BorderPane root = (BorderPane) anchor.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            if (contentArea != null) {
                contentArea.setCenter(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isAdmin() {
        User user = SessionContext.getCurrentUser();
        return user != null
                && user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> role.toUpperCase().contains("ADMIN"));
    }

    private Button styledBtn(String text, String color) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefWidth(120);
        button.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                + "-fx-font-weight:bold;-fx-font-size:12;-fx-background-radius:8;-fx-padding:8 10;");
        return button;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

