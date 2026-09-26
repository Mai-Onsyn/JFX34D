package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GET_4D_FPS：获取 4D 渲染器的帧率。无参数。（v2 文档 §8.5）
 *
 * <p>返回格式：
 * <pre>
 * 4D FPS=30.02
 * </pre>
 */
public class Get4DFpsOperation implements IOperation {

    @Override
    public String name() {
        return "GET_4D_FPS";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        // 无参数操作
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        float fps = renderer.getScene().get4DFPS();
        return "4D FPS=" + OperationParams.fmtFps(fps);
    }
}
