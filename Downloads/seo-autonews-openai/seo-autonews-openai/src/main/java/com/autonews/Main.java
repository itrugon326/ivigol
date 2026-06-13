package com.autonews;

import java.util.List;

public class Main {

    // ─── CONFIGURACIÓN ───────────────────────────────────────────────────────
    // Lee la API key de la variable de entorno OPENAI_API_KEY
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

    static final List<String> RSS_FEEDS = List.of(
            "https://www.marca.com/rss/futbol.xml",
            "https://feeds.elpais.com/mrss-s/pages/ep/site/elpais.com/section/deportes/sub/futbol",
            "https://e00-elmundo.uecdn.es/elmundodeporte/rss/futbol.xml"
    );
    // ─────────────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        System.out.println("=== SEO AutoNews (OpenAI + Static) iniciado ===");

        if (GROQ_API_KEY.startsWith("sk-proj-PON")) {
            System.err.println("[ERROR] Configura la variable de entorno OPENAI_API_KEY antes de ejecutar.");
            System.exit(1);
        }

        RssReader       rssReader  = new RssReader();
        AiRewriter      aiRewriter = new AiRewriter(GROQ_API_KEY);
        DuplicateFilter filter     = new DuplicateFilter(PROCESSED_FILE);
        SiteGenerator   generator  = new SiteGenerator(OUTPUT_DIR, SITE_TITLE, SITE_URL, ADSENSE_ID,PEXELS_API_KEY);

        generator.loadExistingArticles();

        for (String feedUrl : RSS_FEEDS) {
            System.out.println("\n[RSS] Leyendo: " + feedUrl);
            List<RssReader.NewsItem> items = rssReader.fetchItems(feedUrl);

            for (RssReader.NewsItem item : items) {
                if (filter.isProcessed(item.url())) {
                    System.out.println("[SKIP] " + item.url());
                    continue;
                }

                System.out.println("[AI]  Reescribiendo: " + item.title());
                AiRewriter.RewrittenArticle article = aiRewriter.rewrite(item);

                if (article == null) {
                    System.out.println("[ERR] Fallo al reescribir, esperando 60s...");
                    try { Thread.sleep(60000); } catch (InterruptedException ignored) {}
                    // Reintentar una vez
                    article = aiRewriter.rewrite(item);
                    if (article == null) {
                        System.out.println("[ERR] Segundo intento fallido, saltando.");
                        continue;
                    }
                }

                generator.writeArticle(article);
                filter.markProcessed(item.url());
                System.out.println("[OK]  Generado: " + article.slug() + ".html");

                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }

        generator.writeIndex();
        generator.writeSitemap();
        System.out.println("\n=== Sitio generado en /" + OUTPUT_DIR + " ===");
        System.out.println("Ahora ejecuta: git add . && git commit -m 'update' && git push");
    }
}
