# Google Play Store — listing texts and metadata

Finálne texty a metadáta pre submission do Play Console. Pri uploade aplikácie sa tieto hodnoty kopírujú/nastavujú v Play Console.

---

## Základné údaje

| Pole | Hodnota |
|------|---------|
| **Názov aplikácie** | Spacírkovník |
| **Package name** | `sk.spacirkovnik` |
| **Default language** | Slovak — Slovakia (sk-SK) |
| **Trhy** | iba Slovensko |
| **Kategória** | Travel & Local |
| **Cieľová veková skupina** | 7+ |
| **Contact email** | info@spacirkovnik.sk |
| **Privacy policy URL** | https://peterpivarnik.github.io/spacirkovnik/privacy.html |

---

## Tagline / slogan (banner, web, marketing)

```
Sleduj mapu, lúšti hádanky, objavuj mesto!
```

---

## Krátky popis (max 80 znakov)

```
Rodinné prechádzky, ktoré sa hrajú ako dobrodružstvo.
```

(64 znakov)

---

## Dlhý popis (max 4000 znakov)

```
Spacírkovník mení obyčajnú prechádzku na dobrodružstvo.

Stiahneš si hru, vyrazíš na trasu a postupne odhaľuješ
príbeh — pomáhaš lesníkovi nájsť palicu, počúvaš spomienky
starého rybára, objavuješ tajomstvá chorvátskych osadníkov.

Každá hra ťa zavedie po peknej prírodnej trase. Na konkrétnych
miestach sa otvoria úlohy, otázky a scény z príbehu. Žiadne
naháňanie bodov, žiadne odznaky — len objavovanie.

🌳 PRE KOHO
Pre rodiny s deťmi (7+ rokov), pre dvojice, pre kohokoľvek
kto chce vyjsť von a spoznať okolie zaujímavejšie než cez
Wikipedia. Niektoré úlohy si vyžadujú čítanie — vhodné pre
deti, ktoré už čítajú samostatne, alebo s pomocou rodiča.

🗺️ AKTUÁLNE HRY
• Lesnícka palica Gejzu Dražďáka — Petržalka, 3.8 km, 2 hod
• Tajomstvo Janka Kráľa — Petržalka, 2 km, 1.5 hod (pripravujeme)
• Chorvátski osadníci — Petržalka, 2.5 km, 1.5 hod (pripravujeme)
• Danubiana – Umenie na vode — Čunovo, 1.5 km, 1 hod (pripravujeme)
• Rybár z Draždiaka — Petržalka, 2 km, 1.5 hod (pripravujeme)

💶 CENA
Aplikácia je zadarmo. Niektoré hry sú zadarmo, ostatné si
môžeš odomknúť priamo v aplikácii cez Google Play. Žiadne
predplatné, žiadne reklamy — jednorazový nákup, hra ti
ostáva navždy.

⚙️ AKO TO FUNGUJE
1. Otvor appku a vyber hru
2. Príď na štartovacie miesto
3. Počas prechádzky sa ti na GPS bodoch otvárajú úlohy
4. Vyriešiš úlohu → posunieš sa ďalej v príbehu

📍 KDE
Aktuálne v Bratislave (Petržalka, Čunovo). Postupne pribudnú
ďalšie lokality.

🔒 SÚKROMIE
Žiadne sledovanie, žiadne reklamy. Prihlásenie cez Google
slúži len na uloženie aktivovaných hier. Tvoja poloha sa
používa výlučne v rámci aplikácie na zobrazenie mapy.
```

---

## Cenník hier (in-app products)

Konfiguruje sa v Play Console → *Monetize → Products → In-app products*.

| Hra | Product ID | Cena (SK) | Stav |
|-----|-----------|-----------|------|
| Lesnícka palica Gejzu Dražďáka | — | **zadarmo** | bez Play Billing |
| Tajomstvo Janka Kráľa | `game_tajomstvo_janka_krala` | 9.99 € | platená |
| Chorvátski osadníci | `game_chorvatski_osadnici` | 9.99 € | platená |
| Danubiana – Umenie na vode | `game_danubiana_umenie_na_vode` | 9.99 € | platená |
| Rybár z Draždiaka | `game_rybar_z_drazdiaka` | 9.99 € | platená |

Po Google provízii (15% pri prvých $1M ročne) z 9.99 € ostáva ~8.49 €.

Ceny sa dajú meniť v Play Console kedykoľvek bez nutnosti re-submission appky.

---

## Compliance / Data safety form (Play Console)

Pri vyplnení Data safety form deklarovať:

**Zbierané údaje:**
- **Email** (Account info) — collected, linked to user, required, purpose: App functionality, Account management
- **User ID** (Firebase UID, Account info) — collected, linked to user, required, purpose: App functionality
- **App activity** (Purchase history → activated games) — collected, linked to user, required, purpose: App functionality
- **Approximate location / Precise location** — collected, **not linked to user**, optional, purpose: App functionality
  - *In-app only, never sent to server*

**NIE je deklarované:**
- Žiadna analytika
- Žiadne reklamné SDK
- Žiadny crash reporting tretej strany (zatiaľ)
- Žiadny share dát s tretími stranami okrem Google (Firebase, Maps)

---

## Content rating (IARC questionnaire)

Vyplniť v Play Console pri submission. Očakávané odpovede:
- Cieľová veková skupina: 7+
- Žiadne násilie, žiadny sexuálny obsah, žiadne nadávky
- Bez user-generated content (nič nepublikujú používatelia)
- Žiadne reklamy
- In-app purchases: ÁNO

Predpoklad výsledného ratingu: **PEGI 3** alebo **PEGI 7**.

---

## Assets — checklist

| # | Asset | Rozmer / format | Stav |
|---|-------|-----------------|------|
| 1 | App icon (Play Store) | 512×512 PNG | ⏳ čaká na logo |
| 2 | App icon (launcher, adaptive) | foreground + background | ⏳ čaká na logo |
| 3 | Feature graphic | 1024×500 PNG/JPG | ⏳ čaká na logo |
| 4 | Phone screenshots (min 2, ideal 4-8) | 1080×1920 alebo podobné 16:9 | ⏳ |
| 5 | Tablet screenshots (voliteľné) | 1200×1920 alebo podobné | ⏸️ neskôr |
| 6 | Promo video (voliteľné) | YouTube link | ⏸️ neskôr |

---

## Externé URL

- **Privacy Policy:** https://peterpivarnik.github.io/spacirkovnik/privacy.html
- **Privacy Policy (EN):** https://peterpivarnik.github.io/spacirkovnik/privacy-en.html
- **Landing page:** https://peterpivarnik.github.io/spacirkovnik/

---

## Notes

- Po prvom uploade AAB do Play Console sa aktivuje **Play App Signing** — vygeneruje vlastný SHA-1/SHA-256, ktoré treba pridať do Firebase (viď `PUBLISHING.md` Priority 4)
- Ak Lesnícka palica zostane jediná hrateľná pri launchi, ostatné štyri hry si používateľ uvidí ako "*pripravujeme*" — toto **netriggeruje** Google review problém, ale UX sa dá zlepšiť až ich postupným dokončovaním
