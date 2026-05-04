package com.gamilha.entity;

public class CoachingVideo {

    // ─── Attributs (table : coaching_video) ───────────────────────────────────
    private int      id;
    private String   titre;
    private String   description;
    private String   url;
    private String   niveau;
    private boolean  premium;
    private int      duration;   // en secondes (ex: 600 = 10 min)

    // ─── Relation : Plusieurs CoachingVideo appartiennent à une Playlist ──────
    private Playlist playlist;   // correspond à la FK playlist_id

    // ─── Constructeurs ────────────────────────────────────────────────────────
    public CoachingVideo() {}

    public CoachingVideo(String titre, String description, String url,
                         String niveau, boolean premium, int duration,
                         Playlist playlist) {
        this.titre       = titre;
        this.description = description;
        this.url         = url;
        this.niveau      = niveau;
        this.premium     = premium;
        this.duration    = duration;
        this.playlist    = playlist;
    }

    public CoachingVideo(int id, String titre, String description, String url,
                         String niveau, boolean premium, int duration,
                         Playlist playlist) {
        this.id          = id;
        this.titre       = titre;
        this.description = description;
        this.url         = url;
        this.niveau      = niveau;
        this.premium     = premium;
        this.duration    = duration;
        this.playlist    = playlist;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Playlist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }

    // ─── Méthode utilitaire : durée formatée ──────────────────────────────────
    public String getDurationFormatted() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%d min %02d sec", minutes, seconds);
    }

    // ─── toString ─────────────────────────────────────────────────────────────
    @Override
    public String toString() {
        return "CoachingVideo{" +
                "id="              + id          +
                ", titre='"        + titre       + '\'' +
                ", description='"  + description + '\'' +
                ", url='"          + url         + '\'' +
                ", niveau='"       + niveau      + '\'' +
                ", premium="       + premium     +
                ", duration="      + getDurationFormatted() +
                ", playlist_id="   + (playlist != null ? playlist.getId() : "null") +
                '}';
    }
}
