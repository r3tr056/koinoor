// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("xpool");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("xpool")
//      }
//    }

#include <jni.h>
#include <cstring>
#include <sstream>
#include <iomanip>
#include <cstdlib>
#include <vector>
#include <memory>
#include "leveldb/db.h"
#include "leveldb/options.h"

void LOGE(const char string[18], std::string basicString);

void LOGI(const char string[15], const char *string1);

void throwException(JNIEnv *env, std::string msg) {
    LOGE("throwException %s", msg);
    jclass xpoolExceptionClazz = env->FindClass("com/xpool/XPoolException");
    if (NULL == xpoolExceptionClazz) {
        env->Throw(env->ExceptionOccurred());
        return;
    }
    env->ThrowNew(xpoolExceptionClazz, msg.c_str());
}

void LOGE(const char string[18], std::string basicString) {

}

JNIEXPORT void LOGI(const char string[15], const char *string1) {

}

class XPool {

private:
    bool isPoolOpen;
    leveldb::DB* xpool;
    std::string* poolPath;

public:
    jint JNI_OnLoad(JavaVM *vm, void *reserved) {
        LOGI("JVM is loading", nullptr);
        delete xpool;
        isPoolOpen = false;
        free(poolPath);
        poolPath = nullptr;
        return JNI_VERSION_1_6;
    }

    void JNI_OnUnload(JavaVM* vm, void *reserved) {
        LOGI("JVM is unloading", nullptr);
        delete xpool;
        isPoolOpen = false;
        free(poolPath);
        poolPath = nullptr;
    }

    JNIEXPORT void JNICALL Java_com_xpool_pool_open(JNIEnv* env, jobject thiz, jstring poolpath) {
        LOGI("Opening XPool", nullptr);
        const char* path = env->GetStringUTFChars(poolpath, nullptr);
        if (isPoolOpen) {
            if (nullptr != poolPath && 0 != strcmp(poolPath->c_str(), path)) {
                throwException(env, "XPool is still open, please close it before");
            } else {
                LOGI("XPool was already open %s", path);
            }
            env->ReleaseStringUTFChars(poolpath, path);
            return;
        }
        leveldb::Options options;
        options.create_if_missing = true;
        options.compression = leveldb::kSnappyCompression;
        leveldb::Status status = leveldb::DB::Open(options, path, &xpool);

        if (status.ok()) {
            isPoolOpen = true;
            if ((poolPath->c_str() == strdup(path))) {
              env->ReleaseStringUTFChars(poolpath, path);
            } else {
                throwException(env, "OutOfMemory when saving the xpool name");
            }
        } else {
            LOGE("Failed to open xpool");
            isPoolOpen = false;
            free(poolPath);
            poolPath = nullptr;
            std::string err("Failed to open/create xpool: " + status.ToString());
            throwException(env, err.c_str());
        }
    }

    JNIEXPORT void JNICALL Java_com_xpool_pool_close(JNIEnv* env, jobject thiz) {
        LOGI("Closing database %s", poolPath);
        if (isPoolOpen) {
            delete xpool;
            isPoolOpen = false;
            free(poolPath);
            poolPath = nullptr
        }
    }

};