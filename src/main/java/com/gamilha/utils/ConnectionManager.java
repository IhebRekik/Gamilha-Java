package com.gamilha.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    // ───────── CONFIG ─────────
    private static final String URL =
            "jdbc:mysql://localhost:3306/gamylha" +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    // ───────── SINGLETON ─────────
    private static ConnectionManager instance;

    private ConnectionManager() {
        // constructeur privé
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    // 🔥 méthode interne (NON static)
    private Connection createConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 🔥 méthode static que tu veux utiliser
    public static Connection getConnection() {
        try {
            return getInstance().createConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}