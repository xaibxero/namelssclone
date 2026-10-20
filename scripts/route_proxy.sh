#!/system/bin/sh
# route_proxy.sh
# Routes clone network traffic through a local SOCKS5/Transparent proxy using iptables
# Usage: route_proxy.sh <enable|disable> <app_uid> <local_proxy_port>

ACTION="$1"
UID_TARGET="$2"
PROXY_PORT="$3"

if [ -z "$ACTION" ] || [ -z "$UID_TARGET" ]; then
    echo "Usage: $0 <enable|disable> <app_uid> [proxy_port]"
    exit 1
fi

CHAIN_NAME="NAMELESS_PROXY"

if [ "$ACTION" = "enable" ]; then
    if [ -z "$PROXY_PORT" ]; then
        echo "[-] Error: proxy_port required for enable."
        exit 1
    fi

    echo "[+] Enabling iptables proxy redirection for UID $UID_TARGET to port $PROXY_PORT..."

    # Ensure custom chain exists
    iptables -t nat -N "$CHAIN_NAME" 2>/dev/null

    # Flush any previous rules in the chain
    iptables -t nat -F "$CHAIN_NAME"

    # Bypass loopback and local LAN discovery
    iptables -t nat -A "$CHAIN_NAME" -d 127.0.0.0/8 -j RETURN
    iptables -t nat -A "$CHAIN_NAME" -d 192.168.0.0/16 -j RETURN
    iptables -t nat -A "$CHAIN_NAME" -d 10.0.0.0/8 -j RETURN

    # Redirect all other TCP traffic to local transparent proxy
    iptables -t nat -A "$CHAIN_NAME" -p tcp -j REDIRECT --to-ports "$PROXY_PORT"

    # Hook chain into OUTPUT for the target UID
    iptables -t nat -D OUTPUT -m owner --uid-owner "$UID_TARGET" -j "$CHAIN_NAME" 2>/dev/null
    iptables -t nat -A OUTPUT -m owner --uid-owner "$UID_TARGET" -j "$CHAIN_NAME"

    echo "[+] Proxy routing active for UID $UID_TARGET"

elif [ "$ACTION" = "disable" ]; then
    echo "[-] Disabling proxy redirection for UID $UID_TARGET..."
    iptables -t nat -D OUTPUT -m owner --uid-owner "$UID_TARGET" -j "$CHAIN_NAME" 2>/dev/null
    iptables -t nat -F "$CHAIN_NAME" 2>/dev/null
    iptables -t nat -X "$CHAIN_NAME" 2>/dev/null
    echo "[+] Proxy routing cleared."
fi
