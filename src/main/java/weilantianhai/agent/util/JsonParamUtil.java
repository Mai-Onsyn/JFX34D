package weilantianhai.agent.util;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.ir.IRException;

import java.util.Set;

public final class JsonParamUtil {

    private JsonParamUtil() {}

    /**
     * 读取必填 float 字段。兼容 Number 和数字字符串。
     *
     * @param data       操作的 data 对象
     * @param key        字段名，如 "distance"
     * @param codePrefix 错误码前缀，生成 {PREFIX}_MISSING / {PREFIX}_INVALID
     */
    public static float getRequiredFloat(JSONObject data, String key, String codePrefix)
            throws IRException {
        if (data == null) {
            throw new IRException(codePrefix + "_MISSING", key + " 所在 data 为空");
        }
        Object raw = data.get(key);
        if (raw == null) {
            throw new IRException(codePrefix + "_MISSING", key + " 缺失");
        }
        float value;
        if (raw instanceof Number n) {
            value = n.floatValue();
        } else {
            try {
                value = Float.parseFloat(raw.toString());
            } catch (NumberFormatException e) {
                throw new IRException(codePrefix + "_INVALID", key + " 无法解析: " + raw);
            }
        }
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            throw new IRException(codePrefix + "_INVALID", key + " 非法: " + value);
        }
        return value;
    }

    /**
     * 读取必填 float 并做范围校验。
     */
    public static float getRequiredFloatInRange(JSONObject data, String key, String codePrefix,
                                                float min, float max) throws IRException {
        float value = getRequiredFloat(data, key, codePrefix);
        if (value < min || value > max) {
            throw new IRException(codePrefix + "_OUT_OF_RANGE",
                    key + " 超出范围 [" + min + ", " + max + "]: " + value);
        }
        return value;
    }

    /**
     * 读取必填字符串，并校验在允许集合内。
     */
    public static String getRequiredEnum(JSONObject data, String key, String codePrefix,
                                         Set<String> allowed) throws IRException {
        if (data == null) {
            throw new IRException(codePrefix + "_MISSING", key + " 所在 data 为空");
        }
        String value = data.getString(key);
        if (value == null || value.isEmpty()) {
            throw new IRException(codePrefix + "_MISSING", key + " 缺失");
        }
        if (!allowed.contains(value)) {
            throw new IRException(codePrefix + "_INVALID",
                    key + " 非法: " + value + "，允许: " + allowed);
        }
        return value;
    }
}