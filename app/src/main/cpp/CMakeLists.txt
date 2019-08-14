cmake_minimum_required(VERSION 3.4.1)
# opencv精简参考
#https://blog.csdn.net/suwk1009/article/details/79746724

set(distribution_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/)
set(opencv_include ${CMAKE_SOURCE_DIR}/include/opencv)
#set(ffmpeg_include ${CMAKE_SOURCE_DIR}/include/ffmpeg)
#set(ffmpeg_include ${CMAKE_SOURCE_DIR}/include/fastcv)

#添加动态库ffmpeg
#add_library(
#        ffmpeg
#        SHARED
#        IMPORTED)
#set_target_properties(
#        ffmpeg
#        PROPERTIES IMPORTED_LOCATION
#        ${distribution_DIR}/${ANDROID_ABI}/libffmpeg.so)
#添加动态库opencv
add_library(
        opencv
        SHARED
        IMPORTED)
set_target_properties(
        opencv
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/${ANDROID_ABI}/libopencv_java4.so)

#add_library(
#        fastcv
#        STATIC
#        IMPORTED)
#set_target_properties(
#        fastcv
#        PROPERTIES IMPORTED_LOCATION
#        ${distribution_DIR}/${ANDROID_ABI}/libfastcv.a)

set(SRC_DIR sources_root)

FILE(GLOB_RECURSE SRC_DIR "*.cpp" "*.h")

add_library(
        native-lib
        SHARED
        ${SRC_DIR})


target_include_directories(native-lib PRIVATE ${opencv_include} #[[${ffmpeg_include}]])

target_link_libraries(native-lib
        android
        jnigraphics
        opencv
#        fastcv
        #        ffmpeg
        log
        )