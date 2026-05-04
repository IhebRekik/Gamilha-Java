package com.gamilha.controllers.bracket;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.Bracket;
import com.gamilha.entity.Evenement;
import com.gamilha.services.BracketService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Add / Edit form for Bracket.
 */
public class BracketFormController extends BaseController {

    private final BracketService bracketService = new BracketService();

    private NavigationCallback nav;
    private Bracket editingBracket;

    // Form fields
    private ComboBox<Evenement> brEvenement;
    private ComboBox<String> brType;
    private TextField brTours;
    private ComboBox<String> brStatut;

    public void setNav(NavigationCallback nav) { this.nav = nav; }
    public void prepareForCreate() { editingBracket = null; }
    public void prepareForEdit(Bracket b) { editingBracket = b; }

    // â”€â”€â”€ Build page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Node build() {
        VBox root = pageScaffold("Formulaire Bracket", "");
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();

        brEvenement = new ComboBox<>();
        loadEvenementChoices(brEvenement, admin);
        brType = new ComboBox<>(FXCollections.observableArrayList("single elimination", "double elimination"));
        brTours = new TextField();
        brStatut = new ComboBox<>(FXCollections.observableArrayList("en attente", "en cours", "terminé"));

        GridPane form = formGrid();
        addFormRow(form, 0, "Evenement", brEvenement);
        addFormRow(form, 1, "Type", brType);
        addFormRow(form, 2, "Nombre tours", brTours);
        addFormRow(form, 3, "Statut", brStatut);

        if (editingBracket != null) populateForm(editingBracket);

        Button save = new Button("Enregistrer");
        Button clear = new Button("Vider");

        save.setOnAction(e -> handleSave(admin));
        clear.setOnAction(e -> { editingBracket = null; resetForm(); });

        root.getChildren().addAll(form, new HBox(8, save, clear));
        return pageScroller(root);
    }

    // â”€â”€â”€ Handlers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void handleSave(boolean admin) {
        try {
            Bracket b = editingBracket == null ? new Bracket() : editingBracket;
            b.setTypeBracket(brType.getValue());
            b.setNombreTours(parseInt(brTours.getText(), "Nombre de tours invalide."));
            b.setStatut(brStatut.getValue());
            Evenement ev = brEvenement.getValue();
            b.setEvenementId(ev == null ? null : ev.getIdEvenement());

            if (editingBracket == null) {
                if (!admin && ev != null && !canEditEvenement(false, ev)) {
                    error("Vous ne pouvez creer un bracket que pour vos evenements."); return;
                }
                bracketService.ajouterEntite(b);
            } else {
                if (!canEditBracket(admin, editingBracket)) {
                    error("Vous ne pouvez modifier que les brackets de vos evenements."); return;
                }
                bracketService.modifierEntite(b);
            }

            info(editingBracket == null ? "Bracket cree." : "Bracket modifie.");
            editingBracket = null;
            resetForm();
        } catch (Exception ex) {
            error("Enregistrement impossible: " + ex.getMessage());
        }
    }

    // â”€â”€â”€ Populate / clear â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void populateForm(Bracket selected) {
        if (brEvenement == null || selected == null) return;
        brType.setValue(selected.getTypeBracket());
        brTours.setText(str(selected.getNombreTours()));
        brStatut.setValue(selected.getStatut());
        for (Evenement ev : brEvenement.getItems()) {
            if (ev.getIdEvenement().equals(selected.getEvenementId())) { brEvenement.setValue(ev); break; }
        }
    }

    public void refreshEvenementChoices() {
        if (brEvenement == null) return;
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();
        Integer selectedId = brEvenement.getValue() == null ? null : brEvenement.getValue().getIdEvenement();
        loadEvenementChoices(brEvenement, admin);
        if (selectedId != null) {
            for (Evenement ev : brEvenement.getItems()) {
                if (selectedId.equals(ev.getIdEvenement())) { brEvenement.setValue(ev); break; }
            }
        }
    }

    private void resetForm() {
        if (brEvenement == null) return;
        clearBracketForm(brEvenement, brType, brTours, brStatut);
    }
}

