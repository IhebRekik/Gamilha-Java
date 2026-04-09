package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class  NavBarUserController {

    @FXML
    private BorderPane contentArea;
    @FXML
    private Label welcomeLabel;

    @FXML
    public void initialize(){
        User user = SessionContext.getCurrentUser();

        if(user != null){
            welcomeLabel.setText(user.getName());
        }
        chargerPage("Abonnement.fxml");
    }

    public void chargerPage(String page){

        try{

            Parent pageFXML = FXMLLoader.load(
                    getClass().getResource(
                            "/com/gamilha/interfaces/User/" + page
                    )
            );

            contentArea.setCenter(pageFXML);

        }catch(Exception e){

            e.printStackTrace();
        }
    }

    @FXML
    void goAccueil(){

        chargerPage("Accueil.fxml");
    }

    @FXML
    void goAbonnement(){

        chargerPage("Abonnement.fxml");
    }

    @FXML
    void goEquipe(){

        chargerPage("Equipe.fxml");
    }

    @FXML
    void goPlaylists(){

        chargerPageComplete("/com/gamilha/interfaces/coaching/PlaylistList.fxml");
    }

    @FXML
    void goVideos(){

        chargerPageComplete("/com/gamilha/interfaces/coaching/VideoList.fxml");
    }

    @FXML
    void goCoaching(){

        chargerPageComplete("/com/gamilha/interfaces/coaching/PlaylistList.fxml");
    }

    public void chargerPageComplete(String fullPath){

        try{

            Parent pageFXML = FXMLLoader.load(
                    getClass().getResource(fullPath)
            );

            contentArea.setCenter(pageFXML);

        }catch(Exception e){

            e.printStackTrace();
        }
    }
}