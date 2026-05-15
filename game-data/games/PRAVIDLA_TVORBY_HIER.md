# Pravidlá tvorby hier pre Špacírkovník

## 1. Štruktúra JSON súboru

```json
{
  "id": "kebab-case-nazov",
  "title": "Názov hry",
  "version": 1,
  "screens": [ ... ]
}
```

- `id` — kebab-case, bez diakritiky
- `version` — celé číslo, zvyšuje sa pri úpravách
- `screens` — pole obrazoviek, poradie = poradie v hre

---

## 2. Typy obrazoviek (screen types)

| Typ | Kedy sa používa | Povinné polia |
|-----|----------------|----------------|
| `CONTINUE` | Úvodná/záverečná obrazovka, príbehový medzikrok s tlačidlom | `text`, `buttonText` |
| `BROWSE` | Čisto naratívna obrazovka (len text, hráč pokračuje swipom/kliknutím) | `text` |
| `NAVIGATION` | Navigácia na GPS bod – hráč sa musí fyzicky presunúť | `text`, `targetLatitude`, `targetLongitude` |
| `QUESTION` | Kvízová otázka s výberom odpovedí | `text`, `answers` |

Všetky typy majú vždy pole `imageUrl` (môže byť `""`).

---

## 3. Priebeh hry – povinná kostra

1. **Úvodná obrazovka** — typ `CONTINUE`, `fontSize: 26`, `buttonText: "Poď na dobrodružstvo!"`
2. **Intro od Špacírkovníka** — typ `BROWSE`, text o tom, že magický rozcestník sa roztočil a šípka ukazuje smerom k miestu dobrodružstva
3. **Navigácia na štart** — typ `NAVIGATION`, krátky text "presunú sa na miesto"
3. **Úvod do príbehu** — 2–4 obrazovky `BROWSE` / `CONTINUE`, predstavenie postavy/sveta, zápletka
4. **Hlavná slučka** — striedanie:
   - `NAVIGATION` → presun na ďalšie miesto
   - `BROWSE` → príbeh, kontext, zaujímavosť o mieste
   - `QUESTION` → otázka viažuca sa na miesto alebo príbeh
   - `BROWSE` → reakcia na správnu odpoveď, posun deja
5. **Záver** — rozuzlenie príbehu, poďakovanie hráčovi
6. **Outro od Špacírkovníka** — typ `CONTINUE`, text o tom, že šípka zhasla, dobrodružstvo splnené, ostatné šípky ešte čakajú
7. **Posledná obrazovka** — typ `CONTINUE`, `fontSize: 22`, `buttonText: "Späť na zoznam hier"`

---

## 4. Genderovo inkluzívny jazyk

Používa sa formát `{mužský tvar|ženský tvar}` — aplikácia zobrazí správny tvar podľa nastavenia hráča.

### Vzory:
- `{mladý priateľ|mladá priateľka}`
- `{odvážny pomocník|odvážna pomocníčka}`
- `{šikovný bádateľ|šikovná bádateľka}`
- `{múdry|múdra}`
- `{dokázal|dokázala}`
- `{bol|bola}`
- Prípona pre minulý čas: `{·a}` → zobrazí sa ako "" alebo "a" (napr. `počul{·a}`)

---

## 5. Otázky (QUESTION)

- Vždy **4 odpovede** (výnimočne 2 pri áno/nie rozhodnutiach)
- Presne **1 správna** (`"correct": true`), ostatné `false`
- Otázky sa viažu na:
  - niečo viditeľné na mieste (tabuľa, budova, socha)
  - prírodu/zvieratá v okolí
  - historický fakt o mieste
  - všeobecné vedomosti súvisiace s príbehom
- Po správnej odpovedi vždy nasleduje `BROWSE` s pochvalou a posunom deja

---

## 6. Príbeh – pravidlá

