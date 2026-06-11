# Skripty

Pomocné skripty pre Špacírkovník (deploy webu, deploy hier na Firebase, export emailov).
**Všetky spúšťaj z koreňa projektu**, nie z adresára `scripts/`.

---

## Env súbory (nastav raz)

| Súbor | V gite? | Obsah |
|---|---|---|
| `http-client.env.json` | ✅ áno | verejné: `dbUrl` (URL Firebase RTDB) |
| `http-client.private.env.json` | ❌ nie (gitignored) | tajné: `dbSecret` + FTP údaje (`ftpHost`, `ftpUser`, `ftpPass`, `ftpRemoteDir`) |
| `serviceAccountKey.json` | ❌ nie (gitignored) | service-account kľúč z Firebase (pre export emailov) |

`http-client.private.env.json` – príklad:
```json
{
  "dev": {
    "dbSecret": "…",
    "ftpHost": "…",
    "ftpUser": "…",
    "ftpPass": "…",
    "ftpRemoteDir": "spacirkovnik.sk/web/site"
  }
}
```

> `http-client.env.json` + `http-client.private.env.json` používa aj IDE HTTP client (`upload.http`) – preto musia ostať v rovnakom adresári ako `.http` súbor.

---

## `deploy-games.py` — nahratie hier/katalógu na Firebase

Zvýši `version` zmenených hier (v JSON aj v katalógu) a nahrá ich na Firebase RTDB.

```bash
python3 scripts/deploy-games.py                  # auto: hry zmenené oproti poslednému commitu (git diff HEAD)
python3 scripts/deploy-games.py --all            # všetky hry + katalóg
python3 scripts/deploy-games.py --catalog-only   # len katalóg, BEZ zvýšenia verzií
python3 scripts/deploy-games.py rybar-z-drazdiaka            # konkrétna hra
python3 scripts/deploy-games.py rybar-z-drazdiaka lesnicka-palica   # viac hier naraz
```

| Prepínač | Význam |
|---|---|
| *(bez argumentov)* | nahrá hry zmenené oproti poslednému commitu |
| `--all` | nahrá všetky hry + katalóg |
| `--catalog-only` | nahrá len katalóg, nemení verzie |
| `<game-id> …` | nahrá konkrétne hry (podľa názvu súboru bez `.json`) |

Potrebuje: `http-client.env.json` (dbUrl) + `http-client.private.env.json` (dbSecret), `curl`.

---

## `upload.http` — manuálny upload cez IDE

Otvor v IntelliJ/VS Code (REST/HTTP client) a klikni na ▶ pri požiadavke.
Premenné `{{dbUrl}}` / `{{dbSecret}}` sa berú z `http-client(.private).env.json` (prostredie **dev**).
Alternatíva k `deploy-games.py`, keď chceš nahrať ručne jednotlivé súbory.

---

## `deploy-website.sh` — build a nasadenie webu (spacirkovnik.sk)

Zbuilduje Astro stránku (`npm run build`) a nahrá `website/dist` na server (Websupport) cez FTPS.

```bash
./scripts/deploy-website.sh
```

Bez prepínačov. Potrebuje FTP údaje v `http-client.private.env.json` (`ftpHost`, `ftpUser`, `ftpPass`, `ftpRemoteDir`).

### Štruktúra servera
```
FTP root /
├── spacirkovnik.sk/
│   └── web/                 ← document root pre spacirkovnik.sk
│       ├── site/            ← sem deploy-website.sh nahrá Astro stránku (= ftpRemoteDir)
│       ├── wp-admin/        ← WordPress (slúži pre album.spacirkovnik.sk)
│       ├── wp-content/
│       ├── .htaccess        ← rewrite: spacirkovnik.sk → /site/
│       └── index.php
└── ...
```
`ftpRemoteDir` = `spacirkovnik.sk/web/site`.

### .htaccess (už nastavené na serveri)
V `spacirkovnik.sk/web/.htaccess` je na začiatku (pred `# BEGIN W3TC`):
```apache
# Astro site pre spacirkovnik.sk
RewriteEngine On
RewriteCond %{HTTP_HOST} ^(www\.)?spacirkovnik\.sk$ [NC]
RewriteCond %{REQUEST_URI} !^/site/
RewriteRule ^(.*)$ /site/$1 [L]
```

### Overenie po deploy
- `https://spacirkovnik.sk` → Astro landing page
- `https://spacirkovnik.sk/hry/` → zoznam hier
- `https://spacirkovnik.sk/blog/` → blog
- `https://album.spacirkovnik.sk` → WordPress fotoalbum (bez zmeny)

---

## `export-completed-emails.py` — emaily hráčov, čo dokončili hru

Vytiahne z Firebase emaily hráčov, ktorí dokončili danú hru (spojí `completions/{uid}` z RTDB s emailmi z Firebase Auth).

Príprava (raz):
```bash
# 1) Firebase Console → Project settings → Service accounts → Generate new private key
#    → ulož ako scripts/serviceAccountKey.json
# 2) inštalácia závislostí (firebase-admin)
python3 -m pip install -r scripts/requirements.txt
```

Spustenie:
```bash
python3 scripts/export-completed-emails.py rybar-z-drazdiaka            # vypíše emaily
python3 scripts/export-completed-emails.py rybar-z-drazdiaka > emails.txt   # do súboru
```

| Argument | Význam |
|---|---|
| `<game-id>` | hra, ktorej dokončenia hľadáme (default `rybar-z-drazdiaka`) |

> ⚠️ Emaily hráčov sú osobné údaje (GDPR) – narábaj s nimi podľa zásad ochrany súkromia.

---

## `pick-winners.py` — vyžrebovanie výhercov

Náhodne vyberie N výhercov spomedzi hráčov, ktorí dokončili hru (rovnaký zdroj dát ako export vyššie).

```bash
python3 scripts/pick-winners.py rybar-z-drazdiaka          # 3 výhercovia (default)
python3 scripts/pick-winners.py rybar-z-drazdiaka 5        # 5 výhercov
python3 scripts/pick-winners.py rybar-z-drazdiaka > winners.txt
```

| Argument | Význam |
|---|---|
| `<game-id>` | hra, z ktorej žrebujeme (default `rybar-z-drazdiaka`) |
| `[počet]` | koľko výhercov (default `3`); ak je hráčov menej, vyberie všetkých |

Rovnaké požiadavky ako export (`serviceAccountKey.json` + `pip install`). Spoločnú logiku majú v `lib/completed_emails.py`.
