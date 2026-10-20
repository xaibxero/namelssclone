# NamelessClone Architecture & System Specification

A comprehensive technical blueprint for building a zero-signature-break app cloning and anti-fingerprinting engine on rooted Android.

---

## 1. Executive Summary & Design Principles

The objective is to achieve **unlimited app cloning inside User Profile 0** while assigning each instance a unique, isolated device identity and sandboxed environment without modifying or re-signing target APKs.

### Core Tenets
1. **Zero APK Tampering**: Target APKs remain completely unmodified and retain their genuine Play Store signatures, preventing detection by signature checks and app integrity routines.
2. **True Native Execution**: Apps run directly on the host Android runtime with native performance rather than inside slow user-space virtual machines.
3. **Multi-Layer Interception**: Intercept tracking vectors across Java framework APIs, native libc filesystem calls, and kernel-level network packets.
4. **Isolated Data Silos**: Each clone instance operates inside its own private Linux mount namespace, preventing shared storage access and cross-instance data contamination.

---

## 2. GrapheneOS Multi-User vs. Profile 0 Root Spoofing

| Layer | GrapheneOS Multi-User Profiles | Profile 0 Root Virtualization Engine |
| :--- | :--- | :--- |
| **Storage Encryption** | Unique Credential Encrypted (CE) keys per profile derived from separate passcodes; keys are purged from RAM when stopped. | Shared User 0 master CE key; all app storage resides in the same unlocked encryption domain. |
| **Process Sandboxing** | Linux UID offset (`userId * 100000 + appId`) enforced strictly by the Linux kernel and SELinux. | Apps share User 0 UID space (`10000–19999`); separation is enforced via custom mount namespaces and dynamic hooks. |
| **Identifier Access** | Identifiers (Android ID, IMEI, MAC) are blocked, scoped per profile, or randomized by OS policy. | Identifiers are dynamically intercepted and spoofed in runtime memory by Zygote and libc hooks. |
| **System Integrity** | Locked bootloader with full Android Verified Boot (AVB) and hardware attestation. | Unlocked bootloader; root binaries bypass OS restrictions but fail hardware-backed remote attestation. |
| **Leak Surface** | Zero cross-profile IPC leaks without explicit mutual consent (no shared clipboard, sensors, or storage). | Controlled by custom hooks; native C/C++ code querying raw hardware ioctls must be intercepted at the kernel level. |

---

## 3. System Architecture & Control Layers

```
                    ┌──────────────────────────────────────────────┐
                    │               ROOT PRIVILEGES                │
                    └──────┬───────────────┬───────────────┬───────┘
                           │               │               │
            ┌──────────────▼────┐   ┌──────▼──────┐   ┌────▼──────────────┐
            │   Zygote Level    │   │  VFS / Mount│   │   Kernel Net      │
            │ (Memory & Hooks)  │   │ Namespaces  │   │(iptables/nftables)│
            └──────────────┬────┘   └──────┬──────┘   └────┬──────────────┘
                           │               │               │
    ┌──────────────────────┼───────────────┼───────────────┼──────────────────────┐
    ▼                      ▼               ▼               ▼                      ▼
[Java Framework APIs]  [Native .so]   [/proc & /sys]  [Private Storage]     [Network Stack]
- Android ID           - JNI Calls    - cpuinfo       - /data/data isolated - SOCKS5 / WireGuard
- Build Properties     - libc open()  - net/arp       - /sdcard isolated    - LAN scan blocking
- Telephony & Carrier  - ioctl()      - soc0 serial   - Media isolated      - DNS leak blocking
```

### Layer 1: Zygote Fork Injection (Process Birth)
* Android launches every app by forking the `Zygote` process.
* Utilizing a root injection framework (Zygisk, KernelSU, or an LSPosed core), hook `forkAndSpecialize`.
* Code executes inside the newly created process **before** any application logic, third-party tracking SDKs, or native `.so` libraries initialize.

### Layer 2: Linux Mount Namespaces (Filesystem Virtualization)
* When a clone process initializes, invoke `unshare(CLONE_NEWNS)` to isolate its filesystem namespace.
* Bind-mount a dedicated folder over `/data/data/<package_name>` to redirect private databases and caches.
* Bind-mount virtual text files over `/proc/cpuinfo`, `/sys/class/net/`, and `/proc/net/arp` to prevent native C/C++ libraries from reading hardware specs.

### Layer 3: Kernel Packet Filtering (Netfilter / iptables)
* Route traffic independently using Linux UID tracking (`-m owner --uid-owner <UID>`).
* Bind each clone to a dedicated SOCKS5 proxy or WireGuard interface without consuming Android's single system VPN slot.
* Drop broadcast/multicast packets to block local network enumeration.

---

## 4. Zero-Signature-Break Cloning Mechanism

### The Problem with APK Renaming
Standard cloning apps decompile the APK, rename the package (`com.example.app` $\to$ `com.example.app.clone1`), and re-sign it with a custom debug certificate. Modern social and banking apps inspect their own package signatures and reject modified certificates immediately.

