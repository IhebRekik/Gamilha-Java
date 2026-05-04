package com.gamilha.controllers;

import com.gamilha.entity.Commentaire;
import com.gamilha.services.CommentaireService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

public class CommentaireFormController {

    @FXML private TextArea textArea;
    @FXML private Label    errorLabel;
    @FXML private Button   btnSave;

    private Commentaire commentaire;
    private final CommentaireService commentaireService = new CommentaireService();

    public void init(Commentaire c) {
        this.commentaire = c;
        textArea.setText(c.getText());
    }

    @FXML private void onSave() {
        String text = textArea.getText().trim();
        if (text.length() < 5) {
            errorLabel.setText("Minimum 5 caractères requis."); return;
        }
        if (text.length() > 500) {
            errorLabel.setText("Maximum 500 caractères autorisés."); return;
        }
        try {
            commentaire.setText(text);
            commentaireService.update(commentaire);
            ((Stage) btnSave.getScene().getWindow()).close();
        } catch (SQLException e) {
            errorLabel.setText("Erreur BD : " + e.getMessage());
        }
    }

    @FXML private void onCancel() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }
}
