package com.gamilha.utils;

import com.gamilha.services.InscriptionServices;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class StripeAutoHandler {

    private static final String STRIPE_KEY =
            "sk_test_51T3ru9I6m3BSEEF9jEJMzZxCAP5P8FHdFKmxx6TQzM5mQRvKpy5nFTY4DdAjNP3pkb5PzJj1a7aAbVDXLKXRn23200bcX15iIw";

    public static void start(String sessionId){

        new Thread(() -> {

            try {

                Stripe.apiKey = STRIPE_KEY;

                boolean paid = false;

                while(!paid){

                    Thread.sleep(3000);

                    Session session =
                            Session.retrieve(sessionId);

                    // vérifier paiement
                    if("paid".equals(
                            session.getPaymentStatus()
                    )){

                        InscriptionServices service =
                                new InscriptionServices();

                        service.paymentSuccess(sessionId);

                        paid = true;

                        // afficher alert
                        Platform.runLater(() -> {

                            Alert alert =
                                    new Alert(Alert.AlertType.INFORMATION);

                            alert.setTitle("Paiement");

                            alert.setHeaderText(null);

                            alert.setContentText(
                                    "Paiement réussi ✅\nAbonnement activé"
                            );

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