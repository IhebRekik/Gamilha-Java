package com.gamilha.controller;

import com.gamilha.MainApp;
import com.gamilha.entity.Stream;
import com.gamilha.service.StreamService;
import com.gamilha.util.AlertUtil;
import com.gamilha.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * StreamFormController — Créer / Modifier un Stream (vue front)
 *
 * Changements vs version précédente :
 *   - Champs URL et Viewers SUPPRIMÉS du formulaire de création
 *   - Champ thumbnail avec validation URL (obligatoire en création)
 *   - URL et viewers restent modifiables en mode édition (admin)
 */
public class StreamFormController implements Initializable {

    @FXML private Label            pageTitle;
    @FXML private TextField        titleField;
    @FXML private TextArea         descArea;
    @FXML private ComboBox<String> gameCombo;
    @FXML private TextField        thumbField;     // URL image — validé
    @FXML private ComboBox<String> statusCombo;

    // Champs présents UNIQUEMENT en mode édition (cachés en création)
    @FXML private TextField        urlField;
    @FXML private Spinner<Integer> viewersSpinner;
    @FXML private VBox editOnlyBox;    // conteneur des champs édition

    // Labels d'erreur
    @FXML private Label errTitle;
    @FXML private Label errDesc;
    @FXML private Label errGame;
    @FXML private Label errThumb;   // nouveau : validation URL thumbnail
    @FXML private Label errUrl;

    @FXML private Label  obsInfo;
    @FXML private Button submitBtn;

    private final StreamService service = new StreamService();
    private Stream editing = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gameCombo.setItems(FXCollections.observableArrayList("CS2","Valorant","LoL","Dota2"));
        gameCombo.setPromptText("— Choisir un jeu —");
        statusCombo.setItems(FXCollections.observableArrayList("live","offline","ended"));
        statusCombo.setValue("live");

        if (viewersSpinner != null) {
            viewersSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999999, 0));
            viewersSpinner.setEditable(true);
        }

        // Validation en temps réel au focus-out
        titleField.focusedProperty().addListener((o,ov,f) -> { if (!f) vTitle(); });
        descArea.focusedProperty().addListener((o,ov,f)   -> { if (!f) vDesc();  });
        thumbField.focusedProperty().addListener((o,ov,f) -> { if (!f) vThumb(); });
        gameCombo.valueProperty().addListener((o,ov,v)    -> vGame());
        if (urlField != null)
            urlField.focusedProperty().addListener((o,ov,f) -> { if (!f) vUrl(); });
    }

    // ── Mode CRÉATION : URL et Viewers masqués ────────────────────────────
    public void initCreate() {
        editing = null;
        pageTitle.setText("🔴 Lancer un nouveau Stream");
        submitBtn.setText("🔴 Démarrer le Stream");
        // Cacher le bloc édition (url, viewers)
        if (editOnlyBox != null) { editOnlyBox.setVisible(false); editOnlyBox.setManaged(false); }
        if (obsInfo != null)     { obsInfo.setVisible(false);     obsInfo.setManaged(false); }
    }

    // ── Mode ÉDITION : tous les champs visibles ───────────────────────────
    public void initEdit(Stream s) {
        editing = s;
        pageTitle.setText("✏ Modifier le Stream");
        submitBtn.setText("Enregistrer");
        // Afficher le bloc édition
        if (editOnlyBox != null) { editOnlyBox.setVisible(true); editOnlyBox.setManaged(true); }

        titleField.setText(s.getTitle());
        descArea.setText(s.getDescription() != null ? s.getDescription() : "");
        gameCombo.setValue(s.getGame());
        thumbField.setText(s.getThumbnail() != null ? s.getThumbnail() : "");
        statusCombo.setValue(s.getStatus());

        if (urlField != null)
            urlField.setText(s.getUrl() != null ? s.getUrl() : "");
        if (viewersSpinner != null)
            viewersSpinner.getValueFactory().setValue(s.getViewers());

        if (obsInfo != null && s.getStreamKey() != null) {
            obsInfo.setText("🔑 " + s.getStreamKey() + "\n📡 " +
                (s.getRtmpServer() != null ? s.getRtmpServer() : "N/A"));
            obsInfo.setVisible(true); obsInfo.setManaged(true);
        }
    }

    @FXML
    private void onSubmit(ActionEvent e) {
        boolean ok = vTitle() & vDesc() & vGame() & vThumb();
        if (editing != null && urlField != null) ok = ok & vUrl();
        if (!ok) return;

        try {
            Stream s = editing != null ? editing : new Stream();
            s.setTitle(titleField.getText().trim());
            s.setDescription(descArea.getText().trim());
            s.setGame(gameCombo.getValue());
            s.setThumbnail(thumbField.getText().trim());
            s.setStatus(statusCombo.getValue());
            s.setIsLive("live".equals(statusCombo.getValue()));

            // URL et viewers uniquement en édition
            if (editing != null) {
                if (urlField != null)      s.setUrl(urlField.getText().trim());
                if (viewersSpinner != null) s.setViewers(viewersSpinner.getValue());
            } else {
                // Création : viewers = 0, url générée plus tard
                s.setViewers(0);
                s.setUrl(null);
            }

            if (editing == null) {
                s.setUserId(1);
                service.create(s);
                AlertUtil.showSuccess("✅ Stream lancé !",
                    "« " + s.getTitle() + " » est créé.\n🔑 Clé : " + s.getStreamKey());
                StreamShowController c = MainApp.loadSceneWithController("StreamShow.fxml");
                if (c != null) c.setStream(s);
            } else {
                service.update(s);
                AlertUtil.showSuccess("✅ Modifié !", "Stream mis à jour.");
                MainApp.loadScene("StreamList.fxml");
            }
        } catch (SQLException ex) {
            AlertUtil.showError("Erreur BDD", ex.getMessage());
        }
    }

    @FXML private void onCancel(ActionEvent e) { MainApp.loadScene("StreamList.fxml"); }

    // ── Validations ───────────────────────────────────────────────────────
    private boolean vTitle() {
        String err = ValidationUtil.validateTitle(titleField.getText());
        ValidationUtil.mark(titleField, err); ValidationUtil.setErr(errTitle, err);
        return err == null;
    }
    private boolean vDesc() {
        String err = ValidationUtil.validateDescription(descArea.getText());
        ValidationUtil.mark(descArea, err); ValidationUtil.setErr(errDesc, err);
        return err == null;
    }
    private boolean vGame() {
        String err = ValidationUtil.validateGame(gameCombo.getValue());
        ValidationUtil.mark(gameCombo, err); ValidationUtil.setErr(errGame, err);
        return err == null;
    }
    private boolean vThumb() {
        String err = ValidationUtil.validateThumbnailUrl(thumbField.getText());
        ValidationUtil.mark(thumbField, err); ValidationUtil.setErr(errThumb, err);
        return err == null;
    }
    private boolean vUrl() {
        if (urlField == null) return true;
        String err = ValidationUtil.validateUrl(urlField.getText());
        ValidationUtil.mark(urlField, err); ValidationUtil.setErr(errUrl, err);
        return err == null;
    }
}
