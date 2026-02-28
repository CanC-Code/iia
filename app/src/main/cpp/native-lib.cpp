#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "stable-diffusion.h"

#define LOG_TAG "IIA_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global pointers for the session
sd_ctx_t* g_sd_ctx = nullptr;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_canc_iia_NativeLib_loadModel(JNIEnv *env, jobject thiz, 
                                      jstring model_path, 
                                      jstring vae_path, 
                                      jint threads) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    const char *v_path = env->GetStringUTFChars(vae_path, nullptr);

    // If a model is already loaded, free it first
    if (g_sd_ctx != nullptr) {
        free_sd_ctx(g_sd_ctx);
    }

    // Initialize the context
    // We use RNG_STD_DEFAULT and leave special flags empty for maximum compatibility
    g_sd_ctx = new_sd_ctx(path, v_path, "", "", "", "", false, false, false, threads, SD_TYPE_F32, STD_DEFAULT_RNG, REALTIME_STRATEGY, false, false, false);

    env->ReleaseStringUTFChars(model_path, path);
    env->ReleaseStringUTFChars(vae_path, v_path);

    if (g_sd_ctx == nullptr) {
        LOGE("Failed to load model!");
        return JNI_FALSE;
    }

    LOGI("Model loaded successfully with %d threads", threads);
    return JNI_TRUE;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_canc_iia_NativeLib_txt2img(JNIEnv *env, jobject thiz,
                                    jstring prompt,
                                    jstring negative_prompt,
                                    jfloat cfg_scale,
                                    jint width,
                                    jint height,
                                    jint steps,
                                    jlong seed) {
    if (g_sd_ctx == nullptr) return nullptr;

    const char *p = env->GetStringUTFChars(prompt, nullptr);
    const char *np = env->GetStringUTFChars(negative_prompt, nullptr);

    // Perform Inference
    // stable-diffusion.cpp returns an sd_image_t containing raw RGB data
    sd_image_t result = txt2img(g_sd_ctx, p, np, cfg_scale, width, height, EULER_A, steps, seed, 1, nullptr, 0.0f, 0.0f, false, "");

    env->ReleaseStringUTFChars(prompt, p);
    env->ReleaseStringUTFChars(negative_prompt, np);

    if (result.data == nullptr) return nullptr;

    // Convert raw RGB to a Java ByteArray so Kotlin can turn it into a Bitmap
    int size = result.width * result.height * result.channel;
    jbyteArray byteArray = env->NewByteArray(size);
    env->SetByteArrayRegion(byteArray, 0, size, (jbyte *)result.data);

    // Free the image data allocated by C++
    free(result.data);

    return byteArray;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_canc_iia_NativeLib_freeModel(JNIEnv *env, jobject thiz) {
    if (g_sd_ctx != nullptr) {
        free_sd_ctx(g_sd_ctx);
        g_sd_ctx = nullptr;
        LOGI("Model memory freed.");
    }
}
