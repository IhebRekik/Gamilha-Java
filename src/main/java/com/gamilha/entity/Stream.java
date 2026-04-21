package com.gamilha.entity;

import java.time.LocalDateTime;

public class Stream {
    private int id;
    private String title;
    private String description;
    private String game;
    private String thumbnail;
    private int viewers = 0;
    private String status = "live";
    private String url;
    private String streamKey;
    private String rtmpServer;
    private String apiVideoId;
    private boolean isLive = false;
    private LocalDateTime createdAt;
    private int userId;

    public Stream() {
        this.createdAt = LocalDateTime.now();
        this.streamKey = generateKey();
    }

    private static String generateKey() {
        StringBuilder sb = new StringBuilder();
        String h = "0123456789abcdef";
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 16; i++) sb.append(h.charAt(r.nextInt(16)));
        return sb.toString();
    }

    public String getStatusBadge() {
        return switch (status) {
            case "live"    -> "🔴 LIVE";
            case "offline" -> "⚫ OFFLINE";
            case "ended"   -> "✅ TERMINÉ";
            default        -> status.toUpperCase();
        };
    }

    @Override public String toString() { return title + " (" + game + ")"; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getGame() { return game; }
    public void setGame(String g) { this.game = g; }
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String t) { this.thumbnail = t; }
    public int getViewers() { return viewers; }
    public void setViewers(int v) { this.viewers = v; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public String getUrl() { return url; }
    public void setUrl(String u) { this.url = u; }
    public String getStreamKey() { return streamKey; }
    public void setStreamKey(String k) { this.streamKey = k; }
    public String getRtmpServer() { return rtmpServer; }
    public void setRtmpServer(String r) { this.rtmpServer = r; }
    public String getApiVideoId() { return apiVideoId; }
    public void setApiVideoId(String a) { this.apiVideoId = a; }
    public boolean isLive() { return isLive; }
    public void setIsLive(boolean l) { this.isLive = l; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public int getUserId() { return userId; }
    public void setUserId(int u) { this.userId = u; }
}
