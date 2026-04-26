package com.gamilha.controllers;

import com.gamilha.MainApp;

import com.gamilha.services.DonationService;

import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;

import com.gamilha.utils.AlertUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.geometry.Pos;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;

import java.time.format.DateTimeFormatter;

import java.util.*;
import java.util.stream.Collectors;



public class DonationListController implements Initializable {


    @FXML private Label lblTitle;

    @FXML private Label lblTotal;

    @FXML private Label countLabel;

    @FXML private TextField searchField;

    @FXML private ComboBox<String> sortFilter;

    @FXML private FlowPane donationGrid;



    private Stream cur;


    private final DonationService service =
            new DonationService();


    private ObservableList<Donation> all =
            FXCollections.observableArrayList();



    private static final DateTimeFormatter FMT =

            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );



    @Override
    public void initialize(URL url, ResourceBundle rb){

        sortFilter.setItems(

                FXCollections.observableArrayList(

                        "Plus récentes",
                        "Plus anciens",
                        "Montant ↓",
                        "Montant ↑"
                )
        );


        sortFilter.setValue("Plus récentes");


        searchField.textProperty().addListener(

                (o,ov,v)->filter()

        );


        sortFilter.valueProperty().addListener(

                (o,ov,v)->filter()

        );

    }



    public void setStream(Stream s){

        cur = s;

        lblTitle.setText(

                "Donations — "
                        + s.getTitle()

        );

        load();

    }



    private void load(){

        try{

            all.setAll(

                    service.findByStream(

                            cur.getId()

                    )
            );


            filter();

        }

        catch(SQLException e){

            AlertUtil.showError(

                    "Erreur BDD",

                    e.getMessage()

            );

        }

    }



    private void filter(){

        String q =

                searchField.getText()
                        .toLowerCase()
                        .trim();


        List<Donation> res =

                all.stream()

                        .filter(d->

                                q.isBlank()

                                        || d.getDonorName()
                                        .toLowerCase()
                                        .contains(q)

                                        || String.format(

                                        "%.2f",
                                        d.getAmount()

                                ).contains(q)

                        )

                        .sorted(

                                Comparator.comparing(

                                        Donation::getCreatedAt

                                ).reversed()

                        )

                        .collect(Collectors.toList());



        double total =

                res.stream()
                        .mapToDouble(
                                Donation::getAmount
                        )
                        .sum();



        lblTotal.setText(

                "Total : "
                        + total
        );


        countLabel.setText(

                res.size()
                        + " donation(s)"

        );


        buildGrid(res);

    }



    private void buildGrid(List<Donation> list){

        donationGrid.getChildren().clear();


        for(Donation d : list)

            donationGrid.getChildren().add(

                    buildCard(d)

            );

    }



    private VBox buildCard(Donation d){

        VBox card =
                new VBox(8);


        Label donor =
                new Label(

                        d.getDonorName()

                );


        Label amount =
                new Label(

                        d.getFormattedAmount()

                );


        Button show =
                new Button("Voir");


        show.setOnAction(e->{

            DonationShowController c =

                    MainApp.loadSceneWithController(

                            "User/DonationShow.fxml"

                    );


            if(c!=null)

                c.setDonation(d,cur);

        });



        Button edit =
                new Button("Edit");


        edit.setOnAction(e->{

            DonationFormController c =

                    MainApp.loadSceneWithController(

                            "User/DonationForm.fxml"

                    );


            if(c!=null)

                c.initEdit(d,cur);

        });



        card.getChildren().addAll(

                donor,
                amount,
                show,
                edit

        );


        return card;

    }



    @FXML
    private void onNew(ActionEvent e){

        DonationFormController c =

                MainApp.loadSceneWithController(

                        "User/DonationForm.fxml"

                );


        if(c!=null)

            c.initCreate(cur);

    }



    @FXML
    private void onBack(ActionEvent e){

        StreamShowController c =

                MainApp.loadSceneWithController(

                        "User/StreamShow.fxml"

                );


        if(c!=null)

            c.setStream(cur);

    }

}