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

import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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


        Comparator<Donation> sorter = switch (sortFilter.getValue()) {
            case "Plus anciens" -> Comparator.comparing(Donation::getCreatedAt);
            case "Montant ↓" -> Comparator.comparingDouble(Donation::getAmount).reversed();
            case "Montant ↑" -> Comparator.comparingDouble(Donation::getAmount);
            default -> Comparator.comparing(Donation::getCreatedAt).reversed();
        };

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

                        .sorted(sorter)

                        .collect(Collectors.toList());



        double total =

                res.stream()
                        .mapToDouble(
                                Donation::getAmount
                        )
                        .sum();



        lblTotal.setText(String.format("Total : %.2f €", total));


        countLabel.setText(

                res.size()
                        + " donation(s)"

        );


        buildGrid(res);

    }



    private void buildGrid(List<Donation> list){

        donationGrid.getChildren().clear();

        if (list.isEmpty()) {
            Label empty = new Label("Aucune donation trouvée");
            empty.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:15px;");
            donationGrid.getChildren().add(empty);
            return;
        }

        for(Donation d : list)

            donationGrid.getChildren().add(

                    buildCard(d)

            );

    }



    private VBox buildCard(Donation d){

        VBox card = new VBox(10);
        card.setPrefWidth(280);
        card.setMaxWidth(280);
        card.setPadding(new Insets(14));
        card.setStyle("-fx-background-color:#1a1a26;-fx-border-color:#2a2a40;" +
                "-fx-border-radius:12;-fx-background-radius:12;");

        Label donor = new Label("👤 " + d.getDonorName());
        donor.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#e2e8f0;");
        donor.setWrapText(true);

        Label amount = new Label("💰 " + d.getFormattedAmount());
        amount.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");

        Label createdAt = new Label(d.getCreatedAt() != null ? "📅 " + d.getCreatedAt().format(FMT) : "");
        createdAt.setStyle("-fx-font-size:12px;-fx-text-fill:#94a3b8;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button show = new Button("Voir");
        show.setStyle("-fx-background-color:#8b5cf6;-fx-text-fill:white;-fx-font-weight:bold;" +
                "-fx-background-radius:8;-fx-cursor:hand;-fx-padding:6 14;");


        show.setOnAction(e->{

            DonationShowController c =

                    MainApp.loadSceneWithController(

                            "User/DonationShow.fxml"

                    );


            if(c!=null)

                c.setDonation(d,cur);

        });



        Button edit = new Button("Modifier");
        edit.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#e2e8f0;" +
                "-fx-border-color:#334155;-fx-border-radius:8;-fx-background-radius:8;" +
                "-fx-cursor:hand;-fx-padding:6 12;");


        edit.setOnAction(e->{

            DonationFormController c =

                    MainApp.loadSceneWithController(

                            "User/DonationForm.fxml"

                    );


            if(c!=null)

                c.initEdit(d,cur);

        });



        actions.getChildren().addAll(edit, show);
        card.getChildren().addAll(donor, amount, createdAt, actions);


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

            c.setStream(cur, true);

    }

}