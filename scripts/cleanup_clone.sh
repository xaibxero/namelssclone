#!/system/bin/sh
# cleanup_clone.sh
# Terminates clone processes, unmounts temporary storage, and resets state.
# Usage: cleanup_clone.sh <package_name> <clone_id>

PACKAGE="$1"
CLONE_ID="$2"

if [ -z "$PACKAGE" ] || [ -z "$CLONE_ID" ]; then
    echo "Usage: $0 <package_name> <clone_id>"
    exit 1
fi

BASE_DIR="/data/adb/namelssclone"
CLONE_DIR="$BASE_DIR/${PACKAGE}_${CLONE_ID}"

echo "[+] Terminating app processes for $PACKAGE..."
am force-stop "$PACKAGE"

# Remove active clone indicator
rm -f "$BASE_DIR/active_${PACKAGE}.id"

# Check if clone directory is mounted as tmpfs and unmount if so
if mount | grep -q "$CLONE_DIR"; then
    echo "[+] Unmounting ephemeral tmpfs at $CLONE_DIR..."
    umount -l "$CLONE_DIR"
fi

echo "[+] Cleanup complete for $PACKAGE (Clone #$CLONE_ID)."
