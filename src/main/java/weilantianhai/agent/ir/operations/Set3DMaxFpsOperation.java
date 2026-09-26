package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.util.JsonParamUtil;

/**
 * SET_3D_MAX_FPS：限制 3D 渲染器最大帧率（v2 文档 §8.1）。
 *
 * <p>参数：{@code fps} — 最大帧率，必须 {@code > 0}。
 */
public class Set3DMaxFpsOperation implements IOperation {

    private float fps;

    @Override
    public String name() {
        return "SET_3D_MAX_FPS";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        this.fps = JsonParamUtil.getRequiredFloat(data, "fps", "FPS");
        if (fps <= 0f) {
            throw new IRException("FPS_INVALID", "fps 必须大于 0：" + fps);
        }
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        renderer.getScene().set3DMaxFPS(fps);
        return "Success\n3D max FPS = " + OperationParams.fmt(fps);
    }
}
