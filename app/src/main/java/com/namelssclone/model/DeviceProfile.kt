package com.namelssclone.model

import com.google.gson.annotations.SerializedName

data class DeviceProfile(
    @SerializedName("clone_id") val cloneId: Int,
    @SerializedName("package_name") val packageName: String,
    @SerializedName("profile_name") val profileName: String,
    @SerializedName("android_id") val androidId: String,
    @SerializedName("advertising_id") val advertisingId: String,
    @SerializedName("widevine_device_id") val widevineDeviceId: String,
    @SerializedName("build") val build: BuildProperties,
    @SerializedName("telephony") val telephony: TelephonyProperties,
    @SerializedName("network") val network: NetworkProperties,
    @SerializedName("proxy") val proxy: ProxyConfig
)

data class BuildProperties(
    @SerializedName("manufacturer") val manufacturer: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("model") val model: String,
    @SerializedName("device") val device: String,
    @SerializedName("product") val product: String,
    @SerializedName("board") val board: String,
    @SerializedName("hardware") val hardware: String,
    @SerializedName("fingerprint") val fingerprint: String,
    @SerializedName("bootloader") val bootloader: String,
    @SerializedName("radio_version") val radioVersion: String
)

data class TelephonyProperties(
    @SerializedName("imei") val imei: String,
    @SerializedName("imsi") val imsi: String,
    @SerializedName("sim_serial") val simSerial: String,
    @SerializedName("network_operator") val networkOperator: String,
    @SerializedName("sim_operator") val simOperator: String,
    @SerializedName("carrier_name") val carrierName: String
)

data class NetworkProperties(
    @SerializedName("mac_address") val macAddress: String,
    @SerializedName("bssid") val bssid: String,
    @SerializedName("ssid") val ssid: String
)

data class ProxyConfig(
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("type") val type: String,
    @SerializedName("host") val host: String,
    @SerializedName("port") val port: Int
)
