#include <jni.h>
#include <string>
#include "search.h"

#define LOG_TAG "cube-solver"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_artem52_MainActivity_solveCube(JNIEnv *env, jobject thiz, jstring facelets,
                                            jstring pattern) {
    const char *facelets_cstr = env->GetStringUTFChars(facelets, nullptr);
    const char *pattern_cstr = pattern ? env->GetStringUTFChars(pattern, nullptr) : nullptr;

    char patternized[64];
    const char *input = facelets_cstr;

    if (pattern_cstr) {
        patternize(
                const_cast<char*>(facelets_cstr),
                const_cast<char*>(pattern_cstr),
                patternized
        );
        input = patternized;
    }

    jclass cls = env->GetObjectClass(thiz);
    jmethodID getCacheDir = env->GetMethodID(cls, "getCacheDir", "()Ljava/io/File;");
    jobject cacheDir = env->CallObjectMethod(thiz, getCacheDir);
    jmethodID getPath = env->GetMethodID(env->GetObjectClass(cacheDir), "getPath", "()Ljava/lang/String;");
    jstring cachePath = (jstring)env->CallObjectMethod(cacheDir, getPath);
    const char *cachePathStr = env->GetStringUTFChars(cachePath, nullptr);

    char *sol = solution(
            const_cast<char*>(input),
            24,
            1000,
            0,
            cachePathStr  // Используем правильный путь
    );

    env->ReleaseStringUTFChars(cachePath, cachePathStr);

    // Освобождаем ресурсы
    env->ReleaseStringUTFChars(facelets, facelets_cstr);
    if (pattern_cstr) {
        env->ReleaseStringUTFChars(pattern, pattern_cstr);
    }

    if (!sol) {
        return env->NewStringUTF("Ошибка: Не удалось решить");
    }

    jstring result = env->NewStringUTF(sol);
    free(sol);
    return result;
}