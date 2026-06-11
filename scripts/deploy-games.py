#!/usr/bin/env python3
"""
deploy-games.py — Aktualizuje verzie zmenených hier a nahrá ich na Firebase RTDB.

Použitie (spúšťať z koreňa projektu):
  python3 scripts/deploy-games.py                 # automaticky zistí zmenené hry (git diff HEAD)
  python3 scripts/deploy-games.py --all           # nahrá všetky hry + katalóg
  python3 scripts/deploy-games.py --catalog-only  # nahrá len katalóg bez zmeny verzií
  python3 scripts/deploy-games.py lesnicka-palica # konkrétna hra (môžeš zadať viac naraz)
"""

import json
import subprocess
import sys
import os

SCRIPT_DIR  = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)
GAMES_DIR   = os.path.join(PROJECT_DIR, "game-data", "games")
CATALOG     = os.path.join(PROJECT_DIR, "game-data", "catalog", "games-info.json")

# --- Firebase config ---
with open(os.path.join(SCRIPT_DIR, "http-client.env.json")) as f:
    DB_URL = json.load(f)["dev"]["dbUrl"]
with open(os.path.join(SCRIPT_DIR, "http-client.private.env.json")) as f:
    DB_SECRET = json.load(f)["dev"]["dbSecret"]

# --- Zisti ktoré hry treba aktualizovať ---
args = sys.argv[1:]

if "--catalog-only" in args:
    print("Nahrávam len katalóg na Firebase...")
    def firebase_put(path, file_path):
        url = f"{DB_URL}/{path}?auth={DB_SECRET}"
        result = subprocess.run(
            ["curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
             "-X", "PUT", "-H", "Content-Type: application/json",
             "--data-binary", f"@{file_path}", url],
            capture_output=True, text=True
        )
        return result.stdout.strip()
    status = firebase_put("catalog/games-info.json", CATALOG)
    icon = "✅" if status == "200" else "❌"
    print(f"  {icon} katalóg: HTTP {status}")
    sys.exit(0)

if "--all" in args:
    game_ids = sorted([f[:-5] for f in os.listdir(GAMES_DIR) if f.endswith(".json")])
elif args:
    game_ids = [a for a in args if not a.startswith("--")]
else:
    # Automaticky: súbory zmenené oproti poslednému commitu
    result = subprocess.run(
        ["git", "diff", "HEAD", "--name-only", "--", "game-data/games/"],
        capture_output=True, text=True, cwd=PROJECT_DIR
    )
    changed = result.stdout.strip().split("\n")
    game_ids = [
        os.path.basename(f)[:-5]
        for f in changed
        if f.endswith(".json")
    ]

game_ids = [g for g in game_ids if g]  # odfiltruj prázdne reťazce

if not game_ids:
    print("Žiadne zmeny v hrách oproti poslednému commitu.")
    print("Tip: ak chceš nahrať všetko, použi: python3 scripts/deploy-games.py --all")
    sys.exit(0)

print(f"Hry na deploy: {', '.join(game_ids)}\n")

# --- Načítaj katalóg ---
with open(CATALOG) as f:
    catalog = json.load(f)

# --- Aktualizuj verzie v hrách aj katalógu ---
deployed = []
for game_id in game_ids:
    game_path = os.path.join(GAMES_DIR, f"{game_id}.json")
    if not os.path.exists(game_path):
        print(f"⚠️  {game_id}: súbor nenájdený, preskakujem")
        continue

    with open(game_path) as f:
        game = json.load(f)

    old_v = game.get("version", 0)
    new_v = old_v + 1
    game["version"] = new_v

    with open(game_path, "w") as f:
        json.dump(game, f, ensure_ascii=False, indent=2)
        f.write("\n")

    # Aktualizuj verziu aj v katalógu
    for item in catalog:
        if item["id"] == game_id:
            item["version"] = new_v
            break

    print(f"  {game_id}: verzia {old_v} → {new_v}")
    deployed.append(game_id)

# --- Ulož katalóg ---
with open(CATALOG, "w") as f:
    json.dump(catalog, f, ensure_ascii=False, indent=2)
    f.write("\n")

if not deployed:
    print("\nNič na nahratie.")
    sys.exit(0)

# --- Nahraj na Firebase ---
print("\nNahrávam na Firebase...")

def firebase_put(path, file_path):
    url = f"{DB_URL}/{path}?auth={DB_SECRET}"
    result = subprocess.run(
        [
            "curl", "-s", "-o", "/dev/null", "-w", "%{http_code}",
            "-X", "PUT",
            "-H", "Content-Type: application/json",
            "--data-binary", f"@{file_path}",
            url,
        ],
        capture_output=True, text=True
    )
    return result.stdout.strip()

status = firebase_put("catalog/games-info.json", CATALOG)
icon = "✅" if status == "200" else "❌"
print(f"  {icon} katalóg: HTTP {status}")

for game_id in deployed:
    game_path = os.path.join(GAMES_DIR, f"{game_id}.json")
    status = firebase_put(f"games/{game_id}.json", game_path)
    icon = "✅" if status == "200" else "❌"
    print(f"  {icon} {game_id}: HTTP {status}")

print("\nHotovo!")
