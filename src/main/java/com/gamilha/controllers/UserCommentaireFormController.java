package com.gamilha.controllers;

import com.gamilha.entity.Commentaire;
import com.gamilha.services.CommentaireService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;

/**
 * Formulaire d'édition d'un commentaire côté utilisateur.
 * FXML : UserCommentaireFormView.fxml
 */
public class UserCommentaireFormController {

    @FXML private TextArea textArea;
    @FXML private Label    errorLabel;
    @FXML private Label    charCountLabel;
    @FXML private Button   btnSave;

    private Commentaire commentaire;
    private final CommentaireService commentaireService = new CommentaireService();

    public void init(Commentaire c) {
        this.commentaire = c;
        textArea.setText(c.getText());
        updateCharCount();
        textArea.textProperty().addListener((o, ov, nv) -> updateCharCount());
    }

    private void updateCharCount() {
        int len = textArea.getText().trim().length();
        charCountLabel.setText(len + " / 500");
        charCountLabel.setStyle("-fx-font-size:11;-fx-text-fill:" +
            (len > 500 ? "#ef4444" : "#8b949e") + ";");
    }

    @FXML private void onSave() {
        String text = textArea.getText().trim();
        errorLabel.setText("");
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
