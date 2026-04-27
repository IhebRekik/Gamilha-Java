package com.gamilha.utils;

import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/gamylha";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static java.sql.Connection connection = null;

    public static java.sql.Connection getConnection() {


        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(" Connected to database!");
        } catch (SQLException e) {
            System.out.println(" Connection failed!");
            e.printStackTrace();
        }

        return connection;
    }

    public static void close() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException ignored) {
        }
    }
}
