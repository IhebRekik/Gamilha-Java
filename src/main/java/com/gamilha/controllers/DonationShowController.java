package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.utils.NavigationContext;

import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


public class DonationShowController implements Initializable {

    @FXML private Label lblEmoji;
    @FXML private Label lblAmount;
    @FXML private Label lblDonor;
    @FXML private Label lblDate;
    @FXML private Label lblStream;


    private Donation cur;

    private Stream parentStream;


    private static final DateTimeFormatter FMT =

            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm:ss"
            );



    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }



    public void setDonation(Donation d, Stream s) {

        cur = d;

        parentStream = s;


        lblEmoji.setText(
                d.getEmoji()
        );


        lblAmount.setText(
                d.getFormattedAmount()
        );


        lblDonor.setText(
                d.getDonorName()
        );


        lblDate.setText(

                d.getCreatedAt()!=null

                        ? d.getCreatedAt().format(FMT)

                        : "-"
        );


        lblStream.setText(

                s!=null

                        ? s.getTitle()

                        : (

                        d.getStreamTitle()!=null

                                ? d.getStreamTitle()

                                : "-"
                )
        );

    }



    @FXML
    private void onBack(ActionEvent e){

        DonationListController c =

                MainApp.loadSceneWithController(

                        "User/DonationList.fxml"
                );


        if(c!=null && parentStream!=null)

            c.setStream(parentStream);

    }



    @FXML
    private void onEdit(ActionEvent e){

        DonationFormController c =

                MainApp.loadSceneWithController(

                        "User/DonationForm.fxml"
                );


        if(c!=null)

            c.initEdit(cur,parentStream);

    }

}