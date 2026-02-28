package com.canc.iia

/**
 * Modern JNI Bridge for stable-diffusion.cpp
 * This object handles the low-level communication with the C++ AI engine.
 */
object NativeLib {

    init {
        // This must match the name in your CMakeLists.txt (project("iia_native"))
        System.loadLibrary("iia_native")
    }

    /**
     * Initializes the Stable Diffusion context.
     * @param modelPath Full path to the .gguf model file.
     * @param vaePath Full path to the .gguf VAE file (use TAESD for low RAM).
     * @param threads Number of CPU threads (Recommended: 4 for Moto G Play).
     * @return True if model loaded successfully.
     */
    external fun loadModel(
        modelPath: String, 
        vaePath: String, 
        threads: Int
    ): Boolean

    /**
     * Core Text-to-Image / Image-to-Image generation function.
     * @param prompt User's positive prompt.
     * @param negativePrompt Features to avoid.
     * @param cfgScale Guidance scale (Standard: 7.0).
     * @param width Image width (Recommended: 320 or 384 for low RAM).
     * @param height Image height.
     * @param steps Number of sampling steps (Standard: 15-20).
     * @param seed Random seed (-1 for random).
     * @return A ByteArray containing raw RGB pixels, or null if generation failed.
     */
    external fun txt2img(
        prompt: String,
        negativePrompt: String,
        cfgScale: Float,
        width: Int,
        height: Int,
        steps: Int,
        seed: Long
    ): ByteArray?

    /**
     * Frees the C++ memory used by the model. 
     * Call this when the app is closing or switching models to avoid OOM crashes.
     */
    external fun freeModel()
}
