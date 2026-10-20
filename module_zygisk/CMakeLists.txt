cmake_minimum_required(VERSION 3.22.1)
project(namelssclone_zygisk LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(jni)

add_library(namelssclone SHARED
    jni/main.cpp
    jni/prop_spoofer.cpp
)

target_link_libraries(namelssclone
    android
    log
)
