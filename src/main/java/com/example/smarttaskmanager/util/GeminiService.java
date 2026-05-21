package com.example.smarttaskmanager.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiService {

    private static final String API_KEY = "AIzaSyAGDdFUxVgafSXVl2aPjImr0dVxTQYwKsE";
    private static final String MODEL   = "gemini-2.5-flash";

    public static String askGemini(String prompt) {
        try {
            String endpoint =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + MODEL + ":generateContent?key=" + API_KEY;

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);

            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);

            JSONArray parts = new JSONArray();
            parts.put(textPart);

            JSONObject content = new JSONObject();
            content.put("role", "user");
            content.put("parts", parts);

            JSONArray contents = new JSONArray();
            contents.put(content);

            JSONObject body = new JSONObject();
            body.put("contents", contents);
            body.put("generationConfig", new JSONObject()
                    .put("temperature", 0.4)
                    .put("maxOutputTokens", 700));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();
            InputStream responseStream = statusCode >= 400
                    ? conn.getErrorStream()
                    : conn.getInputStream();

            String response = readResponse(responseStream);

            System.out.println("=== GEMINI DEBUG ===");
            System.out.println("Status: " + statusCode);
            System.out.println("Response: " + response.substring(0, Math.min(300, response.length())));

            if (statusCode >= 400) {
                return formatGeminiError(statusCode, response);
            }

            JSONObject json = new JSONObject(response);
            return json
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur : " + e.getMessage();
        }
    }

    private static String readResponse(InputStream stream) throws IOException {
        if (stream == null) return "";
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private static String formatGeminiError(int statusCode, String response) {
        try {
            JSONObject error = new JSONObject(response).getJSONObject("error");
            String message = error.optString("message", "Requête refusée.");
            String status  = error.optString("status", "HTTP_" + statusCode);
            return "❌ Erreur Gemini (" + statusCode + " - " + status + ") : " + message;
        } catch (Exception ignored) {
            return "❌ Erreur Gemini (" + statusCode + ") : " + response;
        }
    }
}