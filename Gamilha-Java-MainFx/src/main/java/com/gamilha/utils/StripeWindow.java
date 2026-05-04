package com.gamilha.utils;

import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class StripeWindow {

    public static void open(String url, String sessionId){

        WebView webView = new WebView();
        webView.getEngine().load(url);

        Stage stage = new Stage();
        stage.setTitle("Paiement sécurisé");
        stage.setScene(new Scene(webView, 900, 600));
        stage.show();

        // 🔥 lancer vérification paiement
        StripeAutoHandler.start(sessionId, stage);
    }
}