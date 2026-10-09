package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.ir.IRException;

/**
 * 形状操作的 {@code target} / {@code name} 两个路径参数的解析结果。
 *
 * <p>文档 §5.1：每个形状操作都要给两个路径参数——
 * {@code target}（目标模型路径，可带多级路径、**不必已存在**，空字符串表示根路径）
 * 与 {@code name}（新部件名，也可带子路径）；结果路径 = {@code target + "/" + name}。
 *
 * <p>因为每个形状操作都要做同一套校验（结果路径不能为空、不能含 {@code #}、
 * 不能有空路径段），这里收敛成一个小的解析器。
 */
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
