package com.gamilha.controllers;

import com.gamilha.entity.ChatAi;
import com.gamilha.services.ChatAiService;
import com.gamilha.utils.SessionContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ChatAIController {

    private final ChatAiService chatService = new ChatAiService();
    @FXML
    private TextField inputField;

    @FXML
    private VBox chatBox;

    @FXML
    public void initialize() {
        loadMessages();
    }

    // 🔥 ENVOYER MESSAGE
    @FXML
    private void sendMessage() {

        String msg = inputField.getText();
        if (msg == null || msg.isEmpty()) return;

        addUserMessage(msg);
        inputField.clear();

        // 🔥 SAVE USER MESSAGE
        ChatAi chat = new ChatAi();
        chat.setRole("user");
        chat.setContent(msg);
        chat.setUser(SessionContext.getCurrentUser());

        chatService.addMessage(chat);

        addAIMessage("thinking...");

        new Thread(() -> {
            try {
                String response = callOllama(msg);

                Platform.runLater(() -> {
                    chatBox.getChildren().removeLast();
                    addAIMessage(response);
                });

                // 🔥 SAVE AI RESPONSE
                ChatAi res = new ChatAi();
                res.setRole("assistant");
                res.setContent(response);
                res.setUser(SessionContext.getCurrentUser());
                chatService.addMessage(res);

            } catch (Exception e) {
                Platform.runLater(() -> addAIMessage("❌ " + e.getMessage()));
            }
        }).start();
    }

    // 🔥 HISTORIQUE
    private void loadMessages() {

        ChatAiService service = new ChatAiService();
        List<String[]> messages = service.getMessagesByUser();

        for (String[] m : messages) {

            String role = m[0];
            String content = m[1];

            if (role.equals("user")) {
                addUserMessage(content);
            } else {
                addAIMessage(content);
            }
        }
    }

    // 🔥 MESSAGE USER (DROITE)
    private void addUserMessage(String msg) {

        Label text = new Label(msg);
        text.setWrapText(true);
        text.setMaxWidth(400);
        text.setStyle("-fx-text-fill:white;");

        HBox bubble = new HBox(text);
        bubble.setStyle("""
            -fx-background-color:#ec4899;
            -fx-padding:10;
            -fx-background-radius:15;
        """);

        HBox container = new HBox(bubble);
        container.setAlignment(Pos.CENTER_RIGHT);

        chatBox.getChildren().add(container);
    }

    // 🔥 MESSAGE AI (GAUCHE)
    private void addAIMessage(String msg) {

        Label text = new Label(msg);
        text.setWrapText(true);
        text.setMaxWidth(400);
        text.setStyle("-fx-text-fill:white;");

        HBox bubble = new HBox(text);
        bubble.setStyle("""
            -fx-background-color:#1e293b;
            -fx-padding:10;
            -fx-background-radius:15;
        """);

        HBox container = new HBox(bubble);
        container.setAlignment(Pos.CENTER_LEFT);

        chatBox.getChildren().add(container);
    }

    private String callOllama(String message) throws Exception {

        URL url = new URL("http://localhost:11434/api/generate");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // 🔥 PROMPT PROPRE
        String prompt = """
    Tu es un coach professionnel en E-sport.

    Règles :
    - Réponds uniquement aux questions liées au gaming et E-sport
    - Si la question est hors sujet, dis que tu ne peux pas répondre
    - Donne des conseils précis et professionnels

    Question :
    %s
    """.formatted(message.replace("\"", "\\\""));

        // 🔥 JSON CORRECT
        String jsonInput = """
    {
      "model": "llama3",
      "prompt": "%s",
      "stream": false
    }
    """.formatted(prompt.replace("\n", "\\n"));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
        );

        StringBuilder response = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            response.append(line);
        }

        br.close();

        String result = response.toString();

        int start = result.indexOf("\"response\":\"") + 12;
        int end = result.indexOf("\",\"done\"");

        if (start < 12 || end <= start) {
            return "⚠️ Réponse invalide";
        }

        return result.substring(start, end);
    }
}