#include <iostream>
#include <jni.h>
#include "mai_onsyn_renderer_cpu4dkt_JNIRasterizer.h"
import Renderer;
import Types;
import Matrix;

JNIEXPORT jfloatArray JNICALL Java_mai_1onsyn_renderer_cpu4dkt_JNIRasterizer_process
  (JNIEnv* env, jclass, jfloatArray inputArray, const jint count, jfloatArray model, jfloatArray view, jfloatArray projection, jfloatArray viewPort) {

    auto* data = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(inputArray, nullptr));
    auto* _model = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(model, nullptr));
    auto* _view = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(view, nullptr));
    auto* _projection = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(projection, nullptr));
    auto* _viewPort = static_cast<jfloat*>(env->GetPrimitiveArrayCritical(viewPort, nullptr));

    if (data == nullptr) {
        return jfloatArray();
    }

    std::cout << env->GetArrayLength(inputArray) << std::endl;

    Int32 resultLen;
    Float* resultBuffer = Renderer::process(data, count, resultLen, {_model}, {_view}, {_projection}, {_viewPort});

    env->ReleasePrimitiveArrayCritical(inputArray, data, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(model, _model, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(view, _view, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(projection, _projection, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(viewPort, _viewPort, JNI_ABORT);

    jfloatArray result = env->NewFloatArray(resultLen);
    if (result == nullptr) return jfloatArray();
    env->SetFloatArrayRegion(result, 0, resultLen, resultBuffer);
    delete[] resultBuffer;
    std::cout << resultLen << std::endl;
    return result;
}