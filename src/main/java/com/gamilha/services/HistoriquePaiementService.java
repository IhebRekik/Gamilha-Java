package com.gamilha.services;

import com.gamilha.entity.Abonnement;
import com.gamilha.entity.HistoriquePaiement;
import com.gamilha.entity.User;
import com.gamilha.services.AbonnementServices;
import com.gamilha.services.UserService;
import com.gamilha.utils.ConnectionManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HistoriquePaiementService {

    public List<HistoriquePaiement> getPaginatedByUser( int page, int size) {

        List<HistoriquePaiement> list = new ArrayList<>();

        int offset = (page - 1) * size;

        String query = """
            SELECT * FROM historique_paiement
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;

        try (Connection conn = ConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, size);
            ps.setInt(2, offset);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int Id = rs.getInt("user_id");
                int abonnementId = rs.getInt("abonnement_id");
                UserService userService = new UserService();
                User user = userService.findById(Id);
                Abonnement abonnement = AbonnementServices.getAbonnementById(abonnementId);

                list.add(new HistoriquePaiement(
                        rs.getInt("id"),
                        user,
                        abonnement,
                        rs.getFloat("montant"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}