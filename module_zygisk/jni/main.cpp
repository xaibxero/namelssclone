#include <jni.h>
#include <android/log.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/mount.h>
#include <sched.h>
#include <string>
#include <fstream>
#include "zygisk.hpp"

#define LOG_TAG "NamelessCloneZygisk"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern int hooked_system_property_get(const char* name, char* value);
extern int (*orig_system_property_get)(const char* name, char* value);

class NamelessCloneModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        this->api = api;
        this->env = env;
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        if (!args->nice_name) return;

        const char *process_name = env->GetStringUTFChars(args->nice_name, nullptr);
        if (!process_name) return;

        std::string pkg(process_name);
        env->ReleaseStringUTFChars(args->nice_name, process_name);

        std::string clone_id = check_active_clone(pkg);
        if (clone_id.empty()) {
            return;
        }

        LOGI("[+] Zygisk Next / ReZygisk detected active clone #%s for: %s", clone_id.c_str(), pkg.c_str());

        // 1. Isolate Mount Namespace
        if (unshare(CLONE_NEWNS) != 0) {
            LOGE("[-] Failed to unshare mount namespace: %d", errno);
            return;
        }

        // 2. Mark mounts private
        mount(nullptr, "/", nullptr, MS_REC | MS_PRIVATE, nullptr);

        // 3. Bind-mount private clone storage directory
        std::string clone_storage = "/data/adb/namelssclone/" + pkg + "_" + clone_id;
        std::string target_data = "/data/data/" + pkg;

        if (mount(clone_storage.c_str(), target_data.c_str(), nullptr, MS_BIND, nullptr) == 0) {
            LOGI("[+] Namespace mount successful: %s -> %s", clone_storage.c_str(), target_data.c_str());
        } else {
            LOGE("[-] Failed to bind mount clone storage: %d", errno);
        }

        // 4. Hook __system_property_get via Zygisk PLT hook engine
        api->pltHookRegister(".*\\.so$", "__system_property_get", 
                             reinterpret_cast<void*>(hooked_system_property_get), 
                             reinterpret_cast<void**>(&orig_system_property_get));
        if (api->pltHookCommit()) {
            LOGI("[+] Zygisk PLT hook committed for __system_property_get");
        } else {
            LOGE("[-] Failed to commit Zygisk PLT hooks");
        }
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override {
    }

private:
    zygisk::Api *api = nullptr;
    JNIEnv *env = nullptr;

    std::string check_active_clone(const std::string &package_name) {
        std::string path = "/data/adb/namelssclone/active_" + package_name + ".id";
        std::ifstream file(path);
        if (!file.is_open()) return "";
        std::string id;
        file >> id;
        return id;
    }
};

static void companion_handler(int client_socket) {
    close(client_socket);
}

REGISTER_ZYGISK_MODULE(NamelessCloneModule)
REGISTER_ZYGISK_COMPANION(companion_handler)