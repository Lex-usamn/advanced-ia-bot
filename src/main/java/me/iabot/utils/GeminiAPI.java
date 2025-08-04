package me.iabot.utils;

import me.iabot.Main;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class GeminiAPI {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final Gson gson = new Gson();

    public static String getDecision(Main plugin, String context) {
        try {
            String apiKey = plugin.getConfig().getString("gemini.api-key");
            String model = plugin.getConfig().getString("gemini.model", "gemini-pro");
            int timeout = plugin.getConfig().getInt("gemini.timeout", 30000);
            
            if (apiKey == null || apiKey.isEmpty()) {
                plugin.getLogger().warning("API key do Gemini não configurada!");
                return getDefaultAction();
            }

            URL url = new URL(API_URL + model + ":generateContent?key=" + apiKey);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setDoOutput(true);

            JsonObject requestBody = buildRequest(context, plugin);
            String jsonInputString = gson.toJson(requestBody);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return parseResponse(response.toString());
                }
            } else {
                plugin.getLogger().warning("Erro na API Gemini: " + responseCode);
                return getDefaultAction();
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao chamar API Gemini: " + e.getMessage());
            return getDefaultAction();
        }
    }

    public static CompletableFuture<String> getDecisionAsync(Main plugin, String context) {
        return CompletableFuture.supplyAsync(() -> getDecision(plugin, context));
    }

    private static JsonObject buildRequest(String context, Main plugin) {
        JsonObject root = new JsonObject();
        
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", "Você é um assistente de IA especializado em controlar bots em Minecraft. " +
            "Com base no contexto fornecido, escolha UMA ÚNICA ação entre estas opções: " +
            "mover, minerar, construir, craftar, combater, coletar, explorar, idle. " +
            "Responda APENAS com o nome da ação em minúsculas. " +
            "Contexto: " + context);
        parts.add(part);
        
        content.add("parts", parts);
        contents.add(content);
        
        root.add("contents", contents);
        
        // Configurações de geração
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.7);
        generationConfig.addProperty("maxOutputTokens", 10);
        generationConfig.addProperty("topK", 40);
        generationConfig.addProperty("topP", 0.95);
        
        root.add("generationConfig", generationConfig);
        
        return root;
    }

    private static String parseResponse(String response) {
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
            
            if (candidates != null && candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                JsonObject content = candidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                
                if (parts != null && parts.size() > 0) {
                    String text = parts.get(0).getAsJsonObject().get("text").getAsString().toLowerCase().trim();
                    
                    // Validar ação
                    String[] validActions = {"mover", "minerar", "construir", "craftar", "combater", "coletar", "explorar", "idle"};
                    for (String action : validActions) {
                        if (text.contains(action)) {
                            return action;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar erros de parsing
        }
        
        return "idle";
    }

    private static String getDefaultAction() {
        String[] actions = {"mover", "minerar", "construir", "coletar", "explorar", "idle"};
        int randomIndex = (int) (Math.random() * actions.length);
        return actions[randomIndex];
    }
}