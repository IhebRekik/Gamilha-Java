package com.gamilha.services;

import com.gamilha.entity.SubtitleCue;
import com.gamilha.entity.VideoSubtitle;
import com.gamilha.utils.DatabaseConnection;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoSubtitleService {

    public VideoSubtitleService() {
        CoachingFeatureSchemaService.ensureSchema();
    }

    private Connection getConnection() {
        return DatabaseConnection.getConnection();
    }

    public List<VideoSubtitle> findByVideoId(int videoId) {
        List<VideoSubtitle> subtitles = new ArrayList<>();
        String sql = "SELECT * FROM video_subtitle WHERE video_id = ? ORDER BY language_label";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return subtitles;
            }
            ps.setInt(1, videoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VideoSubtitle subtitle = new VideoSubtitle();
                    subtitle.setId(rs.getInt("id"));
                    subtitle.setVideoId(rs.getInt("video_id"));
                    subtitle.setLanguageCode(rs.getString("language_code"));
                    subtitle.setLanguageLabel(rs.getString("language_label"));
                    subtitle.setFilePath(rs.getString("file_path"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        subtitle.setCreatedAt(ts.toLocalDateTime());
                    }
                    subtitle.setCues(parseSubtitleFile(subtitle.getFilePath()));
                    subtitles.add(subtitle);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur findByVideoId subtitles: " + e.getMessage());
        }
        return subtitles;
    }

    public void replaceSubtitles(int videoId, List<VideoSubtitle> subtitles) {
        deleteByVideoId(videoId);

        if (subtitles == null || subtitles.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO video_subtitle (video_id, language_code, language_label, file_path, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            for (VideoSubtitle subtitle : subtitles) {
                ps.setInt(1, videoId);
                ps.setString(2, subtitle.getLanguageCode());
                ps.setString(3, subtitle.getLanguageLabel());
                ps.setString(4, subtitle.getFilePath());
                ps.setTimestamp(5, Timestamp.valueOf(
                        subtitle.getCreatedAt() != null ? subtitle.getCreatedAt() : LocalDateTime.now()
                ));
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("Erreur replaceSubtitles: " + e.getMessage());
        }
    }

    public void deleteByVideoId(int videoId) {
        String sql = "DELETE FROM video_subtitle WHERE video_id = ?";
        try (Connection cnx = getConnection();
             PreparedStatement ps = cnx != null ? cnx.prepareStatement(sql) : null) {
            if (ps == null) {
                return;
            }
            ps.setInt(1, videoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur deleteByVideoId subtitles: " + e.getMessage());
        }
    }

    public List<SubtitleCue> parseSubtitleFile(String path) {
        if (path == null || path.isBlank()) {
            return new ArrayList<>();
        }

        File file = new File(path);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try {
            String raw = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            String normalized = raw.replace("\uFEFF", "").replace("\r\n", "\n").replace('\r', '\n').trim();
            if (normalized.startsWith("WEBVTT")) {
                normalized = normalized.substring("WEBVTT".length()).trim();
            }

            List<SubtitleCue> cues = new ArrayList<>();
            String[] blocks = normalized.split("\n\\s*\n");
            for (String block : blocks) {
                List<String> lines = Arrays.stream(block.split("\n"))
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .toList();

                if (lines.isEmpty()) {
                    continue;
                }

                int timingIndex = -1;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).contains("-->")) {
                        timingIndex = i;
                        break;
                    }
                }

                if (timingIndex < 0) {
                    continue;
                }

                String timing = lines.get(timingIndex);
                String[] times = timing.split("-->");
                if (times.length != 2) {
                    continue;
                }

                long start = parseTimecode(times[0].trim());
                String endRaw = times[1].trim().split("\\s+")[0];
                long end = parseTimecode(endRaw);

                StringBuilder text = new StringBuilder();
                for (int i = timingIndex + 1; i < lines.size(); i++) {
                    if (text.length() > 0) {
                        text.append('\n');
                    }
                    text.append(lines.get(i));
                }

                if (text.length() > 0) {
                    cues.add(new SubtitleCue(start, end, text.toString()));
                }
            }
            return cues;
        } catch (IOException e) {
            System.err.println("Erreur lecture sous-titre: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private long parseTimecode(String value) {
        String normalized = value.replace(',', '.');
        String[] parts = normalized.split(":");
        if (parts.length != 3) {
            return 0L;
        }

        double seconds = Double.parseDouble(parts[2]);
        long hours = Long.parseLong(parts[0]);
        long minutes = Long.parseLong(parts[1]);

        return (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
    }
}
