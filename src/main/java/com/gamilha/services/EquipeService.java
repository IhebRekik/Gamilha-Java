package com.gamilha.services;

import com.gamilha.entity.Equipe;
import com.gamilha.tools.DBconnexion;
import com.gamilha.validation.InputValidator;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EquipeService implements ICrud<Equipe> {
    private final Connection cnx = DBconnexion.getInstance().getCnx();

    @Override
    public void ajouterEntite(Equipe equipe) {
        InputValidator.validateEquipe(equipe);
        String sql = "INSERT INTO equipe (nomEquipe, tag, logo, pays, dateCreation, niveau, owner_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, equipe.getNomEquipe());
            ps.setString(2, equipe.getTag());
            ps.setString(3, equipe.getLogo());
            ps.setString(4, equipe.getPays());
            if (equipe.getDateCreation() != null) {
                ps.setDate(5, Date.valueOf(equipe.getDateCreation()));
            } else {
                ps.setDate(5, null);
            }
            ps.setString(6, equipe.getNiveau());
            if (equipe.getOwnerId() != null) {
                ps.setInt(7, equipe.getOwnerId());
            } else {
                ps.setObject(7, null);
            }

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    equipe.setIdEquipe(keys.getInt(1));
                }
            }

            if (equipe.getOwnerId() != null && equipe.getIdEquipe() != null) {
                addMember(equipe.getIdEquipe(), equipe.getOwnerId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout equipe: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Equipe> afficherEntite() {
        return findAll();
    }

    public List<Equipe> findAll() {
        String sql = "SELECT idEquipe, nomEquipe, tag, logo, pays, dateCreation, niveau, owner_id FROM equipe ORDER BY idEquipe DESC";
        List<Equipe> equipes = new ArrayList<>();

        try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                equipes.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement equipes: " + e.getMessage(), e);
        }

        return equipes;
    }

    public List<Equipe> findByOwner(Integer ownerId) {
        String sql = "SELECT idEquipe, nomEquipe, tag, logo, pays, dateCreation, niveau, owner_id FROM equipe WHERE owner_id = ? ORDER BY idEquipe DESC";
        List<Equipe> equipes = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipes.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement equipes proprietaire: " + e.getMessage(), e);
        }

        return equipes;
    }

    @Override
    public void modifierEntite(Equipe equipe) {
        InputValidator.validateEquipe(equipe);
        String sql = "UPDATE equipe SET nomEquipe = ?, tag = ?, logo = ?, pays = ?, dateCreation = ?, niveau = ?, owner_id = ? WHERE idEquipe = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, equipe.getNomEquipe());
            ps.setString(2, equipe.getTag());
            ps.setString(3, equipe.getLogo());
            ps.setString(4, equipe.getPays());
            if (equipe.getDateCreation() != null) {
                ps.setDate(5, Date.valueOf(equipe.getDateCreation()));
            } else {
                ps.setDate(5, null);
            }
            ps.setString(6, equipe.getNiveau());
            if (equipe.getOwnerId() != null) {
                ps.setInt(7, equipe.getOwnerId());
            } else {
                ps.setObject(7, null);
            }
            ps.setInt(8, equipe.getIdEquipe());
            ps.executeUpdate();

            if (equipe.getOwnerId() != null) {
                addMember(equipe.getIdEquipe(), equipe.getOwnerId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur update equipe: " + e.getMessage(), e);
        }
    }

    @Override
    public void supprimerEntite(Equipe equipe) {
        String sql = "DELETE FROM equipe WHERE idEquipe = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, equipe.getIdEquipe());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression equipe: " + e.getMessage(), e);
        }
    }

    public List<Integer> findMemberIds(Integer equipeId) {
        String sql = "SELECT user_id FROM equipe_user WHERE equipe_id = ?";
        List<Integer> members = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    members.add(rs.getInt("user_id"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur chargement membres: " + e.getMessage(), e);
        }

        return members;
    }

    public void replaceMembers(Integer equipeId, List<Integer> userIds) {
        String deleteSql = "DELETE FROM equipe_user WHERE equipe_id = ?";

        try (PreparedStatement deletePs = cnx.prepareStatement(deleteSql)) {
            deletePs.setInt(1, equipeId);
            deletePs.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur reset membres: " + e.getMessage(), e);
        }

        if (userIds == null) {
            return;
        }

        for (Integer userId : userIds) {
            if (userId != null) {
                addMember(equipeId, userId);
            }
        }
    }

    private void addMember(Integer equipeId, Integer userId) {
        String insertSql = "INSERT IGNORE INTO equipe_user (equipe_id, user_id) VALUES (?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(insertSql)) {
            ps.setInt(1, equipeId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout membre equipe: " + e.getMessage(), e);
        }
    }

    private Equipe map(ResultSet rs) throws SQLException {
        Equipe equipe = new Equipe();
        equipe.setIdEquipe(rs.getInt("idEquipe"));
        equipe.setNomEquipe(rs.getString("nomEquipe"));
        equipe.setTag(rs.getString("tag"));
        equipe.setLogo(rs.getString("logo"));
        equipe.setPays(rs.getString("pays"));

        Date dateCreation = rs.getDate("dateCreation");
        equipe.setDateCreation(dateCreation == null ? null : dateCreation.toLocalDate());

        equipe.setNiveau(rs.getString("niveau"));

        int ownerId = rs.getInt("owner_id");
        if (!rs.wasNull()) {
            equipe.setOwnerId(ownerId);
        }

        return equipe;
    }

    public Integer findOwnerId(Integer equipeId) {
        String sql = "SELECT owner_id FROM equipe WHERE idEquipe = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, equipeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int ownerId = rs.getInt("owner_id");
                    return rs.wasNull() ? null : ownerId;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lecture owner equipe: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Equipe> findParticipatingInEvenement(Integer evenementId) {
        String sql = "SELECT e.idEquipe, e.nomEquipe, e.tag, e.logo, e.pays, e.dateCreation, e.niveau, e.owner_id " +
                "FROM equipe e INNER JOIN evenement_equipe ee ON ee.idEquipe = e.idEquipe " +
                "WHERE ee.idEvenement = ? ORDER BY e.nomEquipe ASC";

        List<Equipe> equipes = new ArrayList<>();

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, evenementId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipes.add(map(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur equipes participantes: " + e.getMessage(), e);
        }

        return equipes;
    }
}

