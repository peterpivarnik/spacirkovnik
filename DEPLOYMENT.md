# Deployment – spacirkovnik.sk (Astro)

## Štruktúra servera (Websupport)

```
FTP root /
├── spacirkovnik.sk/
│   └── web/                 ← document root pre spacirkovnik.sk
│       ├── site/            ← Astro stránka (sem deploy.sh nahrá súbory)
│       ├── wp-admin/        ← WordPress (slúži pre album.spacirkovnik.sk)
│       ├── wp-content/
│       ├── .htaccess        ← rewrite: spacirkovnik.sk → /site/
│       └── index.php
├── tmp/
└── ...
```

## Nasadenie (bežná aktualizácia)

```bash
./deploy.sh
```

Skript automaticky:
1. Zbuilduje stránku (`npm run build`)
2. Nahrá zmenené súbory na server cez FTPS

## Prvotné nastavenie (už hotové)

### FTP credentials
Uložené v `website/.env` (nie je v gite):
```
FTP_HOST=...
FTP_USER=...
FTP_PASS=...
FTP_REMOTE_DIR=spacirkovnik.sk/web/site
```

### .htaccess
V `spacirkovnik.sk/web/.htaccess` je na začiatku (pred `# BEGIN W3TC`):
```apache
# Astro site pre spacirkovnik.sk
RewriteEngine On
RewriteCond %{HTTP_HOST} ^(www\.)?spacirkovnik\.sk$ [NC]
RewriteCond %{REQUEST_URI} !^/site/
RewriteRule ^(.*)$ /site/$1 [L]
```

## Overenie

- `https://spacirkovnik.sk` → Astro landing page
- `https://album.spacirkovnik.sk` → WordPress fotoalbum (bez zmeny)
- `https://spacirkovnik.sk/hry/` → zoznam hier
- `https://spacirkovnik.sk/blog/` → blog
