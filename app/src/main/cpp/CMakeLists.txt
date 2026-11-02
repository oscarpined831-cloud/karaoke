cmake_minimum_required(VERSION 3.22.1)

project("voxengine")

add_library(
    voxengine
    SHARED
    dsp_core.cpp
    audio_engine.cpp
)

find_library(
    log-lib
    log
)

find_library(
    aaudio-lib
    aaudio
)

target_link_libraries(
    voxengine
    ${log-lib}
    ${aaudio-lib}
)
