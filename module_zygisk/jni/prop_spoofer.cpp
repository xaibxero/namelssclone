#include <string>
#include <cstring>
#include <unordered_map>
#include <android/log.h>
#include <sys/system_properties.h>

#define LOG_TAG "NamelessCloneProp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// In-memory spoofed property map
static std::unordered_map<std::string, std::string> spoofed_props = {
    {"ro.product.model", "SM-S928B"},
    {"ro.product.brand", "samsung"},
    {"ro.product.manufacturer", "samsung"},
    {"ro.product.device", "e3q"},
    {"ro.build.fingerprint", "samsung/e3qxxx/e3q:14/UP1A.231005.007/S928BXXU1AXB5:user/release-keys"}
};

// Pointer to original __system_property_get (external linkage for main.cpp)
int (*orig_system_property_get)(const char* name, char* value) = nullptr;

// Hooked replacement function
int hooked_system_property_get(const char* name, char* value) {
    if (name && value) {
        auto it = spoofed_props.find(name);
        if (it != spoofed_props.end()) {
            strncpy(value, it->second.c_str(), PROP_VALUE_MAX - 1);
            value[PROP_VALUE_MAX - 1] = '\0';
            return strlen(value);
        }
    }
    if (orig_system_property_get) {
        return orig_system_property_get(name, value);
    }
    return 0;
}

void install_prop_hooks() {
    LOGI("[+] Native system property hook active for build identity protection.");
}
