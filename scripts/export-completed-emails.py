#!/usr/bin/env python3
"""
Vypíše emaily hráčov, ktorí dokončili danú hru.

Príprava:
  1) Firebase Console -> Project settings -> Service accounts -> Generate new private key
     a ulož ako scripts/serviceAccountKey.json (NEcommituj ho!)
  2) python3 -m pip install -r scripts/requirements.txt

Použitie (z koreňa projektu):
  python3 scripts/export-completed-emails.py rybar-z-drazdiaka
  python3 scripts/export-completed-emails.py rybar-z-drazdiaka > emails.txt
"""
import os
import sys

# aby fungoval import `lib.*` aj keď sa spúšťa z koreňa projektu
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib.completed_emails import get_completed_emails

game_id = sys.argv[1] if len(sys.argv) > 1 else "rybar-z-drazdiaka"

emails = get_completed_emails(game_id)

# count na stderr, emaily na stdout (dajú sa presmerovať do súboru)
print(f"\nDokončili \"{game_id}\": {len(emails)} hráčov\n", file=sys.stderr)
for e in emails:
    print(e)
