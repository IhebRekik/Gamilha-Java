package com.gamilha.entity;

import java.time.LocalDate;

public class Equipe {
    private Integer idEquipe;
    private String nomEquipe;
    private String tag;
    private String logo;
    private String pays;
    private LocalDate dateCreation;
    private String niveau;
    private Integer ownerId;

    public Integer getIdEquipe() {
        return idEquipe;
    }

    public void setIdEquipe(Integer idEquipe) {
        this.idEquipe = idEquipe;
    }

    public String getNomEquipe() {
        return nomEquipe;
    }

    public void setNomEquipe(String nomEquipe) {
        this.nomEquipe = nomEquipe;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
    }

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    @Override
    public String toString() {
        if (nomEquipe != null && !nomEquipe.isBlank()) {
            return nomEquipe;
        }
        if (tag != null && !tag.isBlank()) {
            return tag;
        }
        return idEquipe == null ? "Equipe" : "Equipe #" + idEquipe;
    }
}

