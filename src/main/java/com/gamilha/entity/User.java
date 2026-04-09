package com.gamilha.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class User {
    private int id;
    private String email;
    private List<String> roles;
    private String password;
    private String name;
    private LocalDateTime createdAt;
    private String profileImage;
    private LocalDateTime banUntil;
    private int reports;
    private boolean isActive;

    public User(int id, String email, List<String> roles, String password, String name, LocalDateTime createdAt, String profileImage, LocalDateTime banUntil, int reports, boolean isActive) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.name = name;
        this.createdAt = createdAt;
        this.profileImage = profileImage;
        this.banUntil = banUntil;
        this.reports = reports;
        this.isActive = isActive;
    }

    public User() {
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public LocalDateTime getBanUntil() {
        return banUntil;
    }

    public void setBanUntil(LocalDateTime banUntil) {
        this.banUntil = banUntil;
    }

    public int getReports() {
        return reports;
    }

    public void setReports(int reports) {
        this.reports = reports;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", roles=" + roles +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", profileImage='" + profileImage + '\'' +
                ", banUntil=" + banUntil +
                ", reports=" + reports +
                ", isActive=" + isActive +
                '}';
    }
}
