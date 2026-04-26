package com.gamilha.controllers;

import com.gamilha.utils.SessionContext;


import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class EditUserController {

    @FXML private TextField        nameField;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;
    @FXML private CheckBox         activeCheckBox;

    @FXML private ImageView avatarPreview;
    @FXML private Label avatarPathLabel;

    private File selectedAvatarFile;

    private User currentUser;
    private final UserService userService = new UserService();

    public void setUser(User user) {
        this.currentUser = user;

        nameField.setText(user.getName());
        emailField.setText(user.getEmail());

        activeCheckBox.setSelected(user.isActive());

        // Prévisualisation avatar existant
        refreshAvatarPreview(user.getProfileImage());
    }

    @FXML
    private void chooseAvatar() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        FileChooser fc = ImageStorage.createAvatarFileChooser();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedAvatarFile = f;
            if (avatarPathLabel != null) avatarPathLabel.setText(f.getName());
            refreshAvatarPreview(f.toURI().toString()); // preview direct
        }
    }

    private void refreshAvatarPreview(String storedOrUri) {
        if (avatarPreview == null) return;

        try {
            Image img = null;

            if (storedOrUri == null || storedOrUri.isBlank()) {
                // fallback logo
                var is = getClass().getResourceAsStream("/com/gamilha/images/logo.jpg");
                if (is != null) img = new Image(is, 96, 96, true, true);
            } else if (storedOrUri.startsWith("file:") || storedOrUri.startsWith("jar:") || storedOrUri.startsWith("http")) {
                img = new Image(storedOrUri, 96, 96, true, true);
            } else {
                // chemin stocké en DB -> fichier local
                Path p = ImageStorage.resolveToPath(storedOrUri);
                if (p != null) img = new Image(p.toUri().toString(), 96, 96, true, true);
            }

            if (img != null) {
                avatarPreview.setImage(img);
                avatarPreview.setFitWidth(96);
                avatarPreview.setFitHeight(96);
                avatarPreview.setPreserveRatio(true);
                avatarPreview.setClip(new Circle(48, 48, 48));
            }
        } catch (Exception ignored) {
        }
    }

    @FXML
    private void updateUser() {
        if (currentUser == null) return;

        String name  = nameField.getText().trim();
        String email = emailField.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert("Erreur", "Le nom et l'email sont obligatoires !", Alert.AlertType.ERROR);
            return;
        }

        currentUser.setName(name);
        currentUser.setEmail(email);
        currentUser.setActive(activeCheckBox.isSelected());

        if (!passwordField.getText().isEmpty()) {
            currentUser.setPassword(passwordField.getText());
        }

        try {
            if (selectedAvatarFile != null) {
                String storedPath = ImageStorage.saveAvatar(selectedAvatarFile);
                currentUser.setProfileImage(storedPath);
            }
        } catch (IOException e) {
            showAlert("Erreur", "Upload image impossible: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        userService.update(currentUser);

        // ✅ si l'utilisateur modifié est celui connecté, mettre à jour la session
        User sessionUser = com.gamilha.utils.SessionContext.getCurrentUser();
        if (sessionUser != null && sessionUser.getId() == currentUser.getId()) {
            com.gamilha.utils.SessionContext.setCurrentUser(currentUser);
        }

        showAlert("Succès", "Utilisateur modifié avec succès !", Alert.AlertType.INFORMATION);
        backToList();
    }

    @FXML
    private void cancel() {
        backToList();
    }

    @FXML
    private void backToList(){

        try{

            BorderPane contentArea =
                    (BorderPane)

                            nameField

                                    .getScene()

                                    .lookup("#contentArea");


            if(contentArea != null){

                FXMLLoader loader =
                        new FXMLLoader(

                                getClass().getResource(

                                        "/com/gamilha/interfaces/Admin/admin_users.fxml"

                                )

                        );


                Parent content =
                        loader.load();


                contentArea.setCenter(content);

                return;

            }

        }

        catch(Exception ignored){}


        // fallback si page ouverte hors layout admin

        try{

            User userToShow =
                    (currentUser != null)

                            ? currentUser

                            : SessionContext.getCurrentUser();


            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(

                                    "/com/gamilha/interfaces/User/user_profile.fxml"

                            )

                    );


            Parent root =
                    loader.load();


            if(userToShow != null){

                UserProfileController controller =
                        loader.getController();


                controller.setUser(userToShow);

            }


            Stage stage =
                    (Stage)

                            nameField

                                    .getScene()

                                    .getWindow();


            Scene scene =
                    new Scene(root,900,600);


            stage.setScene(scene);

        }

        catch(IOException e){

            e.printStackTrace();

        }

    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}

