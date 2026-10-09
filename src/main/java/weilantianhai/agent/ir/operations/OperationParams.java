package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 操作实现共用的参数解析、校验与格式化工具。
 *
 * <p>只服务于 {@code weilantianhai.agent.ir.operations} 包内的操作类，
 * 把 v2 接口文档里反复出现的几种参数写法和校验规则收敛到一处：
 *
 * <ul>
 *   <li>四维向量：数组 {@code [1,2,3,4]} 或字符串 {@code "(1 2 3 4)"}</li>
 *   <li>5×5 行主序矩阵：{@code row0}~{@code row4} 五个行向量</li>
 *   <li>模型路径：{@code "Tower/Base"} 形式，见文档 §0.3</li>
 *   <li>颜色：{@code "#AARRGGBB"} / {@code "#RRGGBB"} / 整数 {@code 0xAARRGGBB}</li>
 *   <li>枚举：单轴 {@code x/y/z/w}、四维平面 {@code xy/xz/xw/yz/yw/zw}</li>
 * </ul>
 */
final class OperationParams {

    /** 四维坐标轴（单轴） */
    static final Set<String> AXES = Set.of("x", "y", "z", "w");

    /** 四维旋转平面（双轴） */
    static final Set<String> PLANES = Set.of("xy", "xz", "xw", "yz", "yw", "zw");

    /** 三维旋转平面，只用于 3D 网格的 transforms */
    static final Set<String> PLANES_3D = Set.of("xy", "xz", "yz");

    /** 3D 基础几何体种类 */
    static final Set<String> BASE_SHAPES = Set.of("SPHERE", "CUBE", "PRISM", "PYRAMID", "CONE");

    /** 3D 网格变换方式 */
    static final Set<String> MESH_METHODS = Set.of("TRANSLATE", "SCALE", "ROTATE", "MATRIX");

    /** 模型变换方式（TRANSFORM_MODEL 的 transforms[].method） */
    static final Set<String> TRANSFORM_METHODS =
            Set.of("MATRIX", "TRANSLATE", "SCALE", "ROTATE", "CLIP", "COORDINATE");

    /** 顶点编号上限（0~3） */
    static final int MAX_VERTEX_INDEX = 3;

    /** 默认顶点色 {@code #FFB0B0B0}，与渲染侧 generator 的 DEFAULT_MESH_COLOR 一致 */
    static final int DEFAULT_COLOR = 0xFFB0B0B0;

    /** 路径长度上限，与文档 §0.2 一致 */
    private static final int PATH_MAX = 64;

    private OperationParams() {}

    // ------------------------------------------------------------ 字符串与路径

    /** 读取必填的非空字符串（去掉首尾空白后不得为空） */
    static String getRequiredString(JSONObject data, String key, String codePrefix) throws IRException {
        if (data == null) {
            throw new IRException(codePrefix + "_MISSING", codePrefix + " 所在 data 为空");
        }
        Object raw = data.get(key);
        if (raw == null) {
            throw new IRException(codePrefix + "_MISSING", "缺少必填字段 " + key);
        }
        String value = raw.toString().trim();
        if (value.isEmpty()) {
            throw new IRException(codePrefix + "_EMPTY", key + " 不能为空");
        }
        return value;
    }

    /**
     * 读取必填的模型路径，并做文档 §0.3 的路径校验。
     *
     * <p>规则：区分大小写；长度 1–64；不能有空路径段（{@code //}）；
     * 不能以 {@code /} 开头或结尾；不能含 {@code '#'}（{@code #} 留给"某个胞"的写法）。
     */
    static String getRequiredPath(JSONObject data, String key, String codePrefix) throws IRException {
        return validatePath(getRequiredString(data, key, codePrefix), key, codePrefix);
    }

    /** 读取可选的模型路径；缺失或为空时返回 defaultVal（可为 null） */
    static String getOptionalPath(JSONObject data, String key, String codePrefix, String defaultVal)
            throws IRException {
        if (data == null || data.get(key) == null) {
            return defaultVal;
        }
        String raw = data.get(key).toString().trim();
        if (raw.isEmpty()) {
            return defaultVal;
        }
        return validatePath(raw, key, codePrefix);
    }

