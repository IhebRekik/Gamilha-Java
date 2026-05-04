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

public class EditAbonnementController {

    @FXML private TextField typeField;
    @FXML private TextField prixField;
    @FXML private TextField dureeField;
    @FXML private TextArea avantagesArea;

    @FXML private CheckBox optStream;
    @FXML private CheckBox optCoaching;
    @FXML private CheckBox optEvent;
    @FXML private CheckBox optAI;

    @FXML private Label errorLabel;

    private Abonnement abonnement;

    private AbonnementServices service = new AbonnementServices();


    public void setAbonnement(Abonnement a) {

        this.abonnement = a;

        typeField.setText(a.getType());

        prixField.setText(String.valueOf(a.getPrix()));

        dureeField.setText(String.valueOf(a.getDuree()));

        avantagesArea.setText(
                String.join(", ", a.getAvantages())
        );
        a.getOptions().forEach(opt -> {

            switch (opt) {

                case "stream" -> optStream.setSelected(true);
                case "coaching" -> optCoaching.setSelected(true);
                case "evenement" -> optEvent.setSelected(true);
                case "ai" -> optAI.setSelected(true);

            }

        });

    }


    @FXML
    private void update() {

        if (!validate())
            return;

        abonnement.setType(typeField.getText());

        abonnement.setPrix(Float.parseFloat(prixField.getText()));

        abonnement.setDuree(Integer.parseInt(dureeField.getText()));

        abonnement.setAvantages(
                List.of(avantagesArea.getText().split("\\s*,\\s*"))
        );
        abonnement.setOptions(getOptions());

        service.updateAbonnement(abonnement);

        errorLabel.setStyle("-fx-text-fill:#22c55e;");
        errorLabel.setText("Modification réussie ✔");

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

        return true;
    }


    private List<String> getOptions() {

        List<String> list = new ArrayList<>();

        if (optStream.isSelected()) list.add("stream");
        if (optCoaching.isSelected()) list.add("coaching");
        if (optEvent.isSelected()) list.add("evenement");

        if (optEvent.isSelected()) list.add("event");
        if (optAI.isSelected()) list.add("ai");

        return list;
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