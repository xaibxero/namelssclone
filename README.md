# NamelessClone

**NamelessClone** is an open-source, root-native Android multi-instance isolation and anti-fingerprinting engine designed to run unlimited app clones inside User Profile 0 without breaking original APK signatures.

---

## Key Features

* **Zero-Signature-Break Cloning**: Uses Linux mount namespaces (`unshare(CLONE_NEWNS)`) to swap `/data/data/<package>` directories per instance. The original APK installed from Google Play remains completely untouched and genuine.
* **Modern Zygisk Support (Zygisk Next & ReZygisk)**: Built natively against the official `zygisk.hpp` API. Fully compatible with standalone Zygisk implementations like **Zygisk Next** and **ReZygisk** across **KernelSU**, **APatch**, and **Magisk**.
* **14-Vector Anti-Fingerprint Spoofing**: Intercepts Android ID, Google Advertising ID (GAID), Widevine DRM ID, `Build.*` properties, CPU info, telephony IDs, MAC/BSSID, ARP tables, installed packages, storage tokens, sensor noise, battery telemetry, and GPU renderer strings.
* **Multi-Layer Root Control**:
  * **Zygote Level (Zygisk Next / ReZygisk)**: Injects hooks into target app processes before any application or tracking SDK code initializes via `preAppSpecialize`.
  * **VFS Level (Mount Namespaces)**: Isolates app storage and masks `/proc/cpuinfo`, `/proc/net/arp`, and `/sys/class/net`.
  * **Kernel Network Level (iptables / nftables)**: Routes individual clones through independent SOCKS5 or WireGuard proxies simultaneously without occupying Android's single system VPN slot.
* **Ephemeral Storage Mode**: Mounts a RAM disk (`tmpfs`) as app storage so all session data, caches, and cookies vanish permanently upon closing.

---

## Repository Structure

```
namelssclone/
├── README.md                  # Project overview & quickstart
├── .gitignore                 # Standard Android/C++ ignores
├── docs/
│   └── ARCHITECTURE.md        # Technical specification & system design
├── scripts/
│   ├── mount_clone.sh         # Shell script to configure mount namespaces
│   ├── route_proxy.sh         # iptables per-UID proxy routing rules
│   └── cleanup_clone.sh       # Cleans mounts and kills orphaned clone processes
├── profiles/
│   ├── schema.json            # JSON schema for device profiles
│   ├── samsung_s24_ultra.json # Pre-configured Samsung Galaxy S24 Ultra profile
│   └── google_pixel_8_pro.json# Pre-configured Google Pixel 8 Pro profile
├── module_zygisk/             # Native Zygisk C++ module (Zygisk Next & ReZygisk)
│   ├── CMakeLists.txt         # Build configuration for Android NDK
│   ├── module.prop            # Module metadata for Magisk / KernelSU / APatch
│   └── jni/
│       ├── zygisk.hpp         # Official Zygisk API header (v2/v3/v4)
│       ├── main.cpp           # Zygote fork hook & companion communication
│       └── prop_spoofer.cpp   # Native __system_property_get PLT hook
└── app/                       # Android Manager App (Kotlin)
    ├── build.gradle.kts       # App dependencies (libsu, coroutines, gson)
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/namelssclone/
            ├── MainActivity.kt
            ├── core/
            │   ├── RootExecutor.kt # Root shell abstraction via libsu
            │   └── CloneManager.kt # Provisions clone storage & profile configs
            └── model/
                └── DeviceProfile.kt# Data model for hardware & identifier specs
```

---

## Root Environment Compatibility

NamelessClone supports all modern root environments and Zygisk providers:

| Root Manager | Recommended Zygisk Implementation | Status |
| :--- | :--- | :--- |
| **KernelSU** | Zygisk Next / ReZygisk module | Fully Supported (Recommended for stealth) |
| **APatch** | Zygisk Next / ReZygisk module | Fully Supported |
| **Magisk v26+** | Built-in Zygisk or Zygisk Next | Fully Supported |

---

## Quickstart & Installation

### 1. Build the Zygisk Module
```bash
cd module_zygisk
mkdir build && cd build
cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake -DANDROID_ABI=arm64-v8a ..
make

# Packaging into flashable zip:
mkdir -p ../out/zygisk
cp libnamelssclone.so ../out/zygisk/arm64-v8a.so
cp ../module.prop ../out/
cd ../out && zip -r ../namelssclone_zygisk.zip ./*
```
Flash `namelssclone_zygisk.zip` via the KernelSU, APatch, or Magisk app, then reboot.

### 2. Build the Manager App
```bash
./gradlew :app:assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

---

## License

GPL-3.0 License.
