package com.gamilha.controllers;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.PostService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Formulaire de création / modification d'un post côté utilisateur.
 * Pas de sélection d'auteur (l'auteur = currentUser).
 * FXML : UserPostFormView.fxml
 */
public class UserPostFormController {

    @FXML private TextArea  contentArea;
    @FXML private TextField mediaurlField;
    @FXML private ImageView imagePreview;
    @FXML private Label     imageNameLabel;
    @FXML private Label     errorLabel;
    @FXML private Button    btnSave;
    @FXML private Label     titleLabel;

    private Post post;
    private User currentUser;
    private File selectedFile;

    private final PostService postService = new PostService();

    /** null = création, post existant = édition */
    public void init(Post p, User user) {
        this.post        = p;
        this.currentUser = user;

        if (p != null) {
            titleLabel.setText("✏  Modifier mon post");
            btnSave.setText("Mettre à jour");
            contentArea.setText(p.getContent());
            mediaurlField.setText(p.getMediaurl() != null ? p.getMediaurl() : "");

            if (p.getImage() != null && !p.getImage().isBlank()) {
                imageNameLabel.setText(p.getImage());
                File f = new File(AdminPostController.UPLOADS + p.getImage());
                if (!f.exists()) f = new File(AdminPostController.UPLOADS + "images/" + p.getImage());
                if (f.exists()) imagePreview.setImage(new Image(f.toURI().toString()));
            }
        }
    }

    @FXML private void onChooseImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        selectedFile = fc.showOpenDialog(btnSave.getScene().getWindow());
        if (selectedFile != null) {
            imageNameLabel.setText(selectedFile.getName());
            imagePreview.setImage(new Image(selectedFile.toURI().toString()));
        }
    }

    @FXML private void onSave() {
        errorLabel.setText("");
        String content = contentArea.getText().trim();

        if (content.length() < 12) {
            errorLabel.setText("Le contenu doit contenir au moins 12 caractères."); return;
        }
        if (currentUser == null) {
            errorLabel.setText("Utilisateur non identifié."); return;
        }

        // Copier l'image dans uploads si sélectionnée
        String imageName = post != null ? post.getImage() : null;
        if (selectedFile != null) {
            String ext = ext(selectedFile.getName());
            imageName  = UUID.randomUUID().toString().replace("-", "").substring(0, 13) + "." + ext;
            File dest  = new File(AdminPostController.UPLOADS + imageName);
            try {
                dest.getParentFile().mkdirs();
                Files.copy(selectedFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                errorLabel.setText("Impossible de copier l'image : " + e.getMessage()); return;
            }
        }

        try {
            if (post == null) {
                // Création
                Post newPost = new Post();
                newPost.setContent(content);
                newPost.setImage(imageName);
                newPost.setMediaurl(mediaurlField.getText().trim());
                newPost.setUser(currentUser);
                newPost.setCreatedAt(LocalDateTime.now());
                postService.create(newPost);
            } else {
                // Mise à jour
                post.setContent(content);
                post.setImage(imageName);
                post.setMediaurl(mediaurlField.getText().trim());
                postService.update(post);
            }
            ((Stage) btnSave.getScene().getWindow()).close();
        } catch (SQLException e) {
            errorLabel.setText("Erreur BD : " + e.getMessage());
        }
    }

    @FXML private void onCancel() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }

    private String ext(String name) {
        int i = name.lastIndexOf('.');
        return i >= 0 ? name.substring(i + 1) : "jpg";
    }
}
