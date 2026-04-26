package com.gamilha.controllers;

import com.gamilha.MainApp;

import com.gamilha.entity.Stream;

import com.gamilha.services.StreamService;

import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.ValidationUtil;

import javafx.collections.FXCollections;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;



public class StreamFormController implements Initializable {


    @FXML private Label pageTitle;

    @FXML private TextField titleField;

    @FXML private TextArea descArea;

    @FXML private ComboBox<String> gameCombo;

    @FXML private TextField thumbField;

    @FXML private ComboBox<String> statusCombo;

    @FXML private TextField urlField;

    @FXML private Spinner<Integer> viewersSpinner;

    @FXML private VBox editOnlyBox;


    @FXML private Label errTitle;
    @FXML private Label errDesc;
    @FXML private Label errGame;
    @FXML private Label errThumb;
    @FXML private Label errUrl;

    @FXML private Label obsInfo;

    @FXML private Button submitBtn;



    private final StreamService service =
            new StreamService();


    private Stream editing = null;



    @Override
    public void initialize(URL url, ResourceBundle rb){

        gameCombo.setItems(

                FXCollections.observableArrayList(

                        "CS2",
                        "Valorant",
                        "LoL",
                        "Dota2"

                )
        );


        statusCombo.setItems(

                FXCollections.observableArrayList(

                        "live",
                        "offline",
                        "ended"

                )
        );


        statusCombo.setValue("live");


        viewersSpinner.setValueFactory(

                new SpinnerValueFactory
                        .IntegerSpinnerValueFactory(

                        0,
                        999999,
                        0

                )
        );


        titleField.focusedProperty().addListener(
                (o,ov,f)->{ if(!f) vTitle(); }
        );

        descArea.focusedProperty().addListener(
                (o,ov,f)->{ if(!f) vDesc(); }
        );

        thumbField.focusedProperty().addListener(
                (o,ov,f)->{ if(!f) vThumb(); }
        );

        gameCombo.valueProperty().addListener(
                (o,ov,v)-> vGame()
        );

    }



    public void initCreate(){

        editing = null;

        pageTitle.setText(
                "Créer Stream"
        );


        submitBtn.setText(
                "Créer"
        );


        editOnlyBox.setVisible(false);

        editOnlyBox.setManaged(false);

    }



    public void initEdit(Stream s){

        editing = s;


        pageTitle.setText(
                "Modifier Stream"
        );


        submitBtn.setText(
                "Save"
        );


        editOnlyBox.setVisible(true);

        editOnlyBox.setManaged(true);


        titleField.setText(
                s.getTitle()
        );


        descArea.setText(
                s.getDescription()
        );


        gameCombo.setValue(
                s.getGame()
        );


        thumbField.setText(
                s.getThumbnail()
        );


        statusCombo.setValue(
                s.getStatus()
        );


        urlField.setText(
                s.getUrl()
        );


        viewersSpinner
                .getValueFactory()
                .setValue(
                        s.getViewers()
                );

    }



    @FXML
    private void onSubmit(ActionEvent e){

        boolean ok =

                vTitle()
                        & vDesc()
                        & vGame()
                        & vThumb();


        if(!ok) return;


        try{

            Stream s =

                    editing!=null

                            ? editing

                            : new Stream();



            s.setTitle(
                    titleField.getText()
            );


            s.setDescription(
                    descArea.getText()
            );


            s.setGame(
                    gameCombo.getValue()
            );


            s.setThumbnail(
                    thumbField.getText()
            );


            s.setStatus(
                    statusCombo.getValue()
            );


            if(editing!=null){

                s.setUrl(
                        urlField.getText()
                );


                s.setViewers(
                        viewersSpinner.getValue()
                );


                service.update(s);

            }

            else{

                s.setViewers(0);

                service.create(s);

            }


            MainApp.loadScene(

                    "User/StreamList.fxml"

            );

        }

        catch(SQLException ex){

            AlertUtil.showError(

                    "Erreur",

                    ex.getMessage()

            );

        }

    }



    @FXML
    private void onCancel(ActionEvent e){

        MainApp.loadScene(

                "User/StreamList.fxml"

        );

    }



    private boolean vTitle(){

        String err =

                ValidationUtil.validateTitle(

                        titleField.getText()

                );


        ValidationUtil.mark(

                titleField,
                err

        );


        ValidationUtil.setErr(

                errTitle,
                err

        );


        return err==null;

    }



    private boolean vDesc(){

        String err =

                ValidationUtil.validateDescription(

                        descArea.getText()

                );


        ValidationUtil.mark(

                descArea,
                err

        );


        ValidationUtil.setErr(

                errDesc,
                err

        );


        return err==null;

    }



    private boolean vGame(){

        String err =

                ValidationUtil.validateGame(

                        gameCombo.getValue()

                );


        ValidationUtil.mark(

                gameCombo,
                err

        );


        ValidationUtil.setErr(

                errGame,
                err

        );


        return err==null;

    }



    private boolean vThumb(){

        String err =

                ValidationUtil.validateThumbnailUrl(

                        thumbField.getText()

                );


        ValidationUtil.mark(

                thumbField,
                err

        );


        ValidationUtil.setErr(

                errThumb,
                err

        );


        return err==null;

    }



    private boolean vUrl(){

        String err =

                ValidationUtil.validateUrl(

                        urlField.getText()

                );


        ValidationUtil.mark(

                urlField,
                err

        );


        ValidationUtil.setErr(

                errUrl,
                err

        );


        return err==null;

    }

}