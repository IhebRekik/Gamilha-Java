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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;

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


        // Sous-menu toujours caché au démarrage
        if (subMenuSocial != null) {
            subMenuSocial.setVisible(false);
            subMenuSocial.setManaged(false);
        }

        if (user != null && !user.isAdmin()) {
            // User normal → Réseaux actif + sous-menu visible
            setMainActive(btnReseaux);
        } else {
            setMainActive(btnAccueil);
        }

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


            Parent root =
                    loader.load();


            injectUser(loader.getController());


            contentArea.setCenter(root);

        }

        catch(IOException e){

            e.printStackTrace();

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



    // Style bouton normal (thème gamilha.css)
    // Style bouton normal — comme sur l'image de référence
    private static final String BTN_NORMAL =
        "-fx-background-color:transparent;" +
        "-fx-text-fill:#d1d5db;" +
        "-fx-background-radius:7;" +
        "-fx-border-color:transparent;" +
        "-fx-border-radius:7;" +
        "-fx-padding:6 12;" +
        "-fx-font-size:13;" +
        "-fx-cursor:hand;";
    // Style bouton actif — dégradé violet avec texte blanc
    private static final String BTN_ACTIVE =
        "-fx-background-color:linear-gradient(to right,#8b5cf6,#d946ef);" +
        "-fx-text-fill:white;" +
        "-fx-background-radius:20;" +
        "-fx-border-color:transparent;" +
        "-fx-border-radius:20;" +
        "-fx-padding:6 16;" +
        "-fx-font-size:13;" +
        "-fx-font-weight:bold;" +
        "-fx-cursor:hand;";
    // Style sous-bouton normal
    private static final String SUB_NORMAL =
        "-fx-background-color:#1a1e2e;" +
        "-fx-text-fill:#d1d5db;" +
        "-fx-background-radius:20;" +
        "-fx-border-color:rgba(139,92,246,0.25);" +
        "-fx-border-radius:20;" +
        "-fx-border-width:1;" +
        "-fx-padding:4 14;" +
        "-fx-font-size:12;" +
        "-fx-cursor:hand;";
    // Style sous-bouton actif
    private static final String SUB_ACTIVE =
        "-fx-background-color:linear-gradient(to right,#8b5cf6,#d946ef);" +
        "-fx-text-fill:white;" +
        "-fx-background-radius:20;" +
        "-fx-border-color:transparent;" +
        "-fx-border-radius:20;" +
        "-fx-padding:4 14;" +
        "-fx-font-size:12;" +
        "-fx-font-weight:bold;" +
        "-fx-cursor:hand;";

    private void setMainActive(Button btn){
        for(Button b : new Button[]{
                btnAccueil, btnEvenements, btnEquipes, btnCoaching,
                btnAI, btnReseaux, btnStreams, btnAbonnements})
            if(b != null) b.setStyle(BTN_NORMAL);
        if(btn != null) btn.setStyle(BTN_ACTIVE);
        resetSub();
        // Cacher le sous-menu sauf si on clique sur Réseaux
        boolean showSub = (btn == btnReseaux);
        if (subMenuSocial != null) {
            subMenuSocial.setVisible(showSub);
            subMenuSocial.setManaged(showSub);
        }
    }

    private void resetSub(){
        for(Button b : new Button[]{btnFilActualite, btnMesPosts, btnMesCommentaires, btnAmis})
            if(b != null) b.setStyle(SUB_NORMAL);
    }



    private void setSubActive(Button btn){
        // Réseaux reste actif dans la topbar
        for(Button b : new Button[]{
                btnAccueil, btnEvenements, btnEquipes, btnCoaching,
                btnAI, btnReseaux, btnStreams, btnAbonnements})
            if(b != null) b.setStyle(BTN_NORMAL);
        if(btnReseaux != null) btnReseaux.setStyle(BTN_ACTIVE);
        // Sous-menu toujours visible dans les pages sociales
        if (subMenuSocial != null) {
            subMenuSocial.setVisible(true);
            subMenuSocial.setManaged(true);
        }
        resetSub();
        if(btn != null) btn.setStyle(SUB_ACTIVE);
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