package com.gamilha.services;

import com.gamilha.entity.Evenement;
import com.gamilha.entity.Bracket;
import com.gamilha.entity.GameMatch;
import com.gamilha.tools.DBconnexion;
import com.gamilha.validation.InputValidator;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service CRUD pour l'entité {@link Evenement}.
 *
 * Gère également :
 * - Les équipes participantes (table de liaison evenement_equipe)
 * - La génération automatique du bracket et des matchs associés
 * - La recherche/filtrage des événements
 */
public class EvenementService implements ICrud<Evenement> {

    private final Connection cnx = DBconnexion.getInstance().getCnx();

    /**
     * Insère un nouvel événement en base après validation.
     * Récupère l'ID généré automatiquement et le stocke dans l'entité.
     *
     * @param evenement l'événement à créer (doit passer {@link InputValidator#validateEvenement})
     * @throws RuntimeException si la validation échoue ou en cas d'erreur SQL
     */
    @Override
    public void ajouterEntite(Evenement evenement) {
        InputValidator.validateEvenement(evenement);
        String sql = "INSERT INTO evenement (nom, description, jeu, typeEvenement, dateDebut, dateFin, statut, regles, image, created_by_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, evenement.getNom());
            ps.setString(2, evenement.getDescription());
            ps.setString(3, evenement.getJeu());
            ps.setString(4, evenement.getTypeEvenement());
            ps.setDate(5, Date.valueOf(evenement.getDateDebut()));
            ps.setDate(6, Date.valueOf(evenement.getDateFin()));
            ps.setString(7, evenement.getStatut());
            ps.setString(8, evenement.getRegles());
            ps.setString(9, evenement.getImage());
            ps.setInt(10, evenement.getCreatedById());
            LocalDateTime createdAt = evenement.getCreatedAt() == null ? LocalDateTime.now() : evenement.getCreatedAt();
            ps.setTimestamp(11, Timestamp.valueOf(createdAt));
            ps.executeUpdate();

            // Récupère l'ID auto-incrémenté généré par MySQL
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    evenement.setIdEvenement(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout evenement: " + e.getMessage(), e);
        }
    }

    /** @see #findAll() */
    @Override
    public List<Evenement> afficherEntite() {
        return findAll();
    }

