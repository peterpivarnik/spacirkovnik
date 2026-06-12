#!/usr/bin/env bash
set -euo pipefail

# Spustí Astro dev server pre spacirkovnik.sk (lokálny náhľad webu/blogu).
# Beží na http://localhost:4321 ; ukončíš cez Ctrl+C.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
WEB_DIR="$PROJECT_DIR/website"

cd "$WEB_DIR"

# Pri prvom spustení doinštaluj závislosti
if [[ ! -d node_modules ]]; then
    echo "📦  Inštalujem závislosti (prvé spustenie)..."
    npm install
fi

echo ""
echo "🚀  Spúšťam Astro dev server — http://localhost:4321  (Ctrl+C ukončí)"
echo ""
npm run dev
