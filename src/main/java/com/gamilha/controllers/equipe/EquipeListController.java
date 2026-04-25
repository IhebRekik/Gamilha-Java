package com.gamilha.controllers.equipe;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.Equipe;
import com.gamilha.entity.User;
import com.gamilha.services.EquipeService;
import com.gamilha.services.UserLookupService;
import com.gamilha.utils.SessionContext;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

/**
 * Card grid listing all equipes.
 */
public class EquipeListController extends BaseController {

    private final EquipeService equipeService = new EquipeService();
    private final UserLookupService userLookupService = new UserLookupService();

    private NavigationCallback nav;
    private EquipeFormController formController;

    public void setNav(NavigationCallback nav) { this.nav = nav; }
    public void setFormController(EquipeFormController fc) { this.formController = fc; }

    public Node build() {
        VBox root = pageScaffold("Liste des Equipes", "");
        User currentUser = SessionContext.getCurrentUser();
        boolean admin = currentUser != null && currentUser.isAdmin();

        Button add = new Button("Ajouter Equipe");
        Button refresh = new Button("Actualiser");
        Button allBtn = new Button("Tous");
        Button mineBtn = new Button("Mes Equipes");

        Button calendarBtn = new Button("Calendrier participations");

        final boolean[] onlyMine = {false};

        allBtn.setOnAction(e -> { onlyMine[0] = false; setActiveBtn(allBtn, mineBtn, false); refresh.fire(); });
        mineBtn.setOnAction(e -> { onlyMine[0] = true; setActiveBtn(allBtn, mineBtn, true); refresh.fire(); });

        calendarBtn.setOnAction(e -> nav.navigateTo("equipes_calendar"));
        add.setOnAction(e -> { if (formController != null) formController.prepareForCreate(); nav.navigateTo("equipes_form"); });

        setActiveBtn(allBtn, mineBtn, false);

        TilePane grid = new TilePane();
        grid.getStyleClass().add("card-grid");
        grid.setPrefColumns(3);

        final Runnable[] fillRef = new Runnable[1];
        fillRef[0] = () -> {
            grid.getChildren().clear();
            List<Equipe> data = onlyMine[0] && currentUser != null
                    ? equipeService.findByOwner(currentUser.getId())
                    : equipeService.findAll();

            for (Equipe e : data) {
                VBox card = entityCard(e.getNomEquipe(),
                        "Tag: " + nullSafe(e.getTag()),
                        "Pays: " + nullSafe(e.getPays()),
                        "Niveau: " + nullSafe(e.getNiveau()));
                card.getStyleClass().add("team-card");
                card.setOnMouseClicked(ev -> nav.navigateTo("equipes_details:" + e.getIdEquipe()));

                Node logo = createTeamLogoNode(e.getLogo());
                if (logo != null) card.getChildren().add(0, logo);

                Button edit = new Button("Modifier");
                Button del = new Button("Supprimer");
                Button details = new Button("Details");

                edit.setOnAction(ev -> {
                    if (formController != null) formController.prepareForEdit(e);
                    nav.navigateTo("equipes_form");
                });
                del.setOnAction(ev -> {
                    try {
                        equipeService.supprimerEntite(e);
                        info("Equipe supprimee.");
                        fillRef[0].run();
                    } catch (Exception ex) { error("Suppression impossible: " + ex.getMessage()); }
                });
                details.setOnAction(ev -> nav.navigateTo("equipes_details:" + e.getIdEquipe()));

                HBox actions = new HBox(8, details);
                if (admin || canEditEquipe(admin, e)) actions.getChildren().addAll(edit, del);
                actions.getStyleClass().add("card-actions");
                card.getChildren().add(actions);
                grid.getChildren().add(card);
            }
        };

        refresh.setOnAction(e -> fillRef[0].run());
        fillRef[0].run();


        root.getChildren().addAll(new HBox(8, add, refresh, allBtn, mineBtn, calendarBtn), pageScroller(grid));
        return root;
    }


    public Node buildDetailsPage(Equipe equipe) {
        VBox root = detailsPageScaffold("Equipe: " + nullSafe(equipe.getNomEquipe()), "");
        Button back = new Button("Retour");
        back.setMaxWidth(Double.MAX_VALUE);
        back.setOnAction(e -> nav.navigateTo("equipes_list"));

        VBox meta = new VBox(4,
                detailLine("Nom", equipe.getNomEquipe()),
                detailLine("Tag", equipe.getTag()),
                detailLine("Pays", equipe.getPays()),
                detailLine("Niveau", equipe.getNiveau()),
                detailLine("Date creation", formatDate(equipe.getDateCreation())));
        meta.getStyleClass().add("details-section");

        Map<Integer, String> users = userLookupService.findAllUsers();
        List<Integer> memberIds = equipeService.findMemberIds(equipe.getIdEquipe());
        Label membersTitle = new Label("Membres");
        membersTitle.getStyleClass().add("entity-card-title");
        VBox membersInner = new VBox(4);
        if (memberIds.isEmpty()) {
            Label none = new Label("Aucun membre.");
            none.getStyleClass().add("entity-card-line");
            membersInner.getChildren().add(none);
        } else {
            memberIds.forEach(id -> {
                Label m = new Label("- " + nullSafe(users.get(id)));
                m.getStyleClass().add("entity-card-line");
                m.setWrapText(true);
                membersInner.getChildren().add(m);
            });
        }
        ScrollPane membersScroll = new ScrollPane(membersInner);
        membersScroll.getStyleClass().add("details-members-scroll");
        membersScroll.setFitToWidth(true);
        membersScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox members = new VBox(6, membersTitle, membersScroll);
        members.getStyleClass().add("details-section");

        Node logo = createTeamLogoDetailsNode(equipe.getLogo());
        root.getChildren().add(back);
        if (logo != null) root.getChildren().add(logo);
        root.getChildren().addAll(meta, members);
        return pageScroller(root);
    }


    private void setActiveBtn(Button a, Button b, boolean mineActive) {
        a.getStyleClass().remove("nav-item-active");
        b.getStyleClass().remove("nav-item-active");
        (mineActive ? b : a).getStyleClass().add("nav-item-active");
    }

    private Node createTeamLogoNode(String url) {
        return imageCoverInFrame(url, 120, 120, "team-logo-frame");
    }


    /** Logo sur la fiche dÃ©tail (cadre un peu plus petit). */

    private Node createTeamLogoDetailsNode(String url) {
        return imageCoverInFrame(url, 96, 96, "team-logo-frame");
    }
}

