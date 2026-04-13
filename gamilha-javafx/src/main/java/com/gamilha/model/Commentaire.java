package com.gamilha.model;

import java.time.LocalDateTime;

public class Commentaire {

    private int           id;
    private String        text;
    private LocalDateTime createdAt;
    private Post          post;
    private User          user;

    public Commentaire() {}

    public Commentaire(int id, String text, LocalDateTime createdAt, Post post, User user) {
        this.id        = id;
        this.text      = text;
        this.createdAt = createdAt;
        this.post      = post;
        this.user      = user;
    }

    public int           getId()                               { return id; }
    public void          setId(int id)                         { this.id = id; }
    public String        getText()                             { return text; }
    public void          setText(String text)                  { this.text = text; }
    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Post          getPost()                             { return post; }
    public void          setPost(Post post)                    { this.post = post; }
    public User          getUser()                             { return user; }
    public void          setUser(User user)                    { this.user = user; }
}
