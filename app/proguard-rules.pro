# 1. Protect the JNI Bridge
# This ensures R8 doesn't rename our NativeLib or its methods.
-keep class com.canc.iia.NativeLib {
    native <methods>;
}

# 2. Protect the Background Service
# We need to keep the service and its lifecycle methods intact.
-keep class com.canc.iia.InferenceService { *; }

# 3. Protect Jetpack Compose & Material 3
# Modern Compose 2026 relies on specific metadata that shouldn't be stripped.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keep class androidx.compose.** { *; }

# 4. Prevent stripping of the C++ Library loader
# This keeps the "System.loadLibrary" call from being optimized away.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 5. Native Code Support
# Tells ProGuard to leave the compiled .so files alone.
-keep class com.google.android.gms.internal.* { *; }
-dontwarn com.google.android.gms.**
