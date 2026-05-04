package com.gamilha.entity;

import java.util.List;
import java.util.Objects;

public class Abonnement {
    private int id;
    private String type;
    private float prix;
    private List<String> avantages;
    private int duree;
    private List<String> options;

    public Abonnement(int id, String type, float prix, List<String> avantages, int duree, List<String> options) {
        this.id = id;
        this.type = type;
        this.prix = prix;
        this.avantages = avantages;
        this.duree = duree;
        this.options = options;
    }

    public Abonnement() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public float getPrix() {
        return prix;
    }

    public void setPrix(float prix) {
        this.prix = prix;
    }

    public List<String> getAvantages() {
        return avantages;
    }

    public void setAvantages(List<String> avantages) {
        this.avantages = avantages;
    }

    public int getDuree() {
        return duree;
    }

    public void setDuree(int duree) {
        this.duree = duree;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Abonnement that = (Abonnement) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Abonnement{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", prix=" + prix +
                ", avantages=" + avantages +
                ", duree=" + duree +
                ", options=" + options +
                '}';
    }
}

