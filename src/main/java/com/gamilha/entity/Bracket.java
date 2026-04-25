package com.gamilha.entity;

public class Bracket {
    private Integer idBracket;
    private String typeBracket;
    private Integer nombreTours;
    private String statut;
    private Integer evenementId;
    private String evenementNom;

    public Integer getIdBracket() {
        return idBracket;
    }

    public void setIdBracket(Integer idBracket) {
        this.idBracket = idBracket;
    }

    public String getTypeBracket() {
        return typeBracket;
    }

    public void setTypeBracket(String typeBracket) {
        this.typeBracket = typeBracket;
    }

    public Integer getNombreTours() {
        return nombreTours;
    }

    public void setNombreTours(Integer nombreTours) {
        this.nombreTours = nombreTours;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(Integer evenementId) {
        this.evenementId = evenementId;
    }

    public String getEvenementNom() {
        return evenementNom;
    }

    public void setEvenementNom(String evenementNom) {
        this.evenementNom = evenementNom;
    }

    @Override
    public String toString() {
        if (typeBracket == null) {
            return "";
        }
        return idBracket == null ? typeBracket : typeBracket + " (ID: " + idBracket + ")";
    }
}

