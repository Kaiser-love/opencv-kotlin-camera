cmake_minimum_required(VERSION 3.4.1)

add_library( native-lib
             SHARED
             src/main/cpp/native-lib.cpp )

find_library( log-lib
              log )

#set(distribution_DIR ../../../../)
#add_library(ffmpeg
#            SHARED
#            IMPORTED)
#set_target_properties(  ffmpeg
#                        PROPERTIES IMPORTED_LOCATION
#                        ../../../../src/main/cpp/libffmpeg.so)
#include_directories(src/main/cpp/ffmpeg/include)

target_link_libraries( native-lib
 #                      ffmpeg
                       ${log-lib})