package com.gamilha.controllers.video;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.services.CoachingVideoService;
import com.gamilha.services.PlaylistService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller unique pour TOUTES les fonctionnalités CoachingVideo côté User.
 * Gère : liste, recherche, filtre, ajout inline, modification inline, suppression, détails.
 */
public class VideoUserController {

    // ─── Barre de recherche / filtre ──────────────────────────────────────────
    @FXML private TextField        searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private ComboBox<String> filterPremium;
    @FXML private Label            lblPlaylistFilter;
    @FXML private GridPane         tableGrid;

    // ─── Formulaire inline (Add / Edit) ───────────────────────────────────────
    @FXML private VBox             formPanel;
    @FXML private Label            formTitle;
    @FXML private TextField        tfTitre;
    @FXML private TextField        tfDescription;
    @FXML private TextField        tfUrl;
    @FXML private TextField        tfNiveau;
    @FXML private TextField        tfDuration;
    @FXML private CheckBox         cbPremium;
    @FXML private ComboBox<String> cbPlaylist;

    // ─── Services ──────────────────────────────────────────────────────────────
    private final CoachingVideoService service       = new CoachingVideoService();
    private final PlaylistService      playlistService = new PlaylistService();

    private ObservableList<CoachingVideo> masterData = FXCollections.observableArrayList();
    private List<Playlist> allPlaylists;
    private Playlist       playlistFilter  = null;
    private CoachingVideo  editingVideo    = null;    // null = ajout, sinon modification

    // ─── Initialisation ────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        allPlaylists = playlistService.afficherPlaylists();

        // Remplir le ComboBox playlist du formulaire
        if (cbPlaylist != null) {
            cbPlaylist.getItems().clear();
            allPlaylists.forEach(p -> cbPlaylist.getItems().add(p.getTitle()));
        }

        // Charger toutes les vidéos
        masterData.addAll(service.afficherVideos());

        // Filtre niveau
        filterNiveau.getItems().add("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream().map(CoachingVideo::getNiveau)
                        .filter(n -> n != null && !n.isEmpty()).distinct().toList()
        );
        filterNiveau.setValue("Tous");

        // Filtre premium
        filterPremium.getItems().addAll("Tous", "Premium", "Gratuit");
        filterPremium.setValue("Tous");

        buildTable(masterData);

        searchInput.textProperty().addListener((obs, o, n) -> filter());
        filterNiveau.valueProperty().addListener((obs, o, n) -> filter());
        filterPremium.valueProperty().addListener((obs, o, n) -> filter());

