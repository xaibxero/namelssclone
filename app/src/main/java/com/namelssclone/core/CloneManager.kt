package com.namelssclone.core

import android.content.Context
import com.google.gson.Gson
import com.namelssclone.model.*
import java.security.SecureRandom
import java.util.UUID

class CloneManager(private val context: Context) {

    private val gson = Gson()
    private val random = SecureRandom()

    fun generateRandomProfile(packageName: String, cloneId: Int, baseModel: String = "SM-S928B"): DeviceProfile {
        val randomHex = ByteArray(8).apply { random.nextBytes(this) }.joinToString("") { "%02x".format(it) }
        val randomMac = ByteArray(6).apply { random.nextBytes(this) }
        randomMac[0] = (randomMac[0].toInt() and 0xfe or 0x02).toByte() // Locally administered unicast MAC
        val macStr = randomMac.joinToString(":") { "%02x".format(it) }

        return DeviceProfile(
            cloneId = cloneId,
            packageName = packageName,
            profileName = "Clone #$cloneId ($baseModel)",
            androidId = randomHex,
            advertisingId = UUID.randomUUID().toString(),
            widevineDeviceId = UUID.randomUUID().toString().replace("-", ""),
            build = BuildProperties(
                manufacturer = "samsung",
                brand = "samsung",
                model = baseModel,
                device = "e3q",
                product = "e3qxxx",
                board = "pineapple",
                hardware = "qcom",
                fingerprint = "samsung/e3qxxx/e3q:14/UP1A.231005.007/S928BXXU1AXB5:user/release-keys",
                bootloader = "S928BXXU1AXB5",
                radioVersion = "S928BXXU1AXB5"
            ),
            telephony = TelephonyProperties(
                imei = "358" + (100000000000L + (random.nextDouble() * 899999999999L).toLong()),
                imsi = "310410" + (100000000L + (random.nextDouble() * 899999999L).toLong()),
                simSerial = "89014103" + (100000000000L + (random.nextDouble() * 899999999999L).toLong()),
                networkOperator = "310410",
                simOperator = "310410",
                carrierName = "AT&T"
            ),
            network = NetworkProperties(
                macAddress = macStr,
                bssid = "00:11:22:33:44:55",
                ssid = "Private-WiFi"
            ),
            proxy = ProxyConfig(
                enabled = false,
                type = "socks5",
                host = "127.0.0.1",
                port = 10808
            )
        )
    }

    suspend fun launchClone(profile: DeviceProfile, ephemeral: Boolean = false): Boolean {
        // 1. Prepare storage directory and active clone flag
        val storageReady = RootExecutor.prepareClone(profile.packageName, profile.cloneId, ephemeral)
        if (!storageReady) return false

        // 2. Write profile JSON to /data/adb/namelssclone/profile_<pkg>_<id>.json
        val json = gson.toJson(profile)
        val profilePath = "/data/adb/namelssclone/profile_${profile.packageName}_${profile.cloneId}.json"
        RootExecutor.runCommand("cat << 'EOF' > $profilePath\n$json\nEOF")
        RootExecutor.runCommand("chmod 644 $profilePath")

        // 3. Launch the target app
        val launchCmd = "monkey -p ${profile.packageName} -c android.intent.category.LAUNCHER 1"
        RootExecutor.runCommand(launchCmd)
        return true
    }
}
