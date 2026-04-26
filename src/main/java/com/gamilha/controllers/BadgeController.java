package com.gamilha.controllers;

import com.gamilha.services.BadgeService;
import com.gamilha.services.BadgeService.*;
import com.gamilha.utils.SessionContext;
import com.gamilha.utils.AlertUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * BadgeController — Tableau de bord personnel Badges & Récompenses.
 * Données calculées à partir des tables stream et donation existantes.
 */
public class BadgeController implements Initializable {

    // KPI
    @FXML private Label lblStreams, lblDonated, lblReceived, lblViewers;
    @FXML private Label lblLevel, lblXP, lblRank, lblBadgeCount;
    @FXML private Label lblUsername;
    @FXML private ProgressBar xpBar;

    // Grilles
    @FXML private FlowPane unlockedGrid;
    @FXML private FlowPane lockedGrid;

    private final BadgeService svc = new BadgeService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        var user = SessionContext.getCurrentUser();
        if (user != null) {
            lblUsername.setText(user.getName());
            loadAsync(user.getId());
        }
    }

    private void loadAsync(int userId) {
        new Thread(() -> {
            try {
                UserStats stats     = svc.getStats(userId);
                List<BadgeResult> badges = svc.computeBadges(stats);
                int level = svc.computeLevel(badges);
                int xp    = svc.computeXP(badges);
                long unlocked = badges.stream().filter(b -> b.unlocked()).count();

                Platform.runLater(() -> {
                    // KPIs
                    lblStreams.setText(String.valueOf(stats.streamCount));
                    lblDonated.setText(stats.donationsGiven + " (" + String.format("%.0f€", stats.totalAmountGiven) + ")");
                    lblReceived.setText(stats.donationsReceived + " reçues");
                    lblViewers.setText(String.valueOf(stats.totalViewers));
                    lblLevel.setText("Niveau " + level);
                    lblXP.setText(xp + " XP");
                    lblRank.setText("#" + stats.rank);
                    lblBadgeCount.setText(unlocked + " / " + BadgeService.ALL_BADGES.size());

                    // Barre XP (max 350 XP pour niveau max)
                    xpBar.setProgress(Math.min(1.0, xp / 350.0));

                    // Grilles badges
                    buildBadgeGrids(badges);
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                    AlertUtil.showError("Erreur", ex.getMessage()));
            }
        }).start();
    }

    private void buildBadgeGrids(List<BadgeResult> badges) {
        unlockedGrid.getChildren().clear();
        lockedGrid.getChildren().clear();
        for (BadgeResult b : badges) {
            if (b.unlocked()) unlockedGrid.getChildren().add(buildCard(b));
            else              lockedGrid.getChildren().add(buildCard(b));
        }
    }

    private VBox buildCard(BadgeResult b) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(165);
        card.setPrefHeight(160);

        String rarityColor = switch (b.def().rarity()) {
            case "silver"  -> "#94a3b8";
            case "gold"    -> "#fbbf24";
            case "diamond" -> "#67e8f9";
            default        -> "#cd7c2f"; // bronze
        };
        String borderColor = b.unlocked() ? rarityColor : "#1f2937";
        String bgColor     = b.unlocked() ? "#111827" : "#0d1117";
        String opacity     = b.unlocked() ? "1.0" : "0.45";

        card.setStyle(
            "-fx-background-color:" + bgColor + ";" +
            "-fx-border-color:" + borderColor + ";" +
            "-fx-border-radius:12;-fx-background-radius:12;-fx-padding:14;" +
            "-fx-opacity:" + opacity + ";" +
            (b.unlocked() ? "-fx-effect:dropshadow(gaussian," + rarityColor + ",10,0.2,0,2);" : "")
        );

        // Emoji + rareté
        String icon = b.unlocked()
            ? b.def().name().substring(0, b.def().name().indexOf(' '))
            : "🔒";
        Label lblIcon = new Label(icon);
        lblIcon.setStyle("-fx-font-size:32px;");

        // Nom
        Label lblName = new Label(b.def().name().substring(b.def().name().indexOf(' ') + 1));
        lblName.setStyle("-fx-font-size:12px;-fx-font-weight:bold;" +
            "-fx-text-fill:" + (b.unlocked() ? "#f9fafb" : "#6b7280") + ";");
        lblName.setWrapText(true);
        lblName.setAlignment(Pos.CENTER);

        // Description
        Label lblDesc = new Label(b.def().description());
        lblDesc.setStyle("-fx-font-size:10px;-fx-text-fill:#4b5563;");
        lblDesc.setWrapText(true);
        lblDesc.setAlignment(Pos.CENTER);

        if (b.unlocked()) {
            // Badge rareté
            Label lblRarity = new Label(b.def().rarity().toUpperCase());
            lblRarity.setStyle("-fx-font-size:9px;-fx-font-weight:bold;-fx-text-fill:" + rarityColor + ";");
            card.getChildren().addAll(lblIcon, lblName, lblDesc, lblRarity);
        } else {
            // Barre de progression
            ProgressBar pb = new ProgressBar(b.progress() / 100.0);
            pb.setPrefWidth(130);
            pb.setStyle("-fx-accent:#8b5cf6;");
            Label lblProg = new Label(b.current() + " / " + b.def().threshold());
            lblProg.setStyle("-fx-font-size:10px;-fx-text-fill:#4b5563;");
            card.getChildren().addAll(lblIcon, lblName, lblDesc, pb, lblProg);
        }

        return card;
    }

    @FXML private void onRefresh(ActionEvent e) {
        var user = SessionContext.getCurrentUser();
        if (user != null) {
            unlockedGrid.getChildren().clear();
            lockedGrid.getChildren().clear();
            loadAsync(user.getId());
        }
    }
}
