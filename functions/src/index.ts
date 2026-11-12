/**
 * Serverové overovanie nákupov z Google Play.
 *
 * Appka po nákupe nič nezapisuje do databázy — len zavolá [verifyPurchase] s nákupným tokenom.
 * Funkcia nákup overí priamo u Googlu, potvrdí ho (acknowledge) a až potom zapíše aktiváciu.
 * [checkVoidedPurchases] raz denne odoberie aktivácie pri vrátených / zrušených nákupoch.
 *
 * Štruktúra v RTDB (Admin SDK obchádza pravidlá):
 *   purchaseClaims/{sha256(token)} = uid           — jeden nákup patrí jednému účtu (aj staré záznamy z appky)
 *   purchases/{sha256(token)}      = {uid, gameId, productId, orderId, ...}  — evidencia pre vrátenia
 *   activations/{uid}/{gameId}     = {active: true, type: "purchase"}       — zapisuje server po nákupe
 *                                  = {active: true, type: "prize", note}    — zapisuje prevádzkovateľ ručne
 *                                  = true                                   — starý tvar, platí naďalej
 * Server siaha len na aktivácie z nákupu (type "purchase" alebo starý tvar true) — výhry nechá tak.
 */
import { createHash } from "node:crypto";
import { initializeApp } from "firebase-admin/app";
import { getDatabase } from "firebase-admin/database";
import { setGlobalOptions, logger } from "firebase-functions/v2";
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { google } from "googleapis";

const PACKAGE_NAME = "sk.spacirkovnik";
const DATABASE_URL = "https://spacirkovnik-app-default-rtdb.europe-west1.firebasedatabase.app";
/** Koľko dní dozadu kontrolujeme vrátené nákupy. Beh je denný, rezerva pokryje výpadky. */
const VOIDED_LOOKBACK_DAYS = 7;

// Rovnaký región ako databáza — appka volá funkcie v tomto regióne.
setGlobalOptions({ region: "europe-west1", maxInstances: 5 });

initializeApp({ databaseURL: DATABASE_URL });
const db = getDatabase();

// Prihlasuje sa default service accountom funkcie — ten musí byť pozvaný v Play Console.
const publisher = google.androidpublisher({
  version: "v3",
  auth: new google.auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] }),
});

// Hodnoty purchaseState / acknowledgementState z Play Developer API.
const PURCHASE_STATE_PURCHASED = 0;
const PURCHASE_STATE_PENDING = 2;
const ACK_STATE_NOT_ACKNOWLEDGED = 0;

interface CatalogItem {
  id: string;
  googlePlayProductId?: string;
}

interface PurchaseRecord {
  uid: string;
  gameId: string;
  productId: string;
  orderId?: string | null;
  verifiedAt?: number;
  voided?: boolean;
  voidedAt?: number;
}

const sha256 = (s: string) => createHash("sha256").update(s).digest("hex");

/** Aktivácia z nákupu — tú server smie zapísať aj zmazať. Ostatné (výhry) sú ručné. */
const PURCHASE_ACTIVATION = { active: true, type: "purchase" } as const;

/** Je existujúca aktivácia z nákupu (alebo starého tvaru `true`)? Na výhry server nesiaha. */
function isPurchaseActivation(value: unknown): boolean {
  if (value === true) return true;
  return typeof value === "object" && value !== null && (value as { type?: unknown }).type === "purchase";
}

/** Odomyká záznam hru? Rovnaké pravidlo ako v appke (isActivationActive). */
function isActiveActivation(value: unknown): boolean {
  if (value === true) return true;
  return typeof value === "object" && value !== null && (value as { active?: unknown }).active === true;
}

/** Katalóg z RTDB (deploy-games.py ho nahráva na catalog/games-info). Firebase môže pole vrátiť aj ako objekt. */
async function loadCatalog(): Promise<CatalogItem[]> {
  const raw = (await db.ref("catalog/games-info").get()).val();
  if (!raw) return [];
  return (Array.isArray(raw) ? raw : Object.values(raw)).filter(Boolean) as CatalogItem[];
}

