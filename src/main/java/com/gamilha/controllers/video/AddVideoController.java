package com.gamilha.controllers.video;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.services.CoachingVideoService;
import com.gamilha.services.PlaylistService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class AddVideoController {

    @FXML private TextField txtTitre;
    @FXML private TextArea  txtDescription;
    @FXML private TextField txtUrl;
    @FXML private ComboBox<String> cmbNiveau;
    @FXML private CheckBox chkPremium;
    @FXML private TextField txtDuration;
    @FXML private ComboBox<Playlist> cmbPlaylist;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final CoachingVideoService videoService = new CoachingVideoService();
    private final PlaylistService playlistService   = new PlaylistService();

    @FXML
    public void initialize() {
        cmbNiveau.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert");
        cmbNiveau.setValue("Débutant");

        List<Playlist> playlists = playlistService.afficherPlaylists();
        cmbPlaylist.setItems(FXCollections.observableArrayList(playlists));
        cmbPlaylist.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Playlist item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        cmbPlaylist.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Playlist item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "Sélectionner une playlist" : item.getTitle());
            }
        });

        btnSave.setOnAction(e -> sauvegarder());
        btnCancel.setOnAction(e -> retour());
    }

    private void sauvegarder() {
        lblError.setText("");

        if (txtTitre.getText().trim().isEmpty()) {
            lblError.setText("⚠️ Le titre est obligatoire."); return;
        }
        if (txtUrl.getText().trim().isEmpty()) {
            lblError.setText("⚠️ L'URL est obligatoire."); return;
        }
        if (cmbPlaylist.getValue() == null) {
            lblError.setText("⚠️ Veuillez sélectionner une playlist."); return;
        }

        int duration = 0;
        try {
            if (!txtDuration.getText().trim().isEmpty())
                duration = Integer.parseInt(txtDuration.getText().trim());
        } catch (NumberFormatException ex) {
            lblError.setText("⚠️ La durée doit être un nombre entier (secondes)."); return;
        }

        CoachingVideo v = new CoachingVideo();
        v.setTitre(txtTitre.getText().trim());
        v.setDescription(txtDescription.getText().trim());
        v.setUrl(txtUrl.getText().trim());
        v.setNiveau(cmbNiveau.getValue());
        v.setPremium(chkPremium.isSelected());
        v.setDuration(duration);
        v.setPlaylist(cmbPlaylist.getValue());

        videoService.ajouterVideo(v);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅ Vidéo ajoutée avec succès !");
        alert.showAndWait();

        retour();
    }

    private void retour() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/com/gamilha/interfaces/User/VideoUser.fxml"));
            BorderPane root = (BorderPane) txtTitre.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            contentArea.setCenter(view);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
