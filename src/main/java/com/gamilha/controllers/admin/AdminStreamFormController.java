package com.gamilha.controllers.admin;

import com.gamilha.MainApp;
import com.gamilha.services.StreamService;
import com.gamilha.entity.Stream;
import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.ValidationUtil;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminStreamFormController implements Initializable {

    @FXML private Label pageTitle;
    @FXML private TextField titleField, urlField, thumbField;
    @FXML private TextArea descArea;
    @FXML private ComboBox<String> gameCombo, statusCombo;
    @FXML private Spinner<Integer> viewersSpinner;
    @FXML private Label errTitle, errDesc, errGame, errUrl, obsInfo;
    @FXML private Button submitBtn;

    private final StreamService service = new StreamService();

    private Stream editing = null;


    @Override
    public void initialize(URL url, ResourceBundle rb) {

        gameCombo.setItems(
                FXCollections.observableArrayList(
                        "CS2",
                        "Valorant",
                        "LoL",
                        "Dota2"
                )
        );

        gameCombo.setPromptText("Choisir un jeu");

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

        viewersSpinner.setEditable(true);


        titleField.focusedProperty().addListener(
                (o,ov,f) -> { if(!f) vTitle(); }
        );

        descArea.focusedProperty().addListener(
                (o,ov,f) -> { if(!f) vDesc(); }
        );

        urlField.focusedProperty().addListener(
                (o,ov,f) -> { if(!f) vUrl(); }
        );

        gameCombo.valueProperty().addListener(
                (o,ov,v) -> vGame()
        );
    }



    public void initCreate(){

        editing = null;

        pageTitle.setText(
                "Nouveau Stream"
        );

        submitBtn.setText(
                "Créer"
        );

        if(obsInfo!=null){

            obsInfo.setVisible(false);
            obsInfo.setManaged(false);

        }
    }



    public void initEdit(Stream s){

        editing = s;

        pageTitle.setText(
                "Modifier : "
                        + s.getTitle()
        );

        submitBtn.setText(
                "Enregistrer"
        );


        titleField.setText(
                s.getTitle()
        );

        descArea.setText(

                s.getDescription()!=null
                        ? s.getDescription()
                        : ""
        );

        gameCombo.setValue(
                s.getGame()
        );

        urlField.setText(

                s.getUrl()!=null
                        ? s.getUrl()
                        : ""
        );

        thumbField.setText(

                s.getThumbnail()!=null
                        ? s.getThumbnail()
                        : ""
        );


        viewersSpinner
                .getValueFactory()
                .setValue(
                        s.getViewers()
                );


        statusCombo.setValue(
                s.getStatus()
        );


        if(obsInfo!=null && s.getStreamKey()!=null){

            obsInfo.setText(

                    s.getStreamKey()
            );

            obsInfo.setVisible(true);
            obsInfo.setManaged(true);
        }

    }



    @FXML
    private void onSubmit(ActionEvent e){

        if(!(

                vTitle()
                        & vDesc()
                        & vGame()
                        & vUrl()

        )) return;


        try{

            Stream s =

                    editing!=null
                            ? editing
                            : new Stream();



            s.setTitle(
                    titleField.getText().trim()
            );

            s.setDescription(
                    descArea.getText().trim()
            );

            s.setGame(
                    gameCombo.getValue()
            );

            s.setUrl(
                    urlField.getText().trim()
            );

            s.setThumbnail(
                    thumbField.getText().trim()
            );

            s.setViewers(
                    viewersSpinner.getValue()
            );

            s.setStatus(
                    statusCombo.getValue()
            );

            s.setIsLive(

                    "live".equals(
                            statusCombo.getValue()
                    )
            );


            if(editing==null){

                s.setUserId(1);

                service.create(s);

                AlertUtil.showSuccess(
                        "Créé",
                        "Stream ajouté"
                );

            }
            else{

                service.update(s);

                AlertUtil.showSuccess(
                        "Modifié",
                        "Stream mis à jour"
                );

            }


            MainApp.loadScene(
                    "Admin/AdminStreamList.fxml"
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
                "Admin/AdminStreamList.fxml"
        );
    }



    private boolean vTitle(){

        String err = ValidationUtil
                .validateTitle(
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

        String err = ValidationUtil
                .validateDescription(
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

        String err = ValidationUtil
                .validateGame(
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



    private boolean vUrl(){

        String err = ValidationUtil
                .validateUrl(
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