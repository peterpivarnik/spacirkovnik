#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/website/.env"

# ── Kontrola .env ────────────────────────────────────────────────────────────
if [[ ! -f "$ENV_FILE" ]]; then
    echo "❌  Chýba súbor website/.env"
    echo "    Vytvor ho podľa website/.env.example"
    exit 1
fi

# shellcheck source=/dev/null
source "$ENV_FILE"

for var in FTP_HOST FTP_USER FTP_PASS FTP_REMOTE_DIR; do
    if [[ -z "${!var:-}" ]]; then
        echo "❌  Chýba premenná $var v website/.env"
        exit 1
    fi
done

# ── Kontrola lftp ────────────────────────────────────────────────────────────
if ! command -v lftp &>/dev/null; then
    echo "❌  lftp nie je nainštalovaný"
    echo "    Nainštaluj ho: sudo apt install lftp"
    exit 1
fi

# ── Build ─────────────────────────────────────────────────────────────────────
echo ""
echo "🔨  Buildovanie stránky..."
cd "$SCRIPT_DIR/website"
npm run build
echo "✅  Build hotový"

# ── Upload ────────────────────────────────────────────────────────────────────
echo ""
echo "📤  Nahrávanie na $FTP_HOST/$FTP_REMOTE_DIR ..."
echo "    (nahrávajú sa len zmenené súbory)"
echo ""

lftp <<EOF
set ftp:ssl-force true
set ftp:ssl-protect-data true
set ssl:verify-certificate false
set net:timeout 30
set net:max-retries 3
open -u "$FTP_USER","$FTP_PASS" ftp://$FTP_HOST
mirror --reverse --delete --verbose --exclude='\.DS_Store$' "$SCRIPT_DIR/website/dist/" "$FTP_REMOTE_DIR/"
bye
EOF

echo ""
echo "🎉  Hotovo! Stránka je dostupná na https://spacirkovnik.sk"
