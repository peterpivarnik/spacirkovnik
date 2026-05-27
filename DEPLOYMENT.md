# Deployment – spacirkovnik.sk (Astro)

Tento dokument popisuje postup nasadenia novej Astro prezentačnej stránky na Websupport hosting.

## Situácia

- `spacirkovnik.sk/web/` – obsahuje WordPress (slúži pre `album.spacirkovnik.sk`)
- Websupport aliasy nepodporujú vlastný document root
- Riešenie: Astro site sa uloží do `web/site/`, `.htaccess` transparentne presmeruje `spacirkovnik.sk` na tento priečinok

## Postup

### 1. Build

```bash
cd website
npm run build
```

Výstup je v `website/dist/`.

### 2. Nahratie súborov cez Monsta FTP

1. Prihlás sa na Websupport → Pokročilá konfigurácia → Web → File Manager (Monsta FTP)
2. Prejdi do `web/`
3. Vytvor nový priečinok `site/`
4. Nahraj **obsah** `website/dist/` do `web/site/`
   - `index.html`
   - `blog/`
   - `hry/`
   - `kontakt/`
   - `favicon.png`
   - `_astro/`
   - `sitemap-0.xml`
   - `sitemap-index.xml`

### 3. Úprava .htaccess

Otvor `web/.htaccess` a **na úplný začiatok** (pred `# BEGIN W3TC`) pridaj:

```apache
# Astro site pre spacirkovnik.sk
RewriteEngine On
RewriteCond %{HTTP_HOST} ^(www\.)?spacirkovnik\.sk$ [NC]
RewriteCond %{REQUEST_URI} !^/site/
RewriteRule ^(.*)$ /site/$1 [L]
```

> Súčasný blok s `under-construction.html` presmerovaním zmaž – nahradí ho tento.

### 4. Overenie

- `https://spacirkovnik.sk` → zobrazí Astro landing page
- `https://album.spacirkovnik.sk` → zobrazí WordPress fotoalbum (bez zmeny)
- `https://spacirkovnik.sk/blog/` → zobrazí blog
- `https://spacirkovnik.sk/hry/` → zobrazí zoznam hier

## Aktualizácia webu

Pri každej zmene stačí:
1. `npm run build`
2. Nahrať zmenené súbory do `web/site/` (prepísať existujúce)
