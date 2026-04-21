package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import com.gamilha.utils.SessionContext;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private UserService userService;


    @FXML
    private void handleLogin(){

        String email =
                emailField.getText()==null
                        ? ""
                        : emailField.getText().trim();


        String password =
                passwordField.getText()==null
                        ? ""
                        : passwordField.getText();


        if(email.isEmpty() || password.isEmpty()){

            showAlert(
                    "Erreur",
                    "Email et mot de passe requis"
            );

            return;
        }


        try{
            if (userService == null) {
                userService = new UserService();
            }

            User user =
                    userService.login(
                            email,
                            password
                    );


            if(user==null){

                showAlert(
                        "Erreur",
                        "Email ou mot de passe incorrect"
                );

                return;
            }


            if(!user.isActive()){

                showAlert(
                        "Compte désactivé",
                        "Votre compte est désactivé"
                );

                return;
            }


            // sauvegarder session utilisateur
            SessionContext.setCurrentUser(user);


            // ouvrir dashboard
            MainApp.openDashboard(user);

        }

        catch(Exception e){

            showAlert(
                    "Erreur",
                    e.getMessage()
            );

        }

    }



    @FXML
    private void onLogin(){

        handleLogin();

    }



    @FXML
    private void handleRegister(){

        try{

            URL url =
                    getClass().getResource(

                            "/com/gamilha/interfaces/register.fxml"

                    );


            if(url==null){

                showAlert(
                        "Erreur",
                        "register.fxml introuvable"
                );

                return;
            }


            FXMLLoader loader =
                    new FXMLLoader(url);


            Stage stage =
                    new Stage();


            stage.setScene(

                    new Scene(
                            loader.load()
                    )

            );


            stage.setTitle("Créer un compte");

            stage.initModality(
                    Modality.APPLICATION_MODAL
            );


            stage.showAndWait();

        }

        catch(Exception e){

            showAlert(
                    "Erreur",
                    e.getMessage()
            );

        }

    }



    private void showAlert(String title,String msg){

        Alert a =
                new Alert(
                        Alert.AlertType.INFORMATION
                );


        a.setTitle(title);

        a.setHeaderText(null);

        a.setContentText(msg);

        a.showAndWait();

    }

}