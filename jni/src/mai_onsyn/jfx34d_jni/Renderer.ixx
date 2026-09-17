module;
export module Renderer;
import Types;

export namespace Renderer {
    Float* process(const Float* input, const Int32 triangleCount, Int32& resultLen) {
        resultLen = triangleCount * 3;

        Float* result = new Float[resultLen];
        for (Int32 i = 0; i < resultLen; i++) {
            result[i] = input[i] * 16;
        }
        return result;
    }
}