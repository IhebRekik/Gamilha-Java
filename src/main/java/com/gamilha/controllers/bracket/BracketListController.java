package com.gamilha.controllers.bracket;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.Bracket;
import com.gamilha.services.BracketService;
import com.gamilha.utils.SessionContext;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Card grid listing all brackets.
 */
public class BracketListController extends BaseController {

    private final BracketService bracketService = new BracketService();

    private NavigationCallback nav;
    private BracketFormController formController;

    public void setNav(NavigationCallback nav) { this.nav = nav; }
    public void setFormController(BracketFormController fc) { this.formController = fc; }

    // â”€â”€â”€ Build page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Node build() {
        VBox root = pageScaffold("🥇 Liste des 🥇 Brackets", "");
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();

        Button add = new Button("Ajouter Bracket");
        Button refresh = new Button("Actualiser");

        TilePane grid = new TilePane();
        grid.getStyleClass().add("card-grid");
        grid.setPrefColumns(3);

        final Runnable[] fillRef = new Runnable[1];
        fillRef[0] = () -> {
            grid.getChildren().clear();
            List<Bracket> data = bracketService.findAll();
            for (Bracket b : data) {
                VBox card = entityCard(
                        nullSafe(b.getTypeBracket()),
                        "Tours: " + str(b.getNombreTours()),
                        "Statut: " + nullSafe(b.getStatut()),
                        "Evenement: " + nullSafe(b.getEvenementNom()));

                Button edit = new Button("Modifier");
                Button del = new Button("Supprimer");

                edit.setOnAction(ev -> {
                    if (formController != null) formController.prepareForEdit(b);
                    nav.navigateTo("brackets_form");
                });
                del.setOnAction(ev -> {
                    try {
                        bracketService.supprimerEntite(b);
                        info("Bracket supprime.");
                        fillRef[0].run();
                    } catch (Exception ex) { error("Suppression impossible: " + ex.getMessage()); }
                });

                if (admin || canEditBracket(admin, b)) {
                    HBox actions = new HBox(8, edit, del);
                    actions.getStyleClass().add("card-actions");
                    card.getChildren().add(actions);
                }
                grid.getChildren().add(card);
            }
        };

        add.setOnAction(e -> { if (formController != null) formController.prepareForCreate(); nav.navigateTo("brackets_form"); });
        refresh.setOnAction(e -> fillRef[0].run());
        fillRef[0].run();

        root.getChildren().addAll(new HBox(8, add, refresh), pageScroller(grid));
        return root;
    }
}

