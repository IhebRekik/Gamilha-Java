package com.gamilha.validation;

import com.gamilha.entity.Bracket;
import com.gamilha.entity.Equipe;
import com.gamilha.entity.Evenement;
import com.gamilha.entity.GameMatch;

import java.time.LocalDate;
import java.util.Set;

/**
 * Classe utilitaire de validation métier pour toutes les entités.
 *
 * Chaque méthode "validate*" vérifie les contraintes de l'entité et lance
 * une {@link ValidationException} (RuntimeException) dès qu'une règle est violée.
 * Les services appellent ces méthodes avant toute insertion ou mise à jour en base.
 *
 * Classe non instanciable (constructeur privé + méthodes statiques).
 */
public final class InputValidator {

    // Valeurs autorisées pour chaque champ énuméré
    private static final Set<String> EVENEMENT_TYPES   = Set.of("online", "offline");
    private static final Set<String> EVENEMENT_STATUTS = Set.of("prévu", "en cours", "terminé");
    private static final Set<String> BRACKET_TYPES     = Set.of("single elimination", "double elimination");
    private static final Set<String> BRACKET_STATUTS   = Set.of("en attente", "en cours", "terminé");
    private static final Set<String> NIVEAUX           = Set.of("amateur", "semi-pro", "pro");
    private static final Set<String> MATCH_STATUTS     = Set.of("à venir", "en cours", "terminé");

    private InputValidator() {}

    /**
     * Valide un événement avant insertion ou modification.
     *
     * Règles métier :
     * - Nom obligatoire (max 100 caractères)
     * - Description obligatoire (min 10 caractères)
     * - Jeu obligatoire (max 50 caractères)
     * - Type : "online" ou "offline"
     * - Dates de début et fin obligatoires
     * - Date début > aujourd'hui
     * - Date fin > date début
     * - Statut valide ("prévu", "en cours", "terminé")
     * - Règles et image obligatoires
     *
     * @param evenement l'événement à valider
     * @throws ValidationException si une règle est violée
     */
    public static void validateEvenement(Evenement evenement) {
        if (evenement == null) throw new ValidationException("L'evenement est obligatoire.");

        requireNotBlank(evenement.getNom(), "Le nom est obligatoire.");
        requireMaxLen(evenement.getNom(), 100, "Le nom ne doit pas depasser 100 caracteres.");
        requireNotBlank(evenement.getDescription(), "La description est obligatoire.");
        requireMinLen(evenement.getDescription(), 10, "La description doit contenir au moins 10 caracteres.");
        requireNotBlank(evenement.getJeu(), "Le jeu est obligatoire.");
        requireMaxLen(evenement.getJeu(), 50, "Le jeu ne doit pas depasser 50 caracteres.");
        requireChoice(evenement.getTypeEvenement(), EVENEMENT_TYPES, "Choisir online ou offline.");
        requireDate(evenement.getDateDebut(), "La date de debut est obligatoire.");
        requireDate(evenement.getDateFin(), "La date de fin est obligatoire.");
        requireNotBlank(evenement.getRegles(), "Les regles sont obligatoires.");
        requireNotBlank(evenement.getImage(), "L'image est obligatoire.");

        // La date de début doit être dans le futur
        if (evenement.getDateDebut() != null && !evenement.getDateDebut().isAfter(LocalDate.now())) {
            throw new ValidationException("La date de debut doit etre superieure a aujourd'hui.");
        }

        // La date de fin doit être strictement après la date de début
        if (evenement.getDateDebut() != null && evenement.getDateFin() != null
                && !evenement.getDateFin().isAfter(evenement.getDateDebut())) {
            throw new ValidationException("La date de fin doit etre strictement apres la date de debut.");
        }

        requireChoice(evenement.getStatut(), EVENEMENT_STATUTS, "Statut invalide.");
        requireMaxLen(evenement.getImage(), 255, "L'image ne doit pas depasser 255 caracteres.");
    }

    /**
     * Valide un bracket avant insertion ou modification.
     *
     * Règles :
     * - Type : "single elimination" ou "double elimination"
     * - Nombre de tours >= 0
     * - Statut valide
     * - Événement associé obligatoire
     *
     * @param bracket le bracket à valider
     * @throws ValidationException si une règle est violée
     */
    public static void validateBracket(Bracket bracket) {
        if (bracket == null) throw new ValidationException("Le bracket est obligatoire.");

        requireChoice(bracket.getTypeBracket(), BRACKET_TYPES, "Choisir single elimination ou double elimination.");
        if (bracket.getNombreTours() == null || bracket.getNombreTours() < 0) {
            throw new ValidationException("Le nombre de tours doit etre positif ou zero.");
        }
        requireChoice(bracket.getStatut(), BRACKET_STATUTS, "Statut invalide.");
        if (bracket.getEvenementId() == null) {
            throw new ValidationException("L'evenement est obligatoire.");
        }
    }

