package com.autonews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.time.LocalDate;

public class AiRewriter {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.3-70b-versatile";
    private final String apiKey;
    private final OkHttpClient http   = new OkHttpClient();
    private final ObjectMapper  mapper = new ObjectMapper();

    public record RewrittenArticle(String title, String slug, String htmlContent,
                                   String metaDescription, String date) {}

    public AiRewriter(String apiKey) {
        this.apiKey = apiKey;
    }

    public RewrittenArticle rewrite(RssReader.NewsItem item) {
        String prompt = """
            Eres un redactor SEO experto en fútbol. A partir del siguiente titular y resumen,
            escribe un artículo COMPLETAMENTE NUEVO en español.

            TITULAR ORIGINAL: %s
            RESUMEN: %s

                IMPORTANTE: Escribe un artículo bien estructurado con esta estructura EXACTA:
                - Párrafo de introducción de 3-4 frases
                - 4 secciones, cada una con un <h2> y 3-4 párrafos <p> de contenido real y detallado
                - Párrafo de conclusión
                - Mínimo 800 palabras en total
                - Cada <p> debe tener mínimo 3 frases completas
                
                Devuelve ÚNICAMENTE este JSON en UNA SOLA LÍNEA sin saltos de línea dentro de los valores:
                {"title":"título SEO aquí","slug":"slug-aqui","meta":"descripción máx 155 chars","html":"<p>introducción detallada de 3-4 frases</p><h2>Primera sección</h2><p>párrafo 1 con 3 frases</p><p>párrafo 2 con 3 frases</p><p>párrafo 3 con 3 frases</p><h2>Segunda sección</h2><p>párrafo 1</p><p>párrafo 2</p><p>párrafo 3</p><h2>Tercera sección</h2><p>párrafo 1</p><p>párrafo 2</p><p>párrafo 3</p><h2>Cuarta sección</h2><p>párrafo 1</p><p>párrafo 2</p><p>párrafo 3</p><p>conclusión</p>"}
            """.formatted(item.title(), item.description());

        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", MODEL);

            ArrayNode messages = mapper.createArrayNode();

            ObjectNode system = mapper.createObjectNode();
            system.put("role", "system");
            system.put("content", "Eres un redactor SEO experto en fútbol. Respondes SOLO con una línea de JSON válido sin saltos de línea dentro de los valores de las propiedades.");
            messages.add(system);

            ObjectNode user = mapper.createObjectNode();
            user.put("role", "user");
            user.put("content", prompt);
            messages.add(user);

            body.set("messages", messages);
            body.put("max_tokens", 2500);
            body.put("temperature", 0.7);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(RequestBody.create(
                            mapper.writeValueAsString(body),
                            MediaType.parse("application/json")
                    ))
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .build();

            try (Response response = http.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonNode root = mapper.readTree(responseBody);

                if (root.has("error")) {
                    System.err.println("[AiRewriter] Error API Groq: "
                            + root.get("error").get("message").asText());
                    return null;
                }

                JsonNode choices = root.path("choices");
                if (choices.isEmpty()) {
                    System.err.println("[AiRewriter] Respuesta vacía: " + responseBody);
                    return null;
                }

                String text = choices.get(0).path("message").path("content").asText();

                int start = text.indexOf('{');
                int end   = text.lastIndexOf('}');
                if (start == -1 || end == -1) {
                    System.err.println("[AiRewriter] No se encontró JSON en la respuesta.");
                    return null;
                }
                text = text.substring(start, end + 1);
                text = text.replaceAll("(?<!\\\\)[\\n\\r]+", " ").trim();

                JsonNode parsed = mapper.readTree(text);
                return new RewrittenArticle(
                        parsed.get("title").asText(),
                        parsed.get("slug").asText(),
                        parsed.get("html").asText(),
                        parsed.get("meta").asText(),
                        LocalDate.now().toString()
                );
            }
        } catch (Exception e) {
            System.err.println("[AiRewriter] Error: " + e.getMessage());
            return null;
        }
    }
}