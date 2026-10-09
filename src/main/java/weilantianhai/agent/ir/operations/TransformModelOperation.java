package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Coordinate4D;
import mai_onsyn.renderer.utils.Direction;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

import java.util.ArrayList;
import java.util.List;

/**
 * TRANSFORM_MODEL：对模型的 5×5 变换矩阵做操作（**不改顶点**）（文档 §4.2）。
 *
 * <p>参数：
 * <ul>
 *   <li>{@code name} — 目标路径；**可以是分组路径**，此时作用于它自己和全部后代</li>
 *   <li>{@code transforms} — 变换操作数组，每项含 {@code method} 与 {@code data}</li>
 *   <li>{@code apply}（可选，默认 {@code true}）— {@code true} 追加变换；
 *       {@code false} 时**覆盖**模型矩阵（先置单位阵再依次应用）</li>
 * </ul>
 *
 * <p>六种 method：
 * <ul>
 *   <li>{@code MATRIX} — {@code row0}~{@code row4}（5×5 行主序），左乘到模型矩阵</li>
 *   <li>{@code TRANSLATE} — {@code x}/{@code y}/{@code z}/{@code w}，至少给一个，未给按 0</li>
 *   <li>{@code SCALE} — {@code all} 或 {@code x}/{@code y}/{@code z}/{@code w}，未给按 1</li>
 *   <li>{@code ROTATE} — {@code axis}（四维平面）+ {@code angle}（角度制）</li>
 *   <li>{@code CLIP} — {@code source} + {@code target} + {@code amount}</li>
 *   <li>{@code COORDINATE} — 至少给 {@code pos} 或 {@code vx}/{@code vy}/{@code vz}/{@code vw}
 *       之一，未给的保持世界坐标系</li>
 * </ul>
 *
 * <p>返回：
 * <pre>
 * # TRANSFORM_MODEL(id=12)
 * Applied transform
 * "Tower" current is:
 * (1 0 0 0 0)
 * ...
 * </pre>
 */
public class TransformModelOperation extends GuardedOperation {

    /** 单个变换步骤；在 parse 阶段构造，参数错误会在 parse 阶段就抛出来 */
    @FunctionalInterface
    private interface Step {
        void apply(RendererInterface renderer, String name);
    }

    private String name;
    private boolean apply;
    private final List<Step> steps = new ArrayList<>();

    @Override
    public String name() {
        return "TRANSFORM_MODEL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.apply = OperationParams.getOptionalBoolean(data, "apply", true);

        JSONArray transforms = data.getJSONArray("transforms");
        if (transforms == null || transforms.isEmpty()) {
            throw new IRException("TRANSFORMS_MISSING",
                    "缺少必填字段 transforms（至少要有一个变换）");
        }
        for (int i = 0; i < transforms.size(); i++) {
            JSONObject entry = transforms.getJSONObject(i);
            if (entry == null) {
                throw new IRException("TRANSFORM_INVALID", "transforms[" + i + "] 不是对象");
            }
            String method = entry.getString("method");
            if (method == null || method.isBlank()) {
                throw new IRException("METHOD_MISSING", "transforms[" + i + "] 缺少 method");
            }
            method = method.trim().toUpperCase(java.util.Locale.ROOT);
            if (!OperationParams.TRANSFORM_METHODS.contains(method)) {
                throw new IRException("METHOD_INVALID",
                        "transforms[" + i + "].method 非法：" + method
                                + "，允许：" + OperationParams.TRANSFORM_METHODS);
            }
            JSONObject d = entry.getJSONObject("data");
            if (d == null) {
                d = new JSONObject();
            }
            steps.add(buildStep(method, d, i));
        }
    }