/** gameId podľa produktu v Play — zdroj pravdy je katalóg, nie to, čo pošle klient. */
async function gameIdForProduct(productId: string): Promise<string | null> {
  const catalog = await loadCatalog();
  return catalog.find((g) => g.googlePlayProductId === productId)?.id ?? null;
}

/** Atomicky priradí nákup účtu, ak ešte nie je priradený. Vráti konečného vlastníka. */
async function claimPurchase(tokenHash: string, uid: string): Promise<string | null> {
  const result = await db.ref(`purchaseClaims/${tokenHash}`).transaction((current) => current ?? uid);
  return result.committed ? (result.snapshot.val() as string) : null;
}

export const verifyPurchase = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Pre aktiváciu špacírky sa musíš prihlásiť.");

  const { purchaseToken, productId } = (request.data ?? {}) as { purchaseToken?: unknown; productId?: unknown };
  if (typeof purchaseToken !== "string" || !purchaseToken || typeof productId !== "string" || !productId) {
    throw new HttpsError("invalid-argument", "Chýbajú údaje o nákupe.");
  }

  const gameId = await gameIdForProduct(productId);
  if (!gameId) {
    logger.error("Neznámy produkt", { productId, uid });
    throw new HttpsError("not-found", "Produkt nepatrí žiadnej špacírke.");
  }

  // 1. Overenie priamo u Googlu.
  let purchase;
  try {
    purchase = (await publisher.purchases.products.get({
      packageName: PACKAGE_NAME,
      productId,
      token: purchaseToken,
    })).data;
  } catch (e) {
    logger.warn("Play API odmietlo token", { productId, uid, error: String(e) });
    throw new HttpsError("permission-denied", "Nákup sa nepodarilo overiť.");
  }

  if (purchase.purchaseState === PURCHASE_STATE_PENDING) {
    return { status: "pending", gameId };
  }
  if (purchase.purchaseState !== PURCHASE_STATE_PURCHASED) {
    throw new HttpsError("failed-precondition", "Nákup bol zrušený.");
  }

  // 2. Jeden nákup = jeden účet.
  const tokenHash = sha256(purchaseToken);
  const owner = await claimPurchase(tokenHash, uid);
  if (owner === null) throw new HttpsError("unavailable", "Nepodarilo sa overiť nákup. Skús to znova.");
  if (owner !== uid) {
    throw new HttpsError(
      "already-exists",
      "Túto špacírku zakúpil iný účet na tomto zariadení. Ak je tvoja, prihlás sa účtom, ktorým bola kúpená.",
    );
  }

  // 3. Potvrdenie u Googlu — bez neho by Google nákup do 3 dní automaticky vrátil.
  // Zlyhanie neblokuje aktiváciu: nákup je overený, a ak potvrdenie naozaj neprešlo, appka ho
  // pri ďalšom spustení pošle znova (vidí ho ako nepotvrdený). Pri súbežnom overení toho istého
  // nákupu môže druhé volanie zlyhať, lebo nákup už potvrdilo prvé.
  if (purchase.acknowledgementState === ACK_STATE_NOT_ACKNOWLEDGED) {
    try {
      await publisher.purchases.products.acknowledge({ packageName: PACKAGE_NAME, productId, token: purchaseToken });
    } catch (e) {
      logger.warn("Acknowledge zlyhal", { uid, productId, orderId: purchase.orderId, error: String(e) });
    }
  }

  // 4. Aktivácia + evidencia pre prípadné vrátenie.
  // Ak hráč hru už má ako výhru, aktiváciu neprepíšeme — inak by ju vrátenie nákupu zmazalo.
  const record: PurchaseRecord = { uid, gameId, productId, orderId: purchase.orderId ?? null, verifiedAt: Date.now() };
  const existing = (await db.ref(`activations/${uid}/${gameId}`).get()).val();
  const updates: Record<string, unknown> = { [`purchases/${tokenHash}`]: record };
  if (!isActiveActivation(existing) || isPurchaseActivation(existing)) {
    updates[`activations/${uid}/${gameId}`] = PURCHASE_ACTIVATION;
  }
  await db.ref().update(updates);

  logger.info("Nákup overený", { uid, gameId, orderId: purchase.orderId });
  return { status: "activated", gameId };
});

