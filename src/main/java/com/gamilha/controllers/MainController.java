package com.gamilha.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private StackPane contentArea;
    @FXML private Button    btnPosts;
    @FXML private Button    btnCommentaires;

    private static final String ACTIVE =
        "-fx-background-color:#3b82f6;-fx-text-fill:white;-fx-background-radius:8;" +
        "-fx-alignment:CENTER_LEFT;-fx-padding:10 14;-fx-font-size:13;-fx-cursor:hand;";
    private static final String INACTIVE =
        "-fx-background-color:transparent;-fx-text-fill:#c9d1d9;-fx-background-radius:8;" +
        "-fx-alignment:CENTER_LEFT;-fx-padding:10 14;-fx-font-size:13;-fx-cursor:hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        showPosts();
    }

    @FXML
    public void showPosts() {
        load("/com/gamilha/AdminPostView.fxml");
        btnPosts.setStyle(ACTIVE);
        btnCommentaires.setStyle(INACTIVE);
    }

    @FXML
    public void showCommentaires() {
        load("/com/gamilha/AdminCommentaireView.fxml");
        btnCommentaires.setStyle(ACTIVE);
        btnPosts.setStyle(INACTIVE);
    }

    private void load(String path) {
        try {
            Node view = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
