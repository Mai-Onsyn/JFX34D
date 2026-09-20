module;
#include <iostream>
#include <memory>
export module Renderer;
import Types;
import Matrix;
import VertexProcessor4D;
import Data4D;
import Vectors;

inline Float intToFloat(const UInt32 i) {
    Float f;
    memcpy(&f, &i, sizeof(Float));
    return f;
}

inline UInt32 floatToInt(const Float f) {
    UInt32 i;
    memcpy(&i, &f, sizeof(UInt32));
    return i;
}

export namespace Renderer {
    Float* process(const Float* input, const Int32 count, Int32& resultLen, const Matrix5x5& model, const Matrix5x5& view, const Matrix5x5& projection, const Matrix4x4& viewPort) {

        // const Matrix5x5 mvp = projection * view * model;

        List<List<Tetrahedron3D>> transformed{};
        Int32 resultCount = 0;

        Tetrahedron4D* src = new Tetrahedron4D[count];
        memcpy(src, input, sizeof(Tetrahedron4D) * count);
        for (Int32 i = 0; i < count; i++) {
            List<Tetrahedron3D> tet3Ds = VertexProcessor4D_::process(src[i], model, view, projection, viewPort);
            resultCount += tet3Ds.size();
            transformed.push_back(move(tet3Ds));
        }
        delete[] src;

        resultLen = resultCount * 28;
        auto* result = new Float[resultLen];
        Int32 writeOffset = 0;
        for (const auto& tet3DGroup : transformed) {
            for (const auto& j : tet3DGroup) {
                memcpy(result + writeOffset, &j, sizeof(Tetrahedron3D));
                writeOffset += 28;
            }
        }

        return result;
    }
}
        // for (int i = 0; i < resultCount * 4; ++i)
        //     memcpy(result + i * 7, input + i * 9, 3 * sizeof(Float)), std::memcpy(result + i * 7 + 3, input + i * 9 + 4, 4 * sizeof(Float));
