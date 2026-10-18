package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import HCloudbyte.ui.interfaces.UIInterface;
import HCloudbyte.ui.viewport.CameraRecord;
import weilantianhai.agent.ir.IRException;

import java.util.List;

final class CameraBookmarkResolver {
    private CameraBookmarkResolver() {}

    /**
     * 解析机位参数。
     *
     * @param data      操作参数
     * @param codePrefix 错误码前缀，用操作名（如 {@code "GO_CAMERA_BOOKMARK"}）
     * @return 机位在列表中的唯一下标（&gt;= 0）
     * @throws IRException 参数缺失 / 名字不存在 / 名字重名时抛出
     */
    static int resolve(JSONObject data, String codePrefix) throws IRException {
        List<CameraRecord> records = UIInterface.getInstance().getCameraRecords();

        String name = OperationParams.getOptionalString(data, "name", null);
        Integer index = readOptionalIndex(data, codePrefix);

        if (name == null && index == null) {
            throw new IRException(codePrefix + "_MISSING",
                    "必须给出 name 或 index 之一" + availableHint(records));
        }

        // index 单独给定时，做范围校验（越界消息里带上实际数量）
        if (name == null) {
            if (index < 0 || index >= records.size()) {
                throw new IRException(codePrefix + "_INDEX_OUT_OF_RANGE",
                        "index=" + index + " 越界，当前共 " + records.size() + " 个机位");
            }
            return index;
        }

        int found = -1;
        int matches = 0;
        for (int i = 0; i < records.size(); i++) {
            String n = records.get(i).getName();
            if (name.equals(n)) {
                matches++;
                if (found < 0) found = i;
            }
        }

        if (matches == 0) {
            throw new IRException(codePrefix + "_NOT_FOUND",
                    "找不到名为 \"" + name + "\" 的机位" + availableHint(records));
        }
        if (matches > 1) {
            throw new IRException(codePrefix + "_AMBIGUOUS",
                    "机位名 \"" + name + "\" 有 " + matches + " 个，请改用 index 指定");
        }
        return found;
    }

    /** 读取可选的 index；缺失返回 null。负数直接判非法。 */
    private static Integer readOptionalIndex(JSONObject data, String codePrefix) throws IRException {
        if (data == null || data.get("index") == null) {
            return null;
        }
        int value = OperationParams.getRequiredInt(data, "index", codePrefix);
        return value;
    }

    /** "，当前机位：[0]俯视 [1]机位2" 形式提示；没有机位时给出可读文案。 */
    private static String availableHint(List<CameraRecord> records) {
        if (records.isEmpty()) {
            return "（当前没有任何机位，可先用 SAVE_CAMERA_BOOKMARK 记录一个）";
        }
        StringBuilder sb = new StringBuilder("（当前机位：");
        for (int i = 0; i < records.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append('[').append(i).append(']').append(records.get(i).getName());
        }
        return sb.append('）').toString();
    }
}
