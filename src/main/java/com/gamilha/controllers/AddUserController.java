package com.gamilha.controllers;

import com.gamilha.utils.SessionContext;

import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.ImageStorage;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class AddUserController {

    @FXML private TextField        nameField;
    @FXML private TextField        emailField;
    @FXML private PasswordField    passwordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private CheckBox         activeCheckBox;

    @FXML private Label avatarPathLabel;

    private File selectedAvatarFile;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        roleCombo.getItems().addAll("[\"ROLE_USER\"]", "[\"ROLE_ADMIN\"]");
        roleCombo.setValue("[\"ROLE_USER\"]");
    }

    @FXML
    private void chooseAvatar() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        FileChooser fc = ImageStorage.createAvatarFileChooser();
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            selectedAvatarFile = f;
            if (avatarPathLabel != null) avatarPathLabel.setText(f.getName());
        }
    }

    @FXML
    private void saveUser() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert("Erreur", "Tous les champs sont obligatoires !", Alert.AlertType.ERROR);
            return;
        }
        if (password.length() < 6) {
            showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères !", Alert.AlertType.ERROR);
            return;
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setActive(activeCheckBox != null && activeCheckBox.isSelected());
        user.setRoles(roleCombo.getValue());

        try {
            if (selectedAvatarFile != null) {
                String storedPath = ImageStorage.saveAvatar(selectedAvatarFile);
                user.setProfileImage(storedPath);
            }
        } catch (IOException e) {
            showAlert("Erreur", "Upload image impossible: " + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        userService.add(user);
        showAlert("Succès", "Utilisateur ajouté avec succès !", Alert.AlertType.INFORMATION);
        backToList();
    }

    @FXML
    private void cancel() {
        backToList();
    }

    private void backToList() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(

                                    "/com/gamilha/interfaces/Admin/admin_users.fxml"

                            )

                    );


            Parent content =
                    loader.load();


            BorderPane contentArea =
                    (BorderPane)

                            nameField

                                    .getScene()

                                    .lookup("#contentArea");


            if (contentArea != null)

                contentArea.setCenter(content);


        }

        catch (IOException e) {

            e.printStackTrace();

        }

    }
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}