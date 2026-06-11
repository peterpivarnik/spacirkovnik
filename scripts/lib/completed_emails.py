"""
Spoločná logika: emaily hráčov, ktorí dokončili danú hru.
Spája `completions/{uid}` z RTDB s emailmi z Firebase Auth.
Používajú export-completed-emails.py aj pick-winners.py.
"""
import os
import sys
import warnings

# google-auth na Pythone 3.9 (EOL) vypisuje FutureWarning pri importe — len šum, stíšime ho.
warnings.filterwarnings("ignore", category=FutureWarning)

import firebase_admin
from firebase_admin import credentials, db, auth

DB_URL = "https://spacirkovnik-app-default-rtdb.europe-west1.firebasedatabase.app"
SCRIPT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # scripts/


def _ensure_app():
    try:
        firebase_admin.get_app()
    except ValueError:
        cred = credentials.Certificate(os.path.join(SCRIPT_DIR, "serviceAccountKey.json"))
        firebase_admin.initialize_app(cred, {"databaseURL": DB_URL})


def get_completed_emails(game_id):
    """Vráti zoradený zoznam unikátnych emailov hráčov, čo dokončili `game_id`."""
    _ensure_app()

    # 1) UID-y s dokončením danej hry
    completions = db.reference("completions").get() or {}
    uids = [
        uid for uid, games in completions.items()
        if isinstance(games, dict) and game_id in games
    ]

    # 2) UID -> email cez Firebase Auth (dávky po 100 = limit get_users)
    emails = set()
    for i in range(0, len(uids), 100):
        batch = [auth.UidIdentifier(uid) for uid in uids[i:i + 100]]
        result = auth.get_users(batch)
        for u in result.users:
            if u.email:
                emails.add(u.email)
        for nf in result.not_found:
            print(f"UID bez Auth účtu: {nf.uid}", file=sys.stderr)

    return sorted(emails)
