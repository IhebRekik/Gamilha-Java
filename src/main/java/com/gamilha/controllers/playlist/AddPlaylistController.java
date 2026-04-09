package com.gamilha.controllers.playlist;

import com.gamilha.entity.Playlist;
import com.gamilha.services.PlaylistService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.time.LocalDateTime;

public class AddPlaylistController {

    @FXML private TextField txtTitle;
    @FXML private TextArea  txtDescription;
    @FXML private ComboBox<String> cmbNiveau;
    @FXML private TextField txtCategorie;
    @FXML private TextField txtImage;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private final PlaylistService service = new PlaylistService();

    @FXML
    public void initialize() {
        cmbNiveau.getItems().addAll("Débutant", "Intermédiaire", "Avancé", "Expert");
        cmbNiveau.setValue("Débutant");

        btnSave.setOnAction(e -> sauvegarder());
        btnCancel.setOnAction(e -> retour());
    }

    private void sauvegarder() {
        lblError.setText("");

        if (txtTitle.getText().trim().isEmpty()) {
            lblError.setText("⚠️ Le titre est obligatoire.");
            return;
        }

        Playlist p = new Playlist();
        p.setTitle(txtTitle.getText().trim());
        p.setDescription(txtDescription.getText().trim());
        p.setNiveau(cmbNiveau.getValue());
        p.setCategorie(txtCategorie.getText().trim());
        p.setImage(txtImage.getText().trim());
        p.setCreatedAt(LocalDateTime.now());

        service.ajouterPlaylist(p);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅ Playlist ajoutée avec succès !");
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
