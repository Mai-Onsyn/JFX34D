package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GET_MODEL_MATRIX：获取模型的 5×5 变换矩阵（文档 §4.3）。
 *
 * <p>参数：{@code name} — 目标路径，**必须精确存在**（纯分组路径会报"不存在"）。
 *
 * <pre>
 * # GET_MODEL_MATRIX(id=13)
 * "Tower/Base" current is:
 * row0=(1, 0, 0, 0, 0)
 * row1=(0, 1, 0, 0, 0)
 * row2=(0, 0, 1, 0, 0)
 * row3=(0, 0, 0, 1, 0)
 * row4=(0, 0, 0, 0, 1)
 * </pre>
 */
public class GetModelMatrixOperation extends GuardedOperation {

    private String name;

    @Override
    public String name() {
        return "GET_MODEL_MATRIX";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        Matrix5f matrix = renderer.getTransform().getModelMatrix(name);
        return "\"" + name + "\" current is:\n"
                + OperationParams.formatMatrix5Rows(matrix.getData());
    }
}
