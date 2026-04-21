package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.User;
import com.gamilha.utils.SessionContext;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Navbar principale USER + ADMIN
 */

public class NavBarUserController implements Initializable {

    private static final String BASE_USER =
            "/com/gamilha/interfaces/User/";

    private static final String BASE_ADMIN =
            "/com/gamilha/interfaces/Admin/";
    private static final String CORE_CSS_PATH = "/com/gamilha/css/style.css";
    private static final String DASHBOARD_CSS_PATH = "/com/gamilha/styles/gamilha.css";


    @FXML
    private BorderPane contentArea;

    @FXML
    private Label welcomeLabel;

    @FXML
    private HBox subMenuSocial;


    @FXML private Button btnAccueil;
    @FXML private Button btnEvenements;
    @FXML private Button btnEquipes;
    @FXML private Button btnCoaching;
    @FXML private Button btnAI;
    @FXML private Button btnReseaux;
    @FXML private Button btnStreams;
    @FXML private Button btnAbonnements;


    @FXML private Button btnFilActualite;
    @FXML private Button btnMesPosts;
    @FXML private Button btnMesCommentaires;
    @FXML private Button btnAmis;


    @FXML
    private ImageView logoImage;



    @Override
    public void initialize(URL url, ResourceBundle rb){

        User user =
                SessionContext.getCurrentUser();


        if(user != null){

            welcomeLabel.setText(

                    "Bonjour " +

                            user.getName()

            );

        }


        if(user != null && user.isAdmin()){

            subMenuSocial.setVisible(false);

            subMenuSocial.setManaged(false);

        }


        setMainActive(btnReseaux);

        goReseaux();


        try{

            Image img =
                    new Image(

                            getClass()

                                    .getResource(

                                            "/com/gamilha/images/logo.png"

                                    )

                                    .toExternalForm()

                    );


            logoImage.setImage(img);

        }

        catch(Exception ignored){}

    }



    private void load(String fxml){

        try{

            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(fxml)

                    );


            Parent root = loader.load();
            applyCss(root, CORE_CSS_PATH);
            applyCss(root, DASHBOARD_CSS_PATH);


            injectUser(loader.getController());


            contentArea.setCenter(root);

        }

        catch(IOException e){

            e.printStackTrace();

        }

    }

    private void applyCss(Parent root, String cssPath) {
        URL cssUrl = getClass().getResource(cssPath);
        if (cssUrl == null) {
            return;
        }
        String external = cssUrl.toExternalForm();
        if (!root.getStylesheets().contains(external)) {
            root.getStylesheets().add(external);
        }
    }



    private void injectUser(Object ctrl){

        User u =
                SessionContext.getCurrentUser();


        if(u == null)

            return;


        if(ctrl instanceof UserPostController c)

            c.setCurrentUser(u);


        if(ctrl instanceof UserMesPostsController c)

            c.setCurrentUser(u);


        if(ctrl instanceof UserMesCommentairesController c)

            c.setCurrentUser(u);


        if(ctrl instanceof UserAmisController c)

            c.setCurrentUser(u);

    }



    private void setMainActive(Button btn){

        for(Button b : new Button[]{

                btnAccueil,

                btnEvenements,

                btnEquipes,

                btnCoaching,

                btnAI,

                btnReseaux,

                btnStreams,

                btnAbonnements

        })

            if(b != null)

                b.setStyle(

                        "-fx-text-fill:white;"

                );


        if(btn != null)

            btn.setStyle(

                    "-fx-text-fill:#c84cff;"

            );

    }



    private void setSubActive(Button btn){

        setMainActive(btnReseaux);


        for(Button b : new Button[]{

                btnFilActualite,

                btnMesPosts,

                btnMesCommentaires,

                btnAmis

        })

            if(b != null)

                b.setStyle("-fx-text-fill:#9ca3af;");


        if(btn != null)

            btn.setStyle("-fx-text-fill:#c84cff;");

    }



    // NAVIGATION


    @FXML
    void goAccueil(){

        setMainActive(btnAccueil);

        load(BASE_USER + "UserPostView.fxml");

    }



    @FXML
    void goEvenements(){

        setMainActive(btnEvenements);

        openDashboardPage("evenements_list");

    }
    private void openDashboardPage(String pageKey){

        try{

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/gamilha/dashboard-view.fxml"
                            )
                    );

            Parent root = loader.load();

            DashboardController dashboard =
                    loader.getController();

        /*
         cacher header + sidebar
        */

            dashboard.getSideNav().setVisible(false);
            dashboard.getSideNav().setManaged(false);

            dashboard.getTopNav().setVisible(false);
            dashboard.getTopNav().setManaged(false);

            if (dashboard.getHeaderBox() != null) {
                dashboard.getHeaderBox().setVisible(false);
                dashboard.getHeaderBox().setManaged(false);
            }

            dashboard.getTitleLabel().setVisible(false);
            dashboard.getSubtitleLabel().setVisible(false);

        /*
         afficher seulement contenu
        */

            contentArea.setCenter(root);

            dashboard.navigateTo(pageKey);

        }
        catch(Exception e){

            e.printStackTrace();

        }
    }




    @FXML
    void goCoaching(){

        setMainActive(btnCoaching);

        load(BASE_USER + "PlaylistList.fxml");

    }


    @FXML
    void goAI(){

        setMainActive(btnAI);

        load(BASE_USER + "UserPostView.fxml");

    }


    @FXML
    void goStreams(){

        setMainActive(btnStreams);

        load(BASE_USER + "StreamList.fxml");

    }


    @FXML
    void goAbonnement(){

        setMainActive(btnAbonnements);

        load(BASE_USER + "Abonnement.fxml");

    }



    @FXML
    void goEquipe(){

        setMainActive(btnEquipes);

        openDashboardPage("equipes_list");

    }





    @FXML
    public void goReseaux(){

        User u =
                SessionContext.getCurrentUser();


        if(u != null && u.isAdmin()){

            setMainActive(btnReseaux);

            load(BASE_ADMIN + "AdminPostView.fxml");

        }

        else{

            setSubActive(btnFilActualite);

            load(BASE_USER + "UserPostView.fxml");

        }

    }



    @FXML
    public void goFilActualite(){

        setSubActive(btnFilActualite);

        load(BASE_USER + "UserPostView.fxml");

    }



    @FXML
    public void goMesPosts(){

        setSubActive(btnMesPosts);

        load(BASE_USER + "UserMesPostsView.fxml");

    }



    @FXML
    public void goMesCommentaires(){

        setSubActive(btnMesCommentaires);

        load(BASE_USER + "UserMesCommentairesView.fxml");

    }



    @FXML
    public void goAmis(){

        setSubActive(btnAmis);

        load(BASE_USER + "UserAmisView.fxml");

    }



    @FXML
    public void goProfile(){

        try{

            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(

                                    "/com/gamilha/interfaces/User/user_profile.fxml"

                            )

                    );


            Parent root =
                    loader.load();


            UserProfileController ctrl =
                    loader.getController();


            ctrl.setUser(

                    SessionContext.getCurrentUser()

            );


            contentArea.setCenter(root);

        }

        catch(Exception e){

            e.printStackTrace();

        }

    }



    @FXML
    public void logout(ActionEvent e){

        SessionContext.clear();

        MainApp.showLogin();

    }

}