    /**
     * Retourne tous les événements, du plus récent au plus ancien.
     *
     * @return liste complète des événements
     */
    public List<Evenement> findAll() {
        String sql = "SELECT idEvenement, nom, description, jeu, typeEvenement, dateDebut, dateFin, statut, regles, image, created_by_id, created_at FROM evenement ORDER BY idEvenement DESC";
        List<Evenement> list = new ArrayList<>();
        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement evenements: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Retourne uniquement les événements créés par un utilisateur donné.
     * Utilisé pour filtrer la vue "Mes Événements" (mode frontoffice).
     *
     * @param ownerId ID de l'utilisateur propriétaire
     * @return événements dont le créateur correspond à ownerId
     */
    public List<Evenement> findByOwner(Integer ownerId) {
        String sql = "SELECT idEvenement, nom, description, jeu, typeEvenement, dateDebut, dateFin, statut, regles, image, created_by_id, created_at "
                + "FROM evenement WHERE created_by_id = ? ORDER BY idEvenement DESC";
        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement evenements owner: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Met à jour un événement existant en base après validation.
     * Ne met pas à jour created_by_id ni created_at (champs immuables).
     *
     * @param evenement l'événement modifié avec son ID intact
     */
    @Override
    public void modifierEntite(Evenement evenement) {
        InputValidator.validateEvenement(evenement);
        String sql = "UPDATE evenement SET nom = ?, description = ?, jeu = ?, typeEvenement = ?, dateDebut = ?, dateFin = ?, statut = ?, regles = ?, image = ? WHERE idEvenement = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, evenement.getNom());
            ps.setString(2, evenement.getDescription());
            ps.setString(3, evenement.getJeu());
            ps.setString(4, evenement.getTypeEvenement());
            ps.setDate(5, Date.valueOf(evenement.getDateDebut()));
            ps.setDate(6, Date.valueOf(evenement.getDateFin()));
            ps.setString(7, evenement.getStatut());
            ps.setString(8, evenement.getRegles());
            ps.setString(9, evenement.getImage());
            ps.setInt(10, evenement.getIdEvenement());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update evenement: " + e.getMessage(), e);
        }
    }

    /**
     * Supprime un événement de la base par son ID.
     *
     * @param evenement l'événement à supprimer
     */
    @Override
    public void supprimerEntite(Evenement evenement) {
        String sql = "DELETE FROM evenement WHERE idEvenement = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenement.getIdEvenement());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression evenement: " + e.getMessage(), e);
        }
    }

    /**
     * Remplace entièrement la liste des équipes participantes à un événement.
     *
     * Opération en deux temps :
     * 1. Supprime toutes les liaisons existantes (DELETE).
     * 2. Réinsère les nouvelles liaisons (INSERT IGNORE pour éviter les doublons).
     *
     * Contrainte métier : le nombre d'équipes doit être pair et >= 2
     * (nécessaire pour générer des matchs en tournoi).
     *
     * @param evenementId ID de l'événement
     * @param equipeIds   liste des IDs d'équipes sélectionnées
     * @throws RuntimeException si nombre d'équipes invalide
     */
    public void replaceEquipesParticipantes(Integer evenementId, List<Integer> equipeIds) {
        List<Integer> cleanedIds = new ArrayList<>();
        if (equipeIds != null) {
            for (Integer id : equipeIds) {
                if (id != null) cleanedIds.add(id);
            }
        }
        if (cleanedIds.size() < 2 || cleanedIds.size() % 2 != 0) {
            throw new RuntimeException("Selectionnez un nombre pair d'equipes (minimum 2).");
        }

        // Suppression de toutes les liaisons précédentes
        try (PreparedStatement deletePs = cnx.prepareStatement("DELETE FROM evenement_equipe WHERE idEvenement = ?")) {
            deletePs.setInt(1, evenementId);
            deletePs.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur reset equipes participantes: " + e.getMessage(), e);
        }

        // Réinsertion en batch des nouvelles équipes
        try (PreparedStatement insertPs = cnx.prepareStatement("INSERT IGNORE INTO evenement_equipe (idEvenement, idEquipe) VALUES (?, ?)")) {
            for (Integer equipeId : cleanedIds) {
                insertPs.setInt(1, evenementId);
                insertPs.setInt(2, equipeId);
                insertPs.addBatch();
            }
            insertPs.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout equipes participantes: " + e.getMessage(), e);
        }
    }

    /**
     * Retourne les IDs des équipes participant à un événement donné.
     *
     * @param evenementId ID de l'événement
     * @return liste des IDs d'équipes liées
     */
    public List<Integer> findEquipesParticipantes(Integer evenementId) {
        String sql = "SELECT idEquipe FROM evenement_equipe WHERE idEvenement = ?";
        List<Integer> equipeIds = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) equipeIds.add(rs.getInt("idEquipe"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lecture equipes participantes: " + e.getMessage(), e);
        }
        return equipeIds;
    }

    /**
     * Retourne l'ID de l'utilisateur créateur d'un événement.
     * Utilisé pour les vérifications de permissions.
     *
     * @param evenementId ID de l'événement
     * @return ID du créateur, ou null si introuvable
     */
    public Integer findOwnerId(Integer evenementId) {
        String sql = "SELECT created_by_id FROM evenement WHERE idEvenement = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("created_by_id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lecture owner evenement: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Retourne un événement par son ID.
     *
     * @param idEvenement ID de l'événement recherché
     * @return l'événement trouvé, ou null
     */
    public Evenement findById(Integer idEvenement) {
        String sql = "SELECT idEvenement, nom, description, jeu, typeEvenement, dateDebut, dateFin, statut, regles, image, created_by_id, created_at FROM evenement WHERE idEvenement = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idEvenement);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur find evenement: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Recherche des événements avec filtrage textuel et tri dynamique.
     *
     * La recherche porte sur : nom, jeu, type, statut (insensible à la casse).
     * Le tri est validé contre une liste blanche de colonnes pour éviter les injections SQL.
     *
     * @param search    texte recherché (vide = tous les résultats)
     * @param sortBy    colonne de tri (validée, défaut : idEvenement)
     * @param sortOrder "ASC" ou "DESC"
     * @return liste filtrée et triée des événements
     */
    public List<Evenement> searchAndSort(String search, String sortBy, String sortOrder) {
        String order = "DESC".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        // Liste blanche des colonnes de tri pour éviter les injections SQL
        String safeSortBy = switch (sortBy) {
            case "nom", "jeu", "typeEvenement", "dateDebut", "dateFin", "statut", "idEvenement" -> sortBy;
            default -> "idEvenement";
        };

        String sql = "SELECT idEvenement, nom, description, jeu, typeEvenement, dateDebut, dateFin, statut, regles, image, created_by_id, created_at "
                + "FROM evenement WHERE (? = '' OR LOWER(nom) LIKE ? OR LOWER(jeu) LIKE ? OR LOWER(typeEvenement) LIKE ? OR LOWER(statut) LIKE ?) "
                + "ORDER BY " + safeSortBy + " " + order;

        List<Evenement> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            String normalized = search == null ? "" : search.trim().toLowerCase();
            String like = "%" + normalized + "%";
            ps.setString(1, normalized);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            ps.setString(5, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recherche evenements: " + e.getMessage(), e);
        }
        return list;
    }

    /**
     * Génère automatiquement un bracket et tous les matchs pour un événement.
     *
     * Algorithme :
     * 1. Vérifie que les équipes participantes sont en nombre pair >= 2.
     * 2. Calcule le nombre de tours : log2(nb_équipes), arrondi au supérieur.
     * 3. Crée le bracket si aucun n'existe encore pour cet événement.
     * 4. Au tour 1 : crée des matchs en tirant les équipes au hasard (shuffle).
     * 5. Pour les tours suivants : crée des matchs "placeholder" (sans équipes assignées).
     *
     * Si des matchs existent déjà, la génération est annulée (idempotent).
     *
     * @param evenementId ID de l'événement
     * @param typeBracket type de bracket ("single elimination" ou "double elimination")
     * @throws RuntimeException si moins de 2 équipes ou nombre impair
     */
    public void generateBracketAndMatches(Integer evenementId, String typeBracket) {
        List<Integer> equipesParticipantes = findEquipesParticipantes(evenementId);
        if (equipesParticipantes.size() < 2) {
            throw new RuntimeException("Selectionnez au moins 2 equipes participantes.");
        }
        if (equipesParticipantes.size() % 2 != 0) {
            throw new RuntimeException("Le nombre d'equipes participantes doit etre pair.");
        }

        // Calcul du nombre de tours nécessaires
        int nombreTours = (int) Math.max(1, Math.ceil(Math.log(equipesParticipantes.size()) / Math.log(2)));

        BracketService bracketService = new BracketService();
        GameMatchService matchService = new GameMatchService();

        // Création du bracket uniquement s'il n'en existe pas déjà un
        Integer bracketId = bracketService.findFirstBracketIdByEvenement(evenementId);
        if (bracketId == null) {
            Bracket bracket = new Bracket();
            bracket.setTypeBracket(typeBracket);
            bracket.setNombreTours(nombreTours);
            bracket.setStatut("en attente");
            bracket.setEvenementId(evenementId);
            bracketService.ajouterEntite(bracket);
            bracketId = bracket.getIdBracket();
        }

        // Protection idempotente : ne régénère pas si des matchs existent déjà
        if (!matchService.findByBracketId(bracketId).isEmpty()) {
            return;
        }

        // Tirage aléatoire des équipes pour le premier tour
        List<Integer> shuffled = new ArrayList<>(equipesParticipantes);
        Collections.shuffle(shuffled);

        int matchesRound1 = (int) Math.ceil(shuffled.size() / 2.0);
        int idx = 0;
        LocalDate baseDate = findById(evenementId).getDateDebut();

        // Création des matchs du premier tour (avec équipes assignées)
        for (int i = 0; i < matchesRound1; i++) {
            GameMatch match = new GameMatch();
            match.setBracketId(bracketId);
            match.setTour(1);
            match.setStatut("à venir");
            match.setScoreEquipeA(0);
            match.setScoreEquipeB(0);
            match.setEquipeAId(idx < shuffled.size() ? shuffled.get(idx) : null);
            match.setEquipeBId((idx + 1) < shuffled.size() ? shuffled.get(idx + 1) : null);
            idx += 2;
            if (baseDate != null) match.setDateMatch(baseDate.atTime(14, 0));
            matchService.ajouterEntite(match);
        }

        // Création des matchs des tours suivants (placeholders sans équipes)
        int prevCount = matchesRound1;
        for (int tour = 2; tour <= nombreTours; tour++) {
            int matchCount = (int) Math.ceil(prevCount / 2.0);
            for (int i = 0; i < matchCount; i++) {
                GameMatch match = new GameMatch();
                match.setBracketId(bracketId);
                match.setTour(tour);
                match.setStatut("à venir");
                match.setScoreEquipeA(0);
                match.setScoreEquipeB(0);
                if (baseDate != null) match.setDateMatch(baseDate.plusDays(tour - 1L).atTime(14, 0));
                matchService.ajouterEntite(match);
            }
            prevCount = matchCount;
        }
    }

    /**
     * Convertit une ligne SQL (ResultSet) en objet {@link Evenement}.
     * Gère proprement les dates nullables.
     *
     * @param rs curseur positionné sur la ligne courante
     * @return objet Evenement peuplé
     */
    private Evenement map(ResultSet rs) throws SQLException {
        Evenement evenement = new Evenement();
        evenement.setIdEvenement(rs.getInt("idEvenement"));
        evenement.setNom(rs.getString("nom"));
        evenement.setDescription(rs.getString("description"));
        evenement.setJeu(rs.getString("jeu"));
        evenement.setTypeEvenement(rs.getString("typeEvenement"));

        Date dateDebut = rs.getDate("dateDebut");
        evenement.setDateDebut(dateDebut == null ? null : dateDebut.toLocalDate());

        Date dateFin = rs.getDate("dateFin");
        evenement.setDateFin(dateFin == null ? null : dateFin.toLocalDate());

        evenement.setStatut(rs.getString("statut"));
        evenement.setRegles(rs.getString("regles"));
        evenement.setImage(rs.getString("image"));
        evenement.setCreatedById(rs.getInt("created_by_id"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        evenement.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());

        return evenement;
    }
}
