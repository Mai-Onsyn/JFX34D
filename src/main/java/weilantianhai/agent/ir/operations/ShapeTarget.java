package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.ir.IRException;

final class ShapeTarget {
    /** 传给渲染接口的 target（可能为空字符串） */
    final String target;

    /** 传给渲染接口的 name（部件名） */
    final String name;

    /** 拼接后的完整结果路径，用于返回文案与后续查询 */
    final String path;

    private ShapeTarget(String target, String name, String path) {
        this.target = target;
        this.name = name;
        this.path = path;
    }

    static ShapeTarget parse(JSONObject data, String codePrefix) throws IRException {
        String target = OperationParams.getOptionalString(data, "target", "");
        String name = OperationParams.getRequiredString(data, "name", codePrefix + "_NAME");
        String path = OperationParams.joinPartPath(target, name, codePrefix + "_PATH");
        return new ShapeTarget(target.trim(), name.trim(), path);
    }
}
