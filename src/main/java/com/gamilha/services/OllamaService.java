package com.gamilha.services;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaService {

    public static String askAI(String prompt) {
        try {
            URL url = new URL("http://localhost:11434/api/generate");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = """
            {
              "model": "llama3",
              "prompt": "%s",
              "stream": false
            }
            """.formatted(prompt.replace("\"", "\\\""));

            OutputStream os = conn.getOutputStream();
            os.write(jsonInput.getBytes());
            os.flush();
            os.close();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );

            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();

            // 🔥 extraire réponse
            String result = response.toString();

            int start = result.indexOf("\"response\":\"") + 12;
            int end = result.lastIndexOf("\"");

            return result.substring(start, end);

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur Ollama";
        }
    }
}