#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
PRIVATE_ENV="$SCRIPT_DIR/http-client.private.env.json"

# ── FTP údaje z JSON env (tajný súbor, nie je v gite) ─────────────────────────
if [[ ! -f "$PRIVATE_ENV" ]]; then
    echo "❌  Chýba $PRIVATE_ENV"
    echo "    Doplň doň FTP údaje pod \"dev\": ftpHost, ftpUser, ftpPass, ftpRemoteDir."
    exit 1
fi

read_env() {  # $1 = kľúč v dev.* ; vráti hodnotu alebo prázdno
    python3 -c "import json; print(json.load(open('$PRIVATE_ENV')).get('dev',{}).get('$1',''))"
}

FTP_HOST="$(read_env ftpHost)"
FTP_USER="$(read_env ftpUser)"
FTP_PASS="$(read_env ftpPass)"
FTP_REMOTE_DIR="$(read_env ftpRemoteDir)"

for var in FTP_HOST FTP_USER FTP_PASS FTP_REMOTE_DIR; do
    if [[ -z "${!var:-}" ]]; then
        echo "❌  Chýba FTP údaj $var v $PRIVATE_ENV"
        exit 1
    fi
done

# ── Build ─────────────────────────────────────────────────────────────────────
echo ""
echo "🔨  Buildovanie stránky..."
cd "$PROJECT_DIR/website"
npm run build
echo "✅  Build hotový"

# ── Upload cez Python FTP ─────────────────────────────────────────────────────
echo ""
echo "📤  Nahrávanie na $FTP_HOST/$FTP_REMOTE_DIR ..."
echo ""

python3 - "$FTP_HOST" "$FTP_USER" "$FTP_PASS" "$FTP_REMOTE_DIR" "$PROJECT_DIR/website/dist" <<'PYEOF'
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
