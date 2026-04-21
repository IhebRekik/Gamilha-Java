package com.gamilha.controllers.shared;

import com.gamilha.entity.Bracket;
import com.gamilha.entity.Equipe;
import com.gamilha.entity.Evenement;
import com.gamilha.entity.GameMatch;
import com.gamilha.services.BracketService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

/**
 * Classe de base abstraite partagée par tous les contrôleurs d'entité.
 *
 * Regroupe :
 * - La construction des composants UI réutilisables (scaffold, cards, formulaires)
 * - Le chargement des images avec effet "cover" (object-fit: cover)
 * - Les vérifications de permissions (canEdit*)
 * - Les loaders de ComboBox
 * - Les helpers de formulaire (lecture, effacement)
 * - Les utilitaires de chaînes et de dates
 * - Les alertes info/erreur
 *
 * Chaque sous-contrôleur (EvenementListController, EquipeFormController, etc.)
 * hérite de cette classe pour ne pas répéter ces utilitaires.
 */
public abstract class BaseController {

    /** Formateur de date/heure pour l'affichage : "dd/MM/yyyy HH:mm". */
    protected final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ─── UI Scaffold ──────────────────────────────────────────────────────────

    /**
     * Crée le conteneur racine d'une page standard avec titre et sous-titre.
     *
     * @param title    titre affiché en haut de la page
     * @param subtitle sous-titre (peut être vide)
     * @return VBox racine stylisée
     */
    protected VBox pageScaffold(String title, String subtitle) {
        VBox root = new VBox(10);
        root.getStyleClass().add("page-root");
        Label h = new Label(title);
        h.getStyleClass().add("page-header");
        Label s = new Label(subtitle);
        s.getStyleClass().add("page-sub");
        root.getChildren().addAll(h, s);
        return root;
    }

    /**
     * Crée le conteneur racine pour les pages de détail (largeur max 760px, centré).
     * Produit un rendu compact adapté aux fiches de détail d'entité.
     *
     * @param title    titre de la fiche
     * @param subtitle sous-titre
     * @return VBox centrée et limitée en largeur
     */
    protected VBox detailsPageScaffold(String title, String subtitle) {
        VBox root = new VBox(10);
        root.getStyleClass().addAll("page-root", "details-page-root");
        root.setAlignment(Pos.TOP_CENTER);
        root.setMaxWidth(760);
        root.setPrefWidth(760);
        Label h = new Label(title);
        h.getStyleClass().add("page-header");
        h.setMaxWidth(760);
        h.setWrapText(true);
        Label s = new Label(subtitle);
        s.getStyleClass().add("page-sub");
        s.setMaxWidth(760);
        s.setWrapText(true);
        root.getChildren().addAll(h, s);
        return root;
    }

    /**
     * Affiche une image distante en remplissant son cadre (équivalent CSS object-fit: cover).
     *
     * Fonctionnement :
     * - Charge l'image de manière asynchrone (pas de gel de l'interface).
     * - Redimensionne l'ImageView pour couvrir exactement le cadre (scale = max(w/iw, h/ih)).
     * - Applique un clip rectangulaire avec coins arrondis.
     * - Affiche un message d'erreur si l'URL est invalide.
     *
     * @param imageUrl    URL de l'image (HTTP/HTTPS)
     * @param width       largeur du cadre en pixels
     * @param height      hauteur du cadre en pixels
     * @param styleClasses classes CSS à appliquer sur le conteneur
     * @return StackPane contenant l'image, ou null si l'URL est vide
     */
    protected Node imageCoverInFrame(String imageUrl, double width, double height, String... styleClasses) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        String url = normalizeImageUrl(imageUrl.trim());
        double req = Math.max(64, Math.max(width, height) * 2); // Résolution de chargement 2× pour la netteté

        StackPane pane = new StackPane();
        pane.setMinSize(width, height);
        pane.setPrefSize(width, height);
        pane.setMaxSize(width, height);
        pane.getStyleClass().add("image-fill-frame");
        for (String c : styleClasses) {
            if (c != null && !c.isEmpty()) pane.getStyleClass().add(c);
        }

        // Détecte si c'est une image héro (bords plus arrondis, dimensions dynamiques)
        double cssCornerRadius = 10;
        boolean detailsHero = false;
        for (String c : styleClasses) {
            if (c != null && c.contains("details-hero")) { cssCornerRadius = 12; detailsHero = true; break; }
        }

