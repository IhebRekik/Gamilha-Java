package com.gamilha.controllers.inscriptions;

import com.gamilha.entity.Abonnement;
import com.gamilha.entity.Inscription;
import com.gamilha.entity.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import com.gamilha.services.InscriptionServices;

import java.util.List;

public class InscriptionController {

    @FXML
    private TextField txtUserId;

    @FXML
    private TextField txtAbonnementId;

    @FXML
    private DatePicker dateDebut;

    @FXML
    private DatePicker dateFin;

    @FXML
    private TextArea output;

    InscriptionServices ic = new InscriptionServices();


    @FXML
    public void ajouter() {

        User u = new User();
        u.setId(Integer.parseInt(txtUserId.getText()));

        Abonnement a = new Abonnement();
        a.setId(Integer.parseInt(txtAbonnementId.getText()));



        Inscription i = new Inscription();

        i.setDateDebut(dateDebut.getValue());
        i.setDateFin(dateFin.getValue());
        i.setUser(u);
        i.setAbonnements(a);

        ic.ajouter(i);

        output.setText("Ajout effectué");
    }


    @FXML
    public void afficher() {

        List<Inscription> list = ic.afficher();

        output.clear();

        for (Inscription i : list) {

            output.appendText(i.toString() + "\n");
        }
    }


    @FXML
    public void modifier() {

        User u = new User();
        u.setId(Integer.parseInt(txtUserId.getText()));

        Abonnement a = new Abonnement();
        a.setId(Integer.parseInt(txtAbonnementId.getText()));



        Inscription i = new Inscription();

        i.setId(1); // id à modifier
        i.setDateDebut(dateDebut.getValue());
        i.setDateFin(dateFin.getValue());
        i.setUser(u);
        i.setAbonnements(a);

        ic.modifier(i);

        output.setText("Modification effectuée");
    }


    @FXML
    public void supprimer() {

        ic.supprimer(1); // id à supprimer

        output.setText("Suppression effectuée");
    }

}