    private Step buildStep(String method, JSONObject d, int index) throws IRException {
        switch (method) {
            case "MATRIX": {
                float[] m = OperationParams.getRequiredMatrix5(d, "MATRIX");
                return (renderer, target) ->
                        renderer.getTransform().transform(target, new Matrix5f(m));
            }
            case "TRANSLATE": {
                float x = OperationParams.getOptionalFloatOrNaN(d, "x");
                float y = OperationParams.getOptionalFloatOrNaN(d, "y");
                float z = OperationParams.getOptionalFloatOrNaN(d, "z");
                float w = OperationParams.getOptionalFloatOrNaN(d, "w");
                if (Float.isNaN(x) && Float.isNaN(y) && Float.isNaN(z) && Float.isNaN(w)) {
                    throw new IRException("TRANSLATE_EMPTY",
                            "transforms[" + index + "].data 至少要给 x / y / z / w 之一");
                }
                Vector4f v = new Vector4f(
                        Float.isNaN(x) ? 0f : x,
                        Float.isNaN(y) ? 0f : y,
                        Float.isNaN(z) ? 0f : z,
                        Float.isNaN(w) ? 0f : w);
                return (renderer, target) -> renderer.getTransform().move(target, new Vector4f(v));
            }
            case "SCALE": {
                float all = OperationParams.getOptionalFloatOrNaN(d, "all");
                if (!Float.isNaN(all)) {
                    if (all <= 0f) {
                        throw new IRException("SCALE_INVALID", "all 必须大于 0：" + all);
                    }
                    return (renderer, target) -> renderer.getTransform().scale(target, all, all, all, all);
                }
                float x = OperationParams.getOptionalFloatOrNaN(d, "x");
                float y = OperationParams.getOptionalFloatOrNaN(d, "y");
                float z = OperationParams.getOptionalFloatOrNaN(d, "z");
                float w = OperationParams.getOptionalFloatOrNaN(d, "w");
                if (Float.isNaN(x) && Float.isNaN(y) && Float.isNaN(z) && Float.isNaN(w)) {
                    throw new IRException("SCALE_EMPTY",
                            "transforms[" + index + "].data 至少要给 all 或 x / y / z / w 之一");
                }
                float sx = Float.isNaN(x) ? 1f : x;
                float sy = Float.isNaN(y) ? 1f : y;
                float sz = Float.isNaN(z) ? 1f : z;
                float sw = Float.isNaN(w) ? 1f : w;
                return (renderer, target) -> renderer.getTransform().scale(target, sx, sy, sz, sw);
            }
            case "ROTATE": {
                String axis = OperationParams.getRequiredEnum(d, "axis", "AXIS", OperationParams.PLANES);
                float angle = OperationParams.getRequiredFloat(d, "angle", "ANGLE");
                Direction.Plane plane = planeOf(axis);
                return (renderer, target) -> renderer.getTransform().rotate(target, plane, angle);
            }
            case "CLIP": {
                String source = OperationParams.getRequiredEnum(d, "source", "SOURCE", OperationParams.AXES);
                String target = OperationParams.getRequiredEnum(d, "target", "TARGET", OperationParams.AXES);
                float amount = OperationParams.getRequiredFloat(d, "amount", "AMOUNT");
                Direction.Axis src = axisOf(source);
                Direction.Axis dst = axisOf(target);
                return (renderer, t) -> renderer.getTransform().clip(t, src, dst, amount);
            }
            case "COORDINATE": {
                Vector4f origin = OperationParams.getOptionalVector4(d, "pos", "POS");
                if (origin == null) {
                    origin = new Vector4f(0f, 0f, 0f, 0f);
                }
                Vector4f vx = OperationParams.getOptionalVector4(d, "vx", "VX");
                Vector4f vy = OperationParams.getOptionalVector4(d, "vy", "VY");
                Vector4f vz = OperationParams.getOptionalVector4(d, "vz", "VZ");
                Vector4f vw = OperationParams.getOptionalVector4(d, "vw", "VW");
                if (vx == null && vy == null && vz == null && vw == null
                        && d.get("pos") == null) {
                    throw new IRException("COORDINATE_EMPTY",
                            "transforms[" + index + "].data 至少要给 pos 或 vx / vy / vz / vw 之一");
                }
                Coordinate4D coordinate = new Coordinate4D(
                        vx == null ? new Vector4f(1f, 0f, 0f, 0f) : vx,
                        vy == null ? new Vector4f(0f, 1f, 0f, 0f) : vy,
                        vz == null ? new Vector4f(0f, 0f, 1f, 0f) : vz,
                        vw == null ? new Vector4f(0f, 0f, 0f, 1f) : vw);
                Vector4f finalOrigin = origin;
                return (renderer, target) ->
                        renderer.getTransform().setCoordinate(target, finalOrigin, coordinate);
            }
            default:
                throw new IRException("METHOD_INVALID", "未知变换方式：" + method);
        }
    }

    private static Direction.Plane planeOf(String axis) throws IRException {
        return switch (axis) {
            case "xy" -> Direction.Plane.XY;
            case "xz" -> Direction.Plane.XZ;
            case "xw" -> Direction.Plane.XW;
            case "yz" -> Direction.Plane.YZ;
            case "yw" -> Direction.Plane.YW;
            case "zw" -> Direction.Plane.ZW;
            default -> throw new IRException("AXIS_INVALID", "非法旋转平面：" + axis);
        };
    }

    private static Direction.Axis axisOf(String axis) throws IRException {
        return switch (axis) {
            case "x" -> Direction.Axis.X;
            case "y" -> Direction.Axis.Y;
            case "z" -> Direction.Axis.Z;
            case "w" -> Direction.Axis.W;
            default -> throw new IRException("AXIS_INVALID", "非法轴：" + axis);
        };
    }

    @Override
    protected String run(RendererInterface renderer) {
        if (!apply) {
            renderer.getTransform().setModelMatrix(name, Matrix5f.Companion.getIDENTITY());
        }
        for (Step step : steps) {
            step.apply(renderer, name);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(apply ? "Applied transform" : "Setted transform");
        float[] matrix = safeMatrix(renderer);
        if (matrix != null) {
            sb.append('\n').append('"').append(name).append("\" current is:\n")
              .append(OperationParams.formatMatrix5Parens(matrix));
        }
        return sb.toString();
    }

    /**
     * 取当前矩阵。{@code getModelMatrix} 只接受精确路径，
     * 传纯分组路径会抛异常；这里吞掉它，只少显示一段矩阵，不影响变换本身已经生效。
     */
    private float[] safeMatrix(RendererInterface renderer) {
        try {
            return renderer.getTransform().getModelMatrix(name).getData();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
