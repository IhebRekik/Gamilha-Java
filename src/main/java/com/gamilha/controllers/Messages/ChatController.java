package com.gamilha.controllers.Messages;

import com.gamilha.entity.User;
import com.gamilha.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.util.List;

public class ChatController {

    @FXML
    private GridPane gridUsers;

    @FXML
    private TextField searchField;

    private List<User> allUsers;



    @FXML
    public void initialize() {

        UserService userService = new UserService();

        allUsers = userService.getAmis();

        afficherUsers(allUsers);


        searchField.textProperty().addListener((obs, oldVal, newVal) -> {

            List<User> filtered = allUsers.stream()

                    .filter(u ->
                            u.getName().toLowerCase().contains(newVal.toLowerCase())
                                    ||
                                    u.getEmail().toLowerCase().contains(newVal.toLowerCase())
                    )

                    .toList();

            afficherUsers(filtered);

        });

    }


    private void afficherUsers(List<User> users){

        gridUsers.getChildren().clear();

        int row = 0;


        gridUsers.add(createHeader("NOM"),0,row);
        gridUsers.add(createHeader("EMAIL"),1,row);
        gridUsers.add(createHeader("ACTIONS"),2,row);

        row++;

        for(User u : users){

            Label name = createCell(u.getName());

            Label email = createCell(u.getEmail());

            Button btn = new Button("💬 Message");

            btn.setMaxWidth(Double.MAX_VALUE);

            btn.setStyle(
                    "-fx-background-color:transparent;" +
                            "-fx-border-color:#22c55e;" +
                            "-fx-text-fill:#22c55e;" +
                            "-fx-border-radius:6;" +
                            "-fx-padding:6 14;"
            );

            btn.setOnAction(e -> ouvrirConversation(u));


            gridUsers.add(name,0,row);
            gridUsers.add(email,1,row);
            gridUsers.add(btn,2,row);

            row++;
        }

    }


    private Label createHeader(String text){

        Label l = new Label(text);

        l.setMaxWidth(Double.MAX_VALUE);

        l.setStyle(
                "-fx-text-fill:#cbd5f5;" +
                        "-fx-font-weight:bold;" +
                        "-fx-padding:16;" +
                        "-fx-border-color:#1e293b;" +
                        "-fx-border-width:0 0 1 0;" +
                        "-fx-background-color:#0f172a;"
        );

        return l;
    }



    private Label createCell(String text){

        Label l = new Label(text);

        l.setMaxWidth(Double.MAX_VALUE);

        l.setStyle(
                "-fx-text-fill:white;" +
                        "-fx-padding:16;" +
                        "-fx-border-color:#1e293b;" +
                        "-fx-border-width:0 0 1 0;"
        );

        return l;
    }






    private void ouvrirConversation(User user){

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/gamilha/interfaces/User/Conversation.fxml")
            );

            Parent root = loader.load();

            ConversationController controller = loader.getController();

            controller.setUser(user);

            Stage stage = new Stage();

            stage.setScene(new Scene(root));

            stage.setTitle("Conversation");

            stage.show();

        }
        catch (Exception e){

            e.printStackTrace();
        }

    }

}