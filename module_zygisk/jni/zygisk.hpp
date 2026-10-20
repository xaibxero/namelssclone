#pragma once
#include <jni.h>
#include <stdint.h>

namespace zygisk {

struct AppSpecializeArgs {
    jint &uid;
    jint &gid;
    jintArray &gids;
    jint &runtime_flags;
    jint &mount_external;
    jstring &se_info;
    jstring &nice_name;
    jstring &instruction_set;
    jstring &app_data_dir;

    jboolean &is_child_zygote;
    jboolean &is_top_app;
    jobjectArray &pkg_data_info_list;
    jobjectArray &whitelisted_data_info_list;
    jboolean &mount_data_dirs;
    jboolean &mount_storage_dirs;
};

struct ServerSpecializeArgs {
    jint &uid;
    jint &gid;
    jintArray &gids;
    jint &runtime_flags;
    jlong &permitted_capabilities;
    jlong &effective_capabilities;
};

class Api;

class ModuleBase {
public:
    virtual void onLoad(Api *api, JNIEnv *env) {}
    virtual void preAppSpecialize(AppSpecializeArgs *args) {}
    virtual void postAppSpecialize(const AppSpecializeArgs *args) {}
    virtual void preServerSpecialize(ServerSpecializeArgs *args) {}
    virtual void postServerSpecialize(const ServerSpecializeArgs *args) {}
};

class Api {
public:
    virtual bool registerModule(ModuleBase *module) = 0;
    virtual void hookJniNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *methods, int numMethods) = 0;
    virtual void pltHookRegister(const char *regex, const char *symbol, void *newFunc, void **oldFunc) = 0;
    virtual bool pltHookCommit() = 0;
    virtual int connectCompanion() = 0;
    virtual void setOption(int opt) = 0;
    virtual int getModuleDir() = 0;
    virtual int getFlags() = 0;
};

} // namespace zygisk

#define REGISTER_ZYGISK_MODULE(clazz)     extern "C" [[gnu::visibility("default")]]     void zygisk_module_entry(zygisk::Api *api, JNIEnv *env) {         static clazz module;         api->registerModule(&module);         module.onLoad(api, env);     }

#define REGISTER_ZYGISK_COMPANION(func)     extern "C" [[gnu::visibility("default")]]     void zygisk_companion_entry(int client) {         func(client);     }
