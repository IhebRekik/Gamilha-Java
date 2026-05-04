package com.gamilha.controllers;

import com.gamilha.MainApp;

import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;

import com.gamilha.utils.NavigationContext;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

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

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/DonationList.fxml")
            );

            Parent root = loader.load();

            DonationListController c = loader.getController();

            BorderPane contentArea = NavigationContext.getContentArea();

            if (contentArea == null) {
                throw new RuntimeException("contentArea null !");
            }

            contentArea.setCenter(root);

            if (c != null && parentStream != null) {
                c.setStream(parentStream);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onEdit(ActionEvent e){

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/DonationForm.fxml")
            );

            Parent root = loader.load();

            Object controller = loader.getController();

            BorderPane contentArea = NavigationContext.getContentArea();

            if (contentArea == null) {
                throw new RuntimeException("contentArea null !");
            }

            contentArea.setCenter(root);

            // 🔥 éviter ton crash ClassCastException
            if (controller instanceof DonationFormController c) {
                c.initEdit(cur, parentStream);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}