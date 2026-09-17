#include <iostream>
#include <jni.h>
#include "mai_onsyn_renderer_cpu4dkt_JNIRasterizer.h"
import Renderer;
import Types;

JNIEXPORT jfloatArray JNICALL Java_mai_1onsyn_renderer_cpu4dkt_JNIRasterizer_process
  (JNIEnv* env, jclass, jfloatArray inputArray, jint triangleCount) {

    auto* data = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(inputArray, nullptr));

    if (data == nullptr) {
        return jfloatArray();
    }

    Int32 resultLen;
    Float* resultBuffer = Renderer::process(data, triangleCount, resultLen);

    env->ReleasePrimitiveArrayCritical(inputArray, data, JNI_ABORT);

    jfloatArray result = env->NewFloatArray(resultLen);
    if (result == nullptr) return jfloatArray();
    env->SetFloatArrayRegion(result, 0, resultLen, resultBuffer);
    delete[] resultBuffer;
    return result;
}
