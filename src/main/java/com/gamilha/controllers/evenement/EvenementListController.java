package com.gamilha.controllers.evenement;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.User;
import com.gamilha.entity.Bracket;
import com.gamilha.entity.Evenement;
import com.gamilha.entity.GameMatch;
import com.gamilha.services.BracketService;
import com.gamilha.services.EvenementService;
import com.gamilha.services.GameMatchService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays the card grid of all evenements with search / filter.
 */
public class EvenementListController extends BaseController {

    private final EvenementService evenementService = new EvenementService();
    private final BracketService bracketService = new BracketService();
    private final GameMatchService matchService = new GameMatchService();

    private NavigationCallback nav;
    private EvenementFormController formController;

    public void setNav(NavigationCallback nav) { this.nav = nav; }
    public void setFormController(EvenementFormController formController) { this.formController = formController; }

    // â”€â”€â”€ Build page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Node build() {
        VBox root = pageScaffold("Liste des Evenements", "");
        User currentUser = SessionContext.getCurrentUser();
        boolean admin = currentUser != null && currentUser.isAdmin();

        TextField search = new TextField();
        search.setPromptText("Rechercher par nom, jeu, type, statut");
        search.getStyleClass().add("list-search");
        search.setMaxWidth(420);

        ComboBox<String> filterType = new ComboBox<>(
                FXCollections.observableArrayList("Tous types", "online", "offline"));
        filterType.setValue("Tous types");

        ComboBox<String> filterStatut = new ComboBox<>(
                FXCollections.observableArrayList("Tous statuts", "prévu", "en cours", "terminé"));
        filterStatut.setValue("Tous statuts");

        Button refresh = new Button("Actualiser");
        Button add = new Button("Ajouter Evenement");
        Button allBtn = new Button("Tous");
        Button mineBtn = new Button("Mes Evenements");
        final boolean[] onlyMine = {false};

        allBtn.setOnAction(e -> { onlyMine[0] = false; setActiveBtn(allBtn, mineBtn, false); refresh.fire(); });
        mineBtn.setOnAction(e -> { onlyMine[0] = true; setActiveBtn(allBtn, mineBtn, true); refresh.fire(); });
        add.setOnAction(e -> { if (formController != null) formController.prepareForCreate(); nav.navigateTo("evenements_form"); });

        HBox toolbar = new HBox(8, search, filterType, filterStatut, refresh, allBtn, mineBtn, add);
        toolbar.getStyleClass().add("list-toolbar");
        toolbar.setAlignment(Pos.CENTER);

        setActiveBtn(allBtn, mineBtn, false);

        TilePane grid = new TilePane();
        grid.getStyleClass().add("card-grid");
        grid.setPrefColumns(3);

