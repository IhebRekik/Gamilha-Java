package com.gamilha.controllers.playlist;

import com.gamilha.entity.Playlist;
import com.gamilha.services.PlaylistService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Controller unique pour toutes les fonctionnalités Playlist côté User.
 * Gère : liste, recherche, filtre, ajout, modification, suppression, détails, vidéos.
 */
public class PlaylistUserController {

    // ─── Barre de recherche / filtre ─────────────────────────────────────────
    @FXML private TextField    searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private GridPane     tableGrid;

    // ─── Formulaire inline (Add / Edit) ──────────────────────────────────────
    @FXML private VBox         formPanel;
    @FXML private Label        formTitle;
    @FXML private TextField    tfTitle;
    @FXML private TextField    tfDescription;
    @FXML private TextField    tfNiveau;
    @FXML private TextField    tfCategorie;
    @FXML private TextField    tfImage;

    // ─── Services ─────────────────────────────────────────────────────────────
    private final PlaylistService service = new PlaylistService();
    private ObservableList<Playlist> masterData = FXCollections.observableArrayList();
    private Playlist editingPlaylist = null;   // null = ajout, non-null = modification

    // ─── Initialisation ───────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        masterData.addAll(service.afficherPlaylists());

        filterNiveau.getItems().add("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream().map(Playlist::getNiveau)
                        .filter(n -> n != null && !n.isEmpty())
                        .distinct().toList()
        );
        filterNiveau.setValue("Tous");

        buildTable(masterData);

        searchInput.textProperty().addListener((obs, o, n) -> filter());
        filterNiveau.valueProperty().addListener((obs, o, n) -> filter());