    /** 路径合法性校验，返回去掉首尾空白后的路径 */
    static String validatePath(String path, String key, String codePrefix) throws IRException {
        String p = path.trim();
        if (p.isEmpty()) {
            throw new IRException(codePrefix + "_EMPTY", key + " 不能为空");
        }
        if (p.length() > PATH_MAX) {
            throw new IRException(codePrefix + "_TOO_LONG",
                    key + " 长度不能超过 " + PATH_MAX + "：" + p);
        }
        if (p.startsWith("/") || p.endsWith("/")) {
            throw new IRException(codePrefix + "_INVALID", key + " 不能以 '/' 开头或结尾：" + p);
        }
        if (p.contains("//")) {
            throw new IRException(codePrefix + "_INVALID", key + " 不能包含空的路径段：" + p);
        }
        if (p.contains("#")) {
            throw new IRException(codePrefix + "_INVALID", key + " 不能包含 '#'：" + p);
        }
        return p;
    }

    /**
     * 形状操作的路径拼接：结果路径 = {@code target + "/" + name}。
     *
     * <p>{@code target} 允许为空字符串（表示建在根路径）；{@code name} 可带子路径，
     * 但不能为空、不能含 {@code #}、不能有空段。
     */
    static String joinPartPath(String target, String name, String codePrefix) throws IRException {
        String t = target == null ? "" : target.trim();
        t = t.replaceAll("^/+", "").replaceAll("/+$", "");
        if (!t.isEmpty()) {
            t = validatePath(t, "target", codePrefix);
        }
        String n = name.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        if (n.isEmpty()) {
            throw new IRException(codePrefix + "_EMPTY", "name 不能为空");
        }
        if (n.contains("#")) {
            throw new IRException(codePrefix + "_INVALID", "name 不能包含 '#'：" + name);
        }
        if (n.contains("//")) {
            throw new IRException(codePrefix + "_INVALID", "name 不能包含空的路径段：" + name);
        }
        return validatePath(t.isEmpty() ? n : t + "/" + n, "path", codePrefix);
    }

    /** 读取可选字符串参数，缺失或为空时返回 defaultVal */
    static String getOptionalString(JSONObject data, String key, String defaultVal) {
        if (data == null || data.get(key) == null) {
            return defaultVal;
        }
        String value = data.get(key).toString().trim();
        return value.isEmpty() ? defaultVal : value;
    }

    // ------------------------------------------------------------ 布尔 / 数值

