package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.controllers.bracket.BracketFormController;
import com.gamilha.controllers.bracket.BracketListController;
import com.gamilha.controllers.equipe.EquipeFormController;
import com.gamilha.controllers.equipe.EquipeListController;
import com.gamilha.controllers.equipe.EquipeParticipationCalendarController;
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
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur principal du Dashboard — "coquille" (shell) de l'application.
 *
 * Architecture :
 * - Gère la barre de navigation latérale (admin/backoffice) ou supérieure (user/frontoffice).
 * - Contient un {@link StackPane} (pageContainer) qui accueille les pages affichées.
 * - Délègue la construction de chaque page aux sous-contrôleurs spécialisés.
 * - Centralise la navigation via {@link #navigateTo(String)} pour éviter les dépendances croisées.
 *
 * Deux modes d'interface :
 * - Admin (ROLE_ADMIN) → sidebar verticale avec toutes les sections.
 * - Utilisateur standard → barre horizontale en haut, accès limité.
 */
public class DashboardController {

    // ─── Liaisons FXML ────────────────────────────────────────────────────────

    @FXML private Label titleLabel;      // Titre en haut du dashboard
    @FXML private Label subtitleLabel;   // Sous-titre affichant le nom et les rôles de l'utilisateur
    @FXML private VBox headerBox;        // Bandeau supérieur (logo + logout + top nav)
    @FXML private VBox sideNav;          // Barre de navigation latérale (admin uniquement)
    @FXML private HBox topNav;           // Barre de navigation horizontale (utilisateur standard)
    @FXML private StackPane pageContainer; // Zone centrale où les pages sont injectées

    // ─── Services ─────────────────────────────────────────────────────────────

    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService       = new EquipeService();
    private final GameMatchService matchService     = new GameMatchService();

    // ─── Sous-contrôleurs (un par entité × liste/formulaire) ─────────────────

    private final EvenementListController evList = new EvenementListController();
    private final EvenementFormController evForm = new EvenementFormController();
    private final EquipeListController    eqList = new EquipeListController();
    private final EquipeFormController    eqForm = new EquipeFormController();
    private final EquipeParticipationCalendarController eqCalendar = new EquipeParticipationCalendarController();
    private final BracketListController   brList = new BracketListController();
    private final BracketFormController   brForm = new BracketFormController();
    private final GameMatchListController maList = new GameMatchListController();
    private final GameMatchFormController maForm = new GameMatchFormController();

    // ─── État de la navigation ────────────────────────────────────────────────

    /** Bouton actuellement actif dans la nav, pour gérer le style "actif". */
    private Button activeNavButton;

    // ─── Initialisation ───────────────────────────────────────────────────────

    /**
     * Appelé automatiquement par JavaFX après le chargement du FXML.
     *
     * Étapes :
     * 1. Vérifie la session (redirige si expirée).
     * 2. Affiche le titre selon le rôle (backoffice/frontoffice).
     * 3. Câble les callbacks de navigation dans tous les sous-contrôleurs.
     * 4. Construit la nav appropriée et affiche la page par défaut.
     */
    @FXML
    public void initialize() {
        User user = SessionContext.getCurrentUser();
        if (user == null) {
            titleLabel.setText("Session expirée");
            subtitleLabel.setText("Reconnectez-vous.");
            return;
        }

        boolean admin = user.isAdmin();
        titleLabel.setText(admin ? "Backoffice - Gamilha" : "Frontoffice - Gamilha");
        subtitleLabel.setText("Connecte en tant que " + user.getName()
                + " (" + String.join(", ", user.getRoles()) + ")");

        wireSubControllers();

        // Affiche/masque les deux barres de navigation selon le rôle
        if (sideNav != null) { sideNav.setVisible(admin); sideNav.setManaged(admin); }
        if (topNav != null)  { topNav.setVisible(!admin); topNav.setManaged(!admin); }

        if (admin) buildBackofficeSidebar();
        else       buildFrontTopNav();
    }

    // ─── Câblage des sous-contrôleurs ─────────────────────────────────────────

    /**
     * Injecte le callback {@link #navigateTo(String)} dans chaque sous-contrôleur
     * et leur référence croisée liste ↔ formulaire.
     *
     * Ce câblage permet aux sous-contrôleurs de déclencher une navigation
     * sans connaître le DashboardController directement.
     */
    public VBox getSideNav(){
        return sideNav;
    }

    public HBox getTopNav(){
        return topNav;
    }

    public Label getTitleLabel(){
        return titleLabel;
    }

    public Label getSubtitleLabel(){
        return subtitleLabel;
    }

    public VBox getHeaderBox() {
        return headerBox;
    }
    private void wireSubControllers() {
        evList.setNav(this::navigateTo); evList.setFormController(evForm); evForm.setNav(this::navigateTo);
        eqList.setNav(this::navigateTo); eqList.setFormController(eqForm); eqForm.setNav(this::navigateTo);
        eqCalendar.setNav(this::navigateTo);
        brList.setNav(this::navigateTo); brList.setFormController(brForm); brForm.setNav(this::navigateTo);
        maList.setNav(this::navigateTo); maList.setFormController(maForm); maForm.setNav(this::navigateTo);
    }

    // ─── Navigation centrale ──────────────────────────────────────────────────

    /**
     * Point d'entrée unique pour toute navigation dans le dashboard.
     *
     * Supporte deux types de clés :
     * - Clés simples : "evenements_list", "equipes_form", "matchs_list"...
     * - Clés paramétriques : "evenements_details:42", "matchs_form_edit:7"
     *   (format "clé:id" pour accéder à une entité spécifique)
     *
     * @param pageKey identifiant de la page à afficher
     */
    public void navigateTo(String pageKey) {
        // Route vers la page de détail d'un événement
        if (pageKey.startsWith("evenements_details:")) {
            int id = Integer.parseInt(pageKey.split(":")[1]);
            Evenement ev = evenementService.findAll().stream()
                    .filter(e -> e.getIdEvenement() == id).findFirst().orElse(null);
            if (ev != null) showPage(evList.buildDetailsPage(ev), null);
            return;
        }

        // Route vers la page de détail d'une équipe
        if (pageKey.startsWith("equipes_details:")) {
            int id = Integer.parseInt(pageKey.split(":")[1]);
            Equipe eq = equipeService.findAll().stream()
                    .filter(e -> e.getIdEquipe() == id).findFirst().orElse(null);
            if (eq != null) showPage(eqList.buildDetailsPage(eq), null);
            return;
        }

        // Route vers le formulaire d'édition d'un match existant
        if (pageKey.startsWith("matchs_form_edit:")) {
            int id = Integer.parseInt(pageKey.split(":")[1]);
            GameMatch m = matchService.findAll().stream()
                    .filter(gm -> gm.getIdMatch() == id).findFirst().orElse(null);
            if (m != null) { maForm.prepareForEdit(m); pageKey = "matchs_form"; }
        }

        // Navigation standard vers une page identifiée par sa clé
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

    // ─── Construction des pages ───────────────────────────────────────────────

    /**
     * Délègue la construction de la page au sous-contrôleur correspondant.
     * Retourne null pour les clés inconnues (la page container reste inchangée).
     *
     * @param key clé de la page à construire
     * @return le nœud JavaFX représentant la page, ou null
     */
    private Node buildPage(String key) {
        return switch (key) {
            case "evenements_list" -> evList.build();
            case "evenements_form" -> evForm.build();
            case "equipes_list"    -> eqList.build();
            case "equipes_form"    -> eqForm.build();
            case "equipes_calendar" -> eqCalendar.build();
            case "brackets_list"   -> brList.build();
            case "brackets_form"   -> brForm.build();
            case "matchs_list"     -> maList.build();
            case "matchs_form"     -> maForm.build();
            default                -> null;
        };
    }

    /**
     * Affiche une page dans le conteneur central et met à jour le bouton actif.
     *
     * @param page      le nœud à afficher (null = pas de changement)
     * @param navButton le bouton de nav à marquer comme actif (null = inchangé)
     */
    private void showPage(Node page, Button navButton) {
        if (navButton != null) {
            if (activeNavButton != null) activeNavButton.getStyleClass().remove("nav-item-active");
            navButton.getStyleClass().add("nav-item-active");
            activeNavButton = navButton;
        }
        if (page != null) pageContainer.getChildren().setAll(page);
    }

    // ─── Construction des barres de navigation ────────────────────────────────

    /**
     * Construit la sidebar admin avec les 4 sections principales.
     * Affiche la liste des événements par défaut.
     */
    private void buildBackofficeSidebar() {
        sideNav.getChildren().clear();
        Label navTitle = new Label("Navigation");
        navTitle.getStyleClass().add("nav-title");
        sideNav.getChildren().add(navTitle);

        Button evBtn = createNavButton("🗂", "Evenements - Liste", "evenements_list");
        Button eqBtn = createNavButton("👥", "Equipes - Liste",    "equipes_list");
        Button brBtn = createNavButton("📚", "Brackets - Liste",   "brackets_list");
        Button maBtn = createNavButton("🎮", "Matchs - Liste",     "matchs_list");
        sideNav.getChildren().addAll(evBtn, eqBtn, brBtn, maBtn);

        navigateTo("evenements_list"); // Page d'accueil par défaut
    }

    /**
     * Construit la barre de navigation horizontale pour les utilisateurs standards.
     * Affiche uniquement Événements et Équipes.
     */
    private void buildFrontTopNav() {
        topNav.getChildren().clear();
        Button evBtn = createTopNavButton("Evenements", "evenements_list");
        Button eqBtn = createTopNavButton("Equipes",    "equipes_list");
        topNav.getChildren().addAll(evBtn, eqBtn);
        navigateTo("evenements_list");
    }

    private void showNavigationError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.setTitle("Erreur de navigation");
        alert.showAndWait();
    }

    /**
     * Crée un bouton de navigation latérale avec icône, texte et action.
     *
     * @param icon    emoji icône
     * @param text    libellé du bouton
     * @param pageKey clé de navigation associée
     * @return bouton configuré
     */
    private Button createNavButton(String icon, String text, String pageKey) {
        Button btn = new Button(icon + "  " + text);
        btn.getStyleClass().add("nav-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setUserData(pageKey);
        btn.setOnAction(e -> navigateTo(pageKey));
        return btn;
    }

    /**
     * Crée un bouton de navigation horizontale (topbar).
     *
     * @param text    libellé du bouton
     * @param pageKey clé de navigation associée
     * @return bouton configuré
     */
    private Button createTopNavButton(String text, String pageKey) {
        Button btn = new Button(text);
        btn.getStyleClass().add("top-nav-item");
        btn.setUserData(pageKey);
        btn.setOnAction(e -> navigateTo(pageKey));
        return btn;
    }

    /**
     * Cherche dans les barres de navigation le bouton correspondant à une clé de page.
     * Utilisé pour mettre en surbrillance le bouton actif.
     *
     * @param pageKey clé de navigation
     * @return le bouton trouvé, ou null
     */
    private Button findNavButton(String pageKey) {
        String sectionKey = normalizeSectionKey(pageKey);
        if (sideNav != null) {
            for (var node : sideNav.getChildren()) {
                if (node instanceof Button btn && sectionKey.equals(btn.getUserData()))
                    return btn;
            }
        }
        if (topNav != null) {
            for (var node : topNav.getChildren()) {
                if (node instanceof Button btn && sectionKey.equals(btn.getUserData()))
                    return btn;
            }
        }
        return null;
    }

    private String normalizeSectionKey(String pageKey) {
        return switch (pageKey) {
            case "evenements_form" -> "evenements_list";
            case "equipes_form" -> "equipes_list";
            case "equipes_calendar" -> "equipes_list";
            case "brackets_form" -> "brackets_list";
            case "matchs_form" -> "matchs_list";
            default -> pageKey;
        };
    }

    /**
     * Détermine si le texte d'un bouton de navigation correspond à une clé de page.
     * Les clés liste ET formulaire pointent vers le même bouton (même section).
     *
     * @param btnText texte du bouton
     * @param pageKey clé de navigation
     * @return true si correspondance
     */
    // ─── Déconnexion ──────────────────────────────────────────────────────────

    private final com.gamilha.services.UserService userService = new com.gamilha.services.UserService();

    /**
     * Gère le logout : efface la session et retourne à l'écran de login.
     * Appelé par le bouton "Déconnexion" dans le FXML.
     */
    @FXML
    private void onLogout() {
        User user = SessionContext.getCurrentUser();
        if (user != null) {
            userService.logout(user.getId());
        }
        SessionContext.clear(); // Supprime l'utilisateur en session
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("login-view.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 460, 420);
            scene.getStylesheets().add(MainApp.class.getResource("styles/gamilha.css").toExternalForm());
            Stage stage = (Stage) titleLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Gamilha - Login");
            stage.show();
        } catch (IOException ex) {
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                    "Deconnexion impossible: " + ex.getMessage()).showAndWait();
        }
    }
}
