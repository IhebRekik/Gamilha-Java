package com.gamilha.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Post {

    private int               id;
    /**
     * Colonne `image` dans Symfony.
     * On y stocke TOUTES les images séparées par "," :
     * "photo1.jpg,photo2.jpg,photo3.jpg"
     * → Symfony ne lit que la première (compat totale).
     * → Java lit toutes pour la grille Instagram.
     * 
     * Plus de colonne `images` séparée → pas de rupture BD Symfony.
     */
    private String            image;
    private String            mediaurl;
    private String            content;
    private LocalDateTime     createdAt;
    private User              user;
    private int               likesCount;
    private List<Commentaire> commentaires = new ArrayList<>();

    // Smart feed
    private boolean friendPost = false;
    private double  score      = 0.0;

    /**
     * Style d'écriture — stocké dans le CONTENU avec préfixe :
     * "[GRAS]texte"  "[ITALIQUE]texte"  "[CODE]texte"  "[CITATION]texte"
     * → pas de nouvelle colonne BD, compat Symfony.
     */
    private String textStyle = "Normal";

    // Partage — stocké dans shared_from_id (colonne existante ou ignorée)
    private Integer sharedFromId = null;
    private Post    sharedPost   = null;

    public Post() {}

    public Post(int id, String content, String image, String mediaurl,
                LocalDateTime createdAt, User user, int likesCount) {
        this.id = id; this.content = content; this.image = image;
        this.mediaurl = mediaurl; this.createdAt = createdAt;
        this.user = user; this.likesCount = likesCount;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────
    public int           getId()                                 { return id; }
    public void          setId(int id)                           { this.id = id; }
    public String        getContent()                            { return content; }
    public void          setContent(String c)                    { this.content = c; }
    public String        getImage()                              { return image; }
    public void          setImage(String i)                      { this.image = i; }
    public String        getMediaurl()                           { return mediaurl; }
    public void          setMediaurl(String m)                   { this.mediaurl = m; }
    public LocalDateTime getCreatedAt()                          { return createdAt; }
    public void          setCreatedAt(LocalDateTime c)           { this.createdAt = c; }
    public User          getUser()                               { return user; }
    public void          setUser(User u)                         { this.user = u; }
    public int           getLikesCount()                         { return likesCount; }
    public void          setLikesCount(int l)                    { this.likesCount = l; }
    public List<Commentaire> getCommentaires()                   { return commentaires; }
    public void          setCommentaires(List<Commentaire> l)    { this.commentaires = l; }
    public boolean       isFriendPost()                          { return friendPost; }
    public void          setFriendPost(boolean f)                { this.friendPost = f; }
    public double        getScore()                              { return score; }
    public void          setScore(double s)                      { this.score = s; }
    public String        getTextStyle()                          { return textStyle; }
    public void          setTextStyle(String s)                  { this.textStyle = s != null ? s : "Normal"; }
    public Integer       getSharedFromId()                       { return sharedFromId; }
    public void          setSharedFromId(Integer i)              { this.sharedFromId = i; }
    public Post          getSharedPost()                         { return sharedPost; }
    public void          setSharedPost(Post p)                   { this.sharedPost = p; }

    /**
     * Retourne la liste de TOUTES les images.
     * La colonne `image` contient : "img1.jpg" ou "img1.jpg,img2.jpg,img3.jpg"
     */
    public List<String> getAllImages() {
        List<String> all = new ArrayList<>();
        if (image == null || image.isBlank()) return all;
        for (String s : image.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) all.add(t);
        }
        return all;
    }

    /**
     * Première image seulement (compat Symfony).
     */
    public String getFirstImage() {
        List<String> all = getAllImages();
        return all.isEmpty() ? null : all.get(0);
    }
}
