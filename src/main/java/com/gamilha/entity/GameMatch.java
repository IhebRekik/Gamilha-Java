package com.gamilha.entity;

import java.time.LocalDateTime;

public class GameMatch {
    private Integer idMatch;
    private LocalDateTime dateMatch;
    private Integer tour;
    private Integer scoreEquipeA;
    private Integer scoreEquipeB;
    private String statut;
    private Integer equipeAId;
    private String equipeANom;
    private Integer equipeBId;
    private String equipeBNom;
    private Integer bracketId;
    private String bracketDisplay;

    public Integer getIdMatch() {
        return idMatch;
    }

    public void setIdMatch(Integer idMatch) {
        this.idMatch = idMatch;
    }

    public LocalDateTime getDateMatch() {
        return dateMatch;
    }

    public void setDateMatch(LocalDateTime dateMatch) {
        this.dateMatch = dateMatch;
    }

    public Integer getTour() {
        return tour;
    }

    public void setTour(Integer tour) {
        this.tour = tour;
    }

    public Integer getScoreEquipeA() {
        return scoreEquipeA;
    }

    public void setScoreEquipeA(Integer scoreEquipeA) {
        this.scoreEquipeA = scoreEquipeA;
    }

    public Integer getScoreEquipeB() {
        return scoreEquipeB;
    }

    public void setScoreEquipeB(Integer scoreEquipeB) {
        this.scoreEquipeB = scoreEquipeB;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getEquipeAId() {
        return equipeAId;
    }

    public void setEquipeAId(Integer equipeAId) {
        this.equipeAId = equipeAId;
    }

    public String getEquipeANom() {
        return equipeANom;
    }

    public void setEquipeANom(String equipeANom) {
        this.equipeANom = equipeANom;
    }

    public Integer getEquipeBId() {
        return equipeBId;
    }

    public void setEquipeBId(Integer equipeBId) {
        this.equipeBId = equipeBId;
    }

    public String getEquipeBNom() {
        return equipeBNom;
    }

    public void setEquipeBNom(String equipeBNom) {
        this.equipeBNom = equipeBNom;
    }

    public Integer getBracketId() {
        return bracketId;
    }

    public void setBracketId(Integer bracketId) {
        this.bracketId = bracketId;
    }

    public String getBracketDisplay() {
        return bracketDisplay;
    }

    public void setBracketDisplay(String bracketDisplay) {
        this.bracketDisplay = bracketDisplay;
    }
}

