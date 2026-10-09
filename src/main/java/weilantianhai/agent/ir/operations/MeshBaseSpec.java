package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.AgentBridge;
import mai_onsyn.renderer.ogl3d.data.Mesh;
import mai_onsyn.renderer.ogl3d.data.MeshOpsKt;
import mai_onsyn.renderer.ogl3d.data.Transform;
import mai_onsyn.renderer.utils.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import weilantianhai.agent.ir.IRException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 文档 §5.3 的 **3D 底面网格 JSON DSL** 的解析与构建。
 *
 * <p>{@code CREATE_PRISM4} 与 {@code CREATE_CONE4} 需要一个 3D 网格当底面，
 * AI 不写三角形，而是用"{@code shape} + 几何参数 + {@code transforms}"描述一个基本几何体，
 * 由接入层（也就是这里）生成三角形网格。文档 §附录 D 明确这属于接入层职责。
 *
 * <p>流程：解析 → 校验 → 调 {@link AgentBridge#baseMesh} 让渲染侧生成 →
 * 用 3D {@link Transform} 依次累加 {@code transforms} → {@code MeshOps.applyTransform}
 * 把变换**烘焙进顶点**（{@code constructPrism4}/{@code constructCone4} 只读顶点位置，
 * 不会去乘 {@code Mesh.transform}，所以必须先烘焙）。
 */
final class MeshBaseSpec {

    /** 单个 3D 变换步骤 */
    @FunctionalInterface
    private interface Step3D {
        void apply(Transform transform);
    }

    private final String shape;
    private final int sides;
    private final float radius;
    private final float height;
    private final float edge;
    private final float density;
    private final int segments;
    private final int argb;
    private final List<Step3D> steps;
    private final List<String> methodNames;

    private MeshBaseSpec(String shape, int sides, float radius, float height, float edge,
                         float density, int segments, int argb,
                         List<Step3D> steps, List<String> methodNames) {
        this.shape = shape;
        this.sides = sides;
        this.radius = radius;
        this.height = height;
        this.edge = edge;
        this.density = density;
        this.segments = segments;
        this.argb = argb;
        this.steps = steps;
        this.methodNames = methodNames;
    }

    /**
     * 解析并校验 {@code base} 字段。
     *
     * @param base 形如 {@code {"shape":"PRISM","sides":6,"radius":1.0,"height":2.0,"color":"#FF66CCFF","transforms":[...]}}
     */
    static MeshBaseSpec parse(Object base, String codePrefix) throws IRException {
        if (base == null) {
            throw new IRException(codePrefix + "_MISSING", "缺少必填字段 base");
        }
        if (!(base instanceof JSONObject obj)) {
            throw new IRException(codePrefix + "_INVALID", "base 必须是对象：" + base);
        }

        String shape = OperationParams.getRequiredString(obj, "shape", codePrefix + "_SHAPE")
                .toUpperCase(Locale.ROOT);
        if (!OperationParams.BASE_SHAPES.contains(shape)) {
            throw new IRException(codePrefix + "_SHAPE_INVALID",
                    "base.shape 非法：" + shape + "，允许：" + OperationParams.BASE_SHAPES);
        }

        int argb = OperationParams.parseArgb(
                obj.get("color"), OperationParams.DEFAULT_COLOR, "color", codePrefix + "_COLOR");

        // 各形状的必填 / 可选参数，默认值与文档 §5.3 的表一致
        int sides = 3;
        float radius = 1f;
        float height = 1f;
        float edge = 1f;
        float density = 8f;
        int segments = 16;

        switch (shape) {
            case "SPHERE" -> {
                radius = OperationParams.getRequiredPositiveFloat(obj, "radius", codePrefix + "_RADIUS");
                density = OperationParams.getOptionalFloat(obj, "density", 8f);
                if (density <= 0f) {
                    throw new IRException(codePrefix + "_DENSITY_INVALID",
                            "base.density 必须大于 0：" + density);
                }
            }
            case "CUBE" -> edge = OperationParams.getRequiredPositiveFloat(
                    obj, "edge", codePrefix + "_EDGE");
            case "PRISM", "PYRAMID" -> {
                sides = OperationParams.getRequiredIntAtLeast3(obj, "sides", codePrefix + "_SIDES");
                radius = OperationParams.getOptionalFloat(obj, "radius", 1f);
                height = OperationParams.getOptionalFloat(obj, "height", 1f);
                if (radius <= 0f) {
                    throw new IRException(codePrefix + "_RADIUS_INVALID",
                            "base.radius 必须大于 0：" + radius);
                }
                if (height <= 0f) {
                    throw new IRException(codePrefix + "_HEIGHT_INVALID",
                            "base.height 必须大于 0：" + height);
                }
            }
            case "CONE" -> {
                radius = OperationParams.getRequiredPositiveFloat(obj, "radius", codePrefix + "_RADIUS");
                height = OperationParams.getOptionalFloat(obj, "height", 1f);
                segments = OperationParams.getOptionalInt(obj, "segments", 16, codePrefix + "_SEGMENTS");
                if (height <= 0f) {
                    throw new IRException(codePrefix + "_HEIGHT_INVALID",
                            "base.height 必须大于 0：" + height);
                }
                if (segments < 3) {
                    throw new IRException(codePrefix + "_SEGMENTS_INVALID",
                            "base.segments 必须 >= 3：" + segments);
                }
            }
            default -> throw new IRException(codePrefix + "_SHAPE_INVALID",
                    "base.shape 非法：" + shape);
        }

        List<Step3D> steps = new ArrayList<>();
        List<String> names = new ArrayList<>();
        com.alibaba.fastjson2.JSONArray transforms = obj.getJSONArray("transforms");
        if (transforms != null) {
            for (int i = 0; i < transforms.size(); i++) {
                JSONObject entry = transforms.getJSONObject(i);
                if (entry == null) {
                    throw new IRException(codePrefix + "_TRANSFORM_INVALID",
                            "base.transforms[" + i + "] 不是对象");
                }
                String method = OperationParams.getRequiredString(entry, "method",
                        codePrefix + "_METHOD").toUpperCase(Locale.ROOT);
                if (!OperationParams.MESH_METHODS.contains(method)) {
                    throw new IRException(codePrefix + "_METHOD_INVALID",
                            "base.transforms[" + i + "].method 非法：" + method
                                    + "，允许：" + OperationParams.MESH_METHODS);
                }
                JSONObject d = entry.getJSONObject("data");
                if (d == null) {
                    d = new JSONObject();
                }
                steps.add(buildStep(method, d, i, codePrefix));
                names.add(method);
            }
        }

        return new MeshBaseSpec(shape, sides, radius, height, edge, density, segments, argb,
                steps, names);
    }

    private static Step3D buildStep(String method, JSONObject d, int index, String codePrefix)
            throws IRException {
        switch (method) {
            case "TRANSLATE": {
                float x = OperationParams.getOptionalFloat(d, "x", 0f);
                float y = OperationParams.getOptionalFloat(d, "y", 0f);
                float z = OperationParams.getOptionalFloat(d, "z", 0f);
                if (d.get("x") == null && d.get("y") == null && d.get("z") == null) {
                    throw new IRException(codePrefix + "_TRANSLATE_EMPTY",
                            "base.transforms[" + index + "].data 至少要给 x / y / z 之一");
                }
                Vector3f v = new Vector3f(x, y, z);
                return transform -> transform.move(new Vector3f(v));
            }
            case "SCALE": {
                float all = OperationParams.getOptionalFloat(d, "all", Float.NaN);
                float x;
                float y;
                float z;
                if (!Float.isNaN(all)) {
                    x = all;
                    y = all;
                    z = all;
                } else {
                    if (d.get("x") == null && d.get("y") == null && d.get("z") == null) {
                        throw new IRException(codePrefix + "_SCALE_EMPTY",
                                "base.transforms[" + index + "].data 至少要给 all 或 x / y / z 之一");
                    }
                    x = OperationParams.getOptionalFloat(d, "x", 1f);
                    y = OperationParams.getOptionalFloat(d, "y", 1f);
                    z = OperationParams.getOptionalFloat(d, "z", 1f);
                }
                float sx = x;
                float sy = y;
                float sz = z;
                return transform -> transform.scale(sx, sy, sz);
            }
            case "ROTATE": {
                String axis = OperationParams.getRequiredEnum(d, "axis", codePrefix + "_AXIS",
                        OperationParams.PLANES_3D);
                float angle = OperationParams.getRequiredFloat(d, "angle", codePrefix + "_ANGLE");
                Direction.Plane plane = switch (axis) {
                    case "xy" -> Direction.Plane.XY;
                    case "xz" -> Direction.Plane.XZ;
                    case "yz" -> Direction.Plane.YZ;
                    default -> throw new IRException(codePrefix + "_AXIS_INVALID",
                            "3D ROTATE 只接受 xy / xz / yz，得到：" + axis);
                };
                return transform -> transform.rotate(plane, angle);
            }
            case "MATRIX": {
                // 文档：row0~row3 是 4×4 行主序；JOML 的 Matrix4f 构造是列主序，
                // 所以这里按列拼装（第一列 = 各行第一个元素）。
                float[][] r = new float[4][];
                for (int i = 0; i < 4; i++) {
                    r[i] = row4(d, "row" + i, codePrefix);
                }
                Matrix4f m = new Matrix4f(
                        r[0][0], r[1][0], r[2][0], r[3][0],
                        r[0][1], r[1][1], r[2][1], r[3][1],
                        r[0][2], r[1][2], r[2][2], r[3][2],
                        r[0][3], r[1][3], r[2][3], r[3][3]);
                return transform -> transform.applyMatrix(m);
            }
            default:
                throw new IRException(codePrefix + "_METHOD_INVALID", "未知 3D 变换方式：" + method);
        }
    }

    private static float[] row4(JSONObject d, String key, String codePrefix) throws IRException {
        Object raw = d.get(key);
        if (raw == null) {
            throw new IRException(codePrefix + "_ROW_MISSING",
                    "base.transforms[].data 缺少 " + key);
        }
        if (raw instanceof com.alibaba.fastjson2.JSONArray arr) {
            if (arr.size() != 4) {
                throw new IRException(codePrefix + "_ROW_INVALID",
                        key + " 需要 4 个元素，实际 " + arr.size() + " 个");
            }
            float[] out = new float[4];
            for (int i = 0; i < 4; i++) {
                Object o = arr.get(i);
                if (!(o instanceof Number n)) {
                    throw new IRException(codePrefix + "_ROW_INVALID",
                            key + " 第 " + i + " 个元素不是数字：" + o);
                }
                out[i] = n.floatValue();
            }
            return out;
        }
        String body = raw.toString().trim().replace(",", " ");
        if (body.startsWith("(") && body.endsWith(")")) {
            body = body.substring(1, body.length() - 1);
        }
        String[] parts = body.trim().split("\\s+");
        if (parts.length != 4) {
            throw new IRException(codePrefix + "_ROW_INVALID",
                    key + " 需要 4 个元素，实际 " + parts.length + " 个：" + raw);
        }
        float[] out = new float[4];
        for (int i = 0; i < 4; i++) {
            try {
                out[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                throw new IRException(codePrefix + "_ROW_INVALID",
                        key + " 第 " + i + " 个元素无法解析：" + parts[i]);
            }
        }
        return out;
    }

    /** 生成网格并把 {@code transforms} 烘焙进顶点，返回一个顶点已在最终 3D 位置的 {@link Mesh} */
    Mesh build() {
        Mesh mesh = AgentBridge.baseMesh(shape, sides, radius, height, edge, density, segments, argb);
        if (steps.isEmpty()) {
            return mesh;
        }
        Transform transform = new Transform(new Matrix4f());
        for (Step3D step : steps) {
            step.apply(transform);
        }
        return MeshOpsKt.applyTransform(mesh, transform);
    }

    /** 返回文案里的 "Base: ..." 摘要，例如 {@code PRISM sides=6 radius=1 height=2 (20 triangles), transforms=[ROTATE]} */
    String summary(Mesh mesh) {
        StringBuilder sb = new StringBuilder();
        sb.append(shape);
        switch (shape) {
            case "SPHERE" -> sb.append(" radius=").append(OperationParams.fmt(radius))
                    .append(" density=").append(OperationParams.fmt(density));
            case "CUBE" -> sb.append(" edge=").append(OperationParams.fmt(edge));
            case "PRISM", "PYRAMID" -> sb.append(" sides=").append(sides)
                    .append(" radius=").append(OperationParams.fmt(radius))
                    .append(" height=").append(OperationParams.fmt(height));
            case "CONE" -> sb.append(" radius=").append(OperationParams.fmt(radius))
                    .append(" height=").append(OperationParams.fmt(height))
                    .append(" segments=").append(segments);
            default -> { }
        }
        sb.append(" (").append(mesh.getTriangles().size()).append(" triangles)");
        sb.append(", transforms=").append(methodNames);
        return sb.toString();
    }

    /** 供校验用：网格必须至少有一个三角形（文档 §5.3.4） */
    static void requireTriangles(Mesh mesh, String what) throws IRException {
        if (mesh.getTriangles().isEmpty()) {
            throw new IRException("BASE_MESH_EMPTY",
                    what + " base mesh has no triangle");
        }
    }
}