        final Runnable[] fillRef = new Runnable[1];
        fillRef[0] = () -> {
            List<Evenement> data = evenementService.searchAndSort(
                    search.getText() == null ? "" : search.getText(), "idEvenement", "DESC");

            if (onlyMine[0] && currentUser != null) {
                Integer userId = currentUser.getId();
                data = data.stream()
                        .filter(ev -> ev.getCreatedById() != null && ev.getCreatedById().equals(userId))
                        .collect(Collectors.toList());
            }
            if (!"Tous types".equals(filterType.getValue())) {
                String t = filterType.getValue();
                data = data.stream()
                        .filter(ev -> t.equalsIgnoreCase(nullSafe(ev.getTypeEvenement())))
                        .collect(Collectors.toList());
            }
            if (!"Tous statuts".equals(filterStatut.getValue())) {
                String s = filterStatut.getValue();
                data = data.stream()
                        .filter(ev -> s.equalsIgnoreCase(nullSafe(ev.getStatut())))
                        .collect(Collectors.toList());
            }

            grid.getChildren().clear();
            for (Evenement e : data) {
                VBox card = entityCard(e.getNom(),
                        "Jeu: " + nullSafe(e.getJeu()),
                        "Type: " + nullSafe(e.getTypeEvenement()),
                        "Dates: " + formatDate(e.getDateDebut()) + " -> " + formatDate(e.getDateFin()),
                        "Statut: " + nullSafe(e.getStatut()));
                card.getStyleClass().add("event-card");

                Node img = createEventImageNode(e.getImage());
                if (img != null) {
                    HBox imgRow = new HBox(img);
                    imgRow.setAlignment(Pos.CENTER);
                    imgRow.setMaxWidth(Double.MAX_VALUE);
                    imgRow.setMouseTransparent(true);
                    card.getChildren().add(0, imgRow);
                }

                card.setPickOnBounds(true);
                card.setOnMouseClicked(ev -> {
                    if (ev.getButton() == MouseButton.PRIMARY) {
                        showDetails(e);
                    }
                });

                Button edit = new Button("Modifier");
                Button del = new Button("Supprimer");
                Button details = new Button("Details");

                edit.setOnAction(ev -> {
                    if (formController != null) formController.prepareForEdit(e);
                    nav.navigateTo("evenements_form");
                });
                del.setOnAction(ev -> {
                    try {
                        evenementService.supprimerEntite(e);
                        info("Evenement supprime.");
                        fillRef[0].run();
                    } catch (Exception ex) { error("Suppression impossible: " + ex.getMessage()); }
                });
                details.setOnAction(ev -> showDetails(e));

                HBox actions = new HBox(8);
                actions.getStyleClass().add("card-actions");
                actions.getChildren().add(details);
                if (admin || canEditEvenement(admin, e)) {
                    actions.getChildren().addAll(edit, del);
                }
                card.getChildren().add(actions);
                grid.getChildren().add(card);
            }
        };

        search.textProperty().addListener((o, old, now) -> fillRef[0].run());
        filterType.valueProperty().addListener((o, old, now) -> fillRef[0].run());
        filterStatut.valueProperty().addListener((o, old, now) -> fillRef[0].run());
        refresh.setOnAction(e -> fillRef[0].run());
        fillRef[0].run();

