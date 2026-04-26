package com.gamilha.controllers.evenement;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.*;
import com.gamilha.services.BracketService;
import com.gamilha.services.EquipeService;
import com.gamilha.services.EvenementService;
import com.gamilha.services.GameMatchService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Add / Edit form for Evenement.
 */
public class EvenementFormController extends BaseController {

    private final EvenementService evenementService = new EvenementService();
    private final EquipeService equipeService = new EquipeService();
    private final BracketService bracketService = new BracketService();
    private final GameMatchService matchService = new GameMatchService();

    private NavigationCallback nav;
    private Evenement editingEvenement;

    // Form fields
    private TextField evNom;
    private TextArea evDescription;
    private TextField evJeu;
    private ComboBox<String> evType;
    private DatePicker evDateDebut;
    private DatePicker evDateFin;
    private ComboBox<String> evStatut;
    private TextArea evRegles;
    private TextField evImage;
    private VBox evEquipeChecks;
    private final Map<Integer, CheckBox> evEquipeCheckById = new LinkedHashMap<>();
    private ComboBox<String> evBracketType;

    public void setNav(NavigationCallback nav) { this.nav = nav; }

    /** Call before navigating to create mode. */
    public void prepareForCreate() { editingEvenement = null; }

    /** Call before navigating to edit mode. */
    public void prepareForEdit(Evenement ev) { editingEvenement = ev; }

    // â”€â”€â”€ Build page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Node build() {
        VBox root = pageScaffold("Formulaire Evenement", "");
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();

        evNom = new TextField();
        evDescription = new TextArea();
        evJeu = new TextField();
        evType = new ComboBox<>(FXCollections.observableArrayList("online", "offline"));
        evDateDebut = new DatePicker();
        evDateFin = new DatePicker();
        evStatut = new ComboBox<>(FXCollections.observableArrayList("prévu", "en cours", "terminé"));
        evRegles = new TextArea();
        evImage = new TextField();
        evImage.setPromptText("https://.../image.jpg");

        evEquipeChecks = new VBox(6);
        evEquipeChecks.setPadding(new Insets(8));
        loadEquipesToCheckboxes();
        ScrollPane equipesScroll = pageScroller(evEquipeChecks);
        equipesScroll.setPrefHeight(140);

        evBracketType = new ComboBox<>(FXCollections.observableArrayList("single elimination", "double elimination"));
        evBracketType.setValue("single elimination");

        GridPane form = formGrid();
        addFormRow(form, 0, "Nom", evNom);
        addFormRow(form, 1, "Description", evDescription);
        addFormRow(form, 2, "Jeu", evJeu);
        addFormRow(form, 3, "Type", evType);
        addFormRow(form, 4, "Date debut", evDateDebut);
        addFormRow(form, 5, "Date fin", evDateFin);
        addFormRow(form, 6, "Statut", evStatut);
        addFormRow(form, 7, "Regles", evRegles);
        addFormRow(form, 8, "Image", evImage);
        addFormRow(form, 9, "Equipes participantes", equipesScroll);
        addFormRow(form, 10, "Type bracket auto", evBracketType);

        // Populate if editing
        if (editingEvenement != null) populateForm(editingEvenement);

        Button save = new Button("Enregistrer");
        Button clear = new Button("Vider");
        Button generate = new Button("Generer Bracket + Matchs");

        save.setOnAction(e -> handleSave(admin));
        clear.setOnAction(e -> { editingEvenement = null; resetForm(); });
        generate.setOnAction(e -> handleGenerate(admin));

