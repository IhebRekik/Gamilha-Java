package com.gamilha.utils;

import com.gamilha.services.InscriptionServices;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class StripeAutoHandler {

    private static final String STRIPE_KEY =
            "sk_test_51T3ru9I6m3BSEEF9jEJMzZxCAP5P8FHdFKmxx6TQzM5mQRvKpy5nFTY4DdAjNP3pkb5PzJj1a7aAbVDXLKXRn23200bcX15iIw";

    public static void start(String sessionId, Stage stage){

        new Thread(() -> {

            try {

                Stripe.apiKey = STRIPE_KEY;

                boolean paid = false;

                while(!paid){

                    Thread.sleep(3000);

                    Session session = Session.retrieve(sessionId);

                    if("paid".equals(session.getPaymentStatus())){

                        new InscriptionServices().paymentSuccess(sessionId);

                        paid = true;

                        Platform.runLater(() -> {

                            // 🔥 fermer la fenêtre WebView
                            if(stage != null){
                                stage.close();
                            }

                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Paiement");
                            alert.setHeaderText(null);
                            alert.setContentText("Paiement réussi ✅");

                            alert.showAndWait();
                        });
                    }
                }

            } catch (Exception e){
                e.printStackTrace();
            }

        }).start();
    }

}