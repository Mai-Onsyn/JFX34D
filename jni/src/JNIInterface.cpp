#include <iostream>
#include <jni.h>
#include "mai_onsyn_renderer_cpu4dkt_JNIRasterizer.h"
import Renderer;
import Types;

JNIEXPORT jfloatArray JNICALL Java_mai_1onsyn_renderer_cpu4dkt_JNIRasterizer_process
  (JNIEnv* env, jclass, jfloatArray inputArray, const jint count) {

    auto* data = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(inputArray, nullptr));

    if (data == nullptr) {
        return jfloatArray();
    }

    std::cout << env->GetArrayLength(inputArray) << std::endl;

    Int32 resultLen;
    Float* resultBuffer = Renderer::process(data, count, resultLen);

    env->ReleasePrimitiveArrayCritical(inputArray, data, JNI_ABORT);

    jfloatArray result = env->NewFloatArray(resultLen);
    if (result == nullptr) return jfloatArray();
    env->SetFloatArrayRegion(result, 0, resultLen, resultBuffer);
    delete[] resultBuffer;
    std::cout << resultLen << std::endl;
    return result;
}
