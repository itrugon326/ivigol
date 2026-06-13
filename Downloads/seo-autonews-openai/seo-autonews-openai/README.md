# SEO AutoNews — Web Estática Gratis

## Stack
- **Java 17 + Maven** — genera los HTML
- **GitHub** — almacena el código y los HTML generados
- **Vercel / Netlify** — sirve los HTML gratis (hosting gratuito)
- **Dominio propio** — único gasto (~8€/año)

## Configuración rápida

1. Edita `Main.java` y rellena:
   - `ANTHROPIC_API_KEY` → tu clave de api.anthropic.com
   - `SITE_TITLE` → nombre de tu web
   - `SITE_URL` → dominio que vas a usar
   - `ADSENSE_ID` → tu ID de Google AdSense (puedes dejarlo vacío al principio)

2. Compila:
   ```
   mvn clean package
   ```

3. Ejecuta:
   ```
   java -jar target/seo-autonews-static-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```
   Esto genera la carpeta `docs/` con todos los HTML.

4. Sube a GitHub:
   ```
   git add .
   git commit -m "update noticias"
   git push
   ```

5. En Vercel/Netlify conecta tu repositorio GitHub y elige `docs` como carpeta raíz.

## Automatizar con cron (Linux/Mac)
```
0 */2 * * * cd /ruta/proyecto && java -jar target/seo-autonews-static-1.0-SNAPSHOT-jar-with-dependencies.jar && git add . && git commit -m "auto update" && git push
```

## Estructura generada
```
docs/
├── index.html          ← portada con todas las noticias
├── mi-noticia.html     ← cada artículo individual
└── sitemap.xml         ← para Google Search Console
```
