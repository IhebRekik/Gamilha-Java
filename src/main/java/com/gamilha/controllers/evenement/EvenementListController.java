package com.gamilha.controllers.evenement;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.User;
import com.gamilha.entity.Bracket;
import com.gamilha.entity.Evenement;
import com.gamilha.entity.GameMatch;

import com.gamilha.services.AbonnementServices;
import com.gamilha.services.BracketService;
import com.gamilha.services.EvenementService;
import com.gamilha.services.GameMatchService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
<<<<<<< HEAD
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Map;
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

        boolean hasAccess = AbonnementServices.getAbonementActiveUser()
                .stream()
                .anyMatch(a ->
                        a.getOptions().stream()
                                .anyMatch(opt -> opt.contains("evenement"))
                );
        allBtn.setOnAction(e -> { onlyMine[0] = false; setActiveBtn(allBtn, mineBtn, false); refresh.fire(); });
        mineBtn.setOnAction(e -> { onlyMine[0] = true; setActiveBtn(allBtn, mineBtn, true); refresh.fire(); });
        add.setOnAction(e -> { if (formController != null) formController.prepareForCreate(); nav.navigateTo("evenements_form"); });
        add.setDisable(!hasAccess);


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
<<<<<<< HEAD
                    imgRow.setStyle("-fx-padding: 10 10 0 10;");
                    card.getChildren().add(imgRow);
                }

                VBox body = new VBox(6);
                body.getStyleClass().add("event-card-body");

                Label title = new Label(nullSafe(e.getNom()));
                title.getStyleClass().add("event-card-title");
                title.setWrapText(true);

                Label game = new Label("Jeu: " + nullSafe(e.getJeu()));
                game.getStyleClass().add("event-card-info");
                game.setWrapText(true);

                Label type = new Label("Type: " + nullSafe(e.getTypeEvenement()));
                type.getStyleClass().add("event-card-info");
                type.setWrapText(true);

                Label dates = new Label("Dates: " + formatDate(e.getDateDebut()) + " -> " + formatDate(e.getDateFin()));
                dates.getStyleClass().add("event-card-info");
                dates.setWrapText(true);

                Label status = new Label("Statut: " + nullSafe(e.getStatut()));
                status.getStyleClass().add("event-card-info");
                status.setWrapText(true);


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


                body.getChildren().addAll(title, game, type, dates, status, actions);
                card.getChildren().add(body);
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
        Node similarEventsSection = buildSimilarEventsSection(evenement);

        root.getChildren().add(back);
        if (imageNode != null) root.getChildren().add(imageNode);
        root.getChildren().addAll(meta, textBlock, bracketVisual, similarEventsSection);
        return pageScroller(root);
    }


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


    private Node buildBracketVisual(List<Bracket> brackets, List<GameMatch> matchs) {
        VBox section = new VBox(10);
        section.getStyleClass().add("details-section");

        Label title = new Label("Bracket & Matchs");


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

                    .sorted((a, b) -> Integer.compare(
                            a.getTour() == null ? 0 : a.getTour(),
                            b.getTour() == null ? 0 : b.getTour()))
                    .collect(Collectors.toList());

            Label bt = new Label("Bracket: " + nullSafe(bracket.getTypeBracket()) +
                    " - " + nullSafe(bracket.getStatut()));
            bt.getStyleClass().add("entity-card-title");
            section.getChildren().add(bt);


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

<<<<<<< HEAD
    private Node buildTreeLayout(List<GameMatch> bracketMatches) {
        Map<Integer, List<GameMatch>> byRound = bracketMatches.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getTour() == null ? 0 : m.getTour(),
                        java.util.TreeMap::new,
                        Collectors.toList()));

        int rounds = byRound.size();
        if (rounds == 2 && byRound.get(1) != null && byRound.get(2) != null
                && byRound.get(1).size() == 2 && byRound.get(2).size() == 1) {
            return buildSemiFinalLayout(byRound.get(1), byRound.get(2).get(0));
        }

        HBox cols = new HBox(18);
        cols.getStyleClass().add("bracket-tree");

        byRound.forEach((round, matches) -> {
            VBox col = new VBox(10);
            col.getStyleClass().add("round-column");
            Label rt = new Label("Tour " + round);
            rt.getStyleClass().add("round-title");
            col.getChildren().add(rt);
            matches.forEach(m -> col.getChildren().add(matchCardNode(m)));
            cols.getChildren().add(col);
        });

        return cols;
    }

    private Node buildSemiFinalLayout(List<GameMatch> semiFinals, GameMatch finalMatch) {
        VBox leftCol = new VBox(28);
        leftCol.getStyleClass().add("round-column");
        Label leftTitle = new Label("Demi-finales");
        leftTitle.getStyleClass().add("round-title");

        GameMatch semi1 = semiFinals.get(0);
        GameMatch semi2 = semiFinals.get(1);
        leftCol.getChildren().addAll(leftTitle, matchCardNode(semi1), matchCardNode(semi2));

        VBox connectorCol = new VBox();
        connectorCol.getStyleClass().add("tree-connector-col");
        connectorCol.setAlignment(Pos.CENTER);
        connectorCol.getChildren().add(buildConnector());

        VBox rightCol = new VBox(10);
        rightCol.getStyleClass().addAll("round-column", "final-round-column");
        Label rightTitle = new Label("Finale");
        rightTitle.getStyleClass().add("round-title");
        VBox finalSlot = new VBox(matchCardNode(finalMatch));
        finalSlot.getStyleClass().add("final-slot");
        rightCol.getChildren().addAll(rightTitle, finalSlot);

        HBox tree = new HBox(18, leftCol, connectorCol, rightCol);
        tree.getStyleClass().add("bracket-tree");
        tree.setAlignment(Pos.CENTER_LEFT);
        return tree;
    }

    private Node buildConnector() {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setMinWidth(110);
        box.setPrefWidth(110);
        box.setMinHeight(160);
        box.setPrefHeight(160);

        Line top = new Line(0, 20, 42, 20);
        Line bottom = new Line(0, 140, 42, 140);
        Line vertical = new Line(42, 20, 42, 140);
        Line mid = new Line(42, 80, 110, 80);
        for (Line l : new Line[]{top, bottom, vertical, mid}) {
            l.getStyleClass().add("tree-line");
            l.setStroke(Color.web("#f4c64c"));
            l.setStrokeWidth(2.2);
        }
        return new javafx.scene.layout.Pane(top, bottom, vertical, mid);
    }

    private VBox matchCardNode(GameMatch m) {
        VBox card = new VBox(4);
        card.getStyleClass().add("bracket-match");
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

    private Node buildSimilarEventsSection(Evenement current) {

        VBox section = new VBox(10);
        section.getStyleClass().add("details-section");

        Label title = new Label("Evenements similaires");
        title.getStyleClass().add("entity-card-title");
        section.getChildren().add(title);


        List<Evenement> similarEvents = evenementService.findSimilarByDescription(source, 4);
        if (similarEvents.isEmpty()) {
            Label empty = new Label("aucun evenement similaire");
            empty.getStyleClass().add("entity-card-line");
            section.getChildren().add(empty);
            return section;
        }

        for (Evenement e : similarEvents) {
            VBox card = new VBox(6);
            card.getStyleClass().addAll("entity-card", "event-card");
            card.setOnMouseClicked(ev -> showDetails(e));

            Node img = createEventImageNode(e.getImage());
            if (img != null) {
                HBox imgRow = new HBox(img);
                imgRow.setAlignment(Pos.CENTER);
                imgRow.setMaxWidth(Double.MAX_VALUE);
                imgRow.setMouseTransparent(true);
                card.getChildren().add(imgRow);
            }

            Label name = new Label(nullSafe(e.getNom()));
            name.getStyleClass().add("entity-card-title");
            Label game = new Label("Jeu: " + nullSafe(e.getJeu()));
            game.getStyleClass().add("entity-card-line");
            Label desc = new Label(buildPreview(e.getDescription()));
            desc.getStyleClass().add("entity-card-line");
            desc.setWrapText(true);

            Button details = new Button("Voir details");
            details.setOnAction(ev -> showDetails(e));

            card.getChildren().addAll(name, game, desc, details);
            section.getChildren().add(card);
        }
        return section;
    }

    private String buildPreview(String description) {
        String safe = nullSafe(description).trim();
        if (safe.length() <= 120) {
            return safe;
        }
        return safe.substring(0, 117) + "...";
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


