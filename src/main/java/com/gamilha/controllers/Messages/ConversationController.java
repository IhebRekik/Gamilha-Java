package com.gamilha.controllers.Messages;

import com.gamilha.entity.ChatMessage;
import com.gamilha.entity.User;
import com.gamilha.services.ChatService;
import com.gamilha.utils.SessionContext;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ConversationController {

    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private TextField messageField;
    @FXML private Label titleLabel;
    @FXML private Label onlineLabel;
    @FXML private AnchorPane rootPane;
    private User currentUser;
    private User selectedUser;
    private ChatMessage editingMessage = null;
    private final ChatService chatService = new ChatService();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    private final Map<String, Image> emojiCache = new HashMap<>();

    @FXML
    public void initialize() {

        currentUser = SessionContext.getCurrentUser();
        rootPane.setStyle("""
    -fx-background-color: linear-gradient(to bottom,#020617,#0f172a);
""");
        messagesBox.setStyle("-fx-background-color:#0f172a;");
        messagesScrollPane.setStyle("-fx-background:#0f172a;");

        messageField.setStyle("""
            -fx-background-color:#1e293b;
            -fx-text-fill:white;
            -fx-background-radius:12;
            -fx-padding:8;
        """);
        titleLabel.getParent().setStyle("""
    -fx-background-color: linear-gradient(to right,#020617,#0f172a);
    -fx-padding:14;
    -fx-border-color:#1e293b;
    -fx-border-width:0 0 1 0;
""");
        Platform.runLater(() -> {
            Pane root = (Pane) titleLabel.getScene().getRoot();

            root.setStyle("""
        -fx-background-color: linear-gradient(to bottom,#020617,#0f172a);
    """);
        });
        messagesBox.heightProperty().addListener((obs, o, n) ->
                Platform.runLater(() -> messagesScrollPane.setVvalue(1.0))
        );
    }

    public void setUser(User user) {

        this.selectedUser = user;

        titleLabel.setText("Conversation avec " + user.getName());
        titleLabel.setStyle("-fx-text-fill:#ffffff; -fx-font-size:18px; -fx-font-weight:bold;");

        onlineLabel.setText("🟢 En ligne");
        onlineLabel.setStyle("-fx-text-fill:#22c55e;");

        afficherMessages();
    }

    @FXML
    private void envoyerMessage() {

        String text = messageField.getText().trim();
        if (text.isEmpty()) return;

        if (editingMessage != null) {
            // 🔥 MODE EDIT
            editingMessage.setContent(text);

            chatService.updateMessage(editingMessage); // <-- à créer si pas existant

            editingMessage = null;
            messageField.clear();

        } else {
            // 🔥 MODE NORMAL
            ChatMessage msg = new ChatMessage();
            msg.setContent(text);
            msg.setSender(currentUser);
            msg.setRecipient(selectedUser);

            chatService.sendMessage(msg);
            messageField.clear();
        }

        afficherMessages();
    }

    private void afficherMessages() {

        messagesBox.getChildren().clear();

        for (ChatMessage m : chatService.getConversation(currentUser, selectedUser)) {

            boolean isMine = m.getSender().getId() == currentUser.getId();
            messagesBox.getChildren().add(buildMessageLine(m, isMine));
        }
    }

    private HBox buildMessageLine(ChatMessage m, boolean isMine) {

        HBox line = new HBox(8);
        line.setMaxWidth(Double.MAX_VALUE);

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(520);

        if (isMine) {
            bubble.setStyle("""
            -fx-background-color: linear-gradient(to right,#3b82f6,#6366f1);
            -fx-background-radius:16;
            -fx-padding:10;
        """);
        } else {
            bubble.setStyle("""
            -fx-text-fill:#ffffff;
            -fx-background-color:#1e293b;
            -fx-background-radius:16;
            -fx-padding:10;
        """);
        }

        Label time = new Label(
                m.getCreatedAt() != null ? m.getCreatedAt().format(FORMATTER) : ""
        );
        time.setStyle("-fx-text-fill:#f8f4f4; -fx-font-size:10px;");

        TextFlow flow = parseEmojiText(m.getContent());

        bubble.getChildren().addAll(time, flow);

        // ===== MENU =====
        MenuItem editItem = new MenuItem("Modifier");
        MenuItem deleteItem = new MenuItem("Supprimer");

        MenuButton menu = new MenuButton("⋮");
        menu.getItems().addAll(editItem, deleteItem);

        menu.setStyle("""
        -fx-background-color: transparent;
        -fx-text-fill: white;
        -fx-font-size: 14px;
    """);

        editItem.setOnAction(e -> {

            editingMessage = m;

            messageField.setText(m.getContent());
            messageField.requestFocus();

        });

        deleteItem.setOnAction(e -> {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation");
            alert.setHeaderText("Supprimer ce message ?");
            alert.setContentText("Cette action est irréversible.");

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                chatService.deleteMessage(m.getId()); // <-- à créer si pas existant
                afficherMessages();
            }
        });
        // ===== CONTAINER =====
        StackPane bubbleContainer = new StackPane(bubble, menu);

        StackPane.setAlignment(menu, Pos.TOP_RIGHT);



        // IMPORTANT : menu seulement pour MES messages
        if (!isMine) {
            menu.setManaged(false);
            menu.setVisible(false);
        }

        // ===== ALIGNEMENT =====
        if (isMine) {
            line.setAlignment(Pos.CENTER_RIGHT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            line.getChildren().addAll(spacer, bubbleContainer);

        } else {
            line.setAlignment(Pos.CENTER_LEFT);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            line.getChildren().addAll(bubbleContainer, spacer);
        }

        return line;
    }
    // 🔥 FIX TEXTE + EMOJI
    private TextFlow parseEmojiText(String message) {

        TextFlow flow = new TextFlow();
        String remaining = message;

        List<String> emojis = EmojiParser.extractEmojis(message);

        for (String emoji : emojis) {

            int index = remaining.indexOf(emoji);
            if (index < 0) continue;

            if (index > 0) {
                Text text = new Text(remaining.substring(0, index));
                text.setStyle("-fx-fill:white;");
                flow.getChildren().add(text);
            }

            String hex = toHex(emoji);
            URL res = getEmojiURL(hex);

            if (res != null) {

                Image img = emojiCache.computeIfAbsent(hex,
                        h -> new Image(res.toExternalForm(), 20, 20, true, true));

                ImageView iv = new ImageView(img);
                iv.setTranslateY(3);

                flow.getChildren().add(iv);

            } else {
                flow.getChildren().add(new Text(emoji));
            }

            remaining = remaining.substring(index + emoji.length());
        }

        if (!remaining.isEmpty()) {
            flow.getChildren().add(new Text(remaining));
        }


        return flow;
    }

    // 🔥 EMOJI PICKER
    @FXML
    private void openEmojiPicker() {

        ContextMenu menu = new ContextMenu();

        VBox root = new VBox(8);
        root.setStyle("-fx-background-color:#0f172a; -fx-padding:10;");

        HBox categoryBar = new HBox(6);

        ScrollPane scroll = new ScrollPane();
        scroll.setPrefSize(340, 260);
        scroll.setFitToWidth(true);
        menu.setStyle("""
    -fx-background-color:#0f172a;
    -fx-padding:0;
    -fx-background-insets:0;
""");
        scroll.setStyle("""
    -fx-background:#0f172a;
    -fx-background-color:#0f172a;
    -fx-border-color:transparent;
""");
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);

        scroll.setContent(grid);

        Map<String, List<Emoji>> categories = buildCategories();

        for (String cat : categories.keySet()) {

            Button b = new Button(getCategoryIcon(cat));
            b.setStyle("-fx-background-color:transparent; -fx-font-size:18;");

            b.setOnAction(e -> showCategory(grid, categories.get(cat)));

            categoryBar.getChildren().add(b);
        }

        showCategory(grid, categories.values().iterator().next());

        root.getChildren().addAll(categoryBar, scroll);

        CustomMenuItem item = new CustomMenuItem(root);

// 🔥 disable hover highlight
        item.setHideOnClick(false);
        item.setStyle("-fx-background-color: transparent;");

        menu.getItems().add(item);
        menu.show(messageField, Side.TOP, 0, 5);

    }

    private void showCategory(GridPane grid, List<Emoji> emojis) {

        grid.getChildren().clear();

        int col = 0, row = 0, count = 0;

        for (Emoji e : emojis) {

            if (count++ > 200) break;

            String unicode = e.getUnicode();
            String hex = toHex(unicode);

            URL res = getEmojiURL(hex);

            Button btn;

            if (res != null) {

                Image img = emojiCache.computeIfAbsent(hex,
                        h -> new Image(res.toExternalForm(), 24, 24, true, true));

                btn = new Button("", new ImageView(img));

            } else {
                btn = new Button(unicode);
            }

            btn.setStyle("""
    -fx-background-color:transparent;
    -fx-border-color:transparent;
    -fx-focus-color:transparent;
    -fx-faint-focus-color:transparent;
    -fx-background-insets:0;
    -fx-padding:0;
""");

            btn.setFocusTraversable(false);
            btn.setPrefSize(40, 40);



            btn.setOnAction(e2 -> messageField.appendText(unicode));

            grid.add(btn, col, row);

            col++;
            if (col == 8) { col = 0; row++; }
        }
    }

    private Map<String, List<Emoji>> buildCategories() {

        Map<String, List<Emoji>> map = new LinkedHashMap<>();

        map.put("Smileys", new ArrayList<>());
        map.put("People", new ArrayList<>());
        map.put("Animals", new ArrayList<>());
        map.put("Food", new ArrayList<>());
        map.put("Activities", new ArrayList<>());
        map.put("Travel", new ArrayList<>());
        map.put("Objects", new ArrayList<>());
        map.put("Symbols", new ArrayList<>());
        map.put("Flags", new ArrayList<>());

        for (Emoji e : EmojiManager.getAll()) {

            String d = e.getDescription().toLowerCase();

            if (contains(d,"face","smile")) map.get("Smileys").add(e);
            else if (contains(d,"man","woman")) map.get("People").add(e);
            else if (contains(d,"animal","dog")) map.get("Animals").add(e);
            else if (contains(d,"food","pizza")) map.get("Food").add(e);
            else if (contains(d,"ball","sport")) map.get("Activities").add(e);
            else if (contains(d,"car","plane")) map.get("Travel").add(e);
            else if (contains(d,"phone","tool")) map.get("Objects").add(e);
            else if (contains(d,"heart","symbol")) map.get("Symbols").add(e);
            else if (d.contains("flag")) map.get("Flags").add(e);
        }

        return map;
    }

    private boolean contains(String text, String... keys) {
        for (String k : keys) if (text.contains(k)) return true;
        return false;
    }

    private String getCategoryIcon(String cat) {
        return switch (cat) {
            case "Smileys" -> "😀";
            case "People" -> "👤";
            case "Animals" -> "🐶";
            case "Food" -> "🍔";
            case "Activities" -> "⚽";
            case "Travel" -> "✈️";
            case "Objects" -> "💡";
            case "Symbols" -> "❤️";
            case "Flags" -> "🚩";
            default -> "❓";
        };
    }

    private String toHex(String emoji) {

        StringBuilder hex = new StringBuilder();

        emoji.codePoints().forEach(cp -> {

            String h = Integer.toHexString(cp).toLowerCase();

            if (!h.equals("fe0f")) {
                if (hex.length() > 0) hex.append("-");
                hex.append(h);
            }
        });

        return hex.toString();
    }

    private URL getEmojiURL(String hex) {

        String base = "/com/gamilha/emoji/";

        URL res = getClass().getResource(base + hex + ".png");

        if (res == null)
            res = getClass().getResource(base + hex + "-fe0f.png");

        return res;
    }
}