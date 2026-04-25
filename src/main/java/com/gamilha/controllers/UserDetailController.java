package com.gamilha.controllers;

import com.gamilha.entity.User;
import com.gamilha.utils.ImageStorage;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.nio.file.Path;

public class UserDetailController {

    @FXML private ImageView avatarView;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Label createdAtLabel;

    private User currentUser;


    public void setUser(User user){

        this.currentUser = user;


        nameLabel.setText(user.getName());

        emailLabel.setText(user.getEmail());


        roleLabel.setText(

                user.getRoles() != null &&

                        user.getRoles().contains("ROLE_ADMIN")

                        ? "👑 Administrateur"

                        : "👤 Utilisateur"

        );


        statusLabel.setText(

                user.isActive()

                        ? "✅ Compte actif"

                        : "⛔ Compte inactif"

        );


        if(user.getCreatedAt() != null)

            createdAtLabel.setText(

                    "Membre depuis : " +

                            user.getCreatedAt()

                                    .toString()

                                    .substring(0,10)

            );


        try{

            Image image = null;


            if(user.getProfileImage() != null){

                Path p =
                        ImageStorage.resolveToPath(

                                user.getProfileImage()

                        );


                if(p != null)

                    image =
                            new Image(

                                    p.toUri().toString(),

                                    120,

                                    120,

                                    true,

                                    true

                            );

            }


            if(image == null){

                var is =
                        getClass()

                                .getResourceAsStream(

                                        "/com/gamilha/images/logo.jpg"

                                );


                if(is != null)

                    image =
                            new Image(is,120,120,true,true);

            }


            if(image != null){

                avatarView.setImage(image);

                avatarView.setClip(

                        new Circle(60,60,60)

                );

            }

        }

        catch(Exception ignored){}

    }



    @FXML
    private void backToList(){

        try{

            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(

                                    "/com/gamilha/interfaces/Admin/admin_users.fxml"

                            )

                    );


            Parent content =
                    loader.load();


            BorderPane contentArea =
                    (BorderPane)

                            nameLabel

                                    .getScene()

                                    .lookup("#contentArea");


            contentArea.setCenter(content);

        }

        catch(IOException e){

            e.printStackTrace();

        }

    }



    @FXML
    private void editUser(){

        if(currentUser == null) return;


        try{

            FXMLLoader loader =
                    new FXMLLoader(

                            getClass().getResource(

                                    "/com/gamilha/interfaces/Admin/edit_user.fxml"

                            )

                    );


            Parent content =
                    loader.load();


            EditUserController ctrl =
                    loader.getController();


            ctrl.setUser(currentUser);


            BorderPane contentArea =
                    (BorderPane)

                            nameLabel

                                    .getScene()

                                    .lookup("#contentArea");


            contentArea.setCenter(content);

        }

        catch(IOException e){

            e.printStackTrace();

        }

    }

}