package com.gamilha.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VideoSubtitle {

    private int id;
    private int videoId;
    private String languageCode;
    private String languageLabel;
    private String filePath;
    private LocalDateTime createdAt;
    private List<SubtitleCue> cues = new ArrayList<>();

    public VideoSubtitle() {
    }

    public VideoSubtitle(String languageCode, String languageLabel, String filePath) {
        this.languageCode = languageCode;
        this.languageLabel = languageLabel;
        this.filePath = filePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVideoId() {
        return videoId;
    }

    public void setVideoId(int videoId) {
        this.videoId = videoId;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageLabel() {
        return languageLabel;
    }

    public void setLanguageLabel(String languageLabel) {
        this.languageLabel = languageLabel;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SubtitleCue> getCues() {
        return cues;
    }

    public void setCues(List<SubtitleCue> cues) {
        this.cues = cues != null ? cues : new ArrayList<>();
    }

    public String getTextAt(long positionMillis) {
        for (SubtitleCue cue : cues) {
            if (cue.contains(positionMillis)) {
                return cue.getText();
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return languageLabel + " - " + filePath;
    }
}
