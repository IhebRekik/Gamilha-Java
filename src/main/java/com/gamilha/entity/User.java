package com.gamilha.entity;

import java.sql.Timestamp;

public class User {

    private int id;
    private String name;
    private String email;
    private String password;
    private String profileImage;
    private String roles = "[\"ROLE_USER\"]";
    private boolean isActive = true;
    private String banUntil;
    private int reports = 0;
    private Timestamp createdAt;

    // New fields
    private int loginAttempts = 0;
    private boolean lockedUntil = false;
    private Timestamp lockExpiry;
    private boolean twoFactorEnabled = false;
    private String twoFactorCode;
    private Timestamp lastSeen;
    private boolean isOnline = false;
    private String lastIp;
    private String lastDevice;
    private String auditLog;
    private boolean verified = false;
    private Timestamp deactivateUntil;

    public User() {
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public User(int id, String name, String email, String profileImage,
                String roles, boolean isActive, String banUntil) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.profileImage = profileImage;
        this.roles = roles;
        this.isActive = isActive;
        this.banUntil = banUntil;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public boolean isAdmin() {
        return roles != null && roles.contains("ROLE_ADMIN");
    }

    // --- Presence display helper ---
    public String getPresenceLabel() {
        if (isOnline) return "🟢 En ligne";
        if (lastSeen == null) return "⚫ Jamais connecté";
        long diffMs = System.currentTimeMillis() - lastSeen.getTime();
        long minutes = diffMs / 60000;
        if (minutes < 1) return "🟡 Vu il y a quelques secondes";
        if (minutes < 60) return "🟡 Vu il y a " + minutes + " min";
        long hours = minutes / 60;
        if (hours < 24) return "⚫ Vu il y a " + hours + "h";
        long days = hours / 24;
        return "⚫ Vu il y a " + days + " jour(s)";
    }

    // ============ GETTERS / SETTERS ============
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public boolean isActive() { return isActive; }
    public boolean getActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public String getBanUntil() { return banUntil; }
    public void setBanUntil(String banUntil) { this.banUntil = banUntil; }

    public int getReports() { return reports; }
    public void setReports(int reports) { this.reports = reports; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getLoginAttempts() { return loginAttempts; }
    public void setLoginAttempts(int loginAttempts) { this.loginAttempts = loginAttempts; }

    public boolean isLockedUntil() { return lockedUntil; }
    public void setLockedUntil(boolean lockedUntil) { this.lockedUntil = lockedUntil; }

    public Timestamp getLockExpiry() { return lockExpiry; }
    public void setLockExpiry(Timestamp lockExpiry) { this.lockExpiry = lockExpiry; }

    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public void setTwoFactorEnabled(boolean twoFactorEnabled) { this.twoFactorEnabled = twoFactorEnabled; }

    public String getTwoFactorCode() { return twoFactorCode; }
    public void setTwoFactorCode(String twoFactorCode) { this.twoFactorCode = twoFactorCode; }

    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { this.isOnline = online; }

    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }

    public String getLastDevice() { return lastDevice; }
    public void setLastDevice(String lastDevice) { this.lastDevice = lastDevice; }

    public String getAuditLog() { return auditLog; }
    public void setAuditLog(String auditLog) { this.auditLog = auditLog; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Timestamp getDeactivateUntil() { return deactivateUntil; }
    public void setDeactivateUntil(Timestamp deactivateUntil) { this.deactivateUntil = deactivateUntil; }

    @Override
    public String toString() { return name + " <" + email + ">"; }
}
