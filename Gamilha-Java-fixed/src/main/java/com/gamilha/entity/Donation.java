package com.gamilha.entity;

import java.time.LocalDateTime;

public class Donation {
    private int id;
    private double amount;
    private String donorName;
    private LocalDateTime createdAt;
    private int userId;
    private int streamId;
    private String streamTitle;
    private String userEmail;

    public Donation() { this.createdAt = LocalDateTime.now(); }

    public String getEmoji() {
        if (amount >= 50) return "🚀";
        if (amount >= 10) return "💎";
        if (amount >= 5)  return "🍕";
        return "🍩";
    }

    public String getFormattedAmount() { return String.format("%.2f €", amount); }

    @Override public String toString() { return donorName + " — " + getFormattedAmount(); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public double getAmount() { return amount; }
    public void setAmount(double a) { this.amount = a; }
    public String getDonorName() { return donorName; }
    public void setDonorName(String n) { this.donorName = n; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime c) { this.createdAt = c; }
    public int getUserId() { return userId; }
    public void setUserId(int u) { this.userId = u; }
    public int getStreamId() { return streamId; }
    public void setStreamId(int s) { this.streamId = s; }
    public String getStreamTitle() { return streamTitle; }
    public void setStreamTitle(String t) { this.streamTitle = t; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String e) { this.userEmail = e; }
}
