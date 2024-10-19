

#include <stdio.h>
#include "koinoor.h"

#include <android/bitmap.h>
#include <jni.h>

#include "refNR.h"
#include "openCLNR.h"

extern "C" jint
Java_com_gfms_koinoor_compute_service_runOpenCL(JNIEnv* env, jclass clazz