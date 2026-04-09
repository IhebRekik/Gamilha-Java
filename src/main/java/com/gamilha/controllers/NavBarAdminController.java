package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.utils.SessionContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class NavBarAdminController {

    @FXML
    private BorderPane contentArea;
    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize(){
        User user = SessionContext.getCurrentUser();

        if(user != null){
            welcomeLabel.setText("Bonjour " + user.getName());
        }
        chargerPage("/com/gamilha/interfaces/Admin/Abonnement.fxml");


    }

    void chargerPage(String page){

        try{

            Parent root = FXMLLoader.load(
                    getClass().getResource(
                            "/com/gamilha/interfaces/Admin/" + page
                    )
            );

            contentArea.setCenter(root);

        }catch(Exception e){

            e.printStackTrace();
        }
    }

    void chargerPageComplete(String fullPath){

        try{

            Parent root = FXMLLoader.load(
                    getClass().getResource(fullPath)
            );

            contentArea.setCenter(root);

        }catch(Exception e){

            e.printStackTrace();
        }
    }

    @FXML
    void goAbonnement(){

        chargerPage("Abonnement.fxml");
    }

    @FXML
    void goInscriptions(ActionEvent actionEvent) {
        chargerPage("InscriptionAdmin.fxml");
    }

    @FXML
    void goPlaylists() {
        chargerPage("PlaylistAdmin.fxml");
    }

    @FXML
    void goCoaching(ActionEvent actionEvent) {
        chargerPageComplete("/com/gamilha/interfaces/coaching/PlaylistList.fxml");
    }

    @FXML
    void goCoachVideo(ActionEvent actionEvent) {
        chargerPageComplete("/com/gamilha/interfaces/coaching/VideoList.fxml");
    }

    @FXML
    public void logout(ActionEvent actionEvent) {
        try {

            // supprimer utilisateur connecté
            SessionContext.clear();

            // charger page login
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/login-view.fxml")
            );

            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) actionEvent.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(root, 600, 400));
            stage.setTitle("Connexion");
            stage.setMaximized(true);
            stage.show();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}