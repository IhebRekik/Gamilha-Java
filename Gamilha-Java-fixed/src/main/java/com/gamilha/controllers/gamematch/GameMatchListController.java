package com.gamilha.controllers.gamematch;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.GameMatch;
import com.gamilha.services.GameMatchService;
import com.gamilha.utils.SessionContext;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Card grid listing all game matches.
 */
public class GameMatchListController extends BaseController {

    private final GameMatchService matchService = new GameMatchService();

    private NavigationCallback nav;
    private GameMatchFormController formController;

    public void setNav(NavigationCallback nav) { this.nav = nav; }
    public void setFormController(GameMatchFormController fc) { this.formController = fc; }

    // â”€â”€â”€ Build page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Node build() {
        VBox root = pageScaffold("Liste des Matchs", "");
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();

        Button add = new Button("Ajouter Match");
        Button refresh = new Button("Actualiser");
        HBox toolbar = new HBox(8, add, refresh);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setAlignment(Pos.CENTER);

        TilePane grid = new TilePane();
        grid.getStyleClass().add("card-grid");
        grid.setPrefColumns(3);

        final Runnable[] fillRef = new Runnable[1];
        fillRef[0] = () -> {
            grid.getChildren().clear();
            List<GameMatch> data = matchService.findAll();
            for (GameMatch m : data) {
                VBox card = entityCard(
                        nullSafe(m.getBracketDisplay()),
                        "Tour: " + str(m.getTour()),
                        "A: " + nullSafe(m.getEquipeANom()) + " | B: " + nullSafe(m.getEquipeBNom()),
                        "Score: " + str(m.getScoreEquipeA()) + " - " + str(m.getScoreEquipeB()),
                        "Date: " + formatDateTime(m.getDateMatch()),
                        "Statut: " + nullSafe(m.getStatut()));
                card.getStyleClass().add("match-card");

                Button edit = new Button("Modifier");
                Button del = new Button("Supprimer");

                edit.setOnAction(ev -> {
                    if (formController != null) formController.prepareForEdit(m);
                    nav.navigateTo("matchs_form");
                });
                del.setOnAction(ev -> {
                    try {
                        matchService.supprimerEntite(m);
                        info("Match supprime.");
                        fillRef[0].run();
                    } catch (Exception ex) { error("Suppression impossible: " + ex.getMessage()); }
                });

                if (admin || canEditMatch(m)) {
                    HBox actions = new HBox(8, edit, del);
                    actions.getStyleClass().add("card-actions");
                    card.getChildren().add(actions);
                }
                grid.getChildren().add(card);
            }
        };

        add.setOnAction(e -> { if (formController != null) formController.prepareForCreate(); nav.navigateTo("matchs_form"); });
        refresh.setOnAction(e -> fillRef[0].run());
        fillRef[0].run();

        root.getChildren().addAll(toolbar, pageScroller(grid));
        return root;
    }
}

