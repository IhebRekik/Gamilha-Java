package com.gamilha.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlaylistStatistics {

    private int playlistId;
    private int totalVideos;
    private int totalViews;
    private int watchedVideos;
    private int totalFavorites;
    private double averageViewsPerVideo;
    private String topVideoTitle;
    private int topVideoViews;
    private LocalDateTime lastViewedAt;
    private List<String> topVideoSummaries = new ArrayList<>();

    public int getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(int playlistId) {
        this.playlistId = playlistId;
    }

    public int getTotalVideos() {
        return totalVideos;
    }

    public void setTotalVideos(int totalVideos) {
        this.totalVideos = totalVideos;
    }

    public int getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(int totalViews) {
        this.totalViews = totalViews;
    }

    public int getWatchedVideos() {
        return watchedVideos;
    }

    public void setWatchedVideos(int watchedVideos) {
        this.watchedVideos = watchedVideos;
    }

    public int getTotalFavorites() {
        return totalFavorites;
    }

    public void setTotalFavorites(int totalFavorites) {
        this.totalFavorites = totalFavorites;
    }

    public double getAverageViewsPerVideo() {
        return averageViewsPerVideo;
    }

    public void setAverageViewsPerVideo(double averageViewsPerVideo) {
        this.averageViewsPerVideo = averageViewsPerVideo;
    }

    public String getTopVideoTitle() {
        return topVideoTitle;
    }

    public void setTopVideoTitle(String topVideoTitle) {
        this.topVideoTitle = topVideoTitle;
    }

    public int getTopVideoViews() {
        return topVideoViews;
    }

    public void setTopVideoViews(int topVideoViews) {
        this.topVideoViews = topVideoViews;
    }

    public LocalDateTime getLastViewedAt() {
        return lastViewedAt;
    }

    public void setLastViewedAt(LocalDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }

    public List<String> getTopVideoSummaries() {
        return topVideoSummaries;
    }

    public void setTopVideoSummaries(List<String> topVideoSummaries) {
        this.topVideoSummaries = topVideoSummaries != null ? topVideoSummaries : new ArrayList<>();
    }
}
