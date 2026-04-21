package com.gamilha.controllers;

import com.gamilha.MainApp;

import com.gamilha.services.DonationService;

import com.gamilha.entity.Donation;
import com.gamilha.entity.Stream;

import com.gamilha.utils.AlertUtil;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import javafx.geometry.Pos;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;

import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.ResourceBundle;



public class StreamShowController implements Initializable {


    @FXML private Label lblTitle;
    @FXML private Label lblStatus;
    @FXML private Label lblGame;
    @FXML private Label lblViewers;
    @FXML private Label lblDesc;
    @FXML private Label lblDate;

    @FXML private Hyperlink lnkUrl;

    @FXML private ImageView imgThumb;

    @FXML private VBox obsBox;

    @FXML private Label lblKey;
    @FXML private Label lblRtmp;

    @FXML private Label lblTotal;
    @FXML private Label lblCount;

    @FXML private FlowPane donationGrid;



    private Stream cur;


    private final DonationService service =
            new DonationService();



    private static final DateTimeFormatter FMT =

            DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy HH:mm"
            );



    @Override
    public void initialize(URL url, ResourceBundle rb){

    }



    public void setStream(Stream s){

        this.cur = s;

        fillInfo();

        loadDonations();

    }



    private void fillInfo(){

        lblTitle.setText(

                cur.getTitle()

        );


        lblGame.setText(

                cur.getGame()

        );


        lblViewers.setText(

                cur.getViewers()+" viewers"

        );


        lblDesc.setText(

                cur.getDescription()

        );


        if(cur.getCreatedAt()!=null)

            lblDate.setText(

                    cur.getCreatedAt().format(FMT)

            );


        lblStatus.setText(

                cur.getStatus()

        );


        if(cur.getThumbnail()!=null){

            imgThumb.setImage(

                    new Image(

                            cur.getThumbnail()

                    )
            );

        }

    }



    private void loadDonations(){

        try{

            List<Donation> list =

                    service.findByStream(

                            cur.getId()

                    );


            lblCount.setText(

                    list.size()+" donations"

            );


            donationGrid.getChildren().clear();


            for(Donation d : list){

                donationGrid.getChildren().add(

                        buildDonCard(d)

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



    private VBox buildDonCard(Donation d){

        VBox card =
                new VBox(6);


        card.setAlignment(Pos.CENTER);


        Label emoji =
                new Label(

                        d.getEmoji()

                );


        Label amount =
                new Label(

                        d.getFormattedAmount()

                );


        Label donor =
                new Label(

                        d.getDonorName()

                );


        card.getChildren().addAll(

                emoji,
                amount,
                donor

        );


        return card;

    }



    @FXML
    private void onDonate1(ActionEvent e){

        quickDon("🍩");

    }



    @FXML
    private void onDonate5(ActionEvent e){

        quickDon("🍕");

    }



    @FXML
    private void onDonate10(ActionEvent e){

        quickDon("💎");

    }



    @FXML
    private void onDonate50(ActionEvent e){

        quickDon("🚀");

    }



    private void quickDon(String emoji){

        try{

            Donation d =

                    service.donateByEmoji(

                            cur.getId(),
                            1,
                            "Anonymous",
                            emoji
                    );


            AlertUtil.showSuccess(

                    "Merci",

                    d.getFormattedAmount()

            );


            loadDonations();

        }

        catch(Exception ex){

            AlertUtil.showError(

                    "Erreur",

                    ex.getMessage()

            );

        }

    }



    @FXML
    private void onCustomDonate(ActionEvent e){

        DonationFormController c =

                MainApp.loadSceneWithController(

                        "User/DonationForm.fxml"

                );


        if(c!=null)

            c.initCreate(cur);

    }



    @FXML
    private void onAllDonations(ActionEvent e){

        DonationListController c =

                MainApp.loadSceneWithController(

                        "User/DonationList.fxml"

                );


        if(c!=null)

            c.setStream(cur);

    }



    @FXML
    private void onBack(ActionEvent e){

        MainApp.loadScene(

                "User/StreamList.fxml"

        );

    }



    @FXML
    private void onEdit(ActionEvent e){

        StreamFormController c =

                MainApp.loadSceneWithController(

                        "User/StreamForm.fxml"

                );


        if(c!=null)

            c.initEdit(cur);

    }

}