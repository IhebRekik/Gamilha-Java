package com.gamilha.services;

import com.gamilha.entity.ChatAi;
import com.gamilha.entity.User;
import com.gamilha.utils.ConnectionManager;
import com.gamilha.utils.SessionContext;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ChatAiService {

    public List<String[]> getMessagesByUser() {

        List<String[]> list = new ArrayList<>();

        String query = "SELECT role, content, created_at FROM chat_ai WHERE user_id=? ORDER BY created_at ASC";

        try (Connection cnx = ConnectionManager.getConnection();
             PreparedStatement ps = cnx.prepareStatement(query)) {

            ps.setInt(1, SessionContext.getCurrentUser().getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new String[]{
                        rs.getString("role"),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toString()
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
    public void addMessage(ChatAi m) {
        String sql = "INSERT INTO chat_ai (content, role, created_at, user_id) VALUES (?, ?, NOW(), ?)";

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, m.getContent());
            ps.setString(2, m.getRole());
            ps.setInt(3, m.getUser().getId()); // 🔥 FK

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ChatAi> getAllMessages() {
        List<ChatAi> list = new ArrayList<>();

        String sql = "SELECT * FROM message";

        try (Connection conn = ConnectionManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                ChatAi m = new ChatAi();

                m.setContent(rs.getString("content"));
                m.setRole(rs.getString("role"));

                // 🔥 récupérer user

                m.setUser(SessionContext.getCurrentUser());

                list.add(m);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

}