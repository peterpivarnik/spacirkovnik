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

# ── Build ─────────────────────────────────────────────────────────────────────
echo ""
echo "🔨  Buildovanie stránky..."
cd "$SCRIPT_DIR/website"
npm run build
echo "✅  Build hotový"

# ── Upload cez Python FTP ─────────────────────────────────────────────────────
echo ""
echo "📤  Nahrávanie na $FTP_HOST/$FTP_REMOTE_DIR ..."
echo ""

python3 - "$FTP_HOST" "$FTP_USER" "$FTP_PASS" "$FTP_REMOTE_DIR" "$SCRIPT_DIR/website/dist" <<'PYEOF'
import ftplib, os, sys

host, user, passwd, remote_dir, local_dir = sys.argv[1:]

def ensure_dir(ftp, path):
    try:
        ftp.mkd(path)
    except ftplib.error_perm:
        pass  # už existuje

def upload_tree(ftp, local_root, remote_root):
    ensure_dir(ftp, remote_root)
    for entry in sorted(os.listdir(local_root)):
        if entry.startswith('.DS_Store'):
            continue
        local_path  = os.path.join(local_root, entry)
        remote_path = remote_root + '/' + entry
        if os.path.isdir(local_path):
            upload_tree(ftp, local_path, remote_path)
        else:
            print(f"  ↑  {remote_path}")
            with open(local_path, 'rb') as f:
                ftp.storbinary(f'STOR {remote_path}', f)

try:
    ftp = ftplib.FTP_TLS()
    ftp.connect(host, 21)
    ftp.auth()
    ftp.login(user, passwd)
    ftp.prot_p()
    ftp.set_pasv(True)
    print(f"✅  Pripojený na {host} (FTPS)")
except Exception:
    # fallback na plain FTP
    ftp = ftplib.FTP()
    ftp.connect(host, 21)
    ftp.login(user, passwd)
    ftp.set_pasv(True)
    print(f"✅  Pripojený na {host} (FTP)")

upload_tree(ftp, local_dir, remote_dir)
ftp.quit()
print("\n✅  Upload dokončený")
PYEOF

echo ""
echo "🎉  Hotovo! Stránka je dostupná na https://spacirkovnik.sk"