    /**
     * Valide une équipe avant insertion ou modification.
     *
     * Règles :
     * - Nom obligatoire (max 100 caractères)
     * - Tag obligatoire (max 10 caractères)
     * - Logo obligatoire (max 255 caractères)
     * - Pays obligatoire (max 50 caractères)
     * - Date de création obligatoire
     * - Niveau : "amateur", "semi-pro" ou "pro"
     * - Propriétaire (owner_id) obligatoire
     *
     * @param equipe l'équipe à valider
     * @throws ValidationException si une règle est violée
     */
    public static void validateEquipe(Equipe equipe) {
        if (equipe == null) throw new ValidationException("L'equipe est obligatoire.");

        requireNotBlank(equipe.getNomEquipe(), "Le nom de l'equipe est obligatoire.");
        requireMaxLen(equipe.getNomEquipe(), 100, "Le nom de l'equipe ne doit pas depasser 100 caracteres.");
        requireNotBlank(equipe.getTag(), "Le tag est obligatoire.");
        requireMaxLen(equipe.getTag(), 10, "Le tag ne doit pas depasser 10 caracteres.");
        requireNotBlank(equipe.getLogo(), "Le logo est obligatoire.");
        requireMaxLen(equipe.getLogo(), 255, "Le logo ne doit pas depasser 255 caracteres.");
        requireNotBlank(equipe.getPays(), "Le pays est obligatoire.");
        requireMaxLen(equipe.getPays(), 50, "Le pays ne doit pas depasser 50 caracteres.");
        if (equipe.getDateCreation() == null) throw new ValidationException("La date de creation est obligatoire.");
        requireChoice(equipe.getNiveau(), NIVEAUX, "Niveau invalide.");
        if (equipe.getOwnerId() == null) throw new ValidationException("Le proprietaire de l'equipe est obligatoire.");
    }

    /**
     * Valide un match avant insertion ou modification.
     *
     * Règles :
     * - Tour >= 0
     * - Scores A et B >= 0
     * - Statut valide
     * - Bracket associé obligatoire
     * - Équipes A et B obligatoires si le match n'est pas "à venir"
     * - Date du match obligatoire
     * - Équipes A et B doivent être différentes
     *
     * @param match le match à valider
     * @throws ValidationException si une règle est violée
     */
    public static void validateMatch(GameMatch match) {
        if (match == null) throw new ValidationException("Le match est obligatoire.");

        if (match.getTour() == null || match.getTour() < 0) {
            throw new ValidationException("Le tour doit etre positif ou zero.");
        }
        if (match.getScoreEquipeA() == null || match.getScoreEquipeA() < 0) {
            throw new ValidationException("Le score equipe A doit etre positif ou zero.");
        }
        if (match.getScoreEquipeB() == null || match.getScoreEquipeB() < 0) {
            throw new ValidationException("Le score equipe B doit etre positif ou zero.");
        }
        requireChoice(match.getStatut(), MATCH_STATUTS, "Statut invalide.");
        if (match.getBracketId() == null) throw new ValidationException("Le bracket est obligatoire.");

        // Les équipes ne sont pas obligatoires pour les matchs futurs (placeholders)
        boolean pendingMatch = "à venir".equals(match.getStatut());
        if (!pendingMatch && match.getEquipeAId() == null) throw new ValidationException("L'equipe A est obligatoire.");
        if (!pendingMatch && match.getEquipeBId() == null) throw new ValidationException("L'equipe B est obligatoire.");

        if (match.getDateMatch() == null) throw new ValidationException("La date du match est obligatoire.");

        // Les deux équipes doivent être distinctes
        if (match.getEquipeAId() != null && match.getEquipeBId() != null
                && match.getEquipeAId().equals(match.getEquipeBId())) {
            throw new ValidationException("Les deux equipes doivent etre differentes.");
        }
    }

    // ─── Méthodes helper privées ──────────────────────────────────────────────

    /** Vérifie que la valeur n'est pas null ni vide. */
    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new ValidationException(message);
    }

    /** Vérifie que la longueur ne dépasse pas le maximum autorisé. */
    private static void requireMaxLen(String value, int max, String message) {
        if (value != null && value.length() > max) throw new ValidationException(message);
    }

    /** Vérifie que la longueur est au moins égale au minimum requis. */
    private static void requireMinLen(String value, int min, String message) {
        if (value == null || value.trim().length() < min) throw new ValidationException(message);
    }

    /** Vérifie que la valeur fait partie d'un ensemble de valeurs autorisées. */
    private static void requireChoice(String value, Set<String> choices, String message) {
        if (value == null || !choices.contains(value)) throw new ValidationException(message);
    }

    /** Vérifie que la date n'est pas null. */
    private static void requireDate(LocalDate value, String message) {
        if (value == null) throw new ValidationException(message);
    }
}
