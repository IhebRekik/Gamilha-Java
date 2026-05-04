package com.gamilha.services;

import com.gamilha.utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class CoachingFeatureSchemaService {

    private static boolean initialized = false;

    private CoachingFeatureSchemaService() {
    }

    public static synchronized void ensureSchema() {
        if (initialized) {
            return;
        }

        try (Connection cnx = DatabaseConnection.getConnection()) {
            if (cnx == null) {
                return;
            }

            try (Statement st = cnx.createStatement()) {
                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS video_subtitle (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            video_id INT NOT NULL,
                            language_code VARCHAR(20) NOT NULL,
                            language_label VARCHAR(100) NOT NULL,
                            file_path VARCHAR(500) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS video_favorite (
                            user_id INT NOT NULL,
                            video_id INT NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (user_id, video_id)
                        )
                        """);

                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS coaching_notification (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NULL,
                            playlist_id INT NULL,
                            video_id INT NULL,
                            message VARCHAR(255) NOT NULL,
                            is_read TINYINT(1) NOT NULL DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);

                st.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS video_view_stat (
                            id INT PRIMARY KEY AUTO_INCREMENT,
                            user_id INT NULL,
                            playlist_id INT NULL,
                            video_id INT NOT NULL,
                            viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                        """);
            }

            initialized = true;
        } catch (SQLException e) {
            System.err.println("Erreur schema coaching: " + e.getMessage());
        }
    }
}
