# Cloud Functions — overovanie nákupov

| Funkcia | Čo robí |
|---|---|
| `verifyPurchase` (callable) | Appka pošle `purchaseToken` + `productId`. Funkcia nákup overí u Googlu, priradí ho účtu (`purchaseClaims`), potvrdí (acknowledge) a zapíše `activations/{uid}/{gameId}` + záznam do `purchases/`. |
| `checkVoidedPurchases` (denne 4:00) | Načíta vrátené/zrušené nákupy z Play (posledných 7 dní) a odoberie k nim aktivácie. |

Región `europe-west1` (ako databáza). `gameId` sa určuje z katalógu v RTDB podľa `googlePlayProductId`.

## Jednorazové nastavenie

1. **Blaze plán** — Firebase Console → Upgrade. Nastav rozpočtové upozornenie (Google Cloud → Billing → Budgets), napr. 1 €.
2. **Play Developer API** — Google Cloud Console (projekt `spacirkovnik-app`) → APIs & Services → zapni *Google Play Android Developer API*.
3. **Prístup funkcie do Play Console** — Play Console → Users and permissions → Invite new user:
   - e-mail: `1058528478022-compute@developer.gserviceaccount.com` (default service account funkcií v2)
   - App permissions → Špacírkovník: *View financial data, orders and cancellation survey responses* + *Manage orders and subscriptions*
   - Povolenie sa niekedy prejaví až po niekoľkých hodinách.
4. **Firebase CLI** — `npm install -g firebase-tools && firebase login`

## Nasadenie

```bash
cd functions && npm install && cd ..
firebase deploy --only functions
```

`firebase.json` zámerne neobsahuje pravidlá databázy — deploy funkcií ich neprepíše.

Logy: `firebase functions:log` alebo Google Cloud Console → Logging.

## Poradie spustenia

1. Nasadiť funkcie (nič sa nerozbije — zatiaľ ich nikto nevolá).
2. Otestovať s licenčným testovacím účtom (Play Console → Settings → License testing) na internej testovacej verzii.
3. Vydať novú verziu appky.
4. Keď väčšina používateľov aktualizuje (Play Console → Statistics → podľa verzie), sprísniť pravidlá
   v Firebase Console → Realtime Database → Rules:

```json
"activations":    { "$uid": { ".read": "auth != null && auth.uid === $uid", ".write": false } },
"purchaseClaims": { ".read": false, ".write": false },
"purchases":      { ".read": false, ".write": false, ".indexOn": ["uid"] }
```

Nákup v starej verzii appky sa po sprísnení neaktivuje sám, ale nová verzia ho pri prihlásení
nájde (`queryPurchasesAsync`) a doaktivuje cez server.
