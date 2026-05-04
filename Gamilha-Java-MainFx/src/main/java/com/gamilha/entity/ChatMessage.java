package com.gamilha.entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class ChatMessage {

    private int id;
    private String content;
    private LocalDateTime createdAt;
    private User sender;
    private User recipient;

    public ChatMessage(int id, String content, User sender, User recipient) {
        this.id = id;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.sender = sender;
        this.recipient = recipient;
    }

    public ChatMessage() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {

        if(content == null || content.isEmpty())
            throw new IllegalArgumentException("Le contenu ne peut pas être vide");

        if(content.length() > 255)
            throw new IllegalArgumentException("Le contenu ne peut pas dépasser 255 caractères");

        this.content = content;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }




    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }


    public User getRecipient() {
        return recipient;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                ", sender=" + sender +
                ", recipient=" + recipient +
                '}';
    }
}