        // Cacher le formulaire au démarrage
        if (formPanel != null) {
            formPanel.setVisible(false);
            formPanel.setManaged(false);
        }
    }

    // ─── Filtrage depuis PlaylistUserController ────────────────────────────────
    public void setPlaylistFilter(Playlist playlist) {
        this.playlistFilter = playlist;
        if (lblPlaylistFilter != null && playlist != null) {
            lblPlaylistFilter.setText("Playlist : " + playlist.getTitle());
            lblPlaylistFilter.setVisible(true);
        }
        chargerDonnees();
        buildTable(masterData);
    }

    // ─── Ouvrir formulaire AJOUT ───────────────────────────────────────────────
    @FXML
    private void openAddForm() {
        editingVideo = null;
        if (formTitle != null) formTitle.setText("➕ Nouvelle Vidéo");
        clearForm();
        showForm();
    }

    // ─── Sauvegarder (Ajout ou Modification) ──────────────────────────────────
    @FXML
    private void saveForm() {
        String titre = tfTitre.getText().trim();
        if (titre.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire", "Le titre est requis.");
            return;
        }

        int durationSec = 0;
        try {
            String durStr = tfDuration.getText().trim();
            if (!durStr.isEmpty()) durationSec = Integer.parseInt(durStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Durée invalide", "La durée doit être un nombre entier (secondes).");
            return;
        }

        Playlist selectedPlaylist = getSelectedPlaylist();

        if (editingVideo == null) {
            // ── AJOUT ──
            CoachingVideo v = new CoachingVideo(
                    titre,
                    tfDescription.getText().trim(),
                    tfUrl.getText().trim(),
                    tfNiveau.getText().trim(),
                    cbPremium != null && cbPremium.isSelected(),
                    durationSec,
                    selectedPlaylist
            );
            service.ajouterVideo(v);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Vidéo ajoutée avec succès !");
        } else {
            // ── MODIFICATION ──
            editingVideo.setTitre(titre);
            editingVideo.setDescription(tfDescription.getText().trim());
            editingVideo.setUrl(tfUrl.getText().trim());
            editingVideo.setNiveau(tfNiveau.getText().trim());
            editingVideo.setPremium(cbPremium != null && cbPremium.isSelected());
            editingVideo.setDuration(durationSec);
            editingVideo.setPlaylist(selectedPlaylist);
            service.modifierVideo(editingVideo);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Vidéo modifiée avec succès !");
        }

        hideForm();
        refresh();
    }

    // ─── Annuler formulaire ────────────────────────────────────────────────────
    @FXML
    private void cancelForm() {
        hideForm();
    }

    // ─── Retour à la liste des playlists ──────────────────────────────────────
    @FXML
    private void retour() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/com/gamilha/interfaces/User/PlaylistUser.fxml"));
            BorderPane root = (BorderPane) tableGrid.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            contentArea.setCenter(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ─── Ouvrir formulaire MODIFICATION ───────────────────────────────────────
    private void openEditForm(CoachingVideo v) {
        editingVideo = v;
        if (formTitle != null) formTitle.setText("✏️ Modifier Vidéo");
        tfTitre.setText(v.getTitre());
        tfDescription.setText(v.getDescription() != null ? v.getDescription() : "");
        tfUrl.setText(v.getUrl() != null ? v.getUrl() : "");
        tfNiveau.setText(v.getNiveau() != null ? v.getNiveau() : "");
        tfDuration.setText(String.valueOf(v.getDuration()));
        if (cbPremium != null) cbPremium.setSelected(v.isPremium());
        if (cbPlaylist != null && v.getPlaylist() != null) {
            cbPlaylist.setValue(v.getPlaylist().getTitle());
        }
        showForm();
    }

    // ─── Construire la table ───────────────────────────────────────────────────
    private void buildTable(ObservableList<CoachingVideo> data) {
        tableGrid.getChildren().clear();
        tableGrid.getColumnConstraints().clear();

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(18);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(22);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(11);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(11);
        ColumnConstraints c5 = new ColumnConstraints(); c5.setPercentWidth(8);
        ColumnConstraints c6 = new ColumnConstraints(); c6.setPercentWidth(15);
        ColumnConstraints c7 = new ColumnConstraints(); c7.setPercentWidth(15);
        tableGrid.getColumnConstraints().addAll(c1, c2, c3, c4, c5, c6, c7);

        addHeader("Titre",       0);
        addHeader("Description", 1);
        addHeader("Niveau",      2);
        addHeader("Durée",       3);
        addHeader("Premium",     4);
        addHeader("Playlist",    5);
        addHeader("Actions",     6);

        int row = 1;
        for (CoachingVideo v : data) {
            String color = row % 2 == 0 ? "#0f172a" : "#1a2235";
            addCell(v.getTitre(),       row, 0, color);
            addCell(v.getDescription(), row, 1, color);
            addCell(v.getNiveau(),      row, 2, color);
            addCell(v.getDurationFormatted(), row, 3, color);
            addCell(v.isPremium() ? "✅" : "❌", row, 4, color);
            addCell(v.getPlaylist() != null ? v.getPlaylist().getTitle() : "-", row, 5, color);

            HBox actions = new HBox(6);
            actions.setStyle("-fx-alignment:center-left;-fx-background-color:" + color + ";-fx-padding:8;");

            Button modifier  = styledBtn("✏️",  "#d97706");
            Button supprimer = styledBtn("🗑",  "#dc2626");
            Button voir      = styledBtn("👁",  "#2563eb");

            modifier.setOnAction(e  -> openEditForm(v));
            supprimer.setOnAction(e -> confirmDelete(v));
            voir.setOnAction(e      -> openShow(v));

            actions.getChildren().addAll(voir, modifier, supprimer);
            tableGrid.add(actions, 6, row);
            row++;
        }

        if (data.isEmpty()) {
            Label empty = new Label("Aucune vidéo trouvée.");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-padding:20;-fx-font-size:14;");
            tableGrid.add(empty, 0, 1, 7, 1);
        }
    }

    // ─── Voir les détails d'une vidéo ────────────────────────────────────────
    private void openShow(CoachingVideo video) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/ShowVideo.fxml"));
            Parent view = loader.load();
            ShowVideoController ctrl = loader.getController();
            ctrl.setVideo(video);
            navigateTo(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ─── Supprimer avec confirmation ──────────────────────────────────────────
    private void confirmDelete(CoachingVideo v) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer cette vidéo ?");
        confirm.setContentText("Titre : " + v.getTitre());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimerVideo(v.getId());
                masterData.remove(v);
                buildTable(masterData);
                showAlert(Alert.AlertType.INFORMATION, "Supprimé", "Vidéo supprimée avec succès.");
            }
        });
    }

    // ─── Filtre en temps réel ─────────────────────────────────────────────────
    private void filter() {
        String search  = searchInput.getText() == null ? "" : searchInput.getText().toLowerCase();
        String niveau  = filterNiveau.getValue();
        String premium = filterPremium.getValue();

        ObservableList<CoachingVideo> filtered = masterData.stream()
                .filter(v -> v.getTitre().toLowerCase().contains(search)
                        || (v.getDescription() != null && v.getDescription().toLowerCase().contains(search)))
                .filter(v -> niveau == null || niveau.equals("Tous") || niveau.equals(v.getNiveau()))
                .filter(v -> {
                    if ("Premium".equals(premium)) return v.isPremium();
                    if ("Gratuit".equals(premium))  return !v.isPremium();
                    return true;
                })
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        buildTable(filtered);
    }

    // ─── Recharger les données ────────────────────────────────────────────────
    private void chargerDonnees() {
        masterData.clear();
        if (playlistFilter != null) {
            masterData.addAll(service.afficherVideosByPlaylist(playlistFilter.getId()));
        } else {
            masterData.addAll(service.afficherVideos());
        }
    }

    private void refresh() {
        chargerDonnees();
        buildTable(masterData);
    }

    // ─── Navigation ───────────────────────────────────────────────────────────
    private void navigateTo(Parent view) {
        BorderPane root = (BorderPane) tableGrid.getScene().getRoot();
        BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
        contentArea.setCenter(view);
    }

    // ─── Helpers UI ───────────────────────────────────────────────────────────
    private void showForm() {
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    private void hideForm() {
        formPanel.setVisible(false);
        formPanel.setManaged(false);
    }

    private void clearForm() {
        tfTitre.clear();
        tfDescription.clear();
        tfUrl.clear();
        tfNiveau.clear();
        tfDuration.clear();
        if (cbPremium != null) cbPremium.setSelected(false);
        if (cbPlaylist != null) cbPlaylist.setValue(null);
    }

    private Playlist getSelectedPlaylist() {
        if (cbPlaylist == null || cbPlaylist.getValue() == null) return null;
        String selected = cbPlaylist.getValue();
        return allPlaylists.stream()
                .filter(p -> p.getTitle().equals(selected))
                .findFirst().orElse(null);
    }

    private Button styledBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                + "-fx-background-radius:6;-fx-padding:5 10;");
        return btn;
    }

    private void addHeader(String text, int col) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-background-color:#020617;-fx-text-fill:#e2e8f0;-fx-font-weight:bold;"
                + "-fx-font-size:14;-fx-padding:13;-fx-border-color:#1e293b;");
        tableGrid.add(label, col, 0);
    }

    private void addCell(String text, int row, int col, String color) {
        Label label = new Label(text != null ? text : "");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill:#cbd5f5;-fx-padding:9;-fx-border-color:#1e293b;-fx-background-color:" + color + ";");
        tableGrid.add(label, col, row);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
