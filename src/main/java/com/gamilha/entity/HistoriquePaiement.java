package com.gamilha.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HistoriquePaiement {

    private int id;
    private User user;               // relation (objet)
    private Abonnement abonnement;   // relation (objet)
    private float montant;
    private LocalDateTime createdAt;

    // 🧱 Constructeur vide
    public HistoriquePaiement() {
    }

    // 🧱 Constructeur complet
    public HistoriquePaiement(int id, User user, Abonnement abonnement, float montant, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.abonnement = abonnement;
        this.montant = montant;
        this.createdAt = createdAt;
    }

    // 🔁 Getters & Setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Abonnement getAbonnement() {
        return abonnement;
    }

    public void setAbonnement(Abonnement abonnement) {
        this.abonnement = abonnement;
    }

    public float getMontant() {
        return montant;
    }

    public void setMontant(float montant) {
        this.montant = montant;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}