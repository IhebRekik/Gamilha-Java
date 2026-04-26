package com.gamilha.controllers;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.PostService;
import com.gamilha.services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.UUID;

public class PostFormController implements Initializable {

    @FXML private TextArea          contentArea;
    @FXML private ComboBox<User>    userCombo;
    @FXML private TextField         mediaurlField;
    @FXML private ImageView         imagePreview;
    @FXML private Label             imageNameLabel;
    @FXML private Label             errorLabel;
    @FXML private Button            btnSave;

    private Post        post;          // null = création
    private File        selectedFile;

    private final PostService postService = new PostService();
    private final UserService userService = new UserService();

    // Style textarea dark — appliqué en Java car les styles inline ne suffisent pas
    private static final String TA_STYLE =
        "-fx-control-inner-background:#1a1a30;" +
        "-fx-background-color:#1a1a30;" +
        "-fx-text-fill:#e6edf3;" +
        "-fx-prompt-text-fill:#4b5563;" +
        "-fx-font-size:13;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Forcer style textarea après chargement FXML
        javafx.application.Platform.runLater(() -> {
            if (contentArea != null) {
                contentArea.setStyle(TA_STYLE +
                    "-fx-border-color:#3a3a5c;-fx-border-radius:8;-fx-background-radius:8;");
                contentArea.lookup(".content").setStyle(
                    "-fx-background-color:#1a1a30;-fx-control-inner-background:#1a1a30;");
            }
        });
        try {
            userCombo.setItems(FXCollections.observableArrayList(userService.findAll()));
            userCombo.setConverter(new StringConverter<User>() {
                public String toString(User u)    { return u == null ? "" : u.getName() + " <" + u.getEmail() + ">"; }
                public User fromString(String s)  { return null; }
            });
            if (!userCombo.getItems().isEmpty()) userCombo.setValue(userCombo.getItems().get(0));
        } catch (Exception e) {
            errorLabel.setText("Impossible de charger les utilisateurs.");
        }
    }

    /** Appeler avec null pour création, post existant pour édition */
    public void init(Post p) {
        this.post = p;
        if (p == null) return;

        contentArea.setText(p.getContent());
        mediaurlField.setText(p.getMediaurl() != null ? p.getMediaurl() : "");
        btnSave.setText("Mettre à jour");

        // Sélectionner l'auteur dans le combo
        if (p.getUser() != null) {
            userCombo.getItems().stream()
                .filter(u -> u.getId() == p.getUser().getId())
                .findFirst()
                .ifPresent(userCombo::setValue);
        }

        // Prévisualiser l'image existante
        if (p.getImage() != null && !p.getImage().isBlank()) {
            imageNameLabel.setText(p.getImage());
            File f = new File(AdminPostController.UPLOADS + p.getImage());
            if (!f.exists()) f = new File(AdminPostController.UPLOADS + "images/" + p.getImage());
            if (f.exists()) imagePreview.setImage(new Image(f.toURI().toString()));
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
        if (userCombo.getValue() == null) {
            errorLabel.setText("Sélectionner un auteur."); return;
        }

        // Copie image dans uploads Symfony si fichier sélectionné
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
                Post newPost = new Post();
                newPost.setContent(content);
                newPost.setImage(imageName);
                newPost.setMediaurl(mediaurlField.getText().trim());
                newPost.setUser(userCombo.getValue());
                newPost.setCreatedAt(LocalDateTime.now());
                postService.create(newPost);
            } else {
                post.setContent(content);
                post.setImage(imageName);
                post.setMediaurl(mediaurlField.getText().trim());
                post.setUser(userCombo.getValue());
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
