package com.gamilha.services;

import java.util.List;


/**
 * Interface générique définissant les 4 opérations CRUD de base.
 *
 * Toutes les classes de service (EvenementService, EquipeService, etc.)
 * implémentent cette interface, ce qui garantit un contrat uniforme
 * pour créer, lire, modifier et supprimer n'importe quelle entité.
 *
 * @param <T> le type de l'entité gérée (Evenement, Equipe, Bracket, GameMatch...)
 */
public interface ICrud<T> {

    /**
     * Insère une nouvelle entité dans la base de données.
     *
     * @param p l'entité à persister
     */
    void ajouterEntite(T p);

    /**
     * Retourne la liste complète de toutes les entités.
     *
     * @return liste de toutes les instances en base
     */
    List<T> afficherEntite();

    /**
     * Met à jour une entité existante en base de données.
     *
     * @param p l'entité avec les nouvelles valeurs (doit avoir un ID valide)
     */
    void modifierEntite(T p);

    /**
     * Supprime une entité de la base de données.
     *
     * @param p l'entité à supprimer (identifiée par son ID)
     */
    void supprimerEntite(T p);
}
