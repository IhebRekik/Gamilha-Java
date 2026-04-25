package com.gamilha.controllers.abonnement;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.gamilha.entity.Abonnement;
import com.gamilha.services.AbonnementServices;

import java.util.stream.Collectors;

public class AbonnementAdminController {

    @FXML
    private TextField searchInput;

    @FXML
    private ComboBox<String> filterType;

    @FXML
    private ComboBox<String> sortPrice;

    @FXML
    private GridPane tableGrid;

    @FXML
    private Button btnAdd;

    private ObservableList<Abonnement> masterData =
            FXCollections.observableArrayList();

    private AbonnementServices service =
            new AbonnementServices();



    @FXML
    public void initialize(){

        masterData.addAll(
                service.getAbonnements()
        );


        filterType.getItems().add("Tous");

        filterType.getItems().addAll(
                masterData.stream()
                        .map(Abonnement::getType)
                        .distinct()
                        .toList()
        );

        filterType.setValue("Tous");


        sortPrice.getItems().addAll(
                "Défaut",
                "Prix croissant",
                "Prix décroissant"
        );

        sortPrice.setValue("Défaut");


        btnAdd.setOnAction(e -> openPage(
                "/com/gamilha/interfaces/Admin/AddAbonnement.fxml",
                null
        ));


        buildTable(masterData);


        tableGrid.setMaxWidth(Double.MAX_VALUE);
        tableGrid.setPrefWidth(Double.MAX_VALUE);


        searchInput.textProperty().addListener((obs,o,n)->filter());

        filterType.valueProperty().addListener((obs,o,n)->filter());

        sortPrice.valueProperty().addListener((obs,o,n)->filter());

    }



    private void buildTable(ObservableList<Abonnement> data){

        tableGrid.getChildren().clear();

        tableGrid.getColumnConstraints().clear();


        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(20);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(15);

        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(40);

        ColumnConstraints col4 = new ColumnConstraints();
        col4.setPercentWidth(25);


        tableGrid.getColumnConstraints().addAll(
                col1,col2,col3,col4
        );


        addHeader("Type",0);
        addHeader("Prix",1);
        addHeader("Options",2);
        addHeader("Actions",3);


        int row = 1;


        for(Abonnement a : data){

            String rowColor =
                    row % 2 == 0
                            ? "#020617"
                            : "#020c1b";


            addCell(a.getType(),row,0,rowColor);


            addCell(
                    String.format("%.2f",a.getPrix()),
                    row,
                    1,
                    rowColor
            );


            addCell(
                    String.join(", ",a.getOptions()),
                    row,
                    2,
                    rowColor
            );



            HBox actions = new HBox(10);

            actions.setStyle(
                    "-fx-alignment:center-left;" +
                            "-fx-background-color:"+rowColor+";" +
                            "-fx-padding:8;"
            );



            Button voir =
                    new Button("Voir");

            Button modifier =
                    new Button("Modifier");

            Button supprimer =
                    new Button("Supprimer");



            voir.setStyle("""
                -fx-background-color:#2563eb;
                -fx-text-fill:white;
                -fx-background-radius:6;
                -fx-padding:6 12 6 12;
            """);


            modifier.setStyle("""
                -fx-background-color:#facc15;
                -fx-text-fill:black;
                -fx-background-radius:6;
                -fx-padding:6 12 6 12;
            """);


            supprimer.setStyle("""
                -fx-background-color:#ef4444;
                -fx-text-fill:white;
                -fx-background-radius:6;
                -fx-padding:6 12 6 12;
            """);



            voir.setOnAction(e ->
                    openPage(
                            "/com/gamilha/interfaces/Admin/ShowAbonnement.fxml",
                            a
                    )
            );


            modifier.setOnAction(e ->
                    openPage(
                            "/com/gamilha/interfaces/Admin/EditAbonnement.fxml",
                            a
                    )
            );


            supprimer.setOnAction(e ->
                    confirmDelete(a)
            );



            actions.getChildren().addAll(
                    voir,
                    modifier,
                    supprimer
            );


            tableGrid.add(actions,3,row);


            row++;

        }

    }



    private void addHeader(String text,int col){

        Label label =
                new Label(text);


        label.setMaxWidth(Double.MAX_VALUE);


        label.setStyle("""
            -fx-background-color:#020617;
            -fx-text-fill:#e2e8f0;
            -fx-font-weight:bold;
            -fx-font-size:15;
            -fx-padding:14;
            -fx-border-color:#1e293b;
        """);


        tableGrid.add(label,col,0);

    }




    private void addCell(String text,int row,int col,String color){

        Label label =
                new Label(text);


        label.setMaxWidth(Double.MAX_VALUE);


        label.setWrapText(true);


        label.setStyle(
                "-fx-text-fill:#cbd5f5;" +
                        "-fx-padding:10;" +
                        "-fx-border-color:#1e293b;" +
                        "-fx-background-color:" + color + ";"
        );


        tableGrid.add(label,col,row);

    }




    private void filter(){

        String search =
                searchInput.getText() == null
                        ? ""
                        : searchInput.getText().toLowerCase();


        String type =
                filterType.getValue();


        String sort =
                sortPrice.getValue();



        ObservableList<Abonnement> filtered =

                masterData.stream()

                        .filter(a->

                                a.getType().toLowerCase().contains(search)

                                        ||

                                        String.valueOf(a.getPrix()).contains(search)

                        )


                        .filter(a->

                                type == null

                                        ||

                                        type.equals("Tous")

                                        ||

                                        a.getType().equals(type)

                        )


                        .collect(
                                Collectors.toCollection(
                                        FXCollections::observableArrayList
                                )
                        );



        if("Prix croissant".equals(sort))

            filtered.sort(
                    (a,b)->Double.compare(a.getPrix(),b.getPrix())
            );



        if("Prix décroissant".equals(sort))

            filtered.sort(
                    (a,b)->Double.compare(b.getPrix(),a.getPrix())
            );



        buildTable(filtered);

    }




    private void openPage(String fxml, Abonnement abonnement){

        try{

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(fxml)
                    );


            Parent view =
                    loader.load();



            if(abonnement != null){

                Object controller =
                        loader.getController();


                try{

                    controller.getClass()

                            .getMethod(
                                    "setAbonnement",
                                    Abonnement.class
                            )

                            .invoke(controller, abonnement);

                }

                catch(Exception ignored){}

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




    private void confirmDelete(Abonnement a){

        Alert confirm =
                new Alert(Alert.AlertType.CONFIRMATION);


        confirm.setTitle("Confirmation");


        confirm.setHeaderText(
                "Supprimer cet abonnement ?"
        );


        confirm.setContentText(
                "Type : " + a.getType()
        );



        confirm.showAndWait()

                .ifPresent(response -> {

                    if(response == ButtonType.OK){

                        service.deleteAbonnement(a.getId());


                        masterData.remove(a);


                        buildTable(masterData);

                    }

                });

    }

}