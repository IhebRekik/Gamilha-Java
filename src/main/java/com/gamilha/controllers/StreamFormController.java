package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.utils.NavigationContext;
import com.gamilha.entity.Stream;
import com.gamilha.services.InscriptionServices;
import com.gamilha.services.StreamService;
import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.SessionContext;
import com.gamilha.utils.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * StreamFormController — Créer / Modifier un Stream.
 *
 * À la création :
 *   1. Vérifie abonnement actif (InscriptionServices)
 *   2. Appelle api.video → génère streamKey + rtmpServer + playerUrl
 *   3. Sauvegarde en BDD
 *   4. Redirige vers StreamShow avec isStreamer=true → affiche bloc OBS
 *
 * Ably retiré (notifications désactivées — ajouter la clé si besoin).
 */
public class StreamFormController implements Initializable {

    @FXML private Label            pageTitle;
    @FXML private TextField        titleField;
    @FXML private TextArea         descArea;
    @FXML private ComboBox<String> gameCombo;
    @FXML private TextField        thumbField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField        urlField;
    @FXML private Spinner<Integer> viewersSpinner;
    @FXML private VBox             editOnlyBox;
    @FXML private Label            errTitle, errDesc, errGame, errThumb, errUrl;
    @FXML private Label            obsInfo;
    @FXML private Button           submitBtn;

    private final StreamService       streamService      = new StreamService();
    private final InscriptionServices inscriptionService = new InscriptionServices();
    private Stream editing = null;