        root.getChildren().addAll(toolbar, pageScroller(grid));
        return root;
    }

    // â”€â”€â”€ Details page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Builds the full details page for an evenement and returns it. */
    public Node buildDetailsPage(Evenement evenement) {
        VBox root = detailsPageScaffold("Evenement: " + nullSafe(evenement.getNom()), "");

        Button back = new Button("Retour a la liste");
        back.setMaxWidth(Double.MAX_VALUE);
        back.setOnAction(e -> nav.navigateTo("evenements_list"));

        VBox meta = new VBox(4,
                detailLine("Jeu", evenement.getJeu()),
                detailLine("Type", evenement.getTypeEvenement()),
                detailLine("Statut", evenement.getStatut()),
                detailLine("Date debut", formatDate(evenement.getDateDebut())),
                detailLine("Date fin", formatDate(evenement.getDateFin())));
        meta.getStyleClass().add("details-section");

        Label descTitle = new Label("Description");
        descTitle.getStyleClass().add("entity-card-title");
        TextArea desc = new TextArea(nullSafe(evenement.getDescription()));
        desc.setEditable(false);
        desc.setWrapText(true);
        desc.setFocusTraversable(false);
        desc.setPrefRowCount(4);
        desc.setMaxHeight(120);
        desc.getStyleClass().add("details-readonly-text");

        Label rulesTitle = new Label("Regles");
        rulesTitle.getStyleClass().add("entity-card-title");
        TextArea rules = new TextArea(nullSafe(evenement.getRegles()));
        rules.setEditable(false);
        rules.setWrapText(true);
        rules.setFocusTraversable(false);
        rules.setPrefRowCount(4);
        rules.setMaxHeight(120);
        rules.getStyleClass().add("details-readonly-text");

        VBox textBlock = new VBox(6, descTitle, desc, rulesTitle, rules);
        textBlock.getStyleClass().add("details-section");

        Node imageNode = createDetailsHeroImageNode(evenement.getImage());

        List<Bracket> brackets = bracketService.findAll().stream()
                .filter(b -> b.getEvenementId() != null && b.getEvenementId().equals(evenement.getIdEvenement()))
                .collect(Collectors.toList());
        List<Integer> bracketIds = brackets.stream()
                .map(Bracket::getIdBracket).filter(Objects::nonNull).collect(Collectors.toList());
        List<GameMatch> matchs = matchService.findAll().stream()
                .filter(m -> m.getBracketId() != null && bracketIds.contains(m.getBracketId()))
                .collect(Collectors.toList());

        Node bracketVisual = buildBracketVisual(brackets, matchs);

        root.getChildren().add(back);
        if (imageNode != null) root.getChildren().add(imageNode);
        root.getChildren().addAll(meta, textBlock, bracketVisual);
        return pageScroller(root);
    }

    // â”€â”€â”€ Private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void showDetails(Evenement e) {
        if (e == null || e.getIdEvenement() == null) {
            error("Evenement invalide: impossible d'ouvrir les details.");
            return;
        }
        if (nav == null) {
            error("Navigation indisponible. Rechargez la page.");
            return;
        }
        nav.navigateTo("evenements_details:" + e.getIdEvenement());
    }

    private void setActiveBtn(Button allBtn, Button mineBtn, boolean mineActive) {
        allBtn.getStyleClass().remove("nav-item-active");
        mineBtn.getStyleClass().remove("nav-item-active");
        (mineActive ? mineBtn : allBtn).getStyleClass().add("nav-item-active");
    }

    private Node createEventImageNode(String url) {
        return imageCoverInFrame(url, 240, 130, "event-image-frame");
    }

    /** Image sur la fiche dÃ©tail : remplit le cadre (mÃªme largeur que le contenu formulaire). */
    private Node createDetailsHeroImageNode(String url) {
        return imageCoverInFrame(url, 748, 200, "details-hero-image");
    }

    // â”€â”€â”€ Bracket visual â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private Node buildBracketVisual(List<Bracket> brackets, List<GameMatch> matchs) {
        VBox section = new VBox(10);
        section.getStyleClass().add("details-section");
        Label title = new Label("Bracket Design");
        title.getStyleClass().add("entity-card-title");
        section.getChildren().add(title);

        if (brackets == null || brackets.isEmpty()) {
            Label empty = new Label("Aucun bracket disponible.");
            empty.getStyleClass().add("entity-card-line");
            section.getChildren().add(empty);
            return section;
        }

        for (Bracket bracket : brackets) {
            List<GameMatch> bm = matchs.stream()
                    .filter(m -> m.getBracketId() != null && m.getBracketId().equals(bracket.getIdBracket()))
                    .collect(Collectors.toList());
            if (bm.isEmpty()) {
                Label none = new Label("Aucun match pour ce bracket.");
                none.getStyleClass().add("entity-card-line");
                section.getChildren().add(none);
                continue;
            }
            Node treeLayout = buildTreeLayout(bm);
            ScrollPane board = new ScrollPane(new HBox(treeLayout));
            board.getStyleClass().add("bracket-board");
            board.setFitToHeight(true); board.setFitToWidth(false);
            board.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            board.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            board.setMinHeight(250);
            section.getChildren().add(board);
        }
        return section;
    }

    private Node buildTreeLayout(List<GameMatch> bracketMatches) {
        Map<Integer, List<GameMatch>> byRound = bracketMatches.stream()
                .collect(Collectors.groupingBy(m -> m.getTour() == null ? 0 : m.getTour(),
                        TreeMap::new, Collectors.toList()));
        List<Integer> rounds = new ArrayList<>(byRound.keySet());
        if (rounds.size() >= 2
                && byRound.get(rounds.get(0)).size() >= 2
                && byRound.get(rounds.get(1)).size() >= 1) {
            return buildSimpleKnockoutTree(byRound, rounds.get(0), rounds.get(1));
        }
        return buildRoundColumnsFallback(byRound);
    }

    private Node buildSimpleKnockoutTree(Map<Integer, List<GameMatch>> byRound, int semiRound, int finalRound) {
        List<GameMatch> semis = byRound.get(semiRound).stream()
                .sorted(Comparator.comparing(m -> m.getDateMatch() == null ? LocalDateTime.MIN : m.getDateMatch()))
                .collect(Collectors.toList());
        List<GameMatch> finals = byRound.get(finalRound).stream()
                .sorted(Comparator.comparing(m -> m.getDateMatch() == null ? LocalDateTime.MIN : m.getDateMatch()))
                .collect(Collectors.toList());

        GameMatch semi1 = semis.get(0), semi2 = semis.get(1), finalMatch = finals.get(0);

        VBox leftCol = new VBox(34);
        leftCol.getStyleClass().add("round-column");
        Label leftTitle = new Label("Tour " + semiRound);
        leftTitle.getStyleClass().add("round-title");
        leftCol.getChildren().addAll(leftTitle, matchCardNode(semi1), matchCardNode(semi2));

        Pane connector = createConnectorPane();

        VBox rightCol = new VBox(18);
        rightCol.getStyleClass().addAll("round-column", "final-round-column");
        Label rightTitle = new Label("Tour " + finalRound);
        rightTitle.getStyleClass().add("round-title");
        StackPane finalSlot = new StackPane(matchCardNode(finalMatch));
        finalSlot.getStyleClass().add("final-slot");
        StackPane.setAlignment(matchCardNode(finalMatch), Pos.CENTER);
        VBox.setVgrow(finalSlot, Priority.ALWAYS);
        rightCol.getChildren().addAll(rightTitle, finalSlot);

        HBox tree = new HBox(18, leftCol, connector, rightCol);
        tree.getStyleClass().add("bracket-tree");
        return tree;
    }

    private Pane createConnectorPane() {
        Pane p = new Pane();
        p.getStyleClass().add("tree-connector-col");
        p.setPrefWidth(110); p.setMinWidth(110); p.setPrefHeight(260);
        double topY = 68, bottomY = 198, joinX = 56, finalY = (topY + bottomY) / 2.0;
        for (Line line : new Line[]{
                new Line(8, topY, joinX, topY), new Line(8, bottomY, joinX, bottomY),
                new Line(joinX, topY, joinX, bottomY), new Line(joinX, finalY, 102, finalY)}) {
            line.getStyleClass().add("tree-line");
            line.setStroke(Color.web("#f4c64c"));
            line.setStrokeWidth(2.2);
            p.getChildren().add(line);
        }
        return p;
    }

    private Node buildRoundColumnsFallback(Map<Integer, List<GameMatch>> byRound) {
        HBox cols = new HBox(14);
        cols.getStyleClass().add("bracket-tree");
        for (Map.Entry<Integer, List<GameMatch>> entry : byRound.entrySet()) {
            VBox col = new VBox(10);
            col.getStyleClass().add("round-column");
            Label t = new Label("Tour " + entry.getKey());
            t.getStyleClass().add("round-title");
            col.getChildren().add(t);
            entry.getValue().forEach(m -> col.getChildren().add(matchCardNode(m)));
            cols.getChildren().add(col);
        }
        return cols;
    }

    private VBox matchCardNode(GameMatch m) {
        VBox card = new VBox(6);
        card.getStyleClass().add("bracket-match");
        card.setPrefHeight(96); card.setMinHeight(96);
        Label teamA = new Label("A: " + nullSafe(m.getEquipeANom())); teamA.getStyleClass().add("entity-card-line");
        Label teamB = new Label("B: " + nullSafe(m.getEquipeBNom())); teamB.getStyleClass().add("entity-card-line");
        Label score = new Label("Score: " + str(m.getScoreEquipeA()) + " - " + str(m.getScoreEquipeB())); score.getStyleClass().add("entity-card-line");
        Label status = new Label("Statut: " + nullSafe(m.getStatut())); status.getStyleClass().add("entity-card-line");
        card.getChildren().addAll(teamA, teamB, score, status);
        User user = SessionContext.getCurrentUser();
        if (user != null && (user.isAdmin() || canEditMatch(m))) {
            card.getStyleClass().add("match-card-link");
            card.setOnMouseClicked(e -> nav.navigateTo("matchs_form_edit:" + m.getIdMatch()));
        }
        return card;
    }
}