    /**
     * 读取必填枚举值并归一化为小写。
     *
     * <p>文档 §0.2：轴 / 平面枚举统一小写，实际匹配时大小写不敏感。
     */
    static String getRequiredEnum(JSONObject data, String key, String codePrefix, Set<String> allowed)
            throws IRException {
        String value = getRequiredString(data, key, codePrefix).toLowerCase(Locale.ROOT);
        if (!allowed.contains(value)) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 非法：" + value + "，允许：" + allowed);
        }
        return value;
    }

    /** 读取必填 float */
    static float getRequiredFloat(JSONObject data, String key, String codePrefix) throws IRException {
        return readFloat(requireField(data, key, codePrefix), key, codePrefix);
    }

    /** 读取必填布尔参数 */
    static boolean getRequiredBoolean(JSONObject data, String key, String codePrefix) throws IRException {
        Object raw = requireField(data, key, codePrefix);
        if (raw instanceof Boolean b) {
            return b;
        }
        String s = raw.toString().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(s)) return true;
        if ("false".equals(s)) return false;
        throw new IRException(codePrefix + "_INVALID", key + " 必须是布尔值：" + raw);
    }

    /** 读取可选布尔参数，缺失时返回 defaultVal */
    static boolean getOptionalBoolean(JSONObject data, String key, boolean defaultVal)
            throws IRException {
        if (data == null || data.get(key) == null) {
            return defaultVal;
        }
        Object raw = data.get(key);
        if (raw instanceof Boolean b) {
            return b;
        }
        String s = raw.toString().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(s)) return true;
        if ("false".equals(s)) return false;
        throw new IRException(key.toUpperCase(Locale.ROOT) + "_INVALID",
                key + " 必须是布尔值：" + raw);
    }

    /** 读取可选 float 参数，缺失时返回 defaultVal */
    static float getOptionalFloat(JSONObject data, String key, float defaultVal) throws IRException {
        if (data == null || data.get(key) == null) {
            return defaultVal;
        }
        return readFloat(data.get(key), key, key.toUpperCase(Locale.ROOT));
    }

    /** 读取可选 float；缺失返回 NaN，用于"至少给一个轴"这类判断 */
    static float getOptionalFloatOrNaN(JSONObject data, String key) throws IRException {
        if (data == null || data.get(key) == null) {
            return Float.NaN;
        }
        return readFloat(data.get(key), key, key.toUpperCase(Locale.ROOT));
    }

    private static float readFloat(Object raw, String key, String codePrefix) throws IRException {
        float value;
        if (raw instanceof Number n) {
            value = n.floatValue();
        } else {
            try {
                value = Float.parseFloat(raw.toString().trim());
            } catch (NumberFormatException e) {
                throw new IRException(codePrefix + "_INVALID", key + " 无法解析：" + raw);
            }
        }
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IRException(codePrefix + "_INVALID", key + " 非法：" + value);
        }
        return value;
    }

    /** 读取必填非负整数（下标、顶点编号等） */
    static int getRequiredInt(JSONObject data, String key, String codePrefix) throws IRException {
        Object raw = requireField(data, key, codePrefix);
        int value;
        if (raw instanceof Number n) {
            value = n.intValue();
        } else {
            try {
                value = Integer.parseInt(raw.toString().trim());
            } catch (NumberFormatException e) {
                throw new IRException(codePrefix + "_INVALID", key + " 必须是整数：" + raw);
            }
        }
        if (value < 0) {
            throw new IRException(codePrefix + "_INVALID", key + " 必须是非负整数：" + value);
        }
        return value;
    }

    /** 读取可选整数，缺失时返回 defaultVal */
    static int getOptionalInt(JSONObject data, String key, int defaultVal, String codePrefix)
            throws IRException {
        if (data == null || data.get(key) == null) {
            return defaultVal;
        }
        return getRequiredInt(data, key, codePrefix);
    }

    /** 读取必填 float 并校验为正数（半径、棱长、密度等） */
    static float getRequiredPositiveFloat(JSONObject data, String key, String codePrefix)
            throws IRException {
        float value = readFloat(requireField(data, key, codePrefix), key, codePrefix);
        if (value <= 0f) {
            throw new IRException(codePrefix + "_INVALID", key + " 必须大于 0：" + value);
        }
        return value;
    }

    /** 读取必填的 {@code >= 3} 整数（棱柱/棱锥边数、圆锥分段数） */
    static int getRequiredIntAtLeast3(JSONObject data, String key, String codePrefix)
            throws IRException {
        int value = getRequiredInt(data, key, codePrefix);
        if (value < 3) {
            throw new IRException(codePrefix + "_INVALID", key + " 必须 >= 3：" + value);
        }
        return value;
    }

    private static Object requireField(JSONObject data, String key, String codePrefix)
            throws IRException {
        if (data == null) {
            throw new IRException(codePrefix + "_MISSING", codePrefix + " 所在 data 为空");
        }
        Object raw = data.get(key);
        if (raw == null) {
            throw new IRException(codePrefix + "_MISSING", "缺少必填字段 " + key);
        }
        return raw;
    }

    /**
     * 读取整数数组（如 TRANSFORM_TETRAHEDRONS 的 {@code tets}）。
     * 空数组合法（表示"全部"）；字段缺失时返回 null，由调用方决定语义。
     */
    static List<Integer> getOptionalIntList(JSONObject data, String key, String codePrefix)
            throws IRException {
        if (data == null || data.get(key) == null) {
            return null;
        }
        Object raw = data.get(key);
        List<Integer> result = new ArrayList<>();
        if (raw instanceof JSONArray arr) {
            for (int i = 0; i < arr.size(); i++) {
                result.add(asNonNegativeInt(arr.get(i), key, i, codePrefix));
            }
            return result;
        }
        if (raw instanceof java.util.List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                result.add(asNonNegativeInt(list.get(i), key, i, codePrefix));
            }
            return result;
        }
        throw new IRException(codePrefix + "_INVALID", key + " 必须是数组：" + raw);
    }

    private static int asNonNegativeInt(Object o, String key, int idx, String codePrefix)
            throws IRException {
        if (!(o instanceof Number n)) {
            throw new IRException(codePrefix + "_INVALID", key + " 第 " + idx + " 项不是整数：" + o);
        }
        int v = n.intValue();
        if (v < 0) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 第 " + idx + " 项必须是非负整数：" + v);
        }
        return v;
    }

    // ------------------------------------------------------------ 四维向量

    /**
     * 读取四维向量，兼容三种写法：
     * <ul>
     *   <li>数组：{@code [1, 2, 3, 4]}</li>
     *   <li>字符串（括号）：{@code "(1 2 3 4)"} 或 {@code "(1, 2, 3, 4)"}</li>
     *   <li>字符串（空格）：{@code "1 2 3 4"}</li>
     * </ul>
     */
    static Vector4f getRequiredVector4(JSONObject data, String key, String codePrefix)
            throws IRException {
        return toVector4(requireField(data, key, codePrefix), key, codePrefix);
    }

    /** 可选四维向量，缺失时返回 null */
    static Vector4f getOptionalVector4(JSONObject data, String key, String codePrefix)
            throws IRException {
        if (data == null || data.get(key) == null) {
            return null;
        }
        return toVector4(data.get(key), key, codePrefix);
    }

    /** 把 JSON 里的值转成四维向量（数组 / 括号字符串 / 空格字符串） */
    static Vector4f toVector4(Object raw, String key, String codePrefix) throws IRException {
        if (raw instanceof JSONArray arr) {
            return fromSequence(arr, key, codePrefix);
        }
        if (raw instanceof java.util.List<?> list) {
            return fromSequence(list, key, codePrefix);
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            throw new IRException(codePrefix + "_EMPTY", key + " 不能为空");
        }
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

    private static Vector4f fromSequence(Object seq, String key, String codePrefix)
            throws IRException {
        int size = (seq instanceof JSONArray a) ? a.size() : ((java.util.List<?>) seq).size();
        if (size != 4) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 需要 4 个分量，实际 " + size + " 个");
        }
        float[] v = new float[4];
        for (int i = 0; i < 4; i++) {
            Object o = (seq instanceof JSONArray a) ? a.get(i) : ((java.util.List<?>) seq).get(i);
            if (!(o instanceof Number n)) {
                throw new IRException(codePrefix + "_INVALID",
                        key + " 第 " + i + " 个分量不是数字：" + o);
            }
            v[i] = n.floatValue();
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

    // ------------------------------------------------------------ 5×5 矩阵

    /**
     * 读取 5×5 行主序矩阵，要求 {@code row0}~{@code row4} 五个字段齐全。
     *
     * @return 长度 25 的 float 数组，行主序
     */
    static float[] getRequiredMatrix5(JSONObject data, String codePrefix) throws IRException {
        float[] m = new float[25];
        for (int r = 0; r < 5; r++) {
            String key = "row" + r;
            float[] row = toRow5(requireField(data, key, codePrefix), key, codePrefix);
            System.arraycopy(row, 0, m, r * 5, 5);
        }
        return m;
    }

    private static float[] toRow5(Object raw, String key, String codePrefix) throws IRException {
        if (raw instanceof JSONArray a) {
            return row5FromSequence(a, key, codePrefix);
        }
        if (raw instanceof java.util.List<?> l) {
            return row5FromSequence(l, key, codePrefix);
        }
        String body = raw.toString().trim().replace(",", " ");
        if (body.startsWith("(") && body.endsWith(")")) {
            body = body.substring(1, body.length() - 1);
        }
        String[] parts = body.trim().split("\\s+");
        if (parts.length != 5) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 需要 5 个元素，实际 " + parts.length + " 个：" + raw);
        }
        float[] row = new float[5];
        for (int i = 0; i < 5; i++) {
            row[i] = parseFloat(parts[i], key, codePrefix);
        }
        return row;
    }

    private static float[] row5FromSequence(Object seq, String key, String codePrefix)
            throws IRException {
        int size = (seq instanceof JSONArray a) ? a.size() : ((java.util.List<?>) seq).size();
        if (size != 5) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 需要 5 个元素，实际 " + size + " 个");
        }
        float[] row = new float[5];
        for (int i = 0; i < 5; i++) {
            Object o = (seq instanceof JSONArray a) ? a.get(i) : ((java.util.List<?>) seq).get(i);
            if (!(o instanceof Number n)) {
                throw new IRException(codePrefix + "_INVALID",
                        key + " 第 " + i + " 个元素不是数字：" + o);
            }
            row[i] = n.floatValue();
        }
        return row;
    }

    // ------------------------------------------------------------ 颜色

    /**
     * 解析颜色为 ARGB 整数。
     *
     * <p>接受 {@code "#AARRGGBB"}、{@code "#RRGGBB"}（alpha 补 FF）
     * 以及整数 {@code 0xAARRGGBB}；缺失时返回 {@code defaultVal}。
     */
    static int parseArgb(Object raw, int defaultVal, String key, String codePrefix)
            throws IRException {
        if (raw == null) {
            return defaultVal;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return defaultVal;
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            s = s.substring(2);
        }
        try {
            long v = Long.parseLong(s, 16);
            if (s.length() <= 6) {
                v |= 0xFF000000L;
            }
            return (int) v;
        } catch (NumberFormatException e) {
            throw new IRException(codePrefix + "_INVALID", key + " 颜色无法解析：" + raw);
        }
    }

    // ------------------------------------------------------------ 错误正文

    /**
     * 把异常转成该条操作的错误正文（不含 {@code # TYPE(id=n)} 头）。
     *
     * <p>文档 §2.3：单条操作失败时，错误文本**直接采用渲染实现抛出的消息**，
     * 形如 {@code Error: Model "Cube A" already exists}。
     */
    static String errorBody(Throwable t) {
        String message = t.getMessage();
        if (message == null || message.isBlank()) {
            message = t.getClass().getSimpleName();
        }
        return "Error: " + message.trim();
    }

    /** 把 ARGB 整数格式化为 "#AARRGGBB" */
    static String formatArgb(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    // ------------------------------------------------------------ 格式化

    /** 把四维坐标格式化为 "(x, y, z, w)" */
    static String formatVector4(Vector4f v) {
        return "(" + fmt(v.x) + ", " + fmt(v.y) + ", " + fmt(v.z) + ", " + fmt(v.w) + ")";
    }

    /** 把 float[25] 按行主序格式化为 "row0=(1, 0, 0, 0, 0)" 多行 */
    static String formatMatrix5Rows(float[] m) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 5; r++) {
            if (r > 0) {
                sb.append('\n');
            }
            sb.append("row").append(r).append('=').append('(');
            for (int c = 0; c < 5; c++) {
                if (c > 0) {
                    sb.append(", ");
                }
                sb.append(fmt(m[r * 5 + c]));
            }
            sb.append(')');
        }
        return sb.toString();
    }

    /** 把 float[25] 格式化为 "(1 0 0 0 0)" 多行（TRANSFORM_MODEL 的返回格式） */
    static String formatMatrix5Parens(float[] m) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < 5; r++) {
            if (r > 0) {
                sb.append('\n');
            }
            sb.append('(');
            for (int c = 0; c < 5; c++) {
                if (c > 0) {
                    sb.append(' ');
                }
                sb.append(fmt(m[r * 5 + c]));
            }
            sb.append(')');
        }
        return sb.toString();
    }

    /**
     * 统一数值格式化：去掉多余小数位，避免输出 "1.0000001" 这类噪声。
     * 整数显示为整数，小数最多 6 位并去掉尾随 0。
     */
    static String fmt(float f) {
        if (f == Math.rint(f) && !Float.isInfinite(f)) {
            return String.valueOf((long) f);
        }
        String s = String.format(Locale.ROOT, "%.6f", f);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && s.charAt(end - 1) == '.') {
            end--;
        }
        return s.substring(0, end);
    }
}
