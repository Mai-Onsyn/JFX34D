cmake_minimum_required(VERSION 3.20)
project(jfx34d_jni LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_SCAN_FOR_MODULES ON)

# CONFIGURE_DEPENDS: 新增/删除 .cpp/.ixx 后构建时会自动重新 configure，
# 避免新文件没被编进 dll（CLion 里不用手动 Reload CMake Project）
file(GLOB_RECURSE SOURCES CONFIGURE_DEPENDS "src/*.cpp" "src/*.hpp" "src/*.h")
file(GLOB_RECURSE MODULES CONFIGURE_DEPENDS "src/*.ixx")

add_library(${PROJECT_NAME} SHARED ${SOURCES})
target_sources(${PROJECT_NAME} PUBLIC
        FILE_SET CXX_MODULES
        BASE_DIRS src
        FILES ${MODULES}
)

# ---------------- JNI 头文件 ----------------
find_package(JNI QUIET)
if (JNI_INCLUDE_DIRS)
    target_include_directories(${PROJECT_NAME} PRIVATE ${JNI_INCLUDE_DIRS})
    message(STATUS "jfx34d_jni: JNI headers = ${JNI_INCLUDE_DIRS}")
else()
    if (NOT JAVA_HOME)
        set(JAVA_HOME "$ENV{JAVA_HOME}")
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
        message(STATUS "jfx34d_jni: JNI headers = ${JAVA_HOME}/include (from JAVA_HOME)")
    else()
        message(FATAL_ERROR
                "找不到 JNI 头文件。请设置 JAVA_HOME 环境变量后重新加载 CMake 项目，"
                "或在 CLion 的 Settings > Build, Execution, Deployment > CMake 里给 CMake options 加上 "
                "-DJAVA_HOME=<JDK 路径>。")
    endif()
endif()

if (MSVC)
    target_compile_options(${PROJECT_NAME} PRIVATE /W4 /fp:fast /arch:AVX2 /utf-8)
else()
    target_compile_options(${PROJECT_NAME} PRIVATE -Wall -Wextra -ffast-math -mavx2 -mfma -fmodules-ts)
endif()

# ---------------- 产物部署（打包用） ----------------
# Java 侧 (src/main/java/.../JNIRasterizer.java) 用的是
#     System.loadLibrary("./jni/jfx34d_jni")
# 也就是按「进程启动目录」去找 ./jni/jfx34d_jni.dll。
# 本文件所在目录正好就是仓库里的 jni/，所以默认把 dll 复制到这里：
# CLion 打开 jni/ 目录构建，产物就会落在 <仓库>/jni/jfx34d_jni.dll，
# Maven 打包时 (pom.xml) 会把它塞进 jar 并复制到 target/jni/ 下。
# 需要复制到别处（例如直接投放到某个运行目录）时，配置一次即可：
#     cmake -B build -DJFX34D_DLL_DEPLOY_DIR=<目标目录>
if (NOT DEFINED JFX34D_DLL_DEPLOY_DIR)
    set(JFX34D_DLL_DEPLOY_DIR "${CMAKE_CURRENT_LIST_DIR}")
endif()
message(STATUS "jfx34d_jni: dll deploy dir = ${JFX34D_DLL_DEPLOY_DIR}")

add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E make_directory "${JFX34D_DLL_DEPLOY_DIR}"
        COMMAND ${CMAKE_COMMAND} -E copy_if_different
                "$<TARGET_FILE:${PROJECT_NAME}>"
                "${JFX34D_DLL_DEPLOY_DIR}/$<TARGET_FILE_NAME:${PROJECT_NAME}>"
        COMMENT "Deploying $<TARGET_FILE_NAME:${PROJECT_NAME}> -> ${JFX34D_DLL_DEPLOY_DIR}"
        VERBATIM
)

# 分发提醒：Debug 版 dll 依赖不可再分发的 MSVC 调试运行库（VCRUNTIME140D/MSVCP140D）
if (WIN32 AND (NOT CMAKE_BUILD_TYPE OR CMAKE_BUILD_TYPE STREQUAL "Debug"))
    message(STATUS
            "jfx34d_jni: 当前是 Debug 构建；要随 Maven 打包分发给别人时，"
            "建议在 CLion 里切到 Release 配置重新构建一次（Release 只依赖可再分发的 VC++ 运行库）。")
endif()
