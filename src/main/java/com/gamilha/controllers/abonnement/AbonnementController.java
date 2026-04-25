package com.gamilha.controllers.abonnement;

import com.gamilha.entity.Inscription;
import com.gamilha.entity.User;
import com.gamilha.services.InscriptionServices;
import com.gamilha.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import com.gamilha.entity.Abonnement;
import com.gamilha.services.AbonnementServices;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AbonnementController implements Initializable {



    AbonnementServices service = new AbonnementServices();
    InscriptionServices serviceInscription = new InscriptionServices();

    @FXML
    private FlowPane containerAbonnements;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        List<Abonnement> liste = service.getAbonnements();

        User user = SessionContext.getCurrentUser();

        if(user == null){
            System.out.println("Utilisateur non connecté");
            return;
        }

        List<Integer> ids =
                serviceInscription.getUserAbonnementsActifs(user);

        for(Abonnement a : liste){

            VBox card = new VBox();
            card.setSpacing(12);

            card.setStyle(
                    "-fx-background-color:#1b1d2e;" +
                            "-fx-padding:25;" +
                            "-fx-background-radius:18;" +
                            "-fx-pref-width:260;" +
                            "-fx-pref-height:520;"
            );

            Label nom = new Label(a.getType());

            nom.setStyle(
                    "-fx-text-fill:white;" +
                            "-fx-font-size:18;" +
                            "-fx-font-weight:bold;"
            );

            Label prix = new Label(
                    a.getPrix()+" TND / "+a.getDuree()+" mois"
            );

            prix.setStyle(
                    "-fx-text-fill:white;" +
                            "-fx-font-size:14;"
            );

            VBox listeDesc = new VBox(6);

            for(String e : a.getAvantages()){

                Label item = new Label("✔ " + e);

                item.setStyle(
                        "-fx-text-fill:#00ff9d;" +
                                "-fx-font-size:13;"
                );

                listeDesc.getChildren().add(item);
            }

            Button btn = new Button();

            if(ids.contains(a.getId())){

                btn.setText("Déjà abonné");

                btn.setStyle(
                        "-fx-background-color:#00ff9d;" +
                                "-fx-text-fill:black;" +
                                "-fx-background-radius:6;"
                );

                btn.setDisable(true);

            } else {

                btn.setText("S'abonner");

                btn.setStyle(
                        "-fx-background-color:#e6e6e6;" +
                                "-fx-text-fill:black;" +
                                "-fx-background-radius:6;"
                );

                btn.setOnAction(event -> {

                    try {

                        serviceInscription.createCheckoutSession(
                                a.getId()
                        );

                    } catch (Exception ex){

                        ex.printStackTrace();

                    }

                });

            }

            card.getChildren().addAll(
                    nom,
                    prix,
                    listeDesc,
                    btn
            );

            containerAbonnements
                    .getChildren()
                    .add(card);
        }

    }

}