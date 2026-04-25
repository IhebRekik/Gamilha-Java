package com.gamilha.services;

import com.gamilha.entity.PasswordResetToken;
import com.gamilha.entity.User;
import com.gamilha.utils.EmailSender;
import com.gamilha.utils.ConnectionManager;
import com.gamilha.utils.PasswordHasher;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

public class PasswordResetService {

    private String generateCode() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }

    public void requestPasswordReset(String email, String userName) throws Exception {
        // Vérifier si user existe + créer token + envoyer email
        // (logique à compléter)
        String code = generateCode();
        // Sauvegarder token...
        EmailSender.sendResetCode(email, code, userName);
    }

    public void resetPassword(String email, String code, String newPassword) throws Exception {
        // Vérifier token + mettre à jour mot de passe
    }
}