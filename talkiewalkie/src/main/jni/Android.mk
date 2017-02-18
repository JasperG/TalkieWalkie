LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#include the .mk files
include $(LOCAL_PATH)/celt_sources.mk
include $(LOCAL_PATH)/silk_sources.mk
include $(LOCAL_PATH)/opus_sources.mk

LOCAL_MODULE        := talkwalkopus

# path to locally installed Android NDK
NDK_PATH := C:/Users/Jasper/AppData/Local/Android/sdk/ndk-bundle/sources/cxx-stl

#fixed point sources
SILK_SOURCES += $(SILK_SOURCES_FIXED)

#ARM build
CELT_SOURCES += $(CELT_SOURCES_ARM)
SILK_SOURCES += $(SILK_SOURCES_ARM)
LOCAL_SRC_FILES     := \
$(CELT_SOURCES) $(SILK_SOURCES) $(OPUS_SOURCES) $(LOCAL_PATH)/jniopus.cpp

LOCAL_LDLIBS        := -lm -llog

LOCAL_C_INCLUDES    := \
$(LOCAL_PATH)/jniopus.h \
$(LOCAL_PATH)/include \
$(LOCAL_PATH)/silk \
$(LOCAL_PATH)/silk/fixed \
$(LOCAL_PATH)/celt \
$(NDK_PATH)/gnu-libstdc++/4.9/libs/$(TARGET_ARCH)/include \
$(NDK_PATH)/gnu-libstdc++/4.9/libs/armeabi/include \
$(NDK_PATH)/gnu-libstdc++/4.9/include

#$(error LOCAL_C_INCLUDES is $(LOCAL_C_INCLUDES))

LOCAL_CFLAGS        := -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS        += -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT=1 -DDISABLE_FLOAT_API -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF  -DAVOID_TABLES

# gnu99 not allowed for C++
# LOCAL_CFLAGS        +=  -w -std=gnu99 -O3 -fno-strict-aliasing -fprefetch-loop-arrays  -fno-math-errno
LOCAL_CFLAGS        +=  -w -O3 -fno-strict-aliasing -fprefetch-loop-arrays  -fno-math-errno

# ADDED -fexceptions
LOCAL_CPPFLAGS      := -DBSD=1
LOCAL_CPPFLAGS      += -ffast-math -O3 -funroll-loops
LOCAL_CPP_FEATURES  := exceptions

include $(BUILD_SHARED_LIBRARY)