package com.gamilha.controllers.abonnement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import com.gamilha.entity.Abonnement;
import com.gamilha.services.AbonnementServices;

public class ShowAbonnementController {

    @FXML
    private Label idLabel;

    @FXML
    private Label typeLabel;

    @FXML
    private Label prixLabel;

    @FXML
    private Label optionsLabel;

    @FXML
    private Button btnModifier;

    @FXML
    private Button btnSupprimer;

    @FXML
    private Button btnRetour;

    private Abonnement abonnement;

    private AbonnementServices service =
            new AbonnementServices();



    public void setAbonnement(Abonnement a){

        this.abonnement = a;

        idLabel.setText(
                String.valueOf(a.getId())
        );

        typeLabel.setText(
                a.getType()
        );

        prixLabel.setText(
                a.getPrix() + " TND"
        );

        optionsLabel.setText(
                String.join(", ",a.getOptions())
        );

    }



    @FXML
    public void initialize(){

        btnRetour.setOnAction(e ->
                retourListe()
        );

        btnModifier.setOnAction(e ->
                openEdit()
        );

        btnSupprimer.setOnAction(e ->
                supprimer()
        );

    }



    private void retourListe(){

        try{

            Parent view =
                    FXMLLoader.load(
                            getClass().getResource(
                                    "/com/gamilha/interfaces/Admin/Abonnement.fxml"
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

        catch(Exception ex){

            ex.printStackTrace();

        }

    }



    private void openEdit(){

        try{

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/gamilha/interfaces/Admin/EditAbonnement.fxml"
                            )
                    );

            Parent view =
                    loader.load();

            EditAbonnementController controller =
                    loader.getController();

            controller.setAbonnement(abonnement);


            BorderPane root =
                    (BorderPane)
                            btnRetour.getScene().getRoot();

            BorderPane contentArea =
                    (BorderPane)
                            root.lookup("#contentArea");

            contentArea.setCenter(view);

        }

        catch(Exception ex){

            ex.printStackTrace();

        }

    }



    private void supprimer(){

        Alert confirm =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirm.setTitle("Confirmation");

        confirm.setHeaderText(
                "Supprimer cet abonnement ?"
        );

        confirm.showAndWait()

                .ifPresent(r -> {

                    if(r == ButtonType.OK){

                        service.deleteAbonnement(
                                abonnement.getId()
                        );

                        retourListe();

                    }

                });

    }

}