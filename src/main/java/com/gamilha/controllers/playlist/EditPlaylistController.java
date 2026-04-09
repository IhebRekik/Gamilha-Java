package com.gamilha.controllers.playlist;

import com.gamilha.entity.Playlist;
import com.gamilha.services.PlaylistService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

public class EditPlaylistController {

    @FXML private TextField txtTitle;
    @FXML private TextArea  txtDescription;
    @FXML private ComboBox<String> cmbNiveau;
    @FXML private TextField txtCategorie;
    @FXML private TextField txtImage;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final PlaylistService service = new PlaylistService();
    private Playlist playlist;

    @FXML
    public void initialize() {
        cmbNiveau.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert");
        btnSave.setOnAction(e -> sauvegarder());
        btnCancel.setOnAction(e -> retour());
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
        txtTitle.setText(playlist.getTitle());
        txtDescription.setText(playlist.getDescription());
        cmbNiveau.setValue(playlist.getNiveau());
        txtCategorie.setText(playlist.getCategorie());
        txtImage.setText(playlist.getImage());
    }

    private void sauvegarder() {
        lblError.setText("");

        if (txtTitle.getText().trim().isEmpty()) {
            lblError.setText("⚠️ Le titre est obligatoire.");
            return;
        }

        playlist.setTitle(txtTitle.getText().trim());
        playlist.setDescription(txtDescription.getText().trim());
        playlist.setNiveau(cmbNiveau.getValue());
        playlist.setCategorie(txtCategorie.getText().trim());
        playlist.setImage(txtImage.getText().trim());

        service.modifierPlaylist(playlist);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅ Playlist modifiée avec succès !");
        alert.showAndWait();

        retour();
    }

    private void retour() {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/com/gamilha/interfaces/User/PlaylistUser.fxml"));
            BorderPane root = (BorderPane) txtTitle.getScene().getRoot();
            BorderPane contentArea = (BorderPane) root.lookup("#contentArea");
            contentArea.setCenter(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
