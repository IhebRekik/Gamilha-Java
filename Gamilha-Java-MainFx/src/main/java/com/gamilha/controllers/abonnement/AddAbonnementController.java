package com.gamilha.controllers.abonnement;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import com.gamilha.entity.Abonnement;
import com.gamilha.services.AbonnementServices;

import java.util.ArrayList;
import java.util.List;

public class AddAbonnementController {

    @FXML private TextField typeField;
    @FXML private TextField prixField;
    @FXML private TextField dureeField;
    @FXML private TextArea avantagesArea;

    @FXML private CheckBox optStream;
    @FXML private CheckBox optCoaching;
    @FXML private CheckBox optEvent;
    @FXML private CheckBox optAI;

    @FXML private Label errorLabel;

    private AbonnementServices service = new AbonnementServices();


    @FXML
    private void add() {

        if (!validate())
            return;

        Abonnement a = new Abonnement();

        a.setType(typeField.getText());

        a.setPrix(Float.parseFloat(prixField.getText()));

        a.setDuree(Integer.parseInt(dureeField.getText()));

        a.setAvantages(
                List.of(avantagesArea.getText().split("\\s*,\\s*"))
        );
        a.setOptions(getOptions());

        service.addAbonnement(a);

        errorLabel.setStyle("-fx-text-fill:#22c55e;");
        errorLabel.setText("Ajout réussi ✔");

        clear();

    }


    private boolean validate() {

        if (typeField.getText().isEmpty()) {

            errorLabel.setText("Type obligatoire");
            return false;
        }

        try {

            Double.parseDouble(prixField.getText());

        } catch (Exception e) {

            errorLabel.setText("Prix invalide");
            return false;
        }

        try {

            Integer.parseInt(dureeField.getText());

        } catch (Exception e) {

            errorLabel.setText("Durée invalide");
            return false;
        }

        return true;
    }


    private List<String> getOptions() {

        List<String> list = new ArrayList<>();

        if (optStream.isSelected()) list.add("stream");
        if (optCoaching.isSelected()) list.add("coaching");
        if (optEvent.isSelected()) list.add("evenement");
        if (optAI.isSelected()) list.add("ai");

        return list;
    }


    private void clear() {

        typeField.clear();
        prixField.clear();
        dureeField.clear();
        avantagesArea.clear();

        optStream.setSelected(false);
        optCoaching.setSelected(false);
        optEvent.setSelected(false);
        optAI.setSelected(false);

    }
    @FXML
    private void goBack() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/Admin/Abonnement.fxml")
            );

            Parent view = loader.load();

            BorderPane root =
                    (BorderPane) typeField.getScene().getRoot();

            BorderPane contentArea =
                    (BorderPane) root.lookup("#contentArea");

            contentArea.setCenter(view);


        } catch (Exception e) {

            e.printStackTrace();

        }

    }

}