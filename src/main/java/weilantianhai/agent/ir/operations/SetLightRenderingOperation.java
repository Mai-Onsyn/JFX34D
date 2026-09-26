package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * SET_LIGHT_RENDERING：开关光照渲染（v2 文档 §8.7）。
 *
 * <p>参数：{@code enable} — {@code true} 启用光照，模型面片颜色参与光照计算；
 * {@code false} 关闭光照，直接使用面片固有色。
 *
 * <p>底层对应 {@code SceneInterface.enableLightRendering(enable)}。
 */
public class SetLightRenderingOperation implements IOperation {

    private boolean enable;

    @Override
    public String name() {
        return "SET_LIGHT_RENDERING";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        this.enable = OperationParams.getRequiredBoolean(data, "enable", "ENABLE");
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        renderer.getScene().enableLightRendering(enable);
        return "Success\nLight rendering: " + (enable ? "ON" : "OFF");
    }
}
