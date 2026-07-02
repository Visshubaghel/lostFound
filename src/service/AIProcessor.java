package service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AIProcessor {
    private static final HttpClient client = HttpClient.newHttpClient();

    private static String getApiKey() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("gemini.apiKey");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing Gemini API key. Set GEMINI_API_KEY or -Dgemini.apiKey.");
        }
        return apiKey;
    }

    public static class AIResult {
        public String description;
        public double[] embedding;
        public AIResult(String desc, double[] emb) { 
            this.description = desc; 
            this.embedding = emb; 
        }
    }

    public static AIResult analyzeImageAndEmbed(String base64Image) throws Exception {
        String description = generateDescription(base64Image);
        double[] embedding = generateEmbedding(description);
        return new AIResult(description, embedding);
    }

    private static String generateDescription(String base64Image) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + getApiKey();
        
        JSONObject body = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        
        JSONObject textPart = new JSONObject();
        textPart.put("text", "Describe the main item in this image briefly (e.g., 'Black iPhone 14 with a red case', 'Blue wallet'). Keep it under 15 words and be specific.");
        
        JSONObject imagePart = new JSONObject();
        JSONObject inlineData = new JSONObject();
        // Assuming JPEG for simplicity; Gemini can usually infer from base64 but we provide a generic image mimeType
        inlineData.put("mimeType", "image/jpeg");
        inlineData.put("data", base64Image);
        imagePart.put("inlineData", inlineData);
        
        parts.put(textPart);
        parts.put(imagePart);
        content.put("parts", parts);
        contents.put(content);
        body.put("contents", contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject resJson = new JSONObject(response.body());
        
        if (resJson.has("error")) {
            throw new Exception("Gemini API Error: " + resJson.getJSONObject("error").getString("message"));
        }
        
        if (resJson.has("candidates")) {
            return resJson.getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim().replace("\n", "");
        }
        throw new Exception("Unexpected Gemini response: " + response.body());
    }

    public static double[] generateEmbedding(String textDescription) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + getApiKey();
        
        JSONObject body = new JSONObject();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("text", textDescription);
        parts.put(textPart);
        content.put("parts", parts);
        
        body.put("model", "models/gemini-embedding-001");
        body.put("content", content);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject resJson = new JSONObject(response.body());
        
        if (resJson.has("error")) {
            throw new Exception("Embedding API Error: " + resJson.getJSONObject("error").getString("message"));
        }
        
        if (resJson.has("embedding")) {
            JSONArray values = resJson.getJSONObject("embedding").getJSONArray("values");
            double[] vector = new double[values.length()];
            for(int i = 0; i < values.length(); i++) {
                vector[i] = values.getDouble(i);
            }
            return vector;
        }
        
        throw new Exception("Unexpected Embedding response: " + response.body());
    }
}
