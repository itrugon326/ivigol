package com.autonews;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class SiteGenerator {

    private final String outputDir;
    private final String siteTitle;
    private final String siteUrl;
    private final String adsenseId;
    private final ObjectMapper mapper = new ObjectMapper();

    // Registro de artículos para generar el índice
    private final List<Map<String, String>> articles = new ArrayList<>();
    private static final String INDEX_FILE = "articles_index.json";

    public SiteGenerator(String outputDir, String siteTitle, String siteUrl, String adsenseId) {
        this.outputDir = outputDir;
        this.siteTitle = siteTitle;
        this.siteUrl   = siteUrl;
        this.adsenseId = adsenseId;
        new File(outputDir).mkdirs();
    }

    /** Carga artículos previos del índice JSON para no perderlos al regenerar */
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

    public void writeArticle(AiRewriter.RewrittenArticle article) throws IOException {
        String html = buildArticlePage(article);
        Path path = Paths.get(outputDir, article.slug() + ".html");
        Files.writeString(path, html);

        // Registrar en el índice en memoria
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("title", article.title());
        entry.put("slug",  article.slug());
        entry.put("meta",  article.metaDescription());
        entry.put("date",  article.date());
        articles.add(0, entry); // más reciente primero
    }

    public void writeIndex() throws IOException {
        // Guardar índice JSON para la próxima ejecución
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

    // ─── HTML TEMPLATES ──────────────────────────────────────────────────────

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
                .ad{text-align:center;margin:24px 0;padding:12px;background:#eee;border-radius:4px}
                .card{background:#fff;border-radius:6px;padding:20px;margin-bottom:16px;
                      box-shadow:0 1px 4px rgba(0,0,0,.08)}
                .card h2{font-size:1.1rem;margin:0 0 8px}
                .card a{color:#1a1a2e;text-decoration:none}
                .card a:hover{text-decoration:underline}
                .card .date{color:#999;font-size:.8rem}
                footer{text-align:center;padding:32px;color:#aaa;font-size:.85rem}
              </style>
            </head>
            """.formatted(title, siteTitle, meta, adsenseId);
    }

    private String buildArticlePage(AiRewriter.RewrittenArticle a) {
        return baseHead(a.title(), a.metaDescription()) + """
            <body>
              <header><a href="index.html">%s</a></header>
              <nav><a href="index.html">← Volver al inicio</a></nav>
              <div class="container">
                <h1>%s</h1>
                <p class="meta">Publicado el %s</p>
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
            """.formatted(siteTitle, a.title(), a.date(), a.htmlContent(), siteTitle);
    }

    private String buildIndexPage() {
        StringBuilder cards = new StringBuilder();
        for (Map<String, String> a : articles) {
            cards.append("""
                <div class="card">
                  <h2><a href="%s.html">%s</a></h2>
                  <p>%s</p>
                  <span class="date">%s</span>
                </div>
                """.formatted(a.get("slug"), a.get("title"),
                              a.get("meta"),  a.get("date")));
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
