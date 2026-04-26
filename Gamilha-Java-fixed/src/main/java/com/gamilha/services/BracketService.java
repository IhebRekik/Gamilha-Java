package com.gamilha.services;

import com.gamilha.entity.Bracket;
import com.gamilha.tools.DBconnexion;
import com.gamilha.validation.InputValidator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BracketService implements ICrud<Bracket> {
    private final Connection cnx = DBconnexion.getInstance().getCnx();

    @Override
    public void ajouterEntite(Bracket bracket) {
        InputValidator.validateBracket(bracket);
        String sql = "INSERT INTO bracket (type_bracket, nombreTours, statut, evenement_id) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, bracket.getTypeBracket());
            ps.setInt(2, bracket.getNombreTours());
            ps.setString(3, bracket.getStatut());
            ps.setInt(4, bracket.getEvenementId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    bracket.setIdBracket(keys.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout bracket: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Bracket> afficherEntite() {
        return findAll();
    }

    public List<Bracket> findAll() {
        String sql = "SELECT b.id_bracket, b.type_bracket, b.nombreTours, b.statut, b.evenement_id, e.nom AS evenement_nom "
                +
                "FROM bracket b LEFT JOIN evenement e ON e.idEvenement = b.evenement_id ORDER BY b.id_bracket DESC";

        List<Bracket> list = new ArrayList<>();

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement brackets: " + e.getMessage(), e);
        }

        return list;
    }

    public List<Bracket> findByEvenementOwner(Integer ownerId) {
        String sql = "SELECT b.id_bracket, b.type_bracket, b.nombreTours, b.statut, b.evenement_id, e.nom AS evenement_nom "
                +
                "FROM bracket b INNER JOIN evenement e ON e.idEvenement = b.evenement_id " +
                "WHERE e.created_by_id = ? ORDER BY b.id_bracket DESC";

        List<Bracket> list = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement brackets owner: " + e.getMessage(), e);
        }

        return list;
    }

    @Override
    public void modifierEntite(Bracket bracket) {
        InputValidator.validateBracket(bracket);
        String sql = "UPDATE bracket SET type_bracket = ?, nombreTours = ?, statut = ?, evenement_id = ? WHERE id_bracket = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, bracket.getTypeBracket());
            ps.setInt(2, bracket.getNombreTours());
            ps.setString(3, bracket.getStatut());
            ps.setInt(4, bracket.getEvenementId());
            ps.setInt(5, bracket.getIdBracket());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update bracket: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimerEntite(Bracket bracket) {
        String sql = "DELETE FROM bracket WHERE id_bracket = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, bracket.getIdBracket());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression bracket: " + e.getMessage(), e);
        }
    }

    private Bracket map(ResultSet rs) throws SQLException {
        Bracket bracket = new Bracket();
        bracket.setIdBracket(rs.getInt("id_bracket"));
        bracket.setTypeBracket(rs.getString("type_bracket"));
        bracket.setNombreTours(rs.getInt("nombreTours"));
        bracket.setStatut(rs.getString("statut"));
        bracket.setEvenementId(rs.getInt("evenement_id"));
        bracket.setEvenementNom(rs.getString("evenement_nom"));
        return bracket;
    }

    public Integer findFirstBracketIdByEvenement(Integer evenementId) {
        String sql = "SELECT id_bracket FROM bracket WHERE evenement_id = ? ORDER BY id_bracket ASC LIMIT 1";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_bracket");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur bracket evenement: " + e.getMessage(), e);
        }

        return null;
    }

    public Integer findEvenementOwnerId(Integer bracketId) {
        String sql = "SELECT e.created_by_id FROM bracket b INNER JOIN evenement e ON e.idEvenement = b.evenement_id WHERE b.id_bracket = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, bracketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("created_by_id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur owner bracket: " + e.getMessage(), e);
        }

        return null;
    }
}

