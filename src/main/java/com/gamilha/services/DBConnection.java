package com.gamilha.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Connexion MySQL unique — adapter URL / user / password à ton .env Symfony.
 */
public class DBConnection {

    // ⚠️ Modifier ces valeurs selon ton .env Symfony
    private static final String URL      = "jdbc:mysql://localhost:3306/defaultdb" +
                                           "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    private static Connection instance;

    private DBConnection() {}

    public static Connection getInstance() {
        try {
            if (instance == null || instance.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion MySQL établie.");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver MySQL introuvable.", e);
        } catch (SQLException e) {
            throw new RuntimeException("Connexion impossible : " + e.getMessage(), e);
        }
        return instance;
    }
}
