package com.gamilha.controllers;

import com.gamilha.MainApp;
import com.gamilha.entity.Stream;
import com.gamilha.services.StreamService;
import com.gamilha.utils.AlertUtil;
import com.gamilha.utils.NavigationContext;
import com.gamilha.utils.SessionContext;
import com.gamilha.utils.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.net.http.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

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
    @FXML private Label errTitle, errDesc, errGame, errThumb, errUrl;
    @FXML private Label  obsInfo;
    @FXML private Button submitBtn;

    private final StreamService      streamService      = new StreamService();
    private Stream editing = null;

    // ── Clés API ─────────────────────────────────────────────────────────
    // Mettre vos clés dans config.properties (src/main/resources/com/gamilha/)
    //   apivideo.key=votre_cle_ici
    //   ably.key=votre_cle_ably_ici
    private static String APIVIDEO_KEY() { return com.gamilha.utils.AppConfig.get("apivideo.key", ""); }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        gameCombo.setItems(FXCollections.observableArrayList("CS2","Valorant","LoL","Dota2"));
        gameCombo.setPromptText("— Choisir un jeu —");
        statusCombo.setItems(FXCollections.observableArrayList("live","offline","ended"));
        statusCombo.setValue("live");
        if (viewersSpinner != null) {
            viewersSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0,999999,0));
            viewersSpinner.setEditable(true);
        }
        titleField.focusedProperty().addListener((o,ov,f) -> { if (!f) vTitle(); });
        descArea.focusedProperty().addListener((o,ov,f)   -> { if (!f) vDesc();  });
        thumbField.focusedProperty().addListener((o,ov,f) -> { if (!f) vThumb(); });
        gameCombo.valueProperty().addListener((o,ov,v)    -> vGame());
        if (urlField != null)
            urlField.focusedProperty().addListener((o,ov,f) -> { if (!f) vUrl(); });
    }

    public void initCreate() {
        editing = null;
        pageTitle.setText("🔴 Lancer un nouveau Stream");
        submitBtn.setText("🔴 Démarrer le Stream");
        if (editOnlyBox != null) { editOnlyBox.setVisible(false); editOnlyBox.setManaged(false); }
        if (obsInfo != null)     { obsInfo.setVisible(false);     obsInfo.setManaged(false); }
    }

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
    private void onSubmit(ActionEvent e) throws SQLException {
        boolean ok = vTitle() & vDesc() & vGame() & vThumb();
        if (editing != null && urlField != null) ok = ok & vUrl();
        if (!ok) return;

        // ── Test unicité du titre ─────────────────────────────────────────
        if (editing == null && streamService.existsByTitle(titleField.getText().trim())) {
            ValidationUtil.setErr(errTitle, "Un stream avec ce titre existe déjà.");
            ValidationUtil.mark(titleField, "Un stream avec ce titre existe déjà.");
            return;
        }

        // Vérification abonnement supprimée — tous les users peuvent streamer

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
                // ── ② Appel api.video — génère streamKey + rtmpServer + playerUrl ──
                boolean apiVideoOk = !APIVIDEO_KEY().isBlank()
                        && !APIVIDEO_KEY().startsWith("VOTRE")
                        && !APIVIDEO_KEY().startsWith("METTEZ");
                if (apiVideoOk) {
                    try {
                        var liveData = createApiVideoLiveStream(s.getTitle());
                        s.setApiVideoId(liveData[0]);
                        s.setStreamKey(liveData[1]);
                        s.setRtmpServer("rtmp://broadcast.api.video/s");
                        s.setUrl(liveData[2]);
                    } catch (Exception apiEx) {
                        System.err.println("api.video error: " + apiEx.getMessage());
                        // Fallback : générer clé locale
                        s.setStreamKey(generateLocalKey());
                        s.setRtmpServer("rtmp://broadcast.api.video/s");
                    }
                } else {
                    // ⚠ Clé api.video manquante dans config.properties
                    // → La clé générée localement NE FONCTIONNERA PAS avec OBS/api.video
                    AlertUtil.showWarning("⚠ Clé api.video manquante",
                            "Ajoutez votre clé api.video dans :\n" +
                                    "src/main/resources/com/gamilha/config.properties\n\n" +
                                    "apivideo.key=VOTRE_CLE_ICI\n\n" +
                                    "Sans cette clé, OBS ne pourra pas se connecter.");
                    s.setStreamKey("CLEF_INVALIDE_CONFIGURER_APIVIDEO");
                    s.setRtmpServer("rtmp://broadcast.api.video/s");
                }
                s.setStatus("offline");
                s.setIsLive(false);

                streamService.create(s);

                // Pas de notification ici : le toast est envoyé uniquement
                // quand le stream passe vraiment en LIVE (OBS connecté).

                AlertUtil.showSuccess("✅ Stream lancé !",
                        "« " + s.getTitle() + " » est créé.\n🔑 Clé : " + s.getStreamKey());

                // Redirige vers StreamShow — la section OBS y sera visible
                StreamShowController c = MainApp.loadSceneWithController("User/StreamShow.fxml");
                if (c != null) c.setStream(s, true); // true = c'est le streamer

            } else {
                streamService.update(s);
                AlertUtil.showSuccess("✅ Modifié !", "Stream mis à jour.");
                MainApp.loadScene("User/StreamList.fxml");
            }

        } catch (SQLException ex) {
            AlertUtil.showError("Erreur BDD", ex.getMessage());
        }
    }

    @FXML
    private void onCancel(ActionEvent e) {

        // 1. recharger la navbar
        MainApp.openDashboard(SessionContext.getCurrentUser());

        // 2. charger StreamList dans contentArea
        javafx.application.Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/gamilha/interfaces/User/StreamList.fxml")
                );

                Parent root = loader.load();

                BorderPane contentArea = NavigationContext.getContentArea();

                if (contentArea != null) {
                    contentArea.setCenter(root);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
    // ── api.video : crée un live stream et retourne [liveStreamId, streamKey] ──
    // Équivalent de ApiVideoService::createLiveStream() Symfony
    private String[] createApiVideoLiveStream(String title) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        // 1. Obtenir un token
        String tokenResp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://ws.api.video/auth/api-key"))
                        .header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"apiKey\":\"" + APIVIDEO_KEY() + "\"}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).body();
        String token = extractJson(tokenResp, "access_token");
        if (token.isBlank()) {
            throw new IllegalStateException("Token api.video introuvable");
        }

        // 2. Créer le live stream
        String liveResp = client.send(
                HttpRequest.newBuilder()
                        .uri(URI.create("https://ws.api.video/live-streams"))
                        .header("Content-Type","application/json")
                        .header("Authorization","Bearer " + token)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                "{\"name\":\"" + title.replace("\"","\\\"") + "\",\"public\":true}"))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        ).body();

        String liveStreamId = extractJson(liveResp, "liveStreamId");
        String streamKey    = extractJson(liveResp, "streamKey");
        String playerUrl    = extractJsonStringFromPath(liveResp, "assets", "player");
        if (liveStreamId.isBlank() || streamKey.isBlank()) {
            throw new IllegalStateException("Réponse api.video invalide: " + liveResp);
        }
        if (playerUrl == null || playerUrl.isBlank()) {
            playerUrl = "https://embed.api.video/live/" + liveStreamId;
        }
        return new String[]{liveStreamId, streamKey, playerUrl};
    }

    /** Génère une clé de stream locale (fallback sans api.video) */
    private String generateLocalKey() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    private String extractJson(String json, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\""+ java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
    }

    private String extractJsonStringFromPath(String json, String objKey, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\"" + java.util.regex.Pattern.quote(objKey) + "\"\\s*:\\s*\\{[^}]*\"" +
                        java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]+)\"",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : "";
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