package com.gamilha.controllers.equipe;

import com.gamilha.controllers.shared.BaseController;
import com.gamilha.controllers.shared.NavigationCallback;
import com.gamilha.entity.Equipe;
import com.gamilha.entity.User;
import com.gamilha.services.EquipeService;
import com.gamilha.services.UserLookupService;
import com.gamilha.utils.SessionContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Add / Edit form for Equipe.
 */
public class EquipeFormController extends BaseController {

    private final EquipeService equipeService = new EquipeService();
    private final UserLookupService userLookupService = new UserLookupService();

    private NavigationCallback nav;
    private Equipe editingEquipe;

    // Form fields
    private TextField eqNom;
    private TextField eqTag;
    private TextField eqLogo;
    private TextField eqPays;
    private DatePicker eqDateCreation;
    private ComboBox<String> eqNiveau;
    private ListView<Integer> eqMembers;

    public void setNav(NavigationCallback nav) { this.nav = nav; }
    public void prepareForCreate() { editingEquipe = null; }
    public void prepareForEdit(Equipe eq) { editingEquipe = eq; }

    // â”€â”€â”€ Build page â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public Node build() {
        VBox root = pageScaffold("Formulaire Equipe", "");
        boolean admin = SessionContext.getCurrentUser() != null && SessionContext.getCurrentUser().isAdmin();

        eqNom = new TextField();
        eqTag = new TextField();
        eqLogo = new TextField();
        eqPays = new TextField();
        eqDateCreation = new DatePicker();
        eqNiveau = new ComboBox<>(FXCollections.observableArrayList("amateur", "semi-pro", "pro"));

        eqMembers = new ListView<>();
        eqMembers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        eqMembers.setPrefHeight(120);

        Map<Integer, String> users = userLookupService.findAllUsers();
        ObservableList<Integer> userIds = FXCollections.observableArrayList(users.keySet());
        eqMembers.setItems(userIds);
        eqMembers.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                CheckBox cb = new CheckBox(users.get(item));
                cb.setSelected(list.getSelectionModel().getSelectedIndices().contains(getIndex()));
                cb.setOnAction(evt -> {
                    if (cb.isSelected()) list.getSelectionModel().select(getIndex());
                    else list.getSelectionModel().clearSelection(getIndex());
                });
                setText(null); setGraphic(cb);
            }
        });

        GridPane form = formGrid();
        addFormRow(form, 0, "Nom", eqNom);
        addFormRow(form, 1, "Tag", eqTag);
        addFormRow(form, 2, "Logo", eqLogo);
        addFormRow(form, 3, "Pays", eqPays);
        addFormRow(form, 4, "Date creation", eqDateCreation);
        addFormRow(form, 5, "Niveau", eqNiveau);
        addFormRow(form, 6, "Membres", eqMembers);

        if (editingEquipe != null) populateForm(editingEquipe);

        Button save = new Button("Enregistrer");
        Button clear = new Button("Vider");

        save.setOnAction(e -> handleSave(admin));
        clear.setOnAction(e -> { editingEquipe = null; resetForm(); });

        root.getChildren().addAll(form, new HBox(8, save, clear));
        return pageScroller(root);
    }

    // â”€â”€â”€ Handlers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void handleSave(boolean admin) {
        try {
            User user = SessionContext.getCurrentUser();
            Equipe eq = editingEquipe == null ? new Equipe() : editingEquipe;
            eq.setNomEquipe(eqNom.getText());
            eq.setTag(eqTag.getText());
            eq.setLogo(eqLogo.getText());
            eq.setPays(eqPays.getText());
            eq.setDateCreation(eqDateCreation.getValue());
            eq.setNiveau(eqNiveau.getValue());

            if (editingEquipe == null) {
                eq.setOwnerId(user.getId());
                equipeService.ajouterEntite(eq);
            } else {
                if (!canEditEquipe(admin, editingEquipe)) {
                    error("Vous ne pouvez modifier que vos equipes."); return;
                }
                equipeService.modifierEntite(eq);
            }

            List<Integer> selected = new ArrayList<>(eqMembers.getSelectionModel().getSelectedItems());
            equipeService.replaceMembers(eq.getIdEquipe(), selected);

            info(editingEquipe == null ? "Equipe creee." : "Equipe modifiee.");
            editingEquipe = null;
            resetForm();
        } catch (Exception ex) {
            error("Enregistrement impossible: " + ex.getMessage());
        }
    }

    // â”€â”€â”€ Populate / clear â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public void populateForm(Equipe selected) {
        if (eqNom == null || selected == null) return;
        eqNom.setText(selected.getNomEquipe());
        eqTag.setText(selected.getTag());
        eqLogo.setText(selected.getLogo());
        eqPays.setText(selected.getPays());
        eqDateCreation.setValue(selected.getDateCreation());
        eqNiveau.setValue(selected.getNiveau());
        eqMembers.getSelectionModel().clearSelection();
        List<Integer> memberIds = equipeService.findMemberIds(selected.getIdEquipe());
        for (int i = 0; i < eqMembers.getItems().size(); i++) {
            if (memberIds.contains(eqMembers.getItems().get(i))) {
                eqMembers.getSelectionModel().select(i);
            }
        }
    }

    private void resetForm() {
        if (eqNom == null) return;
        clearEquipeForm(eqNom, eqTag, eqLogo, eqPays, eqDateCreation, eqNiveau, eqMembers);
    }
}

