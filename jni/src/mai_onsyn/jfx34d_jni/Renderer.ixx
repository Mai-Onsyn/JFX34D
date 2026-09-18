module;
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

        const Matrix5x5 mvp = projection * view * model;

        List<List<Tetrahedron3D>> transformed{};
        Int32 resultCount = 0;

        Tetrahedron4D* src = new Tetrahedron4D[count];
        memcpy(src, input, sizeof(Tetrahedron4D) * count);
        for (Int32 i = 0; i < count; i++) {
            List<Tetrahedron3D> tet3Ds = VertexProcessor4D::process(src[i], mvp, viewPort);
            resultCount += tet3Ds.size();
        }
        delete[] src;
        // for (int i = 0; i < count; i++) {
        //     Int32 offset = i * 36;
        //     Tetrahedron4D tet{{
        //             extractVertex(offset, input),
        //             extractVertex(offset, input),
        //             extractVertex(offset, input),
        //             extractVertex(offset, input)
        //         }};
        //     List<Tetrahedron3D> tet3D = VertexProcessor4D::process(tet, mvp, viewPort);
        //     transformed.push_back(move(tet3D));
        // }

        resultLen = resultCount * 28;
        auto* result = new Float[resultLen];
        Int32 writeOffset = 0;
        for (const auto& tet3DGroup : transformed) {
            for (const auto& j : tet3DGroup) {
                memcpy(result + writeOffset, &j, sizeof(Tetrahedron3D));
                writeOffset += 28;
            }
        }

        // for (int i = 0; i < resultCount * 4; ++i)
        //     memcpy(result + i * 7, input + i * 9, 3 * sizeof(Float)), std::memcpy(result + i * 7 + 3, input + i * 9 + 4, 4 * sizeof(Float));

        return result;
    }
}