- **Rozprávkový prvok**: každá hra musí obsahovať aspoň jeden magický/rozprávkový element — napr. kúzelná palička, hovoriaci duch, zlatá rybka, čarovný zvonček, enchanted mapa, rozprávková bytosť a pod. Hra má mať rozprávkový nádych, nie byť suchá prechádzka s kvízom.
- **Cieľová skupina**: deti cca 6–12 rokov
- **Jazyk**: slovenčina, jednoduchá, priateľská, nadšená
- **Príbeh má jasný oblúk**: zápletka → hľadanie/plnenie úloh → rozuzlenie
- **Hlavná postava / sprievodca** priamo oslovuje hráča (dialóg v úvodzovkách)
- **Sprievodca je vždy lokálna postava viazaná na miesto** — duch, kúzelné zviera, ožívajúci objekt a pod. Každá hra má svojho unikátneho sprievodcu, nie univerzálnu maskotku.
- **Špacírkovník ako rámec**: Špacírkovník je magický rozcestník, ktorý vysiela deti na dobrodružstvá. Každá hra začína krátkym úvodom od Špacírkovníka ("šípka sa rozžiarila, ukazuje smerom k...") a končí outro ("šípka zhasla, dobrodružstvo splnené"). Lokálny sprievodca potom preberá vedenie počas celej hry.
- **Edukatívny obsah** je vplietaný do príbehu prirodzene (nie ako prednáška)
- **Reálne miesta** — súradnice musia zodpovedať skutočným bodom na mape
- Vzdialenosti medzi bodmi by mali byť **pešo zvládnuteľné** (max ~500 m medzi bodmi)
- Celková trasa: cca **1–3 km**

---

## 7. Počet obrazoviek

- Celkovo **25–46 obrazoviek**
- Z toho **8–10 otázok** (QUESTION)
- Z toho **8–12 navigácií** (NAVIGATION)
- Zvyšok sú BROWSE a CONTINUE

---

## 8. Obrázky (imageUrl)

- Formát: Firebase Storage URL alebo prázdny string `""`
- Obrázky sú voliteľné, ale odporúčané aspoň pre úvod a kľúčové momenty
- Rovnaký obrázok sa môže opakovať na súvisiacich obrazovkách (napr. keď hovorí rovnaká postava)

---

## 9. Voliteľné polia

| Pole | Typ | Kedy použiť |
|------|-----|-------------|
| `fontSize` | number | Len na úvodnej (26) a záverečnej (22) obrazovke |
| `screenId` | number | Sekvenčné číslovanie od 0 — nepovinné, niektoré hry ho nemajú |

---

## 10. Generovanie JSON — úvodzovky a bezpečnosť

Slovenské úvodzovky v JSON sú najčastejší zdroj chýb. **Povinný postup:**

1. **Nikdy nepísať JSON ručne** — vždy generovať cez Python `json.dumps()` s `ensure_ascii=False`
2. **Používať placeholdery** — v textoch písať `<<` a `>>` namiesto `„` (U+201E) a `"` (U+201C), potom zameniť:
   ```python
   text = text.replace("<<", "\u201e").replace(">>", "\u201c")
   ```
3. **Po každej úprave validovať**: `python3 -c "import json; json.load(open('subor.json')); print('OK')"`
4. **Nikdy nepoužívať ASCII `"` (U+0022) vnútri textov** — v JSON to rozbije string delimiter

### Tri typy úvodzoviek:
| Znak | Unicode | Účel |
|------|---------|------|
| `"` | U+0022 | JSON delimiter — NIKDY vnútri textu |
| `„` | U+201E | Slovenská otváracia — vnútri textu |
| `"` | U+201C | Slovenská zatváracia — vnútri textu |

---

## 11. Kontrolný zoznam pred odovzdaním

- [ ] JSON je validný (žiadne trailing čiarky, správne úvodzovky)
- [ ] Prvá obrazovka je CONTINUE s názvom hry
- [ ] Posledná obrazovka je CONTINUE s "Späť na zoznam hier"
- [ ] Každá QUESTION má presne 1 správnu odpoveď
- [ ] Všetky GPS súradnice sú reálne a v rozumnej pešej vzdialenosti
- [ ] Gender formy `{m|ž}` sú konzistentné a kompletné
- [ ] Otázky sú overiteľné (hráč ich môže zodpovedať na mieste alebo zo znalostí)
- [ ] Príbeh má jasný začiatok, stred a koniec
- [ ] Žiadne dva NAVIGATION po sebe bez BROWSE/QUESTION medzi nimi
