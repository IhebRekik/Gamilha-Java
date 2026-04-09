package com.gamilha.controllers.inscriptions;

import com.gamilha.entity.Inscription;
import com.gamilha.services.InscriptionServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.time.LocalDate;

public class EditInscriptionController {

    @FXML private DatePicker dateDebutPicker;
    @FXML private DatePicker dateFinPicker;

    @FXML private TextField userField;
    @FXML private TextField abonnementField;

    @FXML private Label errorLabel;

    private Inscription inscription;

    private InscriptionServices service =
            new InscriptionServices();



    @FXML
    public void initialize(){

        // empêcher date passée
        dateDebutPicker.setDayCellFactory(picker ->
                new DateCell(){

                    @Override
                    public void updateItem(LocalDate date, boolean empty){

                        super.updateItem(date, empty);

                        setDisable(
                                date.isBefore(LocalDate.now())
                        );

                    }

                }
        );

    }



    public void setInscription(Inscription i){

        inscription = i;

        dateDebutPicker.setValue(
                i.getDateDebut()
        );

        dateFinPicker.setValue(
                i.getDateFin()
        );

        // afficher email utilisateur
        userField.setText(
                i.getUser().getEmail()
        );

        // afficher type abonnement
        abonnementField.setText(
                i.getAbonnements().getType()
        );

    }



    @FXML
    private void update(){

        errorLabel.setText("");

        if(dateDebutPicker.getValue()==null){

            errorLabel.setText(
                    "Date début obligatoire"
            );

            return;

        }


        if(dateFinPicker.getValue()==null){

            errorLabel.setText(
                    "Date fin obligatoire"
            );

            return;

        }


        if(dateFinPicker.getValue()
                .isBefore(dateDebutPicker.getValue())){

            errorLabel.setText(
                    "Date fin doit être après date début"
            );

            return;

        }



        inscription.setDateDebut(
                dateDebutPicker.getValue()
        );

        inscription.setDateFin(
                dateFinPicker.getValue()
        );


        service.modifier(inscription);


        goBack();

    }



    @FXML
    private void goBack(){

        try{

            Parent view =
                    FXMLLoader.load(
                            getClass().getResource(
                                    "/com/gamilha/interfaces/Admin/InscriptionAdmin.fxml"
                            )
                    );


            BorderPane root =
                    (BorderPane)
                            userField.getScene().getRoot();


            BorderPane contentArea =
                    (BorderPane)
                            root.lookup("#contentArea");


            contentArea.setCenter(view);

        }

        catch(Exception e){

            e.printStackTrace();

        }

    }

}