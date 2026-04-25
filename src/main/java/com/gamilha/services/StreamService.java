package com.gamilha.services;

import com.gamilha.entity.Stream;

import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * StreamService — couche service pour les opérations sur Stream.
 * Renommé depuis StreamDAO : même logique, nouveau nom de package.
 */
public class StreamService {

    public boolean create(Stream s) throws SQLException {
        String sql = "INSERT INTO stream(title,description,game,thumbnail,viewers,status,url,stream_key,rtmp_server,api_video_id,is_live,created_at,user_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getDescription());
            ps.setString(3, s.getGame());
            ps.setString(4, s.getThumbnail());
            ps.setInt(5, s.getViewers());
            ps.setString(6, s.getStatus());
            ps.setString(7, s.getUrl());
            ps.setString(8, s.getStreamKey());
            ps.setString(9, s.getRtmpServer());
            ps.setString(10, s.getApiVideoId());
            ps.setBoolean(11, s.isLive());
            ps.setTimestamp(12, Timestamp.valueOf(s.getCreatedAt() != null ? s.getCreatedAt() : LocalDateTime.now()));
            ps.setInt(13, s.getUserId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet k = ps.getGeneratedKeys();
                if (k.next()) s.setId(k.getInt(1));
                return true;
            }
        }
        return false;
    }

    public List<Stream> findAll() throws SQLException {
        List<Stream> list = new ArrayList<>();
        try (Statement st = ConnectionManager.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM stream ORDER BY created_at DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Stream findById(int id) throws SQLException {
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement("SELECT * FROM stream WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public List<Stream> search(String query, String game, String sort) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM stream WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (title LIKE ? OR description LIKE ?)");
            params.add("%" + query + "%");
            params.add("%" + query + "%");
        }
        if (game != null && !game.isBlank() && !"Tous".equals(game) && !"Tous les jeux".equals(game)) {
            sql.append(" AND game=?");
            params.add(game);
        }
        sql.append("viewers".equals(sort) ? " ORDER BY viewers DESC" : " ORDER BY created_at DESC");
        List<Stream> list = new ArrayList<>();
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public boolean update(Stream s) throws SQLException {
        String sql = "UPDATE stream SET title=?,description=?,game=?,thumbnail=?,viewers=?,status=?,url=?,stream_key=?,rtmp_server=?,api_video_id=?,is_live=? WHERE id=?";
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, s.getTitle());
            ps.setString(2, s.getDescription());
            ps.setString(3, s.getGame());
            ps.setString(4, s.getThumbnail());
            ps.setInt(5, s.getViewers());
            ps.setString(6, s.getStatus());
            ps.setString(7, s.getUrl());
            ps.setString(8, s.getStreamKey());
            ps.setString(9, s.getRtmpServer());
            ps.setString(10, s.getApiVideoId());
            ps.setBoolean(11, s.isLive());
            ps.setInt(12, s.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (PreparedStatement ps = ConnectionManager.getConnection().prepareStatement("DELETE FROM stream WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }
    // Dans StreamService.java
    public boolean existsByTitle(String title) throws SQLException {
        try (PreparedStatement ps = ConnectionManager.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM stream WHERE title = ?")) {
            ps.setString(1, title);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }
    private Stream map(ResultSet rs) throws SQLException {
        Stream s = new Stream();
        s.setId(rs.getInt("id"));
        s.setTitle(rs.getString("title"));
        s.setDescription(rs.getString("description"));
        s.setGame(rs.getString("game"));
        s.setThumbnail(rs.getString("thumbnail"));
        s.setViewers(rs.getInt("viewers"));
        s.setStatus(rs.getString("status"));
        s.setUrl(rs.getString("url"));
        s.setStreamKey(rs.getString("stream_key"));
        s.setRtmpServer(rs.getString("rtmp_server"));
        s.setApiVideoId(rs.getString("api_video_id"));
        s.setIsLive(rs.getBoolean("is_live"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) s.setCreatedAt(ts.toLocalDateTime());
        s.setUserId(rs.getInt("user_id"));
        return s;
    }
}
