package com.gamilha.controllers.gamematch;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.Bracket;
import com.gamilha.entity.Equipe;
import com.gamilha.entity.GameMatch;
import com.gamilha.services.GameMatchService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

/**
 * Add / Edit form for GameMatch.
 */
public class GameMatchFormController extends BaseController {

    private final GameMatchService matchService = new GameMatchService();

    private NavigationCallback nav;
    private GameMatch editingMatch;

    // Form fields
    private ComboBox<Bracket> maBracket;
    private ComboBox<Equipe> maEquipeA;
    private ComboBox<Equipe> maEquipeB;
    private TextField maTour;
    private TextField maScoreA;
    private TextField maScoreB;
    private ComboBox<String> maStatut;
    private DatePicker maDate;
    private TextField maTime;

    public void setNav(NavigationCallback nav) { this.nav = nav; }
    public void prepareForCreate() { editingMatch = null; }
    public void prepareForEdit(GameMatch m) { editingMatch = m; }

    // â”€â”€â”€ Build page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Node build() {
        VBox root = pageScaffold("Formulaire Match", "");
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();

        maBracket = new ComboBox<>();
        loadBracketChoices(maBracket, admin);
        configureBracketDisplay();
        maEquipeA = new ComboBox<>();
        maEquipeB = new ComboBox<>();
        loadEquipeChoicesForMatch(maEquipeA, maEquipeB, admin);
        maTour = new TextField();
        maScoreA = new TextField("0");
        maScoreB = new TextField("0");
        maStatut = new ComboBox<>(FXCollections.observableArrayList("à venir", "en cours", "terminé"));
        maDate = new DatePicker();
        maTime = new TextField("14:00");

        GridPane form = formGrid();
        addFormRow(form, 0, "Bracket", maBracket);
        addFormRow(form, 1, "Tour", maTour);
        addFormRow(form, 2, "Equipe A", maEquipeA);
        addFormRow(form, 3, "Equipe B", maEquipeB);
        addFormRow(form, 4, "Score A", maScoreA);
        addFormRow(form, 5, "Score B", maScoreB);
        addFormRow(form, 6, "Statut", maStatut);
        addFormRow(form, 7, "Date", maDate);
        addFormRow(form, 8, "Heure", maTime);

        if (editingMatch != null) populateForm(editingMatch);

        Button save = new Button("Enregistrer");
        Button clear = new Button("Vider");

        save.setOnAction(e -> handleSave(admin));
        clear.setOnAction(e -> { editingMatch = null; resetForm(); });

        root.getChildren().addAll(form, new HBox(8, save, clear));
        return pageScroller(root);
    }

    // â”€â”€â”€ Handlers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void handleSave(boolean admin) {
        try {
            GameMatch data = readMatchForm(maBracket, maEquipeA, maEquipeB, maTour, maScoreA, maScoreB, maStatut, maDate, maTime);
            if (editingMatch == null) {
                if (!admin && data.getBracketId() != null && !canEditBracketId(data.getBracketId())) {
                    error("Vous ne pouvez creer un match que pour vos brackets."); return;
                }
                matchService.ajouterEntite(data);
            } else {
                if (!admin && !canEditMatch(editingMatch)) {
                    error("Vous ne pouvez modifier que les matchs de vos evenements."); return;
                }
                editingMatch.setBracketId(data.getBracketId());
                editingMatch.setEquipeAId(data.getEquipeAId());
                editingMatch.setEquipeBId(data.getEquipeBId());
                editingMatch.setTour(data.getTour());
                editingMatch.setScoreEquipeA(data.getScoreEquipeA());
                editingMatch.setScoreEquipeB(data.getScoreEquipeB());
                editingMatch.setStatut(data.getStatut());
                editingMatch.setDateMatch(data.getDateMatch());
                matchService.modifierEntite(editingMatch);
            }
            info(editingMatch == null ? "Match cree." : "Match modifie.");
            editingMatch = null;
            resetForm();
        } catch (Exception ex) {
            error("Enregistrement impossible: " + ex.getMessage());
        }
    }

    // â”€â”€â”€ Populate / clear â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void populateForm(GameMatch selected) {
        if (maBracket == null || selected == null) return;
        for (Bracket b : maBracket.getItems()) {
            if (b.getIdBracket().equals(selected.getBracketId())) { maBracket.setValue(b); break; }
        }
        selectEquipe(maEquipeA, selected.getEquipeAId());
        selectEquipe(maEquipeB, selected.getEquipeBId());
        maTour.setText(str(selected.getTour()));
        maScoreA.setText(str(selected.getScoreEquipeA()));
        maScoreB.setText(str(selected.getScoreEquipeB()));
        maStatut.setValue(selected.getStatut());
        if (selected.getDateMatch() != null) {
            maDate.setValue(selected.getDateMatch().toLocalDate());
            maTime.setText(String.format("%02d:%02d", selected.getDateMatch().getHour(), selected.getDateMatch().getMinute()));
        }
    }

    public void refreshChoices() {
        if (maBracket == null || maEquipeA == null || maEquipeB == null) return;
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();
        Integer selectedBracket = maBracket.getValue() == null ? null : maBracket.getValue().getIdBracket();
        Integer selectedA = maEquipeA.getValue() == null ? null : maEquipeA.getValue().getIdEquipe();
        Integer selectedB = maEquipeB.getValue() == null ? null : maEquipeB.getValue().getIdEquipe();
        loadBracketChoices(maBracket, admin);
        loadEquipeChoicesForMatch(maEquipeA, maEquipeB, admin);
        if (selectedBracket != null) {
            for (Bracket b : maBracket.getItems()) {
                if (selectedBracket.equals(b.getIdBracket())) { maBracket.setValue(b); break; }
            }
        }
        selectEquipe(maEquipeA, selectedA);
        selectEquipe(maEquipeB, selectedB);
        if (editingMatch != null) populateForm(editingMatch);
    }

    private void resetForm() {
        if (maBracket == null) return;
        clearMatchForm(maBracket, maEquipeA, maEquipeB, maTour, maScoreA, maScoreB, maStatut, maDate, maTime);
    }

    private void configureBracketDisplay() {
        maBracket.setConverter(new StringConverter<>() {
            @Override
            public String toString(Bracket bracket) {
                return bracketLabel(bracket);
            }

            @Override
            public Bracket fromString(String string) {
                return null;
            }
        });

        maBracket.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(Bracket item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : bracketLabel(item));
            }
        });
    }

    private String bracketLabel(Bracket bracket) {
        if (bracket == null) return "";
        String type = bracket.getTypeBracket() == null ? "Bracket" : bracket.getTypeBracket();
        String eventName = bracket.getEvenementNom();
        return (eventName == null || eventName.isBlank()) ? type : type + " - " + eventName;
    }
}