        root.getChildren().addAll(form, new HBox(8, save, clear, generate));
        return pageScroller(root);
    }

    // â”€â”€â”€ Handlers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void handleSave(boolean admin) {
        try {
            User user = SessionContext.getCurrentUser();
            Evenement ev = editingEvenement == null ? new Evenement() : editingEvenement;
            ev.setNom(evNom.getText());
            ev.setDescription(evDescription.getText());
            ev.setJeu(evJeu.getText());
            ev.setTypeEvenement(evType.getValue());
            ev.setDateDebut(evDateDebut.getValue());
            ev.setDateFin(evDateFin.getValue());
            ev.setStatut(evStatut.getValue());
            ev.setRegles(evRegles.getText());
            ev.setImage(evImage.getText());

            if (editingEvenement == null) {
                ev.setCreatedById(user.getId());
                ev.setCreatedAt(LocalDateTime.now());
                evenementService.ajouterEntite(ev);
            } else {
                if (!canEditEvenement(admin, editingEvenement)) {
                    error("Vous ne pouvez modifier que vos evenements."); return;
                }
                evenementService.modifierEntite(ev);
            }

            List<Integer> ids = getSelectedEquipeIds();
            if (ids.size() < 2 || ids.size() % 2 != 0) {
                error("Selectionnez un nombre pair d'equipes (minimum 2)."); return;
            }
            evenementService.replaceEquipesParticipantes(ev.getIdEvenement(), ids);

            info(editingEvenement == null ? "Evenement cree." : "Evenement modifie.");
            editingEvenement = null;
            resetForm();
        } catch (Exception ex) {
            error("Enregistrement impossible: " + ex.getMessage());
        }
    }

    private void handleGenerate(boolean admin) {
        if (editingEvenement == null || editingEvenement.getIdEvenement() == null) {
            error("Enregistrez ou chargez un evenement avant la generation."); return;
        }
        if (!canEditEvenement(admin, editingEvenement)) {
            error("Vous ne pouvez generer que pour vos evenements."); return;
        }
        try {
            evenementService.generateBracketAndMatches(editingEvenement.getIdEvenement(), evBracketType.getValue());
            promptScheduleForGeneratedMatches(editingEvenement.getIdEvenement());
            info("Bracket et matchs generes.");
        } catch (Exception ex) {
            error("Generation impossible: " + ex.getMessage());
        }
    }

    // â”€â”€â”€ Populate / clear â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void populateForm(Evenement selected) {
        if (evNom == null || selected == null) return;
        evNom.setText(selected.getNom());
        evDescription.setText(selected.getDescription());
        evJeu.setText(selected.getJeu());
        evType.setValue(selected.getTypeEvenement());
        evDateDebut.setValue(selected.getDateDebut());
        evDateFin.setValue(selected.getDateFin());
        evStatut.setValue(selected.getStatut());
        evRegles.setText(selected.getRegles());
        evImage.setText(selected.getImage());
        List<Integer> ids = evenementService.findEquipesParticipantes(selected.getIdEvenement());
        setSelectedEquipes(ids);
    }

    public void refreshEquipeChoices() {
        if (evEquipeChecks == null) return;
        List<Integer> current = getSelectedEquipeIds();
        loadEquipesToCheckboxes();
        setSelectedEquipes(current);
    }

    private void resetForm() {
        if (evNom == null) return;
        clearEvenementForm(evNom, evDescription, evJeu, evType, evDateDebut, evDateFin, evStatut, evRegles, evImage, null, evBracketType);
        clearEquipeChecks();
    }

    // â”€â”€â”€ Checkbox equipes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void loadEquipesToCheckboxes() {
        evEquipeCheckById.clear();
        if (evEquipeChecks != null) evEquipeChecks.getChildren().clear();
        List<Equipe> equipes = equipeService.findAll();
        for (Equipe equipe : equipes) {
            CheckBox check = new CheckBox(equipe.getNomEquipe() + " (" + str(equipe.getIdEquipe()) + ")");
            check.setUserData(equipe.getIdEquipe());
            evEquipeCheckById.put(equipe.getIdEquipe(), check);
            if (evEquipeChecks != null) evEquipeChecks.getChildren().add(check);
        }
    }

    private List<Integer> getSelectedEquipeIds() {
        return evEquipeCheckById.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void setSelectedEquipes(List<Integer> ids) {
        clearEquipeChecks();
        if (ids == null) return;
        ids.forEach(id -> { CheckBox c = evEquipeCheckById.get(id); if (c != null) c.setSelected(true); });
    }

    private void clearEquipeChecks() {
        evEquipeCheckById.values().forEach(c -> c.setSelected(false));
    }

    // â”€â”€â”€ Schedule dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void promptScheduleForGeneratedMatches(Integer evenementId) {
        List<Bracket> brackets = bracketService.findAll().stream()
                .filter(b -> b.getEvenementId() != null && b.getEvenementId().equals(evenementId))
                .collect(Collectors.toList());
        List<Integer> bracketIds = brackets.stream().map(Bracket::getIdBracket).collect(Collectors.toList());
        List<GameMatch> matches = matchService.findAll().stream()
                .filter(m -> m.getBracketId() != null && bracketIds.contains(m.getBracketId()))
                .sorted(Comparator.comparing((GameMatch m) -> m.getTour() == null ? 0 : m.getTour())
                        .thenComparing(m -> m.getIdMatch() == null ? 0 : m.getIdMatch()))
                .collect(Collectors.toList());
        for (GameMatch match : matches) {
            Optional<LocalDateTime> dt = askMatchDateTime(match);
            if (dt.isPresent()) { match.setDateMatch(dt.get()); matchService.modifierEntite(match); }
        }
    }

    private Optional<LocalDateTime> askMatchDateTime(GameMatch match) {
        Dialog<LocalDateTime> dialog = new Dialog<>();
        dialog.setTitle("Planification du match");
        String teamA = nullSafe(match.getEquipeANom()).isBlank() ? "Equipe A" : nullSafe(match.getEquipeANom());
        String teamB = nullSafe(match.getEquipeBNom()).isBlank() ? "Equipe B" : nullSafe(match.getEquipeBNom());
        dialog.setHeaderText("Tour " + str(match.getTour()) + " - " + teamA + " vs " + teamB);
        ButtonType okType = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, ButtonType.CANCEL);

        DatePicker dp = new DatePicker(match.getDateMatch() == null
                ? LocalDate.now().plusDays(1) : match.getDateMatch().toLocalDate());
        TextField tf = new TextField(match.getDateMatch() == null
                ? "14:00" : String.format("%02d:%02d", match.getDateMatch().getHour(), match.getDateMatch().getMinute()));

        GridPane grid = formGrid();
        addFormRow(grid, 0, "Date", dp);
        addFormRow(grid, 1, "Heure (HH:mm)", tf);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn != okType) return null;
            if (dp.getValue() == null) throw new RuntimeException("La date est obligatoire.");
            String[] hm = tf.getText() == null ? new String[0] : tf.getText().trim().split(":");
            if (hm.length != 2) throw new RuntimeException("Heure invalide. Format attendu HH:mm.");
            int h = Integer.parseInt(hm[0]), m = Integer.parseInt(hm[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) throw new RuntimeException("Heure invalide.");
            return dp.getValue().atTime(h, m);
        });
        try { return dialog.showAndWait(); }
        catch (Exception ex) { error(ex.getMessage()); return Optional.empty(); }
    }
}

