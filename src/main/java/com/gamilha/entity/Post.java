package com.gamilha.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Post {

    private int           id;
    private String        content;
    private String        image;
    private String        mediaurl;
    private LocalDateTime createdAt;
    private User          user;
    private int           likesCount;
    private List<Commentaire> commentaires = new ArrayList<>();

    public Post() {}

    public Post(int id, String content, String image, String mediaurl,
                LocalDateTime createdAt, User user, int likesCount) {
        this.id         = id;
        this.content    = content;
        this.image      = image;
        this.mediaurl   = mediaurl;
        this.createdAt  = createdAt;
        this.user       = user;
        this.likesCount = likesCount;
    }

    public int           getId()                                    { return id; }
    public void          setId(int id)                              { this.id = id; }
    public String        getContent()                               { return content; }
    public void          setContent(String content)                 { this.content = content; }
    public String        getImage()                                  { return image; }
    public void          setImage(String image)                      { this.image = image; }
    public String        getMediaurl()                              { return mediaurl; }
    public void          setMediaurl(String mediaurl)               { this.mediaurl = mediaurl; }
    public LocalDateTime getCreatedAt()                             { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt)      { this.createdAt = createdAt; }
    public User          getUser()                                   { return user; }
    public void          setUser(User user)                          { this.user = user; }
    public int           getLikesCount()                            { return likesCount; }
    public void          setLikesCount(int likesCount)              { this.likesCount = likesCount; }
    public List<Commentaire> getCommentaires()                      { return commentaires; }
    public void          setCommentaires(List<Commentaire> list)    { this.commentaires = list; }
}
