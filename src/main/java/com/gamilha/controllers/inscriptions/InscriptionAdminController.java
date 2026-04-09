package com.gamilha.controllers.inscriptions;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.gamilha.entity.Inscription;
import com.gamilha.services.InscriptionServices;

public class InscriptionAdminController {

    @FXML
    private GridPane tableGrid;



    private ObservableList<Inscription> masterData =
            FXCollections.observableArrayList();

    private InscriptionServices service =
            new InscriptionServices();


    @FXML
    public void initialize(){

        masterData.addAll(
                service.afficher()
        );


        buildTable(masterData);

    }


    private void buildTable(ObservableList<Inscription> data){

        tableGrid.getChildren().clear();

        tableGrid.getColumnConstraints().clear();


        ColumnConstraints c1 = new ColumnConstraints();

        ColumnConstraints c2 = new ColumnConstraints();

        ColumnConstraints c3 = new ColumnConstraints();

        ColumnConstraints c4 = new ColumnConstraints();

        ColumnConstraints c5 = new ColumnConstraints();
        c1.setPercentWidth(8);
        c2.setPercentWidth(8);
        c3.setPercentWidth(12);
        c4.setPercentWidth(6);
        c5.setPercentWidth(14);

        tableGrid.getColumnConstraints().addAll(
                c1,c2,c3,c4,c5
        );


        addHeader("Date début",0);
        addHeader("Date fin",1);
        addHeader("Utilisateur",2);
        addHeader("Abonnement",3);
        addHeader("Actions",4);



        int row = 1;


        for(Inscription i : data){

            String color =
                    row%2==0
                            ? "#020617"
                            : "#020c1b";


            addCell(
                    i.getDateDebut().toString(),
                    row,
                    0,
                    color
            );


            addCell(
                    i.getDateFin().toString(),
                    row,
                    1,
                    color
            );


            addCell(
                    i.getUser().getEmail(),
                    row,
                    2,
                    color
            );


            addCell(
                    i.getAbonnements().getType(),
                    row,
                    3,
                    color
            );



            HBox actions = new HBox(12);

            actions.setStyle(
                    "-fx-alignment:center;" +
                            "-fx-spacing:8;" +
                            "-fx-background-color:"+color+";" +
                            "-fx-padding:8;"
            );



            Button voir =
                    new Button("Voir");

            Button modifier =
                    new Button("Modifier");

            Button supprimer =
                    new Button("Supprimer");
            voir.setMinWidth(80);
            modifier.setMinWidth(95);
            supprimer.setMinWidth(110);

            voir.setMaxWidth(Double.MAX_VALUE);
            modifier.setMaxWidth(Double.MAX_VALUE);
            supprimer.setMaxWidth(Double.MAX_VALUE);


            voir.setStyle("""
    -fx-background-color:#3b82f6;
    -fx-text-fill:white;
    -fx-background-radius:8;
    -fx-padding:6 14 6 14;
""");

            modifier.setStyle("""
    -fx-background-color:#facc15;
    -fx-text-fill:black;
    -fx-background-radius:8;
    -fx-padding:6 14 6 14;
""");

            supprimer.setStyle("""
    -fx-background-color:#ef4444;
    -fx-text-fill:white;
    -fx-background-radius:8;
    -fx-padding:6 14 6 14;
""");


            voir.setOnAction(e ->
                    openPage(
                            "/com/gamilha/interfaces/Admin/ShowInscription.fxml",
                            i
                    )
            );


            modifier.setOnAction(e ->
                    openPage(
                            "/com/gamilha/interfaces/Admin/EditInscription.fxml",
                            i
                    )
            );


            supprimer.setOnAction(e ->
                    confirmDelete(i)
            );



            actions.getChildren().addAll(
                    voir,
                    modifier,
                    supprimer
            );


            GridPane.setHgrow(actions, Priority.ALWAYS);
            tableGrid.add(actions,4,row);


            row++;

        }

    }


    private void confirmDelete(Inscription i){

        Alert confirm =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirm.setHeaderText(
                "Supprimer inscription ?"
        );

        confirm.showAndWait()

                .ifPresent(r->{

                    if(r==ButtonType.OK){

                        service.supprimer(i.getId());

                        masterData.remove(i);

                        buildTable(masterData);

                    }

                });

    }


    private void addHeader(String text,int col){

        Label l = new Label(text);

        l.setMaxWidth(Double.MAX_VALUE);

        l.setStyle("""
        -fx-text-fill:#e2e8f0;
        -fx-font-size:14;
        -fx-font-weight:bold;
        -fx-alignment:center-left;
        -fx-padding:14 10 14 10;
        -fx-background-color:#020617;
        -fx-border-color:#1e293b;
        -fx-border-width:0 0 1 0;
    """);

        tableGrid.add(l,col,0);

    }


    private void addCell(String text,int row,int col,String color){

        Label label = new Label(text);

        label.setMaxWidth(Double.MAX_VALUE);

        label.setWrapText(true);

        label.setStyle(
                "-fx-text-fill:#cbd5e1;" +
                        "-fx-font-size:13;" +
                        "-fx-alignment:center-left;" +
                        "-fx-padding:14 10 14 10;" +
                        "-fx-background-color:" + color + ";" +
                        "-fx-border-color:#1e293b;" +
                        "-fx-border-width:0 0 1 0;"
        );

        tableGrid.add(label,col,row);

    }

    private void openPage(String fxml,Object data){

        try{

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(fxml)
                    );

            Parent view =
                    loader.load();

            if(data!=null){

                loader.getController()

                        .getClass()

                        .getMethod(
                                "setInscription",
                                Inscription.class
                        )

                        .invoke(
                                loader.getController(),
                                data
                        );

            }

            BorderPane root =
                    (BorderPane)
                            tableGrid.getScene().getRoot();

            BorderPane contentArea =
                    (BorderPane)
                            root.lookup("#contentArea");

            contentArea.setCenter(view);

        }

        catch(Exception ex){

            ex.printStackTrace();

        }

    }

}