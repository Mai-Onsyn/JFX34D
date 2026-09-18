module;
#include <memory>
export module VertexProcessor4D;
import Types;
import Vectors;
import Matrix;
import Data4D;

export namespace VertexProcessor4D {
    List<Tetrahedron3D> process(const Tetrahedron4D& tetrahedron, const Matrix5x5& model, const Matrix5x5& view, const Matrix5x5& projection, const Matrix4x4& viewPort) {
        // 示例构造
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


export namespace VertexProcessor4D__ {

    // ---------------- 顶点插值 ----------------
    ClipVertex4D interpolate(const ClipVertex4D& a, const ClipVertex4D& b, Float t) {
        return {
            a.pos * (1.0f - t) + b.pos * t,
            a.color,                                  // 颜色不插值（按需求直接复制）
            a.normal * (1.0f - t) + b.normal * t      // 法向线性插值
        };
    }

    // ---------------- 对一个 4D 凸多边形做单平面裁剪 ----------------
    // plane: dot(n, p) >= 0 为内侧
    void clipAgainstPlane4D(List<ClipVertex4D>& in, const Vector5D& n,
                            List<ClipVertex4D>& out) {
        out.clear();
        if (in.empty()) return;

        for (size_t i = 0; i < in.size(); ++i) {
            const ClipVertex4D& A = in[i];
            const ClipVertex4D& B = in[(i + 1) % in.size()];

            Float da = n.dot(A.pos);
            Float db = n.dot(B.pos);
            Boolean aIn = da >= 0.0f;
            Boolean bIn = db >= 0.0f;

            if (aIn && bIn) {
                out.push_back(B);
            } else if (aIn) {
                Float t = da / (da - db);
                out.push_back(interpolate(A, B, t));
            } else if (bIn) {
                Float t = da / (da - db);
                out.push_back(interpolate(A, B, t));
                out.push_back(B);
            }
        }
    }

    // ---------------- 4D 视锥体裁剪（8 个半空间） ----------------
    // 返回裁剪后多边形的 5D 齐次顶点集合（可能 0~8 个）
    void clipTetrahedron(const List<ClipVertex4D>& tetIn,
                         List<ClipVertex4D>& tetOut) {
        // 平面法向：n·p >= 0 表示内侧
        // 坐标顺序 (x, y, z, w, v)
        static const Vector5D planes[8] = {
            { 1, 0, 0, 0,  1},  //  x + v >= 0  →  x >= -v
            {-1, 0, 0, 0,  1},  // -x + v >= 0  →  x <=  v
            { 0, 1, 0, 0,  1},  //  y >= -v
            { 0,-1, 0, 0,  1},  //  y <=  v
            { 0, 0, 1, 0,  1},  //  z >= -v
            { 0, 0,-1, 0,  1},  //  z <=  v
            { 0, 0, 0, 1,  1},  //  w >= -v
            { 0, 0, 0,-1,  1},  //  w <=  v
        };

        List<ClipVertex4D> a = tetIn;
        List<ClipVertex4D> b;
        for (int i = 0; i < 8; ++i) {
            clipAgainstPlane4D(a, planes[i], b);
            std::swap(a, b);
            if (a.empty()) break;
        }
        tetOut = std::move(a);
    }

    // ---------------- 4D 四面体 → 投影到 3D 的四面体列表 ----------------
    // 注：裁剪后一般不会正好 4 个顶点；这里用 fan 三角化在 4D 上先切出
    //     一组"3D 投影后能组成四面体"的图元
    List<Tetrahedron3D> process(const Tetrahedron4D& tetrahedron,
                                const Matrix5x5& model,
                                const Matrix5x5& view,
                                const Matrix5x5& projection,
                                const Matrix4x4& viewPort) {
        List<Tetrahedron3D> result;

        // ---- 1. 顶点着色阶段：4D 世界 → 5D 齐次裁剪空间 ----
        const Matrix5x5 mvp = projection * view * model;
        const Matrix4x4 modelLinear = static_cast<Matrix4x4>(model);
        const Matrix4x4 normalMat  = modelLinear.inverse().transpose();

        List<ClipVertex4D> clipVerts;
        clipVerts.reserve(4);
        for (int i = 0; i < 4; ++i) {
            const Vertex4D& v = tetrahedron.vertices[i];
            clipVerts.push_back(ClipVertex4D{
                mvp * Vector5D{ v.pos.x, v.pos.y, v.pos.z, v.pos.w, 1.0f },
                v.color,
                normalMat * v.normal
            });
        }

        // ---- 2. 视锥体裁剪 ----
        List<ClipVertex4D> poly;
        clipTetrahedron(clipVerts, poly);
        if (poly.size() < 4) return result;   // 至少 4 个才能组成一个 4D 四面体

        // ---- 3. 投影到 3D 并组装输出 ----
        // 投影函数：5D 齐次 → 3D NDC → 视口
        auto project = [&](const ClipVertex4D& cv) -> Vertex3D {
            Float invV = 1.0f / cv.pos.v;
            Vector3D ndc{ cv.pos.x * invV, cv.pos.y * invV, cv.pos.z * invV };

            // 视口（单位矩阵时 xy 直接是 ndc，深度另由 clip.w/clip.v 给出）
            Vector4D sp = viewPort * Vector4D{ ndc.x, ndc.y, 0.0f, 1.0f };
            Float depth = cv.pos.w * invV;   // 4D 深度

            Vector3D n3 = Vector3D{ cv.normal.x, cv.normal.y, cv.normal.z }.normalize();
            return Vertex3D{ Vector3D{ sp.x, sp.y, depth }, cv.color, n3 };
        };

        // 把裁剪后的 4D 凸包 fan 三角化成若干四面体
        // 固定第 0 个顶点，另外三个从 1..N-1 里取连续三个，组成一个 4D 四面体
        for (size_t i = 1; i + 2 < poly.size(); ++i) {
            Tetrahedron3D tet3;
            tet3.vertices[0] = project(poly[0]);
            tet3.vertices[1] = project(poly[i]);
            tet3.vertices[2] = project(poly[i + 1]);
            tet3.vertices[3] = project(poly[i + 2]);
            result.push_back(tet3);
        }
        return result;
    }
}