### The Root Mount-Redirection Solution
1. **Single Genuine Installation**: The target application is installed once from the official Google Play Store or APK mirror. Its on-disk signature remains 100% valid.
2. **Storage Separation**:
   * Storage is provisioned under `/data/adb/clones/<package_name>_<clone_id>/`.
   * When Clone #1 launches, its private mount namespace maps `/data/adb/clones/<package_name>_1/` to `/data/data/<package_name>`.
   * When Clone #2 launches, its private mount namespace maps `/data/adb/clones/<package_name>_2/` to `/data/data/<package_name>`.
3. **Execution**: Both instances execute the identical, untouched binary with full signature validity while operating on completely isolated data directories.

---

## 5. Anti-Fingerprint & Hardware Spoofing Matrix

| Fingerprint Category | How Apps Harvest It | Root Interception Method |
| :--- | :--- | :--- |
| **Android ID** | `Settings.Secure.getString(..., ANDROID_ID)` | Hook `ContentResolver` to return a unique 64-bit hex per clone. |
| **Google Advertising ID (GAID)** | Google Play Services Advertising API | Intercept `AdvertisingIdClient` to return random UUIDs or a zeroed-out string. |
| **DRM / Widevine ID** | `MediaDrm.getPropertyByteArray("deviceUniqueId")` | Hook DRM framework to return a unique hash per clone. |
| **Hardware & Board Specs** | `android.os.Build.*` fields (`MODEL`, `FINGERPRINT`, `BOARD`, `SERIAL`) | Override Java `Build` class fields and native `__system_property_get()` in libc. |
| **Native CPU & SoC Details** | Reading `/proc/cpuinfo` and `/sys/devices/soc0/*` via C/C++ `fopen()` | Bind-mount custom virtual text files inside the clone's mount namespace. |
| **Telephony & SIM IDs** | `TelephonyManager` (`getDeviceId`, `getImei`, `getSimSerialNumber`) | Hook telephony service IPC to return matched dummy IMEI, IMSI, and ICCID. |
| **Network & Router MACs** | `NetworkInterface.getHardwareAddress()` and Wi-Fi scan results | Intercept Wi-Fi manager and libc socket calls; randomize local MAC and connected BSSID. |
| **Local Network Discovery** | Scanning `/proc/net/arp` to find other devices on your home Wi-Fi | Bind-mount an empty file over `/proc/net/arp` and block LAN subnets (`192.168.x.x`) via iptables. |
| **Installed Apps Probing** | `PackageManager.getInstalledPackages()` to profile user habits | Hook `PackageManager` to return an empty app list (hiding other clones and root tools). |
| **Shared Storage Tracking** | Reading/writing tracking tokens in `/sdcard` or public photos | Isolate `/sdcard` via mount namespaces; assign each clone a private virtual SD card. |
| **Sensor Profiling** | Measuring micro-imperfections in accelerometer and gyroscope hardware | Add random noise or normalize sensor event data in `SensorManager`. |
| **Battery & Charging Telemetry** | Registering `ACTION_BATTERY_CHANGED` to track discharge curves | Intercept battery broadcast intents; report static 85% battery, non-charging state. |
| **Screen & Display Metrics** | `DisplayMetrics` (DPI, resolution, refresh rate, aspect ratio) | Hook window manager callbacks to return matched display profiles for the spoofed model. |
| **OpenGL / Vulkan Renderer** | Querying GPU vendor and renderer strings via EGL | Hook `glGetString(GL_RENDERER)` and `glGetString(GL_VENDOR)` to match the spoofed SoC. |

---

## 6. Complete Privacy & Sandboxing Feature Set

### 1. Network & Location Privacy
* **Per-Clone Proxy Routing**: Route each instance through independent SOCKS5, HTTP, or WireGuard tunnels concurrently.
* **Network Kill Switch**: Cut network access if the assigned proxy disconnects to prevent real IP exposure.
* **LAN Cloaking**: Block local network scanning (`192.168.0.0/16`, `10.0.0.0/8`) to prevent device discovery.
* **Virtual GPS**: Inject custom coordinates, altitude, and movement vectors per clone without enabling developer mock location settings.

### 2. Storage Isolation
* **Ephemeral Storage (RAM Disk)**: Mount a `tmpfs` as the app's data directory. All session tokens, cookies, and local data are purged upon process exit.
* **Virtual Media Sandbox**: Redirect external storage queries to an isolated directory containing only assigned media assets.
* **Snapshot & Backup**: Export and import complete app states (logins, session data, configs) to encrypted archives.

### 3. Telemetry & Peripheral Defenses
* **Clipboard Blocker**: Clear or randomize clipboard contents whenever a clone gains window focus.
* **Mock Contact & Call Logs**: Supply synthetic address books and call histories when permissions are mandatory.
* **Sensor Noise Generation**: Perturb accelerometer and gyroscope streams to disrupt device fingerprinting.