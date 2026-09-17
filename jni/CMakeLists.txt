cmake_minimum_required(VERSION 3.20)
project(jfx34d_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_SCAN_FOR_MODULES ON)

file(GLOB_RECURSE SOURCES "src/*.cpp" "src/*.hpp" "src/*.h")
file(GLOB_RECURSE MODULES "src/*.ixx")

add_library(${PROJECT_NAME} SHARED ${SOURCES})
target_sources(${PROJECT_NAME} PUBLIC
        FILE_SET CXX_MODULES
        BASE_DIRS src
        FILES ${MODULES}
)

find_package(JNI QUIET)
if (JNI_INCLUDE_DIRS)
    target_include_directories(${PROJECT_NAME} PRIVATE ${JNI_INCLUDE_DIRS})
else()
    if (NOT JAVA_HOME)
        set(JAVA_HOME $ENV{JAVA_HOME})
    endif()
    if (JAVA_HOME)
        target_include_directories(${PROJECT_NAME} PRIVATE "${JAVA_HOME}/include")
        if (WIN32)
            target_include_directories(${PROJECT_NAME} PRIVATE "${JAVA_HOME}/include/win32")
        elseif (APPLE)
            target_include_directories(${PROJECT_NAME} PRIVATE "${JAVA_HOME}/include/darwin")
        else()
            target_include_directories(${PROJECT_NAME} PRIVATE "${JAVA_HOME}/include/linux")
        endif()
    else()
        message(WARNING "找不到 JNI 头文件")
    endif()
endif()

if (MSVC)
    target_compile_options(${PROJECT_NAME} PRIVATE /W4 /fp:fast /arch:AVX2 /utf-8)
else()
    target_compile_options(${PROJECT_NAME} PRIVATE -Wall -Wextra -ffast-math -mavx2 -mfma -fmodules-ts)
endif()


add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy
        $<TARGET_FILE:${PROJECT_NAME}>
        ${CMAKE_SOURCE_DIR}
        COMMENT "Copying generated dll to project root directory"
)