/**
 * Nákupy overené ešte pred touto funkciou (aktiváciu si zapísala appka) nemajú záznam v purchases/.
 * Vlastníka nájdeme v purchaseClaims/ a produkt zistíme skúšaním produktov z katalógu.
 */
async function resolveLegacyPurchase(token: string, tokenHash: string): Promise<PurchaseRecord | null> {
  const uid = (await db.ref(`purchaseClaims/${tokenHash}`).get()).val() as string | null;
  if (!uid) return null;
  for (const game of await loadCatalog()) {
    if (!game.googlePlayProductId) continue;
    try {
      const p = (await publisher.purchases.products.get({
        packageName: PACKAGE_NAME,
        productId: game.googlePlayProductId,
        token,
      })).data;
      return { uid, gameId: game.id, productId: game.googlePlayProductId, orderId: p.orderId ?? null };
    } catch {
      // Token nepatrí tomuto produktu — skúsime ďalší.
    }
  }
  return null;
}

/** Má používateľ na túto hru ešte iný, nevrátený nákup? (napr. vrátil a kúpil znova) */
async function hasOtherValidPurchase(uid: string, gameId: string, exceptHash: string): Promise<boolean> {
  const snap = await db.ref("purchases").orderByChild("uid").equalTo(uid).get();
  let found = false;
  snap.forEach((child) => {
    const r = child.val() as PurchaseRecord;
    if (child.key !== exceptHash && r.gameId === gameId && !r.voided) found = true;
  });
  return found;
}

export const checkVoidedPurchases = onSchedule(
  { schedule: "every day 04:00", timeZone: "Europe/Bratislava" },
  async () => {
    const startTime = String(Date.now() - VOIDED_LOOKBACK_DAYS * 24 * 60 * 60 * 1000);
    let pageToken: string | undefined;
    let revoked = 0;

    do {
      const res = (await publisher.purchases.voidedpurchases.list({
        packageName: PACKAGE_NAME,
        startTime,
        token: pageToken,
      })).data;

      for (const v of res.voidedPurchases ?? []) {
        if (!v.purchaseToken) continue;
        const tokenHash = sha256(v.purchaseToken);
        const ref = db.ref(`purchases/${tokenHash}`);

        let record = (await ref.get()).val() as PurchaseRecord | null;
        if (record?.voided) continue; // už spracované v predchádzajúcom behu
        record ??= await resolveLegacyPurchase(v.purchaseToken, tokenHash);
        if (!record) {
          logger.warn("Vrátený nákup bez známeho vlastníka", { orderId: v.orderId });
          continue;
        }

        const updates: Record<string, unknown> = {
          [`purchases/${tokenHash}`]: { ...record, voided: true, voidedAt: Number(v.voidedTimeMillis) || Date.now() },
        };
        const activationPath = `activations/${record.uid}/${record.gameId}`;
        const activation = (await db.ref(activationPath).get()).val();
        if (isPurchaseActivation(activation) && !(await hasOtherValidPurchase(record.uid, record.gameId, tokenHash))) {
          updates[activationPath] = null;
          revoked++;
        }
        await db.ref().update(updates);
        logger.info("Vrátený nákup spracovaný", { uid: record.uid, gameId: record.gameId, orderId: v.orderId });
      }
      pageToken = res.tokenPagination?.nextPageToken ?? undefined;
    } while (pageToken);

    logger.info(`Kontrola vrátených nákupov hotová, odobraté aktivácie: ${revoked}`);
  },
);
