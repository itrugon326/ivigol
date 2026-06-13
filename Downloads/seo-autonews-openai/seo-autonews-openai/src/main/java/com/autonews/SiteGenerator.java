package com.autonews;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SiteGenerator {

    private final String outputDir;
    private final String siteTitle;
    private final String siteUrl;
    private final String adsenseId;
    private final String pexelsApiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient http   = new OkHttpClient();

    private final List<Map<String, String>> articles = new ArrayList<>();
    private static final String INDEX_FILE = "articles_index.json";

    public SiteGenerator(String outputDir, String siteTitle, String siteUrl,
                         String adsenseId, String pexelsApiKey) {
        this.outputDir    = outputDir;
        this.siteTitle    = siteTitle;
        this.siteUrl      = siteUrl;
        this.adsenseId    = adsenseId;
        this.pexelsApiKey = pexelsApiKey;
        new File(outputDir).mkdirs();
    }

    @SuppressWarnings("unchecked")
    public void loadExistingArticles() {
        File f = new File(INDEX_FILE);
        if (!f.exists()) return;
        try {
            List<Map<String, String>> existing =
                    mapper.readValue(f, new TypeReference<>() {});
            articles.addAll(existing);
            System.out.println("[INDEX] Cargados " + existing.size() + " artículos previos.");
        } catch (Exception e) {
            System.err.println("[SiteGenerator] No se pudo cargar índice previo.");
        }
    }

    private String fetchPhotoUrl(String query) {
        try {
            String url = "https://api.pexels.com/v1/search?query="
                    + query.replace(" ", "+") + "&per_page=1&orientation=landscape";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", pexelsApiKey)
                    .build();
            try (Response response = http.newCall(request).execute()) {
                String body = response.body().string();
                JsonNode root = mapper.readTree(body);
                JsonNode photos = root.path("photos");
                if (photos.size() > 0) {
                    return photos.get(0).path("src").path("large").asText();
                }
            }
        } catch (Exception e) {
            System.err.println("[Pexels] Error buscando foto: " + e.getMessage());
        }
        return "https://images.pexels.com/photos/46798/the-ball-stadion-football-the-pitch-46798.jpeg?auto=compress&cs=tinysrgb&w=800";
    }

    public void writeArticle(AiRewriter.RewrittenArticle article) throws IOException {
        String photoUrl = fetchPhotoUrl(article.title());
        String html = buildArticlePage(article, photoUrl);
        Path path = Paths.get(outputDir, article.slug() + ".html");
        Files.writeString(path, html);

        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("title", article.title());
        entry.put("slug",  article.slug());
        entry.put("meta",  article.metaDescription());
        entry.put("date",  article.date());
        entry.put("photo", photoUrl);
        articles.add(0, entry);
    }

    public void writeIndex() throws IOException {
        mapper.writeValue(new File(INDEX_FILE), articles);
        String html = buildIndexPage();
        Files.writeString(Paths.get(outputDir, "index.html"), html);
    }

    public void writeSitemap() throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        sb.append("  <url><loc>").append(siteUrl).append("/</loc></url>\n");
        for (Map<String, String> a : articles) {
            sb.append("  <url><loc>").append(siteUrl).append("/")
                    .append(a.get("slug")).append(".html</loc>")
                    .append("<lastmod>").append(a.get("date")).append("</lastmod></url>\n");
        }
        sb.append("</urlset>");
        Files.writeString(Paths.get(outputDir, "sitemap.xml"), sb.toString());
    }

    private String baseHead(String title, String meta) {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s | %s</title>
              <meta name="description" content="%s">
              <script async src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=%s" crossorigin="anonymous"></script>
              <style>
                *{box-sizing:border-box;margin:0;padding:0}
                body{font-family:Georgia,serif;background:#f9f9f9;color:#222;line-height:1.7}
                header{background:#1a1a2e;color:#fff;padding:16px 24px}
                header a{color:#e0e0e0;text-decoration:none;font-size:1.5rem;font-weight:bold}
                nav{background:#16213e;padding:8px 24px}
                nav a{color:#aaa;text-decoration:none;font-size:.9rem}
                .container{max-width:820px;margin:32px auto;padding:0 16px}
                h1{font-size:2rem;margin-bottom:12px;color:#1a1a2e}
                h2{font-size:1.3rem;margin:24px 0 8px;color:#333}
                p{margin-bottom:14px}
                .meta{color:#888;font-size:.85rem;margin-bottom:20px}
                .hero{width:100%%;height:380px;object-fit:cover;border-radius:8px;margin-bottom:24px}
                .ad{text-align:center;margin:24px 0;padding:12px;background:#eee;border-radius:4px}
                .card{background:#fff;border-radius:6px;overflow:hidden;margin-bottom:16px;
                      box-shadow:0 1px 4px rgba(0,0,0,.08);display:flex;gap:0}
                .card img{width:180px;height:120px;object-fit:cover;flex-shrink:0}
                .card-body{padding:16px}
                .card h2{font-size:1.1rem;margin:0 0 8px}
                .card a{color:#1a1a2e;text-decoration:none}
                .card a:hover{text-decoration:underline}
                .card .date{color:#999;font-size:.8rem;margin-top:8px}
                footer{text-align:center;padding:32px;color:#aaa;font-size:.85rem}
                @media(max-width:600px){.card{flex-direction:column}.card img{width:100%%;height:180px}}
              </style>
            </head>
            """.formatted(title, siteTitle, meta, adsenseId);
    }

    private String buildArticlePage(AiRewriter.RewrittenArticle a, String photoUrl) {
        return baseHead(a.title(), a.metaDescription()) + """
            <body>
              <header><a href="index.html">%s</a></header>
              <nav><a href="index.html">← Volver al inicio</a></nav>
              <div class="container">
                <h1>%s</h1>
                <p class="meta">Publicado el %s</p>
                <img class="hero" src="%s" alt="%s">
                <div class="ad">
                  <ins class="adsbygoogle" style="display:block" data-ad-format="auto"
                       data-full-width-responsive="true"></ins>
                  <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
                </div>
                %s
                <div class="ad">
                  <ins class="adsbygoogle" style="display:block" data-ad-format="auto"
                       data-full-width-responsive="true"></ins>
                  <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
                </div>
              </div>
              <footer>&copy; 2025 %s</footer>
            </body>
            </html>
            """.formatted(siteTitle, a.title(), a.date(), photoUrl, a.title(),
                a.htmlContent(), siteTitle);
    }

    private String buildIndexPage() {
        StringBuilder cards = new StringBuilder();
        for (Map<String, String> a : articles) {
            String photo = a.getOrDefault("photo",
                    "https://images.pexels.com/photos/46798/the-ball-stadion-football-the-pitch-46798.jpeg?auto=compress&cs=tinysrgb&w=400");
            cards.append("""
                <div class="card">
                  <img src="%s" alt="%s" loading="lazy">
                  <div class="card-body">
                    <h2><a href="%s.html">%s</a></h2>
                    <p>%s</p>
                    <span class="date">%s</span>
                  </div>
                </div>
                """.formatted(photo, a.get("title"), a.get("slug"),
                    a.get("title"), a.get("meta"), a.get("date")));
        }

        return baseHead(siteTitle, "Las últimas noticias de fútbol reescritas y optimizadas para SEO.") + """
            <body>
              <header><a href="index.html">%s</a></header>
              <div class="container">
                <div class="ad">
                  <ins class="adsbygoogle" style="display:block" data-ad-format="auto"
                       data-full-width-responsive="true"></ins>
                  <script>(adsbygoogle = window.adsbygoogle || []).push({});</script>
                </div>
                %s
              </div>
              <footer>&copy; 2025 %s</footer>
            </body>
            </html>
            """.formatted(siteTitle, cards.toString(), siteTitle);
    }
}