package com.gamilha.controllers.admin;

import com.gamilha.MainApp;

import com.gamilha.services.StreamService;

import com.gamilha.entity.Stream;

import com.gamilha.utils.AlertUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;

import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;



public class AdminDonationStreamsController implements Initializable {



    @FXML private TextField searchField;

    @FXML private Label countLabel;

    @FXML private FlowPane streamGrid;



    private final StreamService service =
            new StreamService();



    private ObservableList<Stream> all =
            FXCollections.observableArrayList();



    @Override
    public void initialize(URL url, ResourceBundle rb){

        searchField.textProperty().addListener(

                (o,ov,v)->filter()

        );

        load();

    }



    private void load(){

        try{

            all.setAll(

                    service.findAll()

            );

            filter();

        }

        catch(SQLException e){

            AlertUtil.showError(

                    "Erreur",

                    e.getMessage()

            );

        }

    }



    private void filter(){

        String q =
                searchField.getText()
                        .toLowerCase()
                        .trim();



        List<Stream> res =

                all.stream()

                        .filter(s->

                                q.isBlank()

                                        || s.getTitle()
                                        .toLowerCase()
                                        .contains(q)

                        )

                        .sorted(

                                Comparator.comparing(

                                        Stream::getCreatedAt,

                                        Comparator.nullsLast(

                                                Comparator.reverseOrder()

                                        )

                                )

                        )

                        .collect(Collectors.toList());



        countLabel.setText(

                res.size()
                        + " stream(s)"

        );



        streamGrid.getChildren().clear();



        if(res.isEmpty()){

            Label empty =
                    new Label("Aucun stream");


            streamGrid.getChildren().add(empty);

            return;

        }



        for(Stream s : res){

            VBox card =
                    new VBox(8);


            card.setPrefWidth(280);


            Label title =
                    new Label(

                            s.getTitle()

                    );


            Label info =
                    new Label(

                            s.getGame()
                                    + " | "
                                    + s.getViewers()

                    );


            Label badge =
                    new Label(

                            s.getStatus()

                    );


            Button btn =
                    new Button("Voir donations");


            btn.setOnAction(e->{

                AdminDonationListController c =

                        MainApp.loadSceneWithController(

                                "Admin/AdminDonationList.fxml"

                        );


                if(c!=null)

                    c.setStream(s);

            });



            card.getChildren().addAll(

                    title,
                    info,
                    badge,
                    btn

            );



            streamGrid.getChildren().add(card);

        }

    }



    @FXML
    private void onBack(ActionEvent e){

        MainApp.loadScene(

                "Admin/AdminStreamList.fxml"

        );

    }

}