        // Clip arrondi : arcWidth/Height = diamètre de l'arc = 2 × rayon CSS
        Rectangle clip = new Rectangle();
        clip.setArcWidth(2 * cssCornerRadius);
        clip.setArcHeight(2 * cssCornerRadius);
        if (detailsHero) {
            clip.widthProperty().bind(pane.widthProperty());
            clip.heightProperty().bind(pane.heightProperty());
        } else {
            clip.setWidth(width);
            clip.setHeight(height);
        }
        pane.setClip(clip);

        ImageView iv = new ImageView();
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        StackPane.setAlignment(iv, Pos.CENTER);

        Label errLabel = new Label("Image invalide");
        errLabel.getStyleClass().add("entity-card-line");
        errLabel.setVisible(false);
        pane.getChildren().addAll(iv, errLabel);

        Image image;
        try {
            // Chargement asynchrone de l'image
            image = new Image(url, req, req, true, true, true);
            iv.setImage(image);
        } catch (IllegalArgumentException ex) {
            // URL invalide : on n'interrompt pas la construction de la page.
            return null;
        }

        // Calcul du scale "cover" : l'image remplit le cadre sans déformation
        Runnable applyCoverLayout = () -> {
            if (image.isError()) { iv.setVisible(false); errLabel.setVisible(true); return; }
            double iw = image.getWidth(), ih = image.getHeight();
            if (iw <= 0 || ih <= 0) return;
            double w = pane.getWidth() > 0 ? pane.getWidth() : width;
            double h = pane.getHeight() > 0 ? pane.getHeight() : height;
            double scale = Math.max(w / iw, h / ih);
            iv.setFitWidth(iw * scale);
            iv.setFitHeight(ih * scale);
        };

        // Applique le layout dès que l'image est chargée
        if (image.getProgress() >= 1.0) applyCoverLayout.run();
        image.progressProperty().addListener((obs, ov, nv) -> {
            if (nv != null && nv.doubleValue() >= 1.0) applyCoverLayout.run();
        });
        image.errorProperty().addListener((obs, ov, nv) -> {
            if (Boolean.TRUE.equals(nv)) { iv.setVisible(false); errLabel.setVisible(true); }
        });

        // Pour les images héro, recalcule quand le conteneur est redimensionné
        if (detailsHero) {
            pane.widthProperty().addListener((obs, o, n) -> applyCoverLayout.run());
            pane.heightProperty().addListener((obs, o, n) -> applyCoverLayout.run());
        }

