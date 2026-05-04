package com.gamilha.controllers;

import com.gamilha.entity.Post;
import com.gamilha.entity.User;
import com.gamilha.services.PostService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserPostFormController {

    @FXML private TextArea         contentArea;
    @FXML private TextField        mediaurlField;
    @FXML private HBox             imagesPreviewBox;
    @FXML private Label            imageCountLabel;
    @FXML private Label            errorLabel;
    @FXML private Button           btnSave;
    @FXML private Label            titleLabel;
    @FXML private ComboBox<String> styleCombo;
    @FXML private Button           btnSuggestIA;
    @FXML private Label            aiStatusLabel;

    private Post              post;
    private User              currentUser;
    private final List<File>   selectedFiles  = new ArrayList<>();
    private final List<String> selectedImages = new ArrayList<>(); // noms fichiers déjà uploadés
    private final PostService  postService    = new PostService();

    private static final String[] STYLES = {"Normal","Gras","Italique","Code","Citation"};

    @FXML
    public void initialize() {
        setupStyleCombo();
        fixTA();
    }

    private void setupStyleCombo() {
        if (styleCombo == null) return;
        styleCombo.getItems().setAll(STYLES);
        styleCombo.setValue("Normal");

        // Cellule bouton (texte sélectionné) — toujours blanc lisible
        styleCombo.setButtonCell(styledCell());

        // Cellules de la liste déroulante
        styleCombo.setCellFactory(lv -> styledCell());

        // Aperçu visuel dans le textarea quand on change le style
        styleCombo.setOnAction(e -> applyPreview());
    }

    private ListCell<String> styledCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("-fx-text-fill:#e2e8f0;-fx-background-color:#1a1e2e;");
                    return;
                }
                String icon = switch (item) {
                    case "Gras"     -> "B  ";
                    case "Italique" -> "I  ";
                    case "Code"     -> "</>  ";
                    case "Citation" -> "❝  ";
                    default         -> "Aa  ";
                };
                setText(icon + item);
                setStyle("-fx-text-fill:#e2e8f0;-fx-font-size:12;" +
                         "-fx-background-color:#1a1e2e;-fx-padding:5 10;");
            }
        };
    }

    /** Applique un aperçu visuel du style sur le textarea */
    private void applyPreview() {
        if (contentArea == null || styleCombo == null) return;
        String style = styleCombo.getValue();
        String base = "-fx-control-inner-background:#1a1a26;-fx-background-color:#1a1a26;" +
                      "-fx-prompt-text-fill:#475569;-fx-border-color:#2a2a40;" +
                      "-fx-border-radius:8;-fx-background-radius:8;";
        switch (style != null ? style : "Normal") {
            case "Gras"     -> contentArea.setStyle(base + "-fx-font-weight:bold;-fx-text-fill:#f1f5f9;-fx-font-size:14;");
            case "Italique" -> contentArea.setStyle(base + "-fx-font-style:italic;-fx-text-fill:#c9d1d9;-fx-font-size:13;");
            case "Code"     -> contentArea.setStyle(base + "-fx-font-family:'Courier New',monospace;-fx-text-fill:#86efac;-fx-font-size:12;");
            case "Citation" -> contentArea.setStyle(base + "-fx-font-style:italic;-fx-text-fill:#94a3b8;-fx-font-size:13;");
            default         -> contentArea.setStyle(base + "-fx-text-fill:#e2e8f0;-fx-font-size:13;");
        }
        fixTAContent();
    }

    private void fixTA() {
        Platform.runLater(() -> {
            if (contentArea == null) return;
            contentArea.setStyle(
                "-fx-control-inner-background:#1a1a26;-fx-background-color:#1a1a26;" +
                "-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#475569;" +
                "-fx-font-size:13;-fx-border-color:#2a2a40;" +
                "-fx-border-radius:8;-fx-background-radius:8;");
            fixTAContent();
        });
    }

    private void fixTAContent() {
        Platform.runLater(() -> {
            if (contentArea == null) return;
            Node n = contentArea.lookup(".content");
            if (n != null) {
                // Extraire la couleur de fond actuelle du textarea
                String bg = "#1a1a26";
                String currentStyle = contentArea.getStyle();
                if (currentStyle.contains("#86efac")) bg = "#0d1f17"; // Code
                n.setStyle("-fx-background-color:" + bg + ";-fx-control-inner-background:" + bg + ";");
            }
        });
    }

    public void init(Post p, User user) {
        this.post = p;
        this.currentUser = user;
        fixTA();
        if (p != null) {
            if (titleLabel != null) titleLabel.setText("✏  Modifier mon post");
            if (btnSave != null)    btnSave.setText("Mettre à jour");
            // Charger texte sans préfixe style
            if (contentArea != null) contentArea.setText(PostService.stripStylePrefix(p.getContent()));
            if (mediaurlField != null) mediaurlField.setText(p.getMediaurl() != null ? p.getMediaurl() : "");
            // Restaurer le style dans la combobox
            String style = PostService.extractStyle(p.getContent());
            if (styleCombo != null) { styleCombo.setValue(style); applyPreview(); }
            // Charger les images existantes
            for (String img : p.getAllImages()) {
                selectedImages.add(img);
                addThumb(null, img);
            }
        } else {
            fixTA();
        }
    }

    // ── Choisir images (multiple) ─────────────────────────────────────────
    @FXML
    private void onChooseImages() {
        int total = selectedFiles.size() + selectedImages.size();
        if (total >= 5) { showError("Maximum 5 images."); return; }
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir images (max 5)");
        fc.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp"));
        List<File> chosen = fc.showOpenMultipleDialog(btnSave.getScene().getWindow());
        if (chosen == null) return;
        int remaining = 5 - total;
        for (File f : chosen.subList(0, Math.min(chosen.size(), remaining))) {
            selectedFiles.add(f);
            addThumb(f, null);
        }
        updateCount();
    }

    // ── Miniature avec bouton ✕ ───────────────────────────────────────────
    private void addThumb(File file, String existingName) {
        if (imagesPreviewBox == null) return;
        StackPane thumb = new StackPane();
        thumb.setMinSize(90, 70); thumb.setMaxSize(90, 70);
        thumb.setStyle("-fx-background-color:#1a1e2e;-fx-background-radius:8;");
        try {
            String uri = file != null ? file.toURI().toString()
                : new File(AdminPostController.UPLOADS + existingName).toURI().toString();
            ImageView iv = new ImageView(new Image(uri, 90, 70, false, true));
            iv.setFitWidth(90); iv.setFitHeight(70);
            thumb.getChildren().add(iv);
        } catch (Exception e) {
            Label ph = new Label("🖼"); ph.setStyle("-fx-font-size:24;-fx-text-fill:#8b5cf6;");
            thumb.getChildren().add(ph);
        }
        Button rm = new Button("✕");
        rm.setStyle("-fx-background-color:rgba(239,68,68,0.9);-fx-text-fill:white;" +
                    "-fx-background-radius:10;-fx-padding:1 5;-fx-font-size:10;-fx-cursor:hand;");
        StackPane.setAlignment(rm, Pos.TOP_RIGHT);
        StackPane.setMargin(rm, new Insets(3,3,0,0));
        rm.setOnAction(e -> {
            imagesPreviewBox.getChildren().remove(thumb);
            if (file != null) selectedFiles.remove(file);
            if (existingName != null) selectedImages.remove(existingName);
            updateCount();
        });
        thumb.getChildren().add(rm);
        imagesPreviewBox.getChildren().add(thumb);
        updateCount();
    }

    private void updateCount() {
        int t = selectedFiles.size() + selectedImages.size();
        if (imageCountLabel != null) imageCountLabel.setText(t + "/5");
    }

    // ── Suggestion IA ─────────────────────────────────────────────────────
    @FXML
    public void onSuggestIA() {
        String draft = contentArea.getText().trim();
        if (draft.length() < 3) { if (aiStatusLabel!=null) aiStatusLabel.setText("Écrivez quelques mots d'abord..."); return; }
        if (btnSuggestIA!=null) { btnSuggestIA.setDisable(true); btnSuggestIA.setText("⏳"); }
        if (aiStatusLabel!=null) aiStatusLabel.setText("🤖 Génération...");
        Task<String> task = new Task<>() { @Override protected String call() { return PostService.suggestContent(draft); } };
        task.setOnSucceeded(ev -> {
            String s = task.getValue();
            if (s != null && !s.isBlank()) {
                contentArea.appendText((contentArea.getText().endsWith(" ") ? "" : " ") + s);
                contentArea.positionCaret(contentArea.getText().length());
                if (aiStatusLabel!=null) aiStatusLabel.setText("✅ Suggestion ajoutée !");
            }
            if (btnSuggestIA!=null) { btnSuggestIA.setDisable(false); btnSuggestIA.setText("🤖 IA"); }
            fixTA();
        });
        new Thread(task).start();
    }

    // ── Sauvegarder ───────────────────────────────────────────────────────
    @FXML
    private void onSave() {
        errorLabel.setText("");
        String rawContent = contentArea.getText().trim();
        if (rawContent.length() < 12) { showError("Min. 12 caractères."); return; }
        if (currentUser == null) { showError("Non identifié."); return; }

        String style = styleCombo != null ? styleCombo.getValue() : "Normal";

        // Encoder le style comme préfixe dans le contenu
        String finalContent = switch (style != null ? style : "Normal") {
            case "Gras"     -> "[GRAS]"     + rawContent;
            case "Italique" -> "[ITALIQUE]" + rawContent;
            case "Code"     -> "[CODE]"     + rawContent;
            case "Citation" -> "[CITATION]" + rawContent;
            default         -> rawContent;
        };

        // Upload images — toutes stockées dans post.image séparées par ","
        List<String> allImages = new ArrayList<>(selectedImages);
        for (File f : selectedFiles) {
            String ext = f.getName().contains(".") ? f.getName().substring(f.getName().lastIndexOf('.')+1) : "jpg";
            String name = UUID.randomUUID().toString().replace("-","").substring(0,13) + "." + ext;
            File dest = new File(AdminPostController.UPLOADS + name);
            try {
                dest.getParentFile().mkdirs();
                Files.copy(f.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                allImages.add(name);
            } catch (IOException e) { showError("Erreur image : " + e.getMessage()); return; }
        }

        // Stocker dans la colonne `image` de Symfony : "img1.jpg,img2.jpg,img3.jpg"
        String imageValue = allImages.isEmpty() ? null : String.join(",", allImages);

        try {
            if (post == null) {
                Post np = new Post();
                np.setContent(finalContent);
                np.setImage(imageValue);   // colonne `image` de Symfony
                np.setMediaurl(mediaurlField != null ? mediaurlField.getText().trim() : "");
                np.setUser(currentUser);
                np.setCreatedAt(LocalDateTime.now());
                postService.create(np);
            } else {
                post.setContent(finalContent);
                post.setImage(imageValue);
                post.setMediaurl(mediaurlField != null ? mediaurlField.getText().trim() : "");
                postService.update(post);
            }
            ((Stage) btnSave.getScene().getWindow()).close();
        } catch (SQLException e) { showError("Erreur BD : " + e.getMessage()); }
    }

    @FXML private void onCancel() { ((Stage) btnSave.getScene().getWindow()).close(); }
    private void showError(String msg) { if (errorLabel != null) errorLabel.setText(msg); }
}
