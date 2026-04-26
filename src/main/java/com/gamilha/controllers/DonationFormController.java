package com.gamilha.controllers;

import com.gamilha.MainApp;

import com.gamilha.services.DonationService;
import com.gamilha.services.StreamService;

import com.gamilha.entity.Donation;
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



public class DonationFormController implements Initializable {



    @FXML private Label pageTitle;

    @FXML private TextField donorField;

    @FXML private Spinner<Double> amtSpinner;

    @FXML private ComboBox<Stream> streamCombo;

    @FXML private Label emojiPrev;

    @FXML private Label errDonor;
    @FXML private Label errAmt;
    @FXML private Label errStream;

    @FXML private Button submitBtn;



    private final DonationService dService =
            new DonationService();

    private final StreamService sService =
            new StreamService();



    private Donation editing = null;

    private Stream ctxStream = null;



    @Override
    public void initialize(URL url, ResourceBundle rb){

        amtSpinner.setValueFactory(

                new SpinnerValueFactory
                        .DoubleSpinnerValueFactory(

                        0.5,
                        9999.0,
                        1.0,
                        0.5
                )
        );


        amtSpinner.setEditable(true);


        amtSpinner.valueProperty().addListener(

                (o,ov,v)->{

                    updateEmoji(v);

                    vAmt();

                }

        );


        donorField.focusedProperty().addListener(

                (o,ov,f)->{

                    if(!f) vDonor();

                }

        );


        streamCombo.valueProperty().addListener(

                (o,ov,v)-> vStream()

        );


        try{

            streamCombo.setItems(

                    FXCollections.observableArrayList(

                            sService.findAll()

                    )

            );

        }

        catch(Exception ex){

            AlertUtil.showError(

                    "Erreur",

                    "Impossible de charger les streams"

            );

        }


        updateEmoji(1.0);

    }



    public void initCreate(Stream s){

        editing = null;

        ctxStream = s;


        pageTitle.setText(

                "Nouvelle donation"

        );


        submitBtn.setText(

                "Créer"

        );


        if(s!=null){

            streamCombo.setValue(s);

            streamCombo.setDisable(true);

        }

    }



    public void initEdit(Donation d, Stream s){

        editing = d;

        ctxStream = s;


        pageTitle.setText(

                "Modifier donation"

        );


        submitBtn.setText(

                "Enregistrer"

        );


        donorField.setText(

                d.getDonorName()

        );


        amtSpinner
                .getValueFactory()
                .setValue(

                        d.getAmount()

                );


        if(s!=null)

            streamCombo.setValue(s);

    }



    @FXML
    private void onE1(ActionEvent e){

        amtSpinner.getValueFactory().setValue(1.0);

    }



    @FXML
    private void onE5(ActionEvent e){

        amtSpinner.getValueFactory().setValue(5.0);

    }



    @FXML
    private void onE10(ActionEvent e){

        amtSpinner.getValueFactory().setValue(10.0);

    }



    @FXML
    private void onE50(ActionEvent e){

        amtSpinner.getValueFactory().setValue(50.0);

    }



    private void updateEmoji(double v){

        if(emojiPrev==null) return;


        emojiPrev.setText(

                v>=50 ? "🚀"

                        : v>=10 ? "💎"

                        : v>=5 ? "🍕"

                        : "🍩"

        );

    }



    @FXML
    private void onSubmit(ActionEvent e){

        if(!(

                vDonor()

                        & vAmt()

                        & vStream()

        )) return;


        try{

            Donation d =

                    editing!=null

                            ? editing

                            : new Donation();



            d.setDonorName(

                    donorField.getText().trim()

            );


            d.setAmount(

                    amtSpinner.getValue()

            );


            d.setStreamId(

                    streamCombo.getValue().getId()

            );


            d.setUserId(1);



            if(editing==null){

                dService.create(d);

            }

            else{

                dService.update(d);

            }


            Stream s =

                    ctxStream!=null

                            ? ctxStream

                            : streamCombo.getValue();



            if(s!=null){

                DonationListController c =

                        MainApp.loadSceneWithController(

                                "User/DonationList.fxml"

                        );


                if(c!=null)

                    c.setStream(s);

            }

            else{

                MainApp.loadScene(

                        "User/StreamList.fxml"

                );

            }

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

        Stream s =

                ctxStream!=null

                        ? ctxStream

                        : streamCombo.getValue();



        if(s!=null){

            DonationListController c =

                    MainApp.loadSceneWithController(

                            "User/DonationList.fxml"

                    );


            if(c!=null)

                c.setStream(s);

        }

        else{

            MainApp.loadScene(

                    "User/StreamList.fxml"

            );

        }

    }



    private boolean vDonor(){

        String err =

                ValidationUtil.validateDonorName(

                        donorField.getText()

                );


        ValidationUtil.mark(

                donorField,
                err

        );


        ValidationUtil.setErr(

                errDonor,
                err

        );


        return err==null;

    }



    private boolean vAmt(){

        String err =

                ValidationUtil.validateAmount(

                        amtSpinner.getValue()

                );


        ValidationUtil.mark(

                amtSpinner,
                err

        );


        ValidationUtil.setErr(

                errAmt,
                err

        );


        return err==null;

    }



    private boolean vStream(){

        String err =

                ValidationUtil.validateStreamSelected(

                        streamCombo.getValue()

                );


        ValidationUtil.mark(

                streamCombo,
                err

        );


        ValidationUtil.setErr(

                errStream,
                err

        );


        return err==null;

    }

}