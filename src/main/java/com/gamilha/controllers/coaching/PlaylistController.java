package com.gamilha.controllers.coaching;

import com.gamilha.entity.CoachingVideo;
import com.gamilha.entity.Playlist;
import com.gamilha.entity.User;
import com.gamilha.services.CoachingVideoService;
import com.gamilha.services.PlaylistService;
import com.gamilha.utils.SessionContext;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlaylistController {

    private static Playlist selectedPlaylist = null;
    private static boolean editMode = false;

    private static final String BASE =
            "/com/gamilha/interfaces";


    // LIST PAGE
    @FXML private TextField searchInput;
    @FXML private ComboBox<String> filterNiveau;
    @FXML private FlowPane cardsPane;
    @FXML private Button btnAdd;


    // FORM PAGE
    @FXML private Label lblFormTitle;
    @FXML private TextField txtTitle;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbNiveau;
    @FXML private TextField txtCategorie;
    @FXML private ImageView imgPreview;
    @FXML private Label lblImagePath;
    @FXML private Button btnChooseImage;
    @FXML private Label lblError;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;


    // SHOW PAGE
    @FXML private Label lblTitre;
    @FXML private ImageView imgPlaylist;
    @FXML private Label lblDescShow;
    @FXML private Label lblNiveauShow;
    @FXML private Label lblCategorieShow;
    @FXML private Label lblDate;
    @FXML private FlowPane videosPane;
    @FXML private Button btnRetour;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;


    private final PlaylistService playlistService =
            new PlaylistService();

    private final CoachingVideoService videoService =
            new CoachingVideoService();

    private ObservableList<Playlist> masterData =
            FXCollections.observableArrayList();

    private String selectedImagePath = null;



    @FXML
    public void initialize(){

        if(cardsPane != null)
            initListPage();

        else if(txtTitle != null)
            initFormPage();

        else if(lblTitre != null)
            initShowPage();

    }



    // LIST PAGE
    private void initListPage(){

        masterData.addAll(
                playlistService.afficherPlaylists()
        );


        filterNiveau.getItems().add("Tous");

        filterNiveau.getItems().addAll(

                masterData.stream()

                        .map(Playlist::getNiveau)

                        .filter(n -> n!=null && !n.isEmpty())

                        .distinct()

                        .toList()

        );


        filterNiveau.setValue("Tous");


        if(btnAdd!=null){

            btnAdd.setVisible(isAdmin());

            btnAdd.setManaged(isAdmin());

        }


        buildCards(masterData);


        searchInput.textProperty()

                .addListener((obs,o,n)->filterList());


        filterNiveau.valueProperty()

                .addListener((obs,o,n)->filterList());

    }



    private void buildCards(ObservableList<Playlist> data){

        cardsPane.getChildren().clear();


        for(Playlist p : data){

            cardsPane.getChildren()

                    .add(createPlaylistCard(p));

        }

    }



    private VBox createPlaylistCard(Playlist p){

        VBox card = new VBox(10);

        card.setPrefWidth(280);

        card.setStyle(

                "-fx-background-color:#1e293b;" +

                        "-fx-background-radius:10;" +

                        "-fx-padding:15;"

        );


        Label title =
                new Label(p.getTitle());


        title.setStyle(

                "-fx-text-fill:white;" +

                        "-fx-font-size:15;" +

                        "-fx-font-weight:bold;"

        );


        Label niveau =
                new Label(p.getNiveau());


        niveau.setStyle(

                "-fx-text-fill:#c084fc;"

        );


        Button voir =
                new Button("Voir");


        voir.setOnAction(e->{

            selectedPlaylist = p;

            navigateTo("/User/PlaylistShow.fxml");

        });


        card.getChildren()

                .addAll(title,niveau,voir);



        if(isAdmin()){

            Button edit =
                    new Button("Modifier");


            Button delete =
                    new Button("Supprimer");


            Button videos =
                    new Button("Videos");


            edit.setOnAction(e->{

                selectedPlaylist = p;

                editMode = true;

                navigateTo("/Admin/PlaylistForm.fxml");

            });


            delete.setOnAction(e->{

                playlistService.supprimerPlaylist(

                        p.getId()

                );

                masterData.remove(p);

                buildCards(masterData);

            });


            videos.setOnAction(e->{

                selectedPlaylist = p;

                navigateTo("/Admin/VideoList.fxml");

            });


            card.getChildren()

                    .addAll(edit,delete,videos);

        }


        return card;

    }



    @FXML
    private void handleAdd(){

        editMode = false;

        selectedPlaylist = null;

        navigateTo("/Admin/PlaylistForm.fxml");

    }



    private void filterList(){

        String search =
                searchInput.getText()

                        .toLowerCase();


        String niveau =
                filterNiveau.getValue();


        ObservableList<Playlist> filtered =

                masterData.stream()

                        .filter(p->

                                p.getTitle()

                                        .toLowerCase()

                                        .contains(search)

                        )

                        .filter(p->

                                niveau.equals("Tous")

                                        ||

                                        niveau.equals(

                                                p.getNiveau()

                                        )

                        )

                        .collect(

                                Collectors.toCollection(

                                        FXCollections::observableArrayList

                                )

                        );


        buildCards(filtered);

    }



    // FORM PAGE
    private void initFormPage(){

        cmbNiveau.getItems()

                .addAll(

                        "Débutant",

                        "Intermédiaire",

                        "Avancé"

                );


        if(editMode && selectedPlaylist!=null){

            txtTitle.setText(

                    selectedPlaylist.getTitle()

            );

        }


        btnSave.setOnAction(e->handleSave());


        btnCancel.setOnAction(e->

                navigateTo("/Admin/PlaylistList.fxml")

        );

    }



    private void handleSave(){

        String title =
                txtTitle.getText();


        Playlist p =
                new Playlist(

                        title,

                        txtDescription.getText(),

                        cmbNiveau.getValue(),

                        txtCategorie.getText(),

                        selectedImagePath,

                        LocalDateTime.now()

                );


        if(editMode)

            playlistService.modifierPlaylist(p);

        else

            playlistService.ajouterPlaylist(p);


        navigateTo("/Admin/PlaylistList.fxml");

    }



    // SHOW PAGE
    private void initShowPage(){

        if(selectedPlaylist==null)

            return;


        lblTitre.setText(

                selectedPlaylist.getTitle()

        );


        List<CoachingVideo> videos =

                videoService

                        .afficherVideosByPlaylist(

                                selectedPlaylist.getId()

                        );


        videosPane.getChildren().clear();


        for(CoachingVideo v : videos){

            Label l =
                    new Label(v.getTitre());

            videosPane.getChildren().add(l);

        }


        btnRetour.setOnAction(e->

                navigateTo("/Admin/PlaylistList.fxml")

        );

    }



    // NAVIGATION
    private void navigateTo(String fxml){

        try{

            Node n =

                    cardsPane!=null

                            ? cardsPane

                            : txtTitle;


            BorderPane root =

                    (BorderPane)

                            n.getScene()

                                    .lookup("#contentArea");


            Parent view =

                    FXMLLoader.load(

                            getClass()

                                    .getResource(

                                            BASE + fxml

                                    )

                    );


            root.setCenter(view);

        }

        catch(Exception e){

            e.printStackTrace();

        }

    }



    private boolean isAdmin(){

        User u =
                SessionContext.getCurrentUser();


        return u!=null
                &&
                u.getRoles()!=null
                &&
                u.getRoles()

                        .toUpperCase()

                        .contains("ADMIN");

    }

}