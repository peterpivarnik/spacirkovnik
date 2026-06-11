#!/usr/bin/env python3
"""
Náhodne vyžrebuje výhercov spomedzi hráčov, ktorí dokončili danú hru.

Použitie (z koreňa projektu):
  python3 scripts/pick-winners.py rybar-z-drazdiaka          # 3 výhercovia (default)
  python3 scripts/pick-winners.py rybar-z-drazdiaka 5        # 5 výhercov
  python3 scripts/pick-winners.py rybar-z-drazdiaka > winners.txt
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from lib.completed_emails import get_completed_emails

game_id = sys.argv[1] if len(sys.argv) > 1 else "rybar-z-drazdiaka"
count = int(sys.argv[2]) if len(sys.argv) > 2 else 3

emails = get_completed_emails(game_id)

print(f"\nDokončili \"{game_id}\": {len(emails)} hráčov", file=sys.stderr)
if not emails:
    print("Žiadni hráči — nie je z čoho žrebovať.", file=sys.stderr)
    sys.exit(0)

n = min(count, len(emails))
if n < count:
    print(f"Pozor: žiadam {count} výhercov, ale hráčov je len {len(emails)}.", file=sys.stderr)

winners = random.sample(emails, n)  # bez opakovania

# info na stderr, výhercovia na stdout (dajú sa presmerovať do súboru)
print(f"\n🎉 Vyžrebovaní výhercovia ({n}):\n", file=sys.stderr)
for e in winners:
    print(e)
