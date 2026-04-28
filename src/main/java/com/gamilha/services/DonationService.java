package com.gamilha.services;

import com.gamilha.entity.Donation;
import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;


public class DonationService {

    public boolean create(Donation d) throws SQLException {
        String sql = "INSERT INTO donation(amount,donor_name,created_at,user_id,stream_id) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDouble(1, d.getAmount());
            ps.setString(2, d.getDonorName());
            ps.setTimestamp(3, Timestamp.valueOf(d.getCreatedAt() != null ? d.getCreatedAt() : LocalDateTime.now()));
            ps.setInt(4, d.getUserId());
            ps.setInt(5, d.getStreamId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet k = ps.getGeneratedKeys();
                if (k.next()) d.setId(k.getInt(1));
                return true;
            }
        }
        return false;
    }

    public List<Donation> findAll() throws SQLException {
        String sql = "SELECT d.*,s.title AS stream_title,u.email AS user_email FROM donation d LEFT JOIN stream s ON d.stream_id=s.id LEFT JOIN user u ON d.user_id=u.id ORDER BY d.created_at DESC";
        List<Donation> list = new ArrayList<>();
        try (Statement st = ConnectionManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Donation> findByStream(int streamId) throws SQLException {
        String sql = "SELECT d.*,s.title AS stream_title,u.email AS user_email FROM donation d LEFT JOIN stream s ON d.stream_id=s.id LEFT JOIN user u ON d.user_id=u.id WHERE d.stream_id=? ORDER BY d.created_at DESC";
        List<Donation> list = new ArrayList<>();
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, streamId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Donation findById(int id) throws SQLException {
        String sql = "SELECT d.*,s.title AS stream_title,u.email AS user_email FROM donation d LEFT JOIN stream s ON d.stream_id=s.id LEFT JOIN user u ON d.user_id=u.id WHERE d.id=?";
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    /** Donation rapide par emoji — équivalent donation_emoji route Symfony */
    public Donation donateByEmoji(int streamId, int userId, String donorName, String emoji) throws SQLException {
        Map<String, Double> amounts = Map.of("🍩", 1.0, "🍕", 5.0, "💎", 10.0, "🚀", 50.0);
        Double amount = amounts.get(emoji);
        if (amount == null) throw new IllegalArgumentException("Emoji invalide: " + emoji);
        Donation d = new Donation();
        d.setStreamId(streamId);
        d.setUserId(userId);
        d.setDonorName(donorName);
        d.setAmount(amount);
        d.setCreatedAt(LocalDateTime.now());
        create(d);
        return d;
    }

    public boolean update(Donation d) throws SQLException {
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(
                "UPDATE donation SET amount=?,donor_name=?,stream_id=? WHERE id=?")) {
            ps.setDouble(1, d.getAmount());
            ps.setString(2, d.getDonorName());
            ps.setInt(3, d.getStreamId());
            ps.setInt(4, d.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement("DELETE FROM donation WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Donation map(ResultSet rs) throws SQLException {
        Donation d = new Donation();
        d.setId(rs.getInt("id"));
        d.setAmount(rs.getDouble("amount"));
        d.setDonorName(rs.getString("donor_name"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) d.setCreatedAt(ts.toLocalDateTime());
        d.setUserId(rs.getInt("user_id"));
        d.setStreamId(rs.getInt("stream_id"));
        try { d.setStreamTitle(rs.getString("stream_title")); } catch (Exception ignored) {}
        try { d.setUserEmail(rs.getString("user_email")); }     catch (Exception ignored) {}
        return d;
    }
}
