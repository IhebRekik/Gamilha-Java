package com.gamilha.controllers.admin;

import com.gamilha.MainApp;

import com.gamilha.entity.Stream;

import com.gamilha.services.StreamService;

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

import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;



public class AdminStreamListController implements Initializable {



    @FXML private TextField searchField;

    @FXML private ComboBox<String> gameFilter;

    @FXML private ComboBox<String> sortFilter;

    @FXML private ComboBox<String> viewersFilter;

    @FXML private Label countLabel;

    @FXML private GridPane streamGrid;



    private final StreamService service =
            new StreamService();



    private ObservableList<Stream> all =
            FXCollections.observableArrayList();



    private static final int COLS = 3;



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
                        "Plus regardés",
                        "Moins regardés"

                )
        );


        sortFilter.setValue("Plus récents");


        viewersFilter.setItems(

                FXCollections.observableArrayList(

                        "Tous",
                        "+50",
                        "+100",
                        "+500"

                )
        );


        viewersFilter.setValue("Tous");


        searchField.textProperty().addListener(
                (o,ov,v)->filter()
        );

        gameFilter.valueProperty().addListener(
                (o,ov,v)->filter()
        );

        sortFilter.valueProperty().addListener(
                (o,ov,v)->filter()
        );

        viewersFilter.valueProperty().addListener(
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

                                        Stream::getCreatedAt

                                ).reversed()

                        )

                        .collect(Collectors.toList());



        countLabel.setText(

                res.size()
                        + " stream(s)"

        );


        buildGridPane(res);

    }



    private void buildGridPane(List<Stream> streams){

        streamGrid.getChildren().clear();


        for(int i=0;i<streams.size();i++){

            streamGrid.add(

                    buildCard(

                            streams.get(i)

                    ),

                    i%COLS,

                    i/COLS

            );

        }

    }



    private VBox buildCard(Stream s){

        VBox card =
                new VBox(8);


        Label title =
                new Label(

                        s.getTitle()

                );


        Label viewers =
                new Label(

                        s.getViewers()
                                + " viewers"

                );


        Button view =
                new Button("Voir");


        view.setOnAction(e->{

            AdminStreamShowController c =

                    MainApp.loadSceneWithController(

                            "Admin/AdminStreamShow.fxml"

                    );


            if(c!=null)

                c.setStream(s);

        });



        Button edit =
                new Button("Edit");


        edit.setOnAction(e->{

            AdminStreamFormController c =

                    MainApp.loadSceneWithController(

                            "Admin/AdminStreamForm.fxml"

                    );


            if(c!=null)

                c.initEdit(s);

        });



        Button delete =
                new Button("Delete");


        delete.setOnAction(e->

                confirmDelete(s)

        );



        card.getChildren().addAll(

                title,
                viewers,
                view,
                edit,
                delete

        );


        return card;

    }



    private void confirmDelete(Stream s){

        try{

            service.delete(

                    s.getId()

            );


            load();

        }

        catch(SQLException ex){

            AlertUtil.showError(

                    "Erreur",

                    ex.getMessage()

            );

        }

    }



    @FXML
    private void onRefresh(ActionEvent e){

        load();

    }



    @FXML
    private void onFront(ActionEvent e){

        MainApp.loadScene(

                "User/StreamList.fxml"

        );

    }



    @FXML
    private void onDonations(ActionEvent e){

        MainApp.loadScene(

                "Admin/AdminDonationStreams.fxml"

        );

    }

}