        return pane;
    }

    /**
     * Normalise une URL d'image incomplète.
     * Exemple : "//cdn.example.com/img.jpg" → "https://cdn.example.com/img.jpg"
     *
     * @param url URL brute
     * @return URL corrigée utilisable par JavaFX
     */
    private static String normalizeImageUrl(String url) {
        return url.startsWith("//") ? "https:" + url : url;
    }

    /**
     * Enveloppe un contenu dans un ScrollPane vertical (scrollbar horizontale désactivée).
     * Le ScrollPane s'étend pour remplir l'espace vertical disponible.
     *
     * @param content le nœud à rendre défilable
     * @return ScrollPane configuré
     */
    protected ScrollPane pageScroller(Node content) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    /**
     * Crée une "carte" d'entité avec un titre et des lignes de description.
     * Utilisée dans les grilles de liste (TilePane).
     *
     * @param title titre principal de la carte
     * @param lines lignes d'information supplémentaires
     * @return VBox stylisée représentant la carte
     */
    protected VBox entityCard(String title, String... lines) {
        VBox card = new VBox();
        card.getStyleClass().add("entity-card");
        Label t = new Label(title == null ? "-" : title);
        t.getStyleClass().add("entity-card-title");
        card.getChildren().add(t);
        for (String line : lines) {
            Label l = new Label(line);
            l.getStyleClass().add("entity-card-line");
            card.getChildren().add(l);
        }
        return card;
    }

    /**
     * Crée un label "Clé: Valeur" pour les fiches de détail.
     *
     * @param key   libellé du champ
     * @param value valeur du champ (null → chaîne vide)
     * @return Label stylisé
     */
    protected Label detailLine(String key, String value) {
        Label l = new Label(key + ": " + nullSafe(value));
        l.getStyleClass().add("entity-card-line");
        return l;
    }

    /**
     * Crée un GridPane pré-configuré pour les formulaires.
     * Colonnes : label (150px) + champ (extensible jusqu'à 420px).
     *
     * @return GridPane stylisé prêt à recevoir des lignes de formulaire
     */
    protected GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.getStyleClass().add("form-card");
        grid.setMaxWidth(760);
        ColumnConstraints c0 = new ColumnConstraints(); c0.setPrefWidth(150);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS); c1.setFillWidth(true);
        grid.getColumnConstraints().addAll(c0, c1);
        return grid;
    }

    /**
     * Ajoute une ligne label + champ dans un GridPane de formulaire.
     * Applique une largeur préférée de 420px au champ.
     * Pour les TextArea, fixe la hauteur à 90px (3 lignes).
     *
     * @param grid  le GridPane cible
     * @param row   numéro de ligne (0-indexé)
     * @param label libellé du champ
     * @param node  composant de saisie (TextField, DatePicker, ComboBox, etc.)
     */
    protected void addFormRow(GridPane grid, int row, String label, Node node) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        grid.add(l, 0, row);
        if (node instanceof Region region) { region.setMaxWidth(420); region.setPrefWidth(420); }
        if (node instanceof TextArea area) { area.setPrefRowCount(3); area.setMinHeight(90); area.setPrefHeight(90); }
        grid.add(node, 1, row);
    }

    /**
     * Crée une colonne de TableView avec une fonction de mapping texte.
     *
     * @param title  en-tête de la colonne
     * @param mapper fonction T → String pour extraire la valeur affichée
     * @param width  largeur préférée
     * @param <T>    type de l'entité dans la table
     * @return TableColumn configurée
     */
    protected <T> TableColumn<T, String> col(String title, Function<T, String> mapper, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(mapper.apply(data.getValue())));
        c.setPrefWidth(width);
        return c;
    }

    // ─── Permissions ──────────────────────────────────────────────────────────

    /**
     * Vérifie si l'utilisateur courant peut modifier un événement.
     * Admins peuvent tout modifier. Utilisateurs standards : uniquement leurs propres événements.
     *
     * @param admin    true si l'utilisateur est admin
     * @param evenement l'événement à vérifier
     * @return true si modification autorisée
     */
    protected boolean canEditEvenement(boolean admin, Evenement evenement) {
        if (admin) return true;
        return evenement.getCreatedById() != null
                && evenement.getCreatedById().equals(SessionContext.getCurrentUser().getId());
    }

    /**
     * Vérifie si l'utilisateur courant peut modifier une équipe.
     * Utilisateurs standards : uniquement leurs propres équipes (owner_id).
     *
     * @param admin  true si l'utilisateur est admin
     * @param equipe l'équipe à vérifier
     * @return true si modification autorisée
     */
    protected boolean canEditEquipe(boolean admin, Equipe equipe) {
        if (admin) return true;
        return equipe.getOwnerId() != null
                && equipe.getOwnerId().equals(SessionContext.getCurrentUser().getId());
    }

    /**
     * Vérifie si l'utilisateur courant peut modifier un bracket.
     * Délègue à {@link #canEditBracketId(Integer)}.
     *
     * @param admin   true si l'utilisateur est admin
     * @param bracket le bracket à vérifier
     * @return true si modification autorisée
     */
    protected boolean canEditBracket(boolean admin, Bracket bracket) {
        if (admin) return true;
        return canEditBracketId(bracket.getIdBracket());
    }

    /**
     * Vérifie les permissions sur un bracket à partir de son ID.
     * Remonte jusqu'à l'événement parent pour vérifier le créateur.
     *
     * @param bracketId ID du bracket
     * @return true si l'utilisateur courant est le créateur de l'événement associé
     */
    protected boolean canEditBracketId(Integer bracketId) {
        BracketService bracketService = new BracketService();
        Integer ownerId = bracketService.findEvenementOwnerId(bracketId);
        return ownerId != null && ownerId.equals(SessionContext.getCurrentUser().getId());
    }

    /**
     * Vérifie si l'utilisateur courant peut modifier un match.
     * La permission est basée sur le bracket auquel appartient le match.
     *
     * @param match le match à vérifier
     * @return true si modification autorisée
     */
    protected boolean canEditMatch(GameMatch match) {
        return canEditBracketId(match.getBracketId());
    }

    // ─── Loaders de ComboBox ──────────────────────────────────────────────────

    /**
     * Remplit une ComboBox avec la liste des événements disponibles.
     * Admins voient tous les événements, utilisateurs standards uniquement les leurs.
     *
     * @param combo le ComboBox à remplir
     * @param admin true si l'utilisateur est admin
     */
    protected void loadEvenementChoices(ComboBox<Evenement> combo, boolean admin) {
        com.gamilha.services.EvenementService svc = new com.gamilha.services.EvenementService();
        List<Evenement> list = admin ? svc.findAll() : svc.findByOwner(SessionContext.getCurrentUser().getId());
        combo.setItems(FXCollections.observableArrayList(list));
    }

    /**
     * Remplit une ComboBox avec la liste des brackets disponibles.
     * Admins voient tous les brackets, utilisateurs standards uniquement les leurs (via événement).
     *
     * @param combo le ComboBox à remplir
     * @param admin true si l'utilisateur est admin
     */
    protected void loadBracketChoices(ComboBox<Bracket> combo, boolean admin) {
        BracketService svc = new BracketService();
        List<Bracket> list = admin ? svc.findAll() : svc.findByEvenementOwner(SessionContext.getCurrentUser().getId());
        combo.setItems(FXCollections.observableArrayList(list));
    }

    /**
     * Remplit deux ComboBox (équipe A et équipe B) avec les équipes disponibles.
     * Les deux ComboBox partagent la même liste d'équipes.
     *
     * @param equipeA ComboBox pour l'équipe A
     * @param equipeB ComboBox pour l'équipe B
     * @param admin   true si l'utilisateur est admin
     */
    protected void loadEquipeChoicesForMatch(ComboBox<Equipe> equipeA, ComboBox<Equipe> equipeB, boolean admin) {
        com.gamilha.services.EquipeService svc = new com.gamilha.services.EquipeService();
        List<Equipe> equipes = admin ? svc.findAll() : svc.findByOwner(SessionContext.getCurrentUser().getId());
        equipeA.setItems(FXCollections.observableArrayList(equipes));
        equipeB.setItems(FXCollections.observableArrayList(equipes));
    }

    /**
     * Sélectionne une équipe dans un ComboBox par son ID.
     *
     * @param combo ComboBox d'équipes
     * @param id    ID de l'équipe à sélectionner (null → déselectionne)
     */
    protected void selectEquipe(ComboBox<Equipe> combo, Integer id) {
        if (id == null) { combo.setValue(null); return; }
        for (Equipe equipe : combo.getItems()) {
            if (id.equals(equipe.getIdEquipe())) { combo.setValue(equipe); return; }
        }
    }

    // ─── Helpers formulaire ───────────────────────────────────────────────────

    /**
     * Lit les champs du formulaire match et construit un objet {@link GameMatch}.
     * Parse l'heure au format "HH:mm" et combine avec la date choisie.
     *
     * @param bracketBox  ComboBox de bracket
     * @param equipeABox  ComboBox équipe A
     * @param equipeBBox  ComboBox équipe B
     * @param tour        TextField du numéro de tour
     * @param scoreA      TextField du score équipe A
     * @param scoreB      TextField du score équipe B
     * @param statut      ComboBox du statut
     * @param date        DatePicker de la date du match
     * @param time        TextField de l'heure (format HH:mm)
     * @return objet GameMatch peuplé
     * @throws RuntimeException si l'heure est invalide
     */
    protected GameMatch readMatchForm(ComboBox<Bracket> bracketBox, ComboBox<Equipe> equipeABox,
            ComboBox<Equipe> equipeBBox, TextField tour, TextField scoreA, TextField scoreB,
            ComboBox<String> statut, DatePicker date, TextField time) {
        GameMatch match = new GameMatch();
        Bracket b = bracketBox.getValue();
        match.setBracketId(b == null ? null : b.getIdBracket());
        Equipe a = equipeABox.getValue();
        match.setEquipeAId(a == null ? null : a.getIdEquipe());
        Equipe c = equipeBBox.getValue();
        match.setEquipeBId(c == null ? null : c.getIdEquipe());
        match.setTour(parseInt(tour.getText(), "Le tour est invalide."));
        match.setScoreEquipeA(parseInt(scoreA.getText(), "Le score equipe A est invalide."));
        match.setScoreEquipeB(parseInt(scoreB.getText(), "Le score equipe B est invalide."));
        match.setStatut(statut.getValue());
        if (date.getValue() != null && time.getText() != null && !time.getText().isBlank()) {
            String[] hm = time.getText().trim().split(":");
            if (hm.length != 2) throw new RuntimeException("Heure invalide. Format attendu HH:mm.");
            match.setDateMatch(date.getValue().atTime(Integer.parseInt(hm[0]), Integer.parseInt(hm[1])));
        }
        return match;
    }

    /** Efface tous les champs du formulaire Événement et réinitialise aux valeurs par défaut. */
    protected void clearEvenementForm(TextField nom, TextArea description, TextField jeu,
            ComboBox<String> typeEvenement, DatePicker dateDebut, DatePicker dateFin,
            ComboBox<String> statut, TextArea regles, TextField image,
            ListView<Equipe> equipes, ComboBox<String> typeBracketAuto) {
        nom.clear(); description.clear(); jeu.clear();
        typeEvenement.setValue(null); dateDebut.setValue(null); dateFin.setValue(null);
        statut.setValue("prévu"); regles.clear(); image.clear();
        if (equipes != null) equipes.getSelectionModel().clearSelection();
        typeBracketAuto.setValue("single elimination");
    }

    /** Efface tous les champs du formulaire Équipe. */
    protected void clearEquipeForm(TextField nom, TextField tag, TextField logo, TextField pays,
            DatePicker dateCreation, ComboBox<String> niveau, ListView<Integer> members) {
        nom.clear(); tag.clear(); logo.clear(); pays.clear();
        dateCreation.setValue(null); niveau.setValue(null);
        members.getSelectionModel().clearSelection();
    }

    /** Efface tous les champs du formulaire Bracket. */
    protected void clearBracketForm(ComboBox<Evenement> evenement, ComboBox<String> type,
            TextField nombreTours, ComboBox<String> statut) {
        evenement.setValue(null); type.setValue(null); nombreTours.clear(); statut.setValue(null);
    }

    /** Efface tous les champs du formulaire Match et réinitialise aux valeurs par défaut. */
    protected void clearMatchForm(ComboBox<Bracket> bracket, ComboBox<Equipe> equipeA, ComboBox<Equipe> equipeB,
            TextField tour, TextField scoreA, TextField scoreB,
            ComboBox<String> statut, DatePicker date, TextField time) {
        bracket.setValue(null); equipeA.setValue(null); equipeB.setValue(null);
        tour.clear(); scoreA.setText("0"); scoreB.setText("0");
        statut.setValue("à venir"); date.setValue(null); time.setText("14:00");
    }

    // ─── Utilitaires de chaînes et dates ─────────────────────────────────────

    /** Formate une LocalDate en "dd/MM/yyyy", retourne "" si null. */
    protected String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /** Formate un LocalDateTime en "dd/MM/yyyy HH:mm", retourne "" si null. */
    protected String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /** Convertit un Integer en String, retourne "" si null. */
    protected String str(Integer i) {
        return i == null ? "" : String.valueOf(i);
    }

    /** Retourne la valeur si non null, sinon "". Évite les NullPointerException dans les labels. */
    protected String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /** Vérifie si une valeur de chaîne contient une sous-chaîne (insensible à la casse). */
    protected boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    /**
     * Parse une chaîne en int, lance une RuntimeException avec un message lisible si invalide.
     *
     * @param value   chaîne à parser
     * @param message message d'erreur affiché à l'utilisateur
     * @return valeur entière
     * @throws RuntimeException si la chaîne n'est pas un entier valide
     */
    protected int parseInt(String value, String message) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception e) {
            throw new RuntimeException(message);
        }
    }

    // ─── Alertes ──────────────────────────────────────────────────────────────

    /**
     * Affiche une boîte de dialogue d'information.
     *
     * @param message texte à afficher
     */
    protected void info(String message) {
        alert(Alert.AlertType.INFORMATION, "Information", message);
    }

    /**
     * Affiche une boîte de dialogue d'erreur.
     *
     * @param message texte d'erreur à afficher
     */
    protected void error(String message) {
        alert(Alert.AlertType.ERROR, "Erreur", message);
    }

    /**
     * Méthode interne générique pour afficher une alerte JavaFX.
     *
     * @param type    type d'alerte (INFORMATION, ERROR, WARNING…)
     * @param title   titre de la fenêtre
     * @param message contenu du message
     */
    private void alert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
