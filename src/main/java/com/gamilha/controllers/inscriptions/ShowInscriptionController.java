package com.gamilha.controllers.inscriptions;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import com.gamilha.entity.Inscription;
import com.gamilha.services.InscriptionServices;

public class ShowInscriptionController {

    @FXML private Label idLabel;
    @FXML private Label dateDebutLabel;
    @FXML private Label dateFinLabel;
    @FXML private Label userLabel;
    @FXML private Label abonnementLabel;

    @FXML private Button btnRetour;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;

    private Inscription inscription;

    private InscriptionServices service =
            new InscriptionServices();


    public void setInscription(Inscription i){

        inscription = i;



        dateDebutLabel.setText(
                i.getDateDebut().toString()
        );

        dateFinLabel.setText(
                i.getDateFin().toString()
        );

        userLabel.setText(
                i.getUser().getEmail()
        );

        abonnementLabel.setText(
                i.getAbonnements().getType()
        );

    }


    @FXML
    public void initialize(){

        btnRetour.setOnAction(e -> goBack());

        btnModifier.setOnAction(e -> openEdit());

        btnSupprimer.setOnAction(e -> delete());

    }


    private void delete(){

        Alert confirm =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirm.setHeaderText(
                "Supprimer inscription ?"
        );

        confirm.showAndWait()

                .ifPresent(r->{

                    if(r==ButtonType.OK){

                        service.supprimer(inscription.getId());

                        goBack();

                    }

                });

    }


    private void openEdit(){

        try{

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/gamilha/interfaces/Admin/EditInscription.fxml"
                            )
                    );

            Parent view = loader.load();

            EditInscriptionController controller =
                    loader.getController();

            controller.setInscription(inscription);


            BorderPane root =
                    (BorderPane)
                            btnRetour.getScene().getRoot();

            BorderPane contentArea =
                    (BorderPane)
                            root.lookup("#contentArea");

            contentArea.setCenter(view);

        }
        catch(Exception e){

            e.printStackTrace();

        }

    }


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
                            btnRetour.getScene().getRoot();

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