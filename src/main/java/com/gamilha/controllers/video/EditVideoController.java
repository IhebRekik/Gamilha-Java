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

public class EditVideoController {

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
    private CoachingVideo video;

    @FXML
    public void initialize() {
        cmbNiveau.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert");

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

    public void setVideo(CoachingVideo video) {
        this.video = video;
        txtTitre.setText(video.getTitre());
        txtDescription.setText(video.getDescription());
        txtUrl.setText(video.getUrl());
        cmbNiveau.setValue(video.getNiveau());
        chkPremium.setSelected(video.isPremium());
        txtDuration.setText(String.valueOf(video.getDuration()));

        if (video.getPlaylist() != null) {
            cmbPlaylist.getItems().stream()
                    .filter(p -> p.getId() == video.getPlaylist().getId())
                    .findFirst()
                    .ifPresent(cmbPlaylist::setValue);
        }
    }

    private void sauvegarder() {
        lblError.setText("");

        if (txtTitre.getText().trim().isEmpty()) {
            lblError.setText("⚠️ Le titre est obligatoire."); return;
        }
        if (txtUrl.getText().trim().isEmpty()) {
            lblError.setText("⚠️ L'URL est obligatoire."); return;
        }

        int duration = 0;
        try {
            if (!txtDuration.getText().trim().isEmpty())
                duration = Integer.parseInt(txtDuration.getText().trim());
        } catch (NumberFormatException ex) {
            lblError.setText("⚠️ La durée doit être un nombre entier (secondes)."); return;
        }

        video.setTitre(txtTitre.getText().trim());
        video.setDescription(txtDescription.getText().trim());
        video.setUrl(txtUrl.getText().trim());
        video.setNiveau(cmbNiveau.getValue());
        video.setPremium(chkPremium.isSelected());
        video.setDuration(duration);
        video.setPlaylist(cmbPlaylist.getValue());

        videoService.modifierVideo(video);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅ Vidéo modifiée avec succès !");
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
