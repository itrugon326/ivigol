package com.autonews;

import java.util.List;

public class Main {

    // ─── CONFIGURACIÓN ───────────────────────────────────────────────────────
    static final String GROQ_API_KEY = System.getenv("GROQ_API_KEY") != null
            ? System.getenv("GROQ_API_KEY")
            : "PON-TU-CLAVE-AQUI";
    static final String PEXELS_API_KEY = System.getenv("PEXELS_API_KEY") != null
            ? System.getenv("PEXELS_API_KEY") : "";

    static final String SITE_TITLE     = "IVIGOL";
    static final String SITE_URL       = "https://ivigol.com";
    static final String OUTPUT_DIR     = "docs";
    static final String PROCESSED_FILE = "processed_urls.json";
    static final String ADSENSE_ID     = "";
    static final int    MAX_ARTICULOS  = 10; // máximo por ejecución

    static final List<String> RSS_FEEDS = List.of(
            "https://www.marca.com/rss/futbol.xml",
            "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/section/deportes/sub/futbol",
            "https://e00-elmundo.uecdn.es/elmundodeporte/rss/futbol.xml"
    );
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        System.out.println("=== SEO AutoNews (IVIGOL) iniciado ===");

        RssReader       rssReader  = new RssReader();
        AiRewriter      aiRewriter = new AiRewriter(GROQ_API_KEY);
        DuplicateFilter filter     = new DuplicateFilter(PROCESSED_FILE);
        SiteGenerator   generator  = new SiteGenerator(OUTPUT_DIR, SITE_TITLE, SITE_URL,
                ADSENSE_ID, PEXELS_API_KEY);

        generator.loadExistingArticles();

        int contador = 0;

        outer:
        for (String feedUrl : RSS_FEEDS) {
            System.out.println("\n[RSS] Leyendo: " + feedUrl);
            List<RssReader.NewsItem> items = rssReader.fetchItems(feedUrl);

            for (RssReader.NewsItem item : items) {
                if (contador >= MAX_ARTICULOS) {
                    System.out.println("[STOP] Límite de " + MAX_ARTICULOS + " artículos alcanzado.");
                    break outer;
                }

                if (filter.isProcessed(item.url())) {
                    System.out.println("[SKIP] " + item.url());
                    continue;
                }

                System.out.println("[AI]  Reescribiendo: " + item.title());
                AiRewriter.RewrittenArticle article = aiRewriter.rewrite(item);

                if (article == null) {
                    System.out.println("[ERR] Fallo al reescribir, esperando 60s...");
                    try { Thread.sleep(60000); } catch (InterruptedException ignored) {}
                    article = aiRewriter.rewrite(item);
                    if (article == null) {
                        System.out.println("[ERR] Segundo intento fallido, saltando.");
                        continue;
                    }
                }

                generator.writeArticle(article);
                filter.markProcessed(item.url());
                System.out.println("[OK]  Generado: " + article.slug() + ".html");
                contador++;

                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }

        generator.writeIndex();
        generator.writeSitemap();
        System.out.println("\n=== Sitio generado en /" + OUTPUT_DIR + " — " + contador + " artículos nuevos ===");
        System.out.println("Ahora ejecuta: git add . && git commit -m 'update' && git push");
    }
}