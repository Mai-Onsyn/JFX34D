package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * 操作实现共用的参数解析与校验工具。
 *
 * <p>只服务于 {@code weilantianhai.agent.ir.operations} 包内的操作类，
 * 把 v2 接口文档里反复出现的几种参数写法和校验规则收敛到一处：
 *
 * <ul>
 *   <li>四维向量：数组 {@code [1,2,3,4]} 或字符串 {@code "(1 2 3 4)"} 两种写法都接受</li>
 *   <li>布尔开关：{@code true}/{@code false}，字符串形式也接受</li>
 *   <li>枚举：轴 {@code x/y/z/w}、平面 {@code xy/xz/xw/yz/yw/zw}</li>
 * </ul>
 *
 * <p><b>注意</b>：模型管理、几何体、雕刻相关操作已取消接入，
 * 因此与模型名、5×5 矩阵、顶点相关的解析方法已一并移除。
 * 后续若重新接入，需要在这里补回 {@code getRequiredModelName} 与 {@code getRequiredMatrix5}。
 */
final class OperationParams {

    /** 四维坐标轴 */
    static final java.util.Set<String> AXES = java.util.Set.of("x", "y", "z", "w");

    /** 四维旋转平面 */
    static final java.util.Set<String> PLANES = java.util.Set.of("xy", "xz", "xw", "yz", "yw", "zw");

    private OperationParams() {}

    // ---------- 布尔 ----------

    /** 读取必填的布尔参数 */
    static boolean getRequiredBoolean(JSONObject data, String key, String codePrefix) throws IRException {
        if (data == null) {
            throw new IRException(codePrefix + "_MISSING", codePrefix + " 所在 data 为空");
        }
        Object raw = data.get(key);
        if (raw == null) {
            throw new IRException(codePrefix + "_MISSING", "缺少必填字段 " + key);
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        String s = raw.toString().trim().toLowerCase();
        if ("true".equals(s)) return true;
        if ("false".equals(s)) return false;
        throw new IRException(codePrefix + "_INVALID", key + " 必须是布尔值：" + raw);
    }

    // ---------- 四维向量 ----------

    /**
     * 读取四维向量，兼容三种写法：
     * <ul>
     *   <li>数组：{@code [1, 2, 3, 4]}</li>
     *   <li>字符串（括号）：{@code "(1 2 3 4)"} 或 {@code "(1, 2, 3, 4)"}</li>
     *   <li>字符串（空格）：{@code "1 2 3 4"}</li>
     * </ul>
     */
    static Vector4f getRequiredVector4(JSONObject data, String key, String codePrefix) throws IRException {
        if (data == null) {
            throw new IRException(codePrefix + "_MISSING", codePrefix + " 所在 data 为空");
        }
        Object raw = data.get(key);
        if (raw == null) {
            throw new IRException(codePrefix + "_MISSING", "缺少必填字段 " + key);
        }

        if (raw instanceof JSONArray arr) {
            return fromComponents(arr, key, codePrefix);
        }
        if (raw instanceof java.util.List<?> list) {
            return fromList(list, key, codePrefix);
        }

        String s = raw.toString().trim();
        if (s.isEmpty()) {
            throw new IRException(codePrefix + "_EMPTY", key + " 不能为空");
        }
        // 去掉括号后按空白或逗号切分
        String body = s;
        if (body.startsWith("(") && body.endsWith(")")) {
            body = body.substring(1, body.length() - 1);
        }
        body = body.replace(",", " ").trim();
        if (body.isEmpty()) {
            throw new IRException(codePrefix + "_INVALID", key + " 不是合法的四维向量：" + s);
        }
        String[] parts = body.split("\\s+");
        if (parts.length != 4) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 需要 4 个分量，实际 " + parts.length + " 个：" + s);
        }
        float[] v = new float[4];
        for (int i = 0; i < 4; i++) {
            v[i] = parseFloat(parts[i], key, codePrefix);
        }
        return new Vector4f(v[0], v[1], v[2], v[3]);
    }

    private static Vector4f fromComponents(JSONArray arr, String key, String codePrefix) throws IRException {
        if (arr.size() != 4) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 需要 4 个分量，实际 " + arr.size() + " 个");
        }
        float[] v = new float[4];
        for (int i = 0; i < 4; i++) {
            Object o = arr.get(i);
            if (!(o instanceof Number)) {
                throw new IRException(codePrefix + "_INVALID",
                        key + " 第 " + i + " 个分量不是数字：" + o);
            }
            v[i] = ((Number) o).floatValue();
        }
        return new Vector4f(v[0], v[1], v[2], v[3]);
    }

    private static Vector4f fromList(java.util.List<?> list, String key, String codePrefix) throws IRException {
        if (list.size() != 4) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 需要 4 个分量，实际 " + list.size() + " 个");
        }
        float[] v = new float[4];
        for (int i = 0; i < 4; i++) {
            Object o = list.get(i);
            if (!(o instanceof Number)) {
                throw new IRException(codePrefix + "_INVALID",
                        key + " 第 " + i + " 个分量不是数字：" + o);
            }
            v[i] = ((Number) o).floatValue();
        }
        return new Vector4f(v[0], v[1], v[2], v[3]);
    }

    private static float parseFloat(String s, String key, String codePrefix) throws IRException {
        try {
            float f = Float.parseFloat(s.trim());
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                throw new IRException(codePrefix + "_INVALID", key + " 分量非法：" + s);
            }
            return f;
        } catch (NumberFormatException e) {
            throw new IRException(codePrefix + "_INVALID", key + " 分量无法解析：" + s);
        }
    }

    /** 校验向量是否为单位向量 */
    static void requireUnit(Vector4f v, String key, String codePrefix) throws IRException {
        float lenSq = v.lengthSquared();
        if (Math.abs(lenSq - 1f) > 1e-3f) {
            throw new IRException(codePrefix + "_NOT_UNIT",
                    key + " 必须是单位向量，实际模长=" + (float) Math.sqrt(lenSq));
        }
    }

    /** 校验两个向量是否正交 */
    static void requireOrthogonal(Vector4f a, Vector4f b, String ka, String kb, String codePrefix)
            throws IRException {
        if (Math.abs(a.dot(b)) > 1e-3f) {
            throw new IRException(codePrefix + "_NOT_ORTHOGONAL",
                    ka + " 与 " + kb + " 必须正交，实际点积=" + a.dot(b));
        }
    }

    // ---------- 格式化输出 ----------

    /** 把四维坐标格式化为 "(x, y, z, w)" */
    static String formatVector4(Vector4f v) {
        return "(" + fmt(v.x) + ", " + fmt(v.y) + ", " + fmt(v.z) + ", " + fmt(v.w) + ")";
    }

    /**
     * 统一数值格式化：去掉多余的小数位，避免输出 "1.0000001" 这种噪声。
     * 整数显示为整数，小数最多保留 6 位并去掉尾随 0。
     */
    static String fmt(float f) {
        if (f == Math.rint(f) && !Float.isInfinite(f)) {
            return String.valueOf((long) f);
        }
        String s = String.format(java.util.Locale.ROOT, "%.6f", f);
        // 去掉尾随 0
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }

    /** 帧率保留两位小数 */
    static String fmtFps(float f) {
        return String.format(java.util.Locale.ROOT, "%.2f", f);
    }
}
