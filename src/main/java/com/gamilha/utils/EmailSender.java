package com.gamilha.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    private static final String FROM_EMAIL = "tonemail@gmail.com";  // ← Change avec ton email
    private static final String PASSWORD = "ton_app_password";      // ← Mot de passe d'application Gmail

    public static void sendResetCode(String toEmail, String code, String userName) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Réinitialisation de votre mot de passe - Gamilha");

            String html = "<h2>Bonjour " + userName + ",</h2>" +
                    "<p>Votre code de vérification est : <b>" + code + "</b></p>" +
                    "<p>Ce code expire dans 15 minutes.</p>";

            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("✅ Email envoyé à " + toEmail);
        } catch (Exception e) {
            System.out.println("❌ Erreur envoi email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}