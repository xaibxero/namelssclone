#!/system/bin/sh
# mount_clone.sh
# Prepares isolated storage and filesystem mounts for a specific clone instance.
# Usage: mount_clone.sh <package_name> <clone_id> [ephemeral]

PACKAGE="$1"
CLONE_ID="$2"
EPHEMERAL="$3"

if [ -z "$PACKAGE" ] || [ -z "$CLONE_ID" ]; then
    echo "Usage: $0 <package_name> <clone_id> [ephemeral (0|1)]"
    exit 1
fi

BASE_DIR="/data/adb/namelssclone"
CLONE_DIR="$BASE_DIR/${PACKAGE}_${CLONE_ID}"
REAL_DATA_DIR="/data/data/${PACKAGE}"

# Retrieve target app UID and GID
APP_UID=$(dumpsys package "$PACKAGE" | grep userId= | head -n 1 | awk -F'=' '{print $2}' | tr -d ' ')
if [ -z "$APP_UID" ]; then
    echo "[-] Error: Package $PACKAGE not found on device."
    exit 1
fi

echo "[+] Preparing clone storage for $PACKAGE (Clone #$CLONE_ID, UID: $APP_UID)..."

mkdir -p "$BASE_DIR"

if [ "$EPHEMERAL" = "1" ]; then
    echo "[+] Initializing ephemeral RAM disk (tmpfs)..."
    mkdir -p "$CLONE_DIR"
    mount -t tmpfs -o size=256M,mode=0700 tmpfs "$CLONE_DIR"
else
    mkdir -p "$CLONE_DIR"
fi

# Ensure correct file ownership and SELinux contexts
chown -R "$APP_UID:$APP_UID" "$CLONE_DIR"
chcon -R u:object_r:app_data_file:s0 "$CLONE_DIR"

# Mark clone active for Zygisk hook detection
echo "$CLONE_ID" > "$BASE_DIR/active_${PACKAGE}.id"

echo "[+] Storage ready at $CLONE_DIR"
exit 0
