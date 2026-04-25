package com.gamilha.controllers;

import com.gamilha.controllers.bracket.BracketFormController;
import com.gamilha.controllers.bracket.BracketListController;
import com.gamilha.controllers.equipe.EquipeFormController;
import com.gamilha.controllers.equipe.EquipeListController;
import com.gamilha.controllers.evenement.EvenementFormController;
import com.gamilha.controllers.evenement.EvenementListController;
import com.gamilha.controllers.gamematch.GameMatchFormController;
import com.gamilha.controllers.gamematch.GameMatchListController;
import com.gamilha.entity.Equipe;
import com.gamilha.entity.Evenement;
import com.gamilha.entity.GameMatch;
import com.gamilha.entity.User;
import com.gamilha.services.EquipeService;
import com.gamilha.services.EvenementService;
import com.gamilha.services.GameMatchService;
import com.gamilha.utils.SessionContext;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private VBox headerBox;
    @FXML private VBox sideNav;
    @FXML private HBox topNav;
    @FXML private StackPane pageContainer;

    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService = new EquipeService();
    private final GameMatchService matchService = new GameMatchService();

    private final EvenementListController evList = new EvenementListController();
    private final EvenementFormController evForm = new EvenementFormController();
    private final EquipeListController eqList = new EquipeListController();
    private final EquipeFormController eqForm = new EquipeFormController();
    private final BracketListController brList = new BracketListController();
    private final BracketFormController brForm = new BracketFormController();
    private final GameMatchListController maList = new GameMatchListController();
    private final GameMatchFormController maForm = new GameMatchFormController();

    private Button activeNavButton;

    @FXML
    public void initialize() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            if (titleLabel != null) titleLabel.setText("Session expiree");
            if (subtitleLabel != null) subtitleLabel.setText("Reconnectez-vous.");
            return;
        }

        boolean admin = user.isAdmin();
        if (titleLabel != null) {
            titleLabel.setText(admin ? "Backoffice - Gamilha" : "Frontoffice - Gamilha");
        }
        if (subtitleLabel != null) {
            String roles = user.getRoles() == null ? "" : user.getRoles();
            subtitleLabel.setText("Connecte en tant que " + user.getName()
                    + " (" + roles + ")");
        }

        wireSubControllers();

        if (sideNav != null) {
            sideNav.setVisible(admin);
            sideNav.setManaged(admin);
        }
        if (topNav != null) {
            topNav.setVisible(!admin);
            topNav.setManaged(!admin);
        }

        if (admin) {
            buildBackofficeSidebar();
        } else {
            buildFrontTopNav();
        }
    }

    public VBox getSideNav() {
        return sideNav;
    }

    public HBox getTopNav() {
        return topNav;
    }

    public Label getTitleLabel() {
        return titleLabel;
    }

    public Label getSubtitleLabel() {
        return subtitleLabel;
    }

    public VBox getHeaderBox() {
        return headerBox;
    }

    private void wireSubControllers() {
        evList.setNav(this::navigateTo);
        evList.setFormController(evForm);
        evForm.setNav(this::navigateTo);

        eqList.setNav(this::navigateTo);
        eqList.setFormController(eqForm);
        eqForm.setNav(this::navigateTo);

        brList.setNav(this::navigateTo);
        brList.setFormController(brForm);
        brForm.setNav(this::navigateTo);

        maList.setNav(this::navigateTo);
        maList.setFormController(maForm);
        maForm.setNav(this::navigateTo);
    }

    public void navigateTo(String pageKey) {
        if (pageKey.startsWith("evenements_details:")) {
            int id = Integer.parseInt(pageKey.split(":")[1]);
            Evenement ev = evenementService.findAll().stream()
                    .filter(e -> e.getIdEvenement() == id)
                    .findFirst()
                    .orElse(null);
            if (ev != null) {
                showPage(evList.buildDetailsPage(ev), null);
            }
            return;
        }

        if (pageKey.startsWith("equipes_details:")) {
            int id = Integer.parseInt(pageKey.split(":")[1]);
            Equipe eq = equipeService.findAll().stream()
                    .filter(e -> e.getIdEquipe() == id)
                    .findFirst()
                    .orElse(null);
            if (eq != null) {
                showPage(eqList.buildDetailsPage(eq), null);
            }
            return;
        }

        if (pageKey.startsWith("matchs_form_edit:")) {
            int id = Integer.parseInt(pageKey.split(":")[1]);
            GameMatch m = matchService.findAll().stream()
                    .filter(gm -> gm.getIdMatch() == id)
                    .findFirst()
                    .orElse(null);
            if (m != null) {
                maForm.prepareForEdit(m);
                pageKey = "matchs_form";
            }
        }

        Button found = findNavButton(pageKey);
        Node page;
        try {
            page = buildPage(pageKey);
        } catch (Exception ex) {
            showNavigationError("Erreur lors de l'ouverture de la page " + pageKey + ":\n" + ex.getMessage());
            return;
        }

        if (page == null) {
            showNavigationError("Navigation impossible vers: " + pageKey);
            return;
        }

        showPage(page, found);
    }

    private Node buildPage(String key) {
        return switch (key) {
            case "evenements_list" -> evList.build();
            case "evenements_form" -> evForm.build();
            case "equipes_list" -> eqList.build();
            case "equipes_form" -> eqForm.build();
            case "brackets_list" -> brList.build();
            case "brackets_form" -> brForm.build();
            case "matchs_list" -> maList.build();
            case "matchs_form" -> maForm.build();
            default -> null;
        };
    }

    private void showPage(Node page, Button navButton) {
        if (navButton != null) {
            if (activeNavButton != null) {
                activeNavButton.getStyleClass().remove("nav-item-active");
            }
            navButton.getStyleClass().add("nav-item-active");
            activeNavButton = navButton;
        }
        if (page != null) {
            pageContainer.getChildren().setAll(page);
        }
    }

    private void buildBackofficeSidebar() {
        sideNav.getChildren().clear();
        Label navTitle = new Label("Navigation");
        navTitle.getStyleClass().add("nav-title");
        sideNav.getChildren().add(navTitle);

        Button evBtn = createNavButton("Evenements - Liste", "evenements_list");
        Button eqBtn = createNavButton("Equipes - Liste", "equipes_list");
        Button brBtn = createNavButton("Brackets - Liste", "brackets_list");
        Button maBtn = createNavButton("Matchs - Liste", "matchs_list");
        sideNav.getChildren().addAll(evBtn, eqBtn, brBtn, maBtn);

        navigateTo("evenements_list");
    }

    private void buildFrontTopNav() {
        topNav.getChildren().clear();
        Button evBtn = createTopNavButton("Evenements", "evenements_list");
        Button eqBtn = createTopNavButton("Equipes", "equipes_list");
        topNav.getChildren().addAll(evBtn, eqBtn);
        navigateTo("evenements_list");
    }

    private void showNavigationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.setTitle("Erreur de navigation");
        alert.showAndWait();
    }

    private Button createNavButton(String text, String pageKey) {
        Button btn = new Button(text);
        btn.getStyleClass().add("nav-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setUserData(pageKey);
        btn.setOnAction(e -> navigateTo(pageKey));
        return btn;
    }

    private Button createTopNavButton(String text, String pageKey) {
        Button btn = new Button(text);
        btn.getStyleClass().add("top-nav-item");
        btn.setUserData(pageKey);
        btn.setOnAction(e -> navigateTo(pageKey));
        return btn;
    }

    private Button findNavButton(String pageKey) {
        String sectionKey = normalizeSectionKey(pageKey);
        if (sideNav != null) {
            for (Node node : sideNav.getChildren()) {
                if (node instanceof Button btn && sectionKey.equals(btn.getUserData())) {
                    return btn;
                }
            }
        }
        if (topNav != null) {
            for (Node node : topNav.getChildren()) {
                if (node instanceof Button btn && sectionKey.equals(btn.getUserData())) {
                    return btn;
                }
            }
        }
        return null;
    }

    private String normalizeSectionKey(String pageKey) {
        return switch (pageKey) {
            case "evenements_form" -> "evenements_list";
            case "equipes_form" -> "equipes_list";
            case "brackets_form" -> "brackets_list";
            case "matchs_form" -> "matchs_list";
            default -> pageKey;
        };
    }

    @FXML
    private void onLogout() {
        SessionContext.clear();
        com.gamilha.MainApp.showLogin();
    }
}
