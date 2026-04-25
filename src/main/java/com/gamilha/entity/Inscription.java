package com.gamilha.entity;

import java.time.LocalDate;
import java.util.Objects;

public class Inscription {

    private int id;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private User user;
    private Abonnement abonnements;

    public Inscription(int id, LocalDate dateDebut, LocalDate dateFin, User user, Abonnement abonnements) {
        this.id = id;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.user = user;
        this.abonnements = abonnements;
    }

    public Inscription() {
    }

    public Abonnement getAbonnements() {
        return abonnements;
    }

    public void setAbonnements(Abonnement abonnements) {
        this.abonnements = abonnements;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Inscription that = (Inscription) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Inscription{" +
                "id=" + id +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", user=" + user +
                ", abonnements=" + abonnements +
                '}';
    }
}

