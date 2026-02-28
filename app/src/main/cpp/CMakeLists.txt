cmake_minimum_required(VERSION 3.22.1)

# Define the project name
project("iia_native")

# 1. Set C++ Standard (Stable Diffusion requires C++11 or higher, we use 17 for 2026)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 2. Add the Stable Diffusion Submodule
# We disable the examples and CLI to keep your APK small.
set(SD_BUILD_EXAMPLES OFF CACHE BOOL "Do not build sd-cli" FORCE)
set(SD_BUILD_TESTS OFF CACHE BOOL "Do not build tests" FORCE)

add_subdirectory(stable-diffusion.cpp)

# 3. Create your JNI Bridge Library
add_library(iia_native SHARED
        native-lib.cpp)

# 4. Include Headers
# This tells the compiler where the .h files for stable-diffusion are located.
target_include_directories(iia_native PRIVATE 
        stable-diffusion.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/stable-diffusion.cpp)

# 5. Include the NDK's logging library
find_library(log-lib log)

# 6. Link everything together
# 'sd' is the target defined inside the stable-diffusion.cpp/CMakeLists.txt
target_link_libraries(iia_native
        sd
        ${log-lib})

# 7. Optimization for Android (Moto G Play performance)
target_compile_options(iia_native PRIVATE -fdata-sections -ffunction-sections -O3)
target_link_options(iia_native PRIVATE "-Wl,--gc-sections")
