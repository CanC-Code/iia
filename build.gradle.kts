// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android Application Plugin - AGP 9.0.1 (Latest Stable January 2026)
    id("com.android.application") version "9.0.1" apply false
    
    // Kotlin Android Plugin - Version 2.3.10 (Latest Stable February 2026)
    id("org.jetbrains.kotlin.android") version "2.3.10" apply false
    
    // Compose Compiler is now built into Kotlin 2.0+, 
    // but we declare the plugin here for modern configuration
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
