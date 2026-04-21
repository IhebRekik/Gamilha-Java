package com.gamilha.controllers.admin;

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

public class AdminDonationListController implements Initializable {

    @FXML private Label lblTitle, lblTotal, countLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFilter;
    @FXML private FlowPane donationGrid;

    private Stream cur;

    private final DonationService service = new DonationService();

    private ObservableList<Donation> all =
            FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    @Override
    public void initialize(URL url, ResourceBundle rb) {

        sortFilter.setItems(
                FXCollections.observableArrayList(
                        "Plus récentes",
                        "Plus anciens",
                        "Montant ↓",
                        "Montant ↑"
                )
        );

        sortFilter.setValue("Plus récentes");

        searchField.textProperty().addListener((o,ov,v) -> filter());

        sortFilter.valueProperty().addListener((o,ov,v) -> filter());
    }


    public void setStream(Stream s) {

        cur = s;

        lblTitle.setText(
                "💰 Donations — " + s.getTitle()
        );

        load();
    }


    private void load() {

        try {

            all.setAll(
                    service.findByStream(cur.getId())
            );

            filter();

        } catch (SQLException e) {

            AlertUtil.showError(
                    "Erreur",
                    e.getMessage()
            );
        }
    }


    private void filter() {

        String q =
                searchField.getText()
                        .toLowerCase()
                        .trim();

        List<Donation> res =
                all.stream()

                        .filter(d ->
                                q.isBlank()

                                        || d.getDonorName()
                                        .toLowerCase()
                                        .contains(q)

                                        || String.format("%.2f",
                                                d.getAmount())
                                        .contains(q)
                        )

                        .sorted(
                                switch (sortFilter.getValue()) {

                                    case "Plus anciens" ->

                                            Comparator.comparing(
                                                    Donation::getCreatedAt
                                            );

                                    case "Montant ↓" ->

                                            Comparator.comparingDouble(
                                                    Donation::getAmount
                                            ).reversed();

                                    case "Montant ↑" ->

                                            Comparator.comparingDouble(
                                                    Donation::getAmount
                                            );

                                    default ->

                                            Comparator.comparing(
                                                    Donation::getCreatedAt
                                            ).reversed();
                                }
                        )

                        .collect(Collectors.toList());


        double total =
                res.stream()
                        .mapToDouble(
                                Donation::getAmount
                        )
                        .sum();


        lblTotal.setText(
                String.format(
                        "Total : %.2f €",
                        total
                )
        );


        countLabel.setText(
                res.size()
                        + " donation(s)"
        );


        donationGrid.getChildren().clear();


        if(res.isEmpty()){

            Label empty =
                    new Label(
                            "Aucune donation"
                    );

            donationGrid
                    .getChildren()
                    .add(empty);

            return;
        }


        for(Donation d : res){

            donationGrid
                    .getChildren()
                    .add(
                            buildCard(d)
                    );
        }

    }


    private VBox buildCard(Donation d){

        VBox card =
                new VBox(8);

        card.setPrefWidth(250);

        HBox top =
                new HBox(10);

        top.setAlignment(
                Pos.CENTER_LEFT
        );


        Label emoji =
                new Label(
                        d.getEmoji()
                );

        Label amt =
                new Label(
                        d.getFormattedAmount()
                );


        top.getChildren().addAll(
                emoji,
                amt
        );


        Label donor =
                new Label(
                        "👤 "
                                + d.getDonorName()
                );


        Label date =
                new Label(

                        d.getCreatedAt()!=null

                                ? "📅 "
                                + d.getCreatedAt()
                                .format(FMT)

                                : ""
                );


        Button bEdit =
                new Button("Modifier");

        Button bDel =
                new Button("Supprimer");


        bDel.setOnAction(
                e -> confirmDel(d)
        );


        card.getChildren().addAll(

                top,
                donor,
                date,
                bEdit,
                bDel
        );

        return card;
    }


    private void confirmDel(Donation d){

        try {

            service.delete(
                    d.getId()
            );

            load();

        } catch (SQLException ex) {

            AlertUtil.showError(
                    "Erreur",
                    ex.getMessage()
            );
        }
    }


    @FXML
    private void onBack(ActionEvent e){

        System.out.println("back");

    }

}