    // ── Clé api.video — remplacer par votre clé ──────────────────────────
    // Obtenez-la sur https://dashboard.api.video
    private static final String APIVIDEO_KEY = ""; // ex: "abcd1234ef5678..."

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gameCombo.setItems(FXCollections.observableArrayList("CS2", "Valorant", "LoL", "Dota2"));
        gameCombo.setPromptText("— Choisir un jeu —");
        statusCombo.setItems(FXCollections.observableArrayList("live", "offline", "ended"));
        statusCombo.setValue("live");
        if (viewersSpinner != null) {
            viewersSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 999999, 0));
            viewersSpinner.setEditable(true);
        }
        titleField.focusedProperty().addListener((o, ov, f) -> { if (!f) vTitle(); });
        descArea.focusedProperty().addListener((o, ov, f)   -> { if (!f) vDesc();  });
        thumbField.focusedProperty().addListener((o, ov, f) -> { if (!f) vThumb(); });
        gameCombo.valueProperty().addListener((o, ov, v)    -> vGame());
        if (urlField != null)
            urlField.focusedProperty().addListener((o, ov, f) -> { if (!f) vUrl(); });
    }

    // ── Mode création ─────────────────────────────────────────────────────
    public void initCreate() {
        editing = null;
        pageTitle.setText("🔴 Lancer un nouveau Stream");
        submitBtn.setText("🔴 Démarrer le Stream");
        if (editOnlyBox != null) { editOnlyBox.setVisible(false); editOnlyBox.setManaged(false); }
        if (obsInfo != null)     { obsInfo.setVisible(false);     obsInfo.setManaged(false); }
    }

    // ── Mode édition ──────────────────────────────────────────────────────
    public void initEdit(Stream s) {
        editing = s;
        pageTitle.setText("✏ Modifier le Stream");
        submitBtn.setText("Enregistrer");
        if (editOnlyBox != null) { editOnlyBox.setVisible(true); editOnlyBox.setManaged(true); }
        titleField.setText(s.getTitle());
        descArea.setText(s.getDescription() != null ? s.getDescription() : "");
        gameCombo.setValue(s.getGame());
        thumbField.setText(s.getThumbnail() != null ? s.getThumbnail() : "");
        statusCombo.setValue(s.getStatus());
        if (urlField != null)       urlField.setText(s.getUrl() != null ? s.getUrl() : "");
        if (viewersSpinner != null) viewersSpinner.getValueFactory().setValue(s.getViewers());
        if (obsInfo != null && s.getStreamKey() != null) {
            obsInfo.setText("🔑 " + s.getStreamKey() + "\n📡 " +
                    (s.getRtmpServer() != null ? s.getRtmpServer() : "N/A"));
            obsInfo.setVisible(true); obsInfo.setManaged(true);
        }
    }

    @FXML
    private void onSubmit(ActionEvent e) {
        // 1. Validation champs
        boolean ok = vTitle() & vDesc() & vGame() & vThumb();
        if (editing != null && urlField != null) ok = ok & vUrl();
        if (!ok) return;

        // 2. Unicité du titre (seulement en création)
        if (editing == null) {
            try {
                if (streamService.existsByTitle(titleField.getText().trim())) {
                    ValidationUtil.setErr(errTitle, "Un stream avec ce titre existe déjà.");
                    ValidationUtil.mark(titleField, "Un stream avec ce titre existe déjà.");
                    return;
                }
            } catch (SQLException ex) {
                AlertUtil.showError("Erreur BDD", ex.getMessage()); return;
            }
        }

        // 3. Vérification abonnement actif (en création uniquement)
        if (editing == null) {
            var user = SessionContext.getCurrentUser();
            if (user == null) {
                AlertUtil.showError("Non connecté", "Vous devez être connecté pour lancer un stream.");
                return;
            }
            try {
                List<Integer> abonnementIds = inscriptionService.getUserAbonnementsActifs(user);
                if (abonnementIds.isEmpty()) {
                    AlertUtil.showWarning("Abonnement requis",
                            "Votre abonnement ne permet pas le streaming.\n" +
                            "Veuillez souscrire à un abonnement incluant l'option 'stream'.");
                    return;
                }
            } catch (Exception ex) {
                // Si la table n'existe pas encore, on ignore et on continue
                System.err.println("Inscription check skipped: " + ex.getMessage());
            }
        }

        try {
            Stream s = editing != null ? editing : new Stream();
            s.setTitle(titleField.getText().trim());
            s.setDescription(descArea.getText().trim());
            s.setGame(gameCombo.getValue());
            s.setThumbnail(thumbField.getText().trim());
            s.setStatus(statusCombo.getValue());
            s.setIsLive("live".equals(statusCombo.getValue()));

            if (editing != null) {
                if (urlField != null)       s.setUrl(urlField.getText().trim());
                if (viewersSpinner != null) s.setViewers(viewersSpinner.getValue());
            } else {
                s.setViewers(0);
                s.setUrl(null);
                s.setUserId(SessionContext.getCurrentUser().getId());
            }

            if (editing == null) {
                // 4. Appel api.video — génère streamKey + rtmpServer + playerUrl
                //    Identique à ApiVideoService::createLiveStream() Symfony
                if (!APIVIDEO_KEY.isBlank()) {
                    try {
                        String[] liveData = createApiVideoLiveStream(s.getTitle());
                        s.setApiVideoId(liveData[0]);
                        s.setStreamKey(liveData[1]);
                        s.setRtmpServer("rtmp://broadcast.api.video/s");
                        s.setUrl("https://embed.api.video/live/" + liveData[0]);
                        s.setStatus("offline");
                        s.setIsLive(false);
                    } catch (Exception apiEx) {
                        System.err.println("api.video error: " + apiEx.getMessage());
                        // Si api.video échoue → on garde la streamKey générée localement
                    }
                }

                streamService.create(s);

                // 5. Redirection vers StreamShow avec isStreamer = true
                //    → le bloc OBS avec la clé sera visible uniquement à cet utilisateur
                StreamShowController c = NavigationContext.navigateWithController("User/StreamShow.fxml");
                if (c != null) c.setStream(s, true);

            } else {
                streamService.update(s);
                AlertUtil.showSuccess("✅ Modifié !", "Stream mis à jour.");
                NavigationContext.navigate("User/StreamList.fxml");
            }

        } catch (SQLException ex) {
            AlertUtil.showError("Erreur BDD", ex.getMessage());
        }
    }

    @FXML private void onCancel(ActionEvent e) { NavigationContext.navigate("User/StreamList.fxml"); }

    // ── api.video — équivalent de ApiVideoService::createLiveStream() ────
    private String[] createApiVideoLiveStream(String title) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // 1. Authentification
        String tokenResp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://ws.api.video/auth/api-key"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"apiKey\":\"" + APIVIDEO_KEY + "\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()).body();
        String token = extractJson(tokenResp, "access_token");

        // 2. Création du live stream
        String liveResp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://ws.api.video/live-streams"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"name\":\"" + title.replace("\"", "\\\"") + "\",\"record\":false}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()).body();

        return new String[]{
                extractJson(liveResp, "liveStreamId"),
                extractJson(liveResp, "streamKey")
        };
    }

    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? "" : json.substring(start, end);
    }

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
