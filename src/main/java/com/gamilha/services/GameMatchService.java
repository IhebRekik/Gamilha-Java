package com.gamilha.services;

import com.gamilha.entity.GameMatch;

import com.gamilha.utils.ConnectionManager;
import com.gamilha.validation.InputValidator;

import java.sql.*;

import java.util.ArrayList;
import java.util.List;

public class GameMatchService implements ICrud<GameMatch> {

    private final Connection cnx = ConnectionManager.getConnection();


    @Override
    public void ajouterEntite(GameMatch match) {
        InputValidator.validateMatch(match);

        String sql = "INSERT INTO `match` (dateMatch, tour, scoreEquipeA, scoreEquipeB, statut, equipea_id, equipeb_id, bracket_id) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (match.getDateMatch() != null) {
                ps.setTimestamp(1, Timestamp.valueOf(match.getDateMatch()));
            } else {
                ps.setTimestamp(1, null);
            }
            ps.setInt(2, match.getTour());
            ps.setInt(3, match.getScoreEquipeA());
            ps.setInt(4, match.getScoreEquipeB());
            ps.setString(5, match.getStatut());

            if (match.getEquipeAId() != null) {
                ps.setInt(6, match.getEquipeAId());
            } else {
                ps.setObject(6, null);
            }

            if (match.getEquipeBId() != null) {
                ps.setInt(7, match.getEquipeBId());
            } else {
                ps.setObject(7, null);
            }

            ps.setInt(8, match.getBracketId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    match.setIdMatch(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout match: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GameMatch> afficherEntite() {
        return findAll();
    }

    public List<GameMatch> findAll() {
        String sql = "SELECT m.idMatch, m.dateMatch, m.tour, m.scoreEquipeA, m.scoreEquipeB, m.statut, " +
                "m.equipea_id, ea.nomEquipe AS equipeA_nom, m.equipeb_id, eb.nomEquipe AS equipeB_nom, m.bracket_id, " +
                "CONCAT(b.type_bracket, ' - ', IFNULL(e.nom, '')) AS bracket_display " +
                "FROM `match` m " +
                "LEFT JOIN equipe ea ON ea.idEquipe = m.equipea_id " +
                "LEFT JOIN equipe eb ON eb.idEquipe = m.equipeb_id " +
                "LEFT JOIN bracket b ON b.id_bracket = m.bracket_id " +
                "LEFT JOIN evenement e ON e.idEvenement = b.evenement_id " +
                "ORDER BY m.idMatch DESC";

        List<GameMatch> list = new ArrayList<>();

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement matchs: " + e.getMessage(), e);
        }

        return list;
    }

    public List<GameMatch> findByOwner(Integer ownerId) {
        String sql = "SELECT m.idMatch, m.dateMatch, m.tour, m.scoreEquipeA, m.scoreEquipeB, m.statut, " +
                "m.equipea_id, ea.nomEquipe AS equipeA_nom, m.equipeb_id, eb.nomEquipe AS equipeB_nom, m.bracket_id, " +
                "CONCAT(b.type_bracket, ' - ', IFNULL(e.nom, '')) AS bracket_display " +
                "FROM `match` m " +
                "LEFT JOIN equipe ea ON ea.idEquipe = m.equipea_id " +
                "LEFT JOIN equipe eb ON eb.idEquipe = m.equipeb_id " +
                "INNER JOIN bracket b ON b.id_bracket = m.bracket_id " +
                "INNER JOIN evenement e ON e.idEvenement = b.evenement_id " +
                "WHERE e.created_by_id = ? ORDER BY m.idMatch DESC";

        List<GameMatch> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement matchs owner: " + e.getMessage(), e);
        }

        return list;
    }

    public List<GameMatch> findByBracketId(Integer bracketId) {
        String sql = "SELECT m.idMatch, m.dateMatch, m.tour, m.scoreEquipeA, m.scoreEquipeB, m.statut, " +
                "m.equipea_id, ea.nomEquipe AS equipeA_nom, m.equipeb_id, eb.nomEquipe AS equipeB_nom, m.bracket_id, " +
                "CONCAT(b.type_bracket, ' - ', IFNULL(e.nom, '')) AS bracket_display " +
                "FROM `match` m " +
                "LEFT JOIN equipe ea ON ea.idEquipe = m.equipea_id " +
                "LEFT JOIN equipe eb ON eb.idEquipe = m.equipeb_id " +
                "LEFT JOIN bracket b ON b.id_bracket = m.bracket_id " +
                "LEFT JOIN evenement e ON e.idEvenement = b.evenement_id " +
                "WHERE m.bracket_id = ? ORDER BY m.tour ASC, m.idMatch ASC";

        List<GameMatch> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, bracketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur matchs bracket: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public void modifierEntite(GameMatch match) {
        InputValidator.validateMatch(match);

        String sql = "UPDATE `match` SET dateMatch = ?, tour = ?, scoreEquipeA = ?, scoreEquipeB = ?, statut = ?, " +
                "equipea_id = ?, equipeb_id = ?, bracket_id = ? WHERE idMatch = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            if (match.getDateMatch() != null) {
                ps.setTimestamp(1, Timestamp.valueOf(match.getDateMatch()));
            } else {
                ps.setTimestamp(1, null);
            }
            ps.setInt(2, match.getTour());
            ps.setInt(3, match.getScoreEquipeA());
            ps.setInt(4, match.getScoreEquipeB());
            ps.setString(5, match.getStatut());

            if (match.getEquipeAId() != null) {
                ps.setInt(6, match.getEquipeAId());
            } else {
                ps.setObject(6, null);
            }

            if (match.getEquipeBId() != null) {
                ps.setInt(7, match.getEquipeBId());
            } else {
                ps.setObject(7, null);
            }

            ps.setInt(8, match.getBracketId());
            ps.setInt(9, match.getIdMatch());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update match: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimerEntite(GameMatch match) {
        String sql = "DELETE FROM `match` WHERE idMatch = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, match.getIdMatch());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression match: " + e.getMessage(), e);
        }
    }

    private GameMatch map(ResultSet rs) throws SQLException {
        GameMatch match = new GameMatch();
        match.setIdMatch(rs.getInt("idMatch"));

        Timestamp ts = rs.getTimestamp("dateMatch");
        match.setDateMatch(ts == null ? null : ts.toLocalDateTime());

        match.setTour(rs.getInt("tour"));
        match.setScoreEquipeA(rs.getInt("scoreEquipeA"));
        match.setScoreEquipeB(rs.getInt("scoreEquipeB"));
        match.setStatut(rs.getString("statut"));

        int equipeA = rs.getInt("equipea_id");
        if (!rs.wasNull()) {
            match.setEquipeAId(equipeA);
        }
        match.setEquipeANom(rs.getString("equipeA_nom"));

        int equipeB = rs.getInt("equipeb_id");
        if (!rs.wasNull()) {
            match.setEquipeBId(equipeB);
        }
        match.setEquipeBNom(rs.getString("equipeB_nom"));

        match.setBracketId(rs.getInt("bracket_id"));
        match.setBracketDisplay(rs.getString("bracket_display"));

        return match;
    }
}

