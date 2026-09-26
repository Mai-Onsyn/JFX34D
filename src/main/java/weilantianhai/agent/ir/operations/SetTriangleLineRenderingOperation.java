package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * SET_TRIANGLE_LINE_RENDERING：开关"仅线框渲染"（v2 文档 §8.6）。
 *
 * <p>参数：{@code enable} — {@code true} 只用线框画三角形，{@code false} 恢复实体填充。
 *
 * <p>底层对应 {@code GL3DRegion.setOutlineRendering}
 * （{@code glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)}）。
 */
public class SetTriangleLineRenderingOperation implements IOperation {

    private boolean enable;

    @Override
    public String name() {
        return "SET_TRIANGLE_LINE_RENDERING";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        this.enable = OperationParams.getRequiredBoolean(data, "enable", "ENABLE");
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        renderer.getScene().enableTriangleLineRendering(enable);
        return "Success\nTriangle line rendering: " + (enable ? "ON" : "OFF");
    }
}
