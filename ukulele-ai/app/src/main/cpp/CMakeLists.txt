cmake_minimum_required(VERSION 3.22.1)
project(ukulele_analysis)

add_library(ukulele_analysis SHARED ukulele_analysis.cpp)

set(THIRD_PARTY_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../third_party")
set(ESSENTIA_LIB "${THIRD_PARTY_DIR}/essentia/prebuilt/active/${ANDROID_ABI}/libessentia.a")
set(ESSENTIA_INCLUDE "${THIRD_PARTY_DIR}/essentia/prebuilt/active/include")

if (NOT EXISTS "${ESSENTIA_LIB}")
    message(FATAL_ERROR "Essentia prebuilt library missing for ${ANDROID_ABI}. Run third_party/essentia/fetch_prebuilt.sh")
endif()

add_library(essentia_static STATIC IMPORTED)
set_target_properties(essentia_static PROPERTIES IMPORTED_LOCATION "${ESSENTIA_LIB}")
target_include_directories(ukulele_analysis PRIVATE "${ESSENTIA_INCLUDE}")
target_compile_options(ukulele_analysis PRIVATE -Wno-undefined-var-template)
target_link_options(ukulele_analysis PRIVATE "-Wl,-z,common-page-size=16384" "-Wl,-z,max-page-size=16384")
target_link_libraries(ukulele_analysis essentia_static log atomic)
