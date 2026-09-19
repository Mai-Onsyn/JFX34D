module;
#include <memory>
export module VertexProcessor4D;
import Types;
import Vectors;
import Matrix;
import Data4D;

export namespace VertexProcessor4D {
    List<Tetrahedron3D> process(
        const Tetrahedron4D& tetrahedron,
        const Matrix5x5& model,
        const Matrix5x5& view,
        const Matrix5x5& projection,
        const Matrix4x4& viewPort
    ) {
        const Matrix5x5 mvp = model * view * projection;
        List<Tetrahedron3D> results;

        List<ClipVertex4D> clipVertices;

        Tetrahedron3D res{{
            Vertex3D{
                static_cast<Vector3D>(tetrahedron.vertices[0].pos),
                tetrahedron.vertices[0].color,
                static_cast<Vector3D>(tetrahedron.vertices[0].normal)
            },
            Vertex3D{
                static_cast<Vector3D>(tetrahedron.vertices[1].pos),
                tetrahedron.vertices[1].color,
                static_cast<Vector3D>(tetrahedron.vertices[1].normal)
            },
            Vertex3D{
                static_cast<Vector3D>(tetrahedron.vertices[2].pos),
                tetrahedron.vertices[2].color,
                static_cast<Vector3D>(tetrahedron.vertices[2].normal)
            },
            Vertex3D{
                static_cast<Vector3D>(tetrahedron.vertices[3].pos),
                tetrahedron.vertices[3].color,
                static_cast<Vector3D>(tetrahedron.vertices[3].normal)
            }
        }};
        return {res};
    }
}

export namespace VertexProcessor4D_ {
    List<Tetrahedron3D> process(const Tetrahedron4D& tetrahedron,
                                const Matrix5x5& model,
                                const Matrix5x5& view,
                                const Matrix5x5& projection,
                                const Matrix4x4& viewPort) {
        // 组合矩阵：MVP = projection * view * model
        const Matrix5x5 viewProj = projection * view;
        const Matrix5x5 mvp = viewProj * model;

        // 法向量变换矩阵：取 model 的 4x4 线性部分，求逆转置
        // 注意：4D 法向量是 Vector4D，因此使用 4x4 部分，忽略平移和第五维
        const Matrix4x4 modelLinear = static_cast<Matrix4x4>(model); // 假设存在转换，取左上角 4x4
        const Matrix4x4 normalMatrix = modelLinear.inverse().transpose();

        Tetrahedron3D resultTet;

        for (int i = 0; i < 4; ++i) {
            const Vertex4D& v4 = tetrahedron.vertices[i];

            // 1. 位置变换：4D 点扩展为 5D 齐次坐标 (x, y, z, w, 1)
            Vector5D pos5{ v4.pos.x, v4.pos.y, v4.pos.z, v4.pos.w, 1.0f };
            Vector5D clipPos = mvp * pos5;

            // 2. 透视除法：除以第五分量（齐次 w），得到 4D NDC
            //    然后取前三个分量作为 3D NDC
            Float invW = 1.0f / clipPos.v; // 假设 Vector5D 的第五分量为 .v
            Vector3D ndc{
                clipPos.x * invW,
                clipPos.y * invW,
                clipPos.z * invW
            };

            // 3. 视口变换：将 3D NDC 映射到屏幕空间
            Vector4D screen4 = viewPort * Vector4D{ ndc.x, ndc.y, ndc.z, 1.0f };
            Vector3D screenPos{ screen4.x, screen4.y, screen4.z };

            // 4. 法向量变换：用模型矩阵的逆转置变换 4D 法向量
            Vector4D normal4 = normalMatrix * v4.normal;
            // 投影到 3D 空间：取前三个分量并归一化
            Vector3D normal3 = Vector3D{ normal4.x, normal4.y, normal4.z }.normalize();

            // 5. 颜色直接复制
            resultTet.vertices[i] = Vertex3D{
                screenPos,
                v4.color,
                normal3
            };
        }

        return { resultTet };
    }
}