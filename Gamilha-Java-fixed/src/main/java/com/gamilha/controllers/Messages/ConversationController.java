package com.gamilha.controllers.Messages;

import com.gamilha.entity.ChatMessage;
import com.gamilha.entity.User;
import com.gamilha.services.ChatService;
import com.gamilha.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConversationController {

    @FXML
    private VBox messagesBox;

    @FXML
    private TextField messageField;

    @FXML
    private Label titleLabel;

    private User currentUser;

    private User selectedUser;

    private ChatService chatService = new ChatService();



    @FXML
    public void initialize(){

        currentUser = SessionContext.getCurrentUser();

    }



    public void setUser(User user){

        this.selectedUser = user;

        titleLabel.setText("Conversation avec " + user.getName());

        afficherMessages();

    }


    @FXML
    private void envoyerMessage() {

        if(messageField.getText().isEmpty())
            return;

        ChatMessage msg = new ChatMessage();

        msg.setContent(messageField.getText());

        msg.setSender(currentUser);

        msg.setRecipient(selectedUser);

        chatService.sendMessage(msg);

        messageField.clear();

        afficherMessages();
    }



    private void afficherMessages(){

        messagesBox.getChildren().clear();

        List<ChatMessage> messages =
                chatService.getConversation(currentUser,selectedUser);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


        for(ChatMessage m : messages){

            HBox line = new HBox();

            VBox bubble = new VBox();

            Label text = new Label(m.getContent());

            Label info = new Label(
                    m.getCreatedAt().format(formatter)
            );
            info.setStyle("-fx-text-fill:#cbd5f5; -fx-font-size:10;");

            bubble.getChildren().addAll(info,text);

            bubble.setSpacing(5);



            // boutons modifier supprimer
            if(m.getSender().getId()==currentUser.getId()){

                HBox actions = new HBox(5);

                Button editBtn = new Button("✏");

                Button deleteBtn = new Button("🗑");


                editBtn.setStyle("-fx-background-color:#facc15;");

                deleteBtn.setStyle("-fx-background-color:#ef4444; -fx-text-fill:white;");


                // modifier
                editBtn.setOnAction(e -> {

                    TextInputDialog dialog =
                            new TextInputDialog(m.getContent());

                    dialog.setTitle("Modifier message");

                    dialog.setHeaderText(null);

                    dialog.setContentText("Nouveau message:");

                    dialog.showAndWait().ifPresent(newText -> {

                        m.setContent(newText);

                        chatService.updateMessage(m);

                        afficherMessages();

                    });

                });



                // supprimer
                deleteBtn.setOnAction(e -> {

                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

                    alert.setTitle("Confirmation");

                    alert.setHeaderText("Supprimer message ?");

                    alert.setContentText("Cette action est irreversible");

                    alert.showAndWait().ifPresent(response -> {

                        if(response==ButtonType.OK){

                            chatService.deleteMessage(m.getId());

                            afficherMessages();

                        }

                    });

                });


                actions.getChildren().addAll(editBtn,deleteBtn);

                bubble.getChildren().add(actions);


                line.setAlignment(Pos.CENTER_RIGHT);

                bubble.setStyle(
                        "-fx-background-color:#2563eb;" +
                                "-fx-text-fill:white;" +
                                "-fx-padding:10;" +
                                "-fx-background-radius:10;"
                );

            }
            else{

                line.setAlignment(Pos.CENTER_LEFT);

                bubble.setStyle(
                        "-fx-background-color:#374151;" +
                                "-fx-text-fill:white;" +
                                "-fx-padding:10;" +
                                "-fx-background-radius:10;"
                );

            }


            line.getChildren().add(bubble);

            messagesBox.getChildren().add(line);

        }

    }


    @FXML
    private void initializeSendButton(){

        // envoyer avec bouton

    }

}