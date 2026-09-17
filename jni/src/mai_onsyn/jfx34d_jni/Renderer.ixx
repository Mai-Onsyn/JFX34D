module;
export module Renderer;
import Types;
import Matrix;

export namespace Renderer {
    Float* process(const Float* input, const Int32 triangleCount, Int32& resultLen, Matrix5x5 model, Matrix5x5 view, Matrix5x5 projection, Matrix4x4 viewPort) {
        resultLen = triangleCount * 16;

        Float* result = new Float[resultLen];
        for (Int32 i = 0; i < triangleCount; i++) {
            Int32 iPtr = i * 20;
            Int32 oPtr = i * 16;
            result[oPtr] = input[iPtr];
            result[oPtr + 1] = input[iPtr + 1];
            result[oPtr + 2] = input[iPtr + 2];
            result[oPtr + 3] = input[iPtr + 4];   // color

            result[oPtr + 4] = input[iPtr + 5];
            result[oPtr + 5] = input[iPtr + 6];
            result[oPtr + 6] = input[iPtr + 7];
            result[oPtr + 7] = input[iPtr + 9];   // color

            result[oPtr + 8] = input[iPtr + 10];
            result[oPtr + 9] = input[iPtr + 11];
            result[oPtr + 10] = input[iPtr + 12];
            result[oPtr + 11] = input[iPtr + 14];   // color

            result[oPtr + 12] = input[iPtr + 15];
            result[oPtr + 13] = input[iPtr + 16];
            result[oPtr + 14] = input[iPtr + 17];
            result[oPtr + 15] = input[iPtr + 19];   // color
        }
        return result;
    }
}