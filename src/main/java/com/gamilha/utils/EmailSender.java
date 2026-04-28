package com.gamilha.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    private static final String FROM_EMAIL = "bjaouijihen03@gmail.com";  // ← Change avec ton email
    private static final String PASSWORD = "gkbe actx vbzg mflv";      // ← Mot de passe d'application Gmail

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }
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
    // ── NOUVEAU : Alerte connexion suspecte ───────────────────────────────

    /**
     * Envoie un email d'alerte à l'utilisateur lorsqu'une connexion suspecte
     * est détectée (nouvel appareil ou nouvelle localisation).
     */
    public static void sendSuspiciousLoginAlert(String toEmail, String userName,
                                                String ipAddress, String deviceInfo,
                                                String location) {
        try {
            Message message = new MimeMessage(buildSession());
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 Nouvelle connexion détectée - Gamilha");

            String safeIp     = ipAddress  != null ? ipAddress  : "Inconnue";
            String safeDevice = deviceInfo != null ? deviceInfo : "Inconnu";
            String safeLoc    = location   != null ? location   : "Inconnue";
            String safeTime   = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                    .format(new java.util.Date());

            String html =
                    "<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;" +
                            "background:#0d1117;color:#e2e8f0;border-radius:16px;overflow:hidden;'>" +
                            "<div style='background:linear-gradient(135deg,#7c3aed,#4f46e5);padding:32px 36px;text-align:center;'>" +
                            "<h1 style='color:#fff;font-size:24px;margin:0;'>🎮 Gamilha</h1>" +
                            "<p style='color:rgba(255,255,255,0.8);margin:8px 0 0;font-size:14px;'>Alerte de sécurité</p></div>" +
                            "<div style='padding:32px 36px;'>" +
                            "<h2 style='color:#fbbf24;font-size:18px;margin:0 0 12px;'>⚠ Nouvelle connexion détectée</h2>" +
                            "<p style='color:#94a3b8;line-height:1.6;'>Bonjour <strong style='color:#fff;'>" + userName + "</strong>,<br>" +
                            "Une connexion depuis un <strong>nouvel appareil ou une nouvelle localisation</strong> a été détectée.</p>" +
                            "<div style='background:#12172b;border:1px solid #1e2a45;border-radius:12px;padding:20px;margin:24px 0;'>" +
                            "<p style='margin:6px 0;color:#e2e8f0;'>📅 <b>Date :</b> " + safeTime + "</p>" +
                            "<p style='margin:6px 0;color:#e2e8f0;'>🌐 <b>IP :</b> " + safeIp + "</p>" +
                            "<p style='margin:6px 0;color:#e2e8f0;'>💻 <b>Appareil :</b> " + safeDevice + "</p>" +
                            "<p style='margin:6px 0;color:#e2e8f0;'>📍 <b>Localisation :</b> " + safeLoc + "</p></div>" +
                            "<div style='background:#7f1d1d;border:1px solid #991b1b;border-radius:10px;padding:16px 20px;'>" +
                            "<p style='color:#fca5a5;margin:0;font-size:14px;'>" +
                            "<strong>Si ce n'est pas vous</strong>, changez votre mot de passe immédiatement.</p></div></div>" +
                            "<div style='padding:20px 36px;border-top:1px solid #1e2a45;text-align:center;'>" +
                            "<p style='color:#475569;font-size:12px;margin:0;'>© 2024 Gamilha Platform</p></div></div>";

            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("✅ Email alerte connexion suspecte envoyé à " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Erreur email alerte suspecte : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendIntruderPhoto(String adminEmail, String suspectEmail, byte[] photoBytes) {
        try {
            Message message = new MimeMessage(buildSession());
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(adminEmail));
            message.setSubject("🚨 Tentatives suspectes - " + suspectEmail);

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("Plusieurs tentatives de connexion échouées ont été détectées pour votre compte : " + suspectEmail +
                    "\n\nVoici une photo prise lors de la dernière tentative.");

            MimeBodyPart photoPart = new MimeBodyPart();
            photoPart.setDataHandler(new jakarta.activation.DataHandler(new jakarta.mail.util.ByteArrayDataSource(photoBytes, "image/jpeg")));
            photoPart.setFileName("intruder.jpg");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(photoPart);

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("✅ Email d'intrusion envoyé à " + adminEmail);
        } catch (Exception e) {
            System.err.println("❌ Email photo erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }
}