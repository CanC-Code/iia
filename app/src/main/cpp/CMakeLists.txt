cmake_minimum_required(VERSION 3.22.1)

# Define the project name
project("iia_native")

# 1. Set C++ Standard (Stable Diffusion requires C++11 or higher, we use 17 for 2026)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 2. Add the Stable Diffusion Submodule
# This tells CMake to look into the submodule folder and run ITS build logic.
# We disable the examples and CLI to keep your APK small.
set(SD_BUILD_EXAMPLES OFF CACHE BOOL "Do not build sd-cli" FORCE)
set(SD_BUILD_TESTS OFF CACHE BOOL "Do not build tests" FORCE)

add_subdirectory(stable-diffusion.cpp)

# 3. Create your JNI Bridge Library
# This is where your custom Android-to-C++ code (native-lib.cpp) lives.
add_library(iia_native SHARED
        native-lib.cpp)

# 4. Include the NDK's logging library so we can see "Logcat" messages from C++
find_library(log-lib log)

# 5. Link everything together
# We link your library to 'sd', which is the target produced by the submodule.
target_link_libraries(iia_native
        sd
        ${log-lib})

# 6. Optimization for Android
# This ensures that unused code is stripped out, making your APK as small as possible.
target_compile_options(iia_native PRIVATE -fdata-sections -ffunction-sections)
target_link_options(iia_native PRIVATE "-Wl,--gc-sections")
