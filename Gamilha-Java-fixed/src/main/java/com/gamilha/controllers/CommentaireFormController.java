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

    
    private void fixTextAreaStyle() {
        javafx.application.Platform.runLater(() -> {
            String style = "-fx-control-inner-background:#1a1a30;" +
                           "-fx-background-color:#1a1a30;-fx-text-fill:#e6edf3;" +
                           "-fx-prompt-text-fill:#4b5563;-fx-font-size:13;" +
                           "-fx-border-color:#3a3a5c;-fx-border-radius:8;-fx-background-radius:8;";
            if (textArea != null) {
                textArea.setStyle(style);
                javafx.scene.Node n = textArea.lookup(".content");
                if (n != null) n.setStyle("-fx-background-color:#1a1a30;-fx-control-inner-background:#1a1a30;");
            }
        });
    }
    public void init(Commentaire c) {
        fixTextAreaStyle();
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
