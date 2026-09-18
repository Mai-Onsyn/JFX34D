module;
#include <memory>
export module VertexProcessor4D;
import Types;
import Vectors;
import Matrix;
import Data4D;

export namespace VertexProcessor4D {
    List<Tetrahedron3D> process(const Tetrahedron4D& tetrahedrons, const Matrix5x5& mvp, const Matrix4x4& viewPort) {
        Tetrahedron3D res;
        std::memcpy(&res + 7, &tetrahedrons + 9, 3 * sizeof(Float)), std::memcpy(&res + 7 + 3, &tetrahedrons + 9 + 4, 4 * sizeof(Float));
        return {res};
    }
}