        // Cacher le formulaire au démarrage
        if (formPanel != null) {
            formPanel.setVisible(false);
            formPanel.setManaged(false);
        }
    }

    // ─── Ouvrir formulaire AJOUT ──────────────────────────────────────────────
    @FXML
    private void openAddForm() {
        editingPlaylist = null;
        if (formTitle != null) formTitle.setText("➕ Nouvelle Playlist");
        clearForm();
        showForm();
    }

    // ─── Sauvegarder (Ajout ou Modification) ─────────────────────────────────
    @FXML
    private void saveForm() {
        String title = tfTitle.getText().trim();
        if (title.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Champ obligatoire", "Le titre est requis.");
            return;
        }

        if (editingPlaylist == null) {
            // ── AJOUT ──
            Playlist p = new Playlist(
                    title,
                    tfDescription.getText().trim(),
                    tfNiveau.getText().trim(),
                    tfCategorie.getText().trim(),
                    tfImage.getText().trim(),
                    LocalDateTime.now()
            );
            service.ajouterPlaylist(p);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Playlist ajoutée avec succès !");
        } else {
            // ── MODIFICATION ──
            editingPlaylist.setTitle(title);
            editingPlaylist.setDescription(tfDescription.getText().trim());
            editingPlaylist.setNiveau(tfNiveau.getText().trim());
            editingPlaylist.setCategorie(tfCategorie.getText().trim());
            editingPlaylist.setImage(tfImage.getText().trim());
            service.modifierPlaylist(editingPlaylist);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Playlist modifiée avec succès !");
        }
        hideForm();
        refresh();
    }

    // ─── Annuler formulaire ───────────────────────────────────────────────────
    @FXML
    private void cancelForm() {
        hideForm();
    }

    // ─── Ouvrir formulaire MODIFICATION ──────────────────────────────────────
    private void openEditForm(Playlist p) {
        editingPlaylist = p;
        if (formTitle != null) formTitle.setText("✏️ Modifier Playlist");
        tfTitle.setText(p.getTitle());
        tfDescription.setText(p.getDescription() != null ? p.getDescription() : "");
        tfNiveau.setText(p.getNiveau()    != null ? p.getNiveau()    : "");
        tfCategorie.setText(p.getCategorie() != null ? p.getCategorie() : "");
        tfImage.setText(p.getImage()      != null ? p.getImage()      : "");
        showForm();
    }

    // ─── Voir les vidéos d'une playlist ──────────────────────────────────────
    private void openVideos(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/VideoUser.fxml"));
            Parent view = loader.load();
            com.gamilha.controllers.video.VideoUserController ctrl = loader.getController();
            ctrl.setPlaylistFilter(playlist);
            navigateTo(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ─── Voir les détails d'une playlist ─────────────────────────────────────
    private void openShowPlaylist(Playlist playlist) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/ShowPlaylistUser.fxml"));
            Parent view = loader.load();
            ShowPlaylistUserController ctrl = loader.getController();
            ctrl.setPlaylist(playlist);
            navigateTo(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ─── Supprimer avec confirmation ──────────────────────────────────────────
    private void confirmDelete(Playlist p) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de suppression");
        confirm.setHeaderText("Supprimer la playlist ?");
        confirm.setContentText("Titre : " + p.getTitle());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimerPlaylist(p.getId());
                masterData.remove(p);
                buildTable(masterData);
                showAlert(Alert.AlertType.INFORMATION, "Supprimé", "Playlist supprimée avec succès.");
            }
        });
    }

    // ─── Construire la table ──────────────────────────────────────────────────
    private void buildTable(ObservableList<Playlist> data) {
        tableGrid.getChildren().clear();
        tableGrid.getColumnConstraints().clear();

        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(20);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(28);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(12);
        ColumnConstraints c4 = new ColumnConstraints(); c4.setPercentWidth(12);
        ColumnConstraints c5 = new ColumnConstraints(); c5.setPercentWidth(28);
        tableGrid.getColumnConstraints().addAll(c1, c2, c3, c4, c5);

        addHeader("Titre",      0);
        addHeader("Description",1);
        addHeader("Niveau",     2);
        addHeader("Catégorie",  3);
        addHeader("Actions",    4);

        int row = 1;
        for (Playlist p : data) {
            String color = row % 2 == 0 ? "#0f172a" : "#1a2235";
            addCell(p.getTitle(),      row, 0, color);
            addCell(p.getDescription(),row, 1, color);
            addCell(p.getNiveau(),     row, 2, color);
            addCell(p.getCategorie(),  row, 3, color);

            HBox actions = new HBox(6);
            actions.setStyle("-fx-alignment:center-left;-fx-background-color:" + color + ";-fx-padding:8;");

            Button voir      = styledBtn("👁",      "#2563eb");
            Button modifier  = styledBtn("✏️",      "#d97706");
            Button supprimer = styledBtn("🗑",      "#dc2626");
            Button videos    = styledBtn("🎬 Vidéos","#7c3aed");

            voir.setOnAction(e      -> openShowPlaylist(p));
            modifier.setOnAction(e  -> openEditForm(p));
            supprimer.setOnAction(e -> confirmDelete(p));
            videos.setOnAction(e    -> openVideos(p));

            actions.getChildren().addAll(voir, modifier, supprimer, videos);
            tableGrid.add(actions, 4, row);
            row++;
        }

        if (data.isEmpty()) {
            Label empty = new Label("Aucune playlist trouvée.");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-padding:20;-fx-font-size:14;");
            tableGrid.add(empty, 0, 1, 5, 1);
        }
    }

    // ─── Filtre en temps réel ─────────────────────────────────────────────────
    private void filter() {
        String search = searchInput.getText() == null ? "" : searchInput.getText().toLowerCase();
        String niveau = filterNiveau.getValue();

        ObservableList<Playlist> filtered = masterData.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(search)
                        || (p.getDescription() != null && p.getDescription().toLowerCase().contains(search)))
                .filter(p -> niveau == null || niveau.equals("Tous") || niveau.equals(p.getNiveau()))
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        buildTable(filtered);
    }

    // ─── Rafraîchir les données ───────────────────────────────────────────────
    private void refresh() {
        masterData.clear();
        masterData.addAll(service.afficherPlaylists());
        filterNiveau.getItems().clear();
        filterNiveau.getItems().add("Tous");
        filterNiveau.getItems().addAll(
                masterData.stream().map(Playlist::getNiveau)
                        .filter(n -> n != null && !n.isEmpty())
                        .distinct().toList()
        );
        filterNiveau.setValue("Tous");
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
        tfTitle.clear();
        tfDescription.clear();
        tfNiveau.clear();
        tfCategorie.clear();
        tfImage.clear();
    }

    private Button styledBtn(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;"
                + "-fx-background-radius:6;-fx-padding:5 10;-fx-font-size:12;");
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
