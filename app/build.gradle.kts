plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.canc.iia"
    compileSdk = 35 // Android 15 (Standard for 2026)

    defaultConfig {
        applicationId = "com.canc.iia"
        minSdk = 26    // Android 8.0 (Required for modern hardware features)
        targetSdk = 35 
        versionCode = 1
        versionName = "1.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // C++ / NDK Configuration
        externalNativeBuild {
            cmake {
                // Optimization for Moto G Play (ARM64)
                cppFlags("-std=c++17 -O3 -fexceptions -frtti")
                arguments("-DSD_OPENCL=ON", "-DSD_BUILD_EXAMPLES=OFF")
                abiFilters("arm64-v8a") 
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug") // Temporary for development
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // Link to our Stable Diffusion C++ Logic
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core Android & Lifecycle
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7") // Required for our background AI service

    // Jetpack Compose (UI)
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.0")

    // Image Loading
    implementation("io.coil-kt:coil-compose:2.7.0")
}
