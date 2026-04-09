package com.gamilha.util;

import java.sql.*;

public class DatabaseConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/defaultdb";
    private static final String USER = "root";
    private static final String PASS = "";

    private static Connection instance;

    public static Connection getConnection() throws SQLException {
        if (instance == null || instance.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                instance = DriverManager.getConnection(URL, USER, PASS);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Driver MySQL introuvable: " + e.getMessage());
            }
        }
        return instance;
    }

    public static void close() {
        try { if (instance != null) { instance.close(); instance = null; } }
        catch (SQLException ignored) {}
    }
}
