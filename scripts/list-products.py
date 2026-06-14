#!/usr/bin/env python3
"""
Vypíše všetky jednorazové (in-app) produkty appky aj s cenami a aktívnymi zľavami.
Dáta ťahá z Google Play Developer API (monetization.onetimeproducts).

Príprava (raz):
  1) Google Cloud Console → projekt prepojený s Play Console → Service accounts
     → vytvor service account + JSON kľúč → ulož ako scripts/play-service-account.json
  2) Play Console → Používatelia a povolenia → pozvi ten service account (email z JSON)
     s prístupom „Zobraziť informácie o aplikácii a stiahnuť hromadné prehľady"
  3) python3 -m pip install -r scripts/requirements.txt

Použitie (z koreňa projektu):
  python3 scripts/list-products.py
  python3 scripts/list-products.py --region DE     # ceny pre iný región (default SK)
"""
import os
import sys
import warnings

warnings.filterwarnings("ignore", category=FutureWarning)

from google.oauth2 import service_account
from googleapiclient.discovery import build

PACKAGE = "sk.spacirkovnik"
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_FILE = os.path.join(SCRIPT_DIR, "play-service-account.json")
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]

region = "SK"
if "--region" in sys.argv:
    region = sys.argv[sys.argv.index("--region") + 1].upper()


def fmt_money(m):
    if not m:
        return None
    amount = int(m.get("units", "0") or 0) + (m.get("nanos", 0) or 0) / 1e9
    return f"{amount:.2f} {m.get('currencyCode', '')}".strip()


def pick(configs):
    """Konfig pre zvolený región, inak prvý dostupný."""
    if not configs:
        return None
    for c in configs:
        if c.get("regionCode") == region:
            return c
    return configs[0]


def discount_text(cfg):
    if cfg is None:
        return "?"
    if cfg.get("relativeDiscount") is not None:
        rd = cfg["relativeDiscount"]
        pct = rd * 100 if rd <= 1 else rd
        return f"-{pct:.0f}%"
    if cfg.get("absoluteDiscount"):
        return f"-{fmt_money(cfg['absoluteDiscount'])}"
    return "?"


def main():
    if not os.path.exists(KEY_FILE):
        sys.exit(f"Chýba {KEY_FILE} (service-account kľúč s prístupom k Play Console).")

    creds = service_account.Credentials.from_service_account_file(KEY_FILE, scopes=SCOPES)
    service = build("androidpublisher", "v3", credentials=creds, cache_discovery=False)
    ot = service.monetization().onetimeproducts()

    products = ot.list(packageName=PACKAGE).execute().get("oneTimeProducts", [])
    print(f"\nJednorazové produkty pre {PACKAGE} (ceny pre región {region}):\n")

    for p in products:
        pid = p.get("productId")
        print(f"■ {pid}")

        for po in p.get("purchaseOptions", []):
            po_id = po.get("purchaseOptionId")
            po_state = po.get("state", "")
            base_cfg = pick(po.get("regionalPricingAndAvailabilityConfigs"))
            base_price = fmt_money(base_cfg.get("price")) if base_cfg else "—"
            print(f"    • možnosť '{po_id}' [{po_state}]: {base_price}")

            offers = ot.purchaseOptions().offers().list(
                packageName=PACKAGE, productId=pid, purchaseOptionId=po_id
            ).execute().get("oneTimeProductOffers", [])

            for o in offers:
                cfg = pick(o.get("regionalPricingAndAvailabilityConfigs"))
                do = o.get("discountedOffer", {}) or {}
                validity = ""
                if do.get("startTime") or do.get("endTime"):
                    validity = f"  ({do.get('startTime', '?')} → {do.get('endTime', '∞')})"
                print(
                    f"        ↳ zľava '{o.get('offerId')}' [{o.get('state', '')}]: "
                    f"{discount_text(cfg)}{validity}"
                )
        print()


if __name__ == "__main__":
    main()
