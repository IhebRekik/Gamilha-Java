package com.gamilha.controllers;

import com.gamilha.MainApp;

import com.gamilha.services.StreamService;

import com.gamilha.entity.Stream;

import com.gamilha.utils.AlertUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.control.*;
import javafx.scene.image.*;

import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.sql.SQLException;

import java.util.*;
import java.util.stream.Collectors;



public class StreamListController implements Initializable {



    @FXML private TextField searchField;

    @FXML private ComboBox<String> gameFilter;

    @FXML private ComboBox<String> sortFilter;

    @FXML private Label countLabel;

    @FXML private FlowPane streamGrid;



    private final StreamService service =
            new StreamService();



    private ObservableList<Stream> allStreams =
            FXCollections.observableArrayList();



    @Override
    public void initialize(URL url, ResourceBundle rb){

        gameFilter.setItems(

                FXCollections.observableArrayList(

                        "Tous les jeux",
                        "CS2",
                        "Valorant",
                        "LoL",
                        "Dota2"
                )
        );


        gameFilter.setValue("Tous les jeux");


        sortFilter.setItems(

                FXCollections.observableArrayList(

                        "Plus récents",
                        "Plus regardés"

                )
        );


        sortFilter.setValue("Plus récents");


        searchField.textProperty().addListener(
                (o,old,v)->filter()
        );

        gameFilter.valueProperty().addListener(
                (o,old,v)->filter()
        );

        sortFilter.valueProperty().addListener(
                (o,old,v)->filter()
        );


        load();

    }



    private void load(){

        try{

            allStreams.setAll(

                    service.findAll()

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


        String game =
                gameFilter.getValue();


        boolean byViewers =
                "Plus regardés"
                        .equals(sortFilter.getValue());


        List<Stream> result =

                allStreams.stream()

                        .filter(s ->

                                q.isBlank()

                                        || s.getTitle()
                                        .toLowerCase()
                                        .contains(q)

                                        || (

                                        s.getDescription()!=null

                                                && s.getDescription()
                                                .toLowerCase()
                                                .contains(q)
                                )

                        )

                        .filter(s ->

                                "Tous les jeux"
                                        .equals(game)

                                        || (

                                        s.getGame()!=null

                                                && s.getGame()
                                                .equalsIgnoreCase(game)

                                )

                        )

                        .sorted(

                                byViewers

                                        ? Comparator.comparingInt(
                                        Stream::getViewers
                                ).reversed()

                                        : Comparator.comparing(
                                        Stream::getCreatedAt,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )

                        )

                        .collect(Collectors.toList());



        countLabel.setText(

                result.size()
                        + " stream(s)"

        );


        buildGrid(result);

    }



    private void buildGrid(List<Stream> streams){

        streamGrid.getChildren().clear();


        if(streams.isEmpty()){

            Label lbl =
                    new Label("Aucun stream");


            streamGrid.getChildren().add(lbl);

            return;
        }


        for(Stream s : streams)

            streamGrid.getChildren().add(

                    buildCard(s)

            );

    }



    private VBox buildCard(Stream stream){

        VBox card =
                new VBox(0);


        card.setPrefWidth(310);



        StackPane imgBox =
                new StackPane();


        imgBox.setPrefHeight(175);



        ImageView img =
                new ImageView();


        img.setFitWidth(310);

        img.setFitHeight(175);



        Rectangle clip =
                new Rectangle(310,175);


        img.setClip(clip);



        if(stream.getThumbnail()!=null){

            img.setImage(

                    new Image(
                            stream.getThumbnail(),
                            true
                    )
            );

        }



        Label title =
                new Label(
                        stream.getTitle()
                );



        Button btnWatch =
                new Button("Watch");


        btnWatch.setOnAction(e->{

            StreamShowController c =

                    MainApp.loadSceneWithController(

                            "User/StreamShow.fxml"

                    );


            if(c!=null)

                c.setStream(stream);

        });



        Button btnDonate =
                new Button("Donate");


        btnDonate.setOnAction(e->{

            DonationFormController c =

                    MainApp.loadSceneWithController(

                            "User/DonationForm.fxml"

                    );


            if(c!=null)

                c.initCreate(stream);

        });



        card.getChildren().addAll(

                img,
                title,
                btnWatch,
                btnDonate

        );


        return card;

    }



    @FXML
    private void onNewStream(ActionEvent e){

        StreamFormController c =

                MainApp.loadSceneWithController(

                        "User/StreamForm.fxml"

                );


        if(c!=null)

            c.initCreate();

    }



    @FXML
    private void onRefresh(ActionEvent e){

        searchField.clear();

        gameFilter.setValue("Tous les jeux");

        sortFilter.setValue("Plus récents");


        load();

    }



    @FXML
    private void onGoAdmin(ActionEvent e){

        MainApp.loadScene(

                "Admin/AdminStreamList.fxml"

        );

    }

}