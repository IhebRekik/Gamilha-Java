package com.gamilha.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public class Playlist {

    // ─── Attributs (table : playlist) ─────────────────────────────────────────
    private int           id;
    private String        title;
    private String        description;
    private String        niveau;
    private String        categorie;
    private String        image;
    private LocalDateTime createdAt;

    // ─── Relation : Une Playlist contient plusieurs CoachingVideo ─────────────
    private List<CoachingVideo> videos = new ArrayList<>();

    // ─── Constructeurs ────────────────────────────────────────────────────────
    public Playlist() {}

    public Playlist(String title, String description, String niveau,
                    String categorie, String image, LocalDateTime createdAt) {
        this.title       = title;
        this.description = description;
        this.niveau      = niveau;
        this.categorie   = categorie;
        this.image       = image;
        this.createdAt   = createdAt;
    }

    public Playlist(int id, String title, String description, String niveau,
                    String categorie, String image, LocalDateTime createdAt) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.niveau      = niveau;
        this.categorie   = categorie;
        this.image       = image;
        this.createdAt   = createdAt;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<CoachingVideo> getVideos() {
        return videos;
    }

    public void setVideos(List<CoachingVideo> videos) {
        this.videos = videos;
    }

    public void addVideo(CoachingVideo video) {
        this.videos.add(video);
        video.setPlaylist(this);
    }

    // ─── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "Playlist{" +
                "id="          + id          +
                ", title='"    + title       + '\'' +
                ", description='" + description + '\'' +
                ", niveau='"   + niveau      + '\'' +
                ", categorie='"+ categorie   + '\'' +
                ", image='"    + image       + '\'' +
                ", createdAt=" + createdAt   +
                ", videos="    + videos.size() + " vidéo(s)" +
                '}';
    }
}
