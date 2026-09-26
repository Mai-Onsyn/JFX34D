package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GET_3D_FPS：获取 3D 渲染器最近约 1 秒的平均帧率。无参数。
 *
 * <p>返回格式（v2 文档 §8.2）：
 * <pre>
 * 3D FPS=59.87
 * </pre>
 */
public class Get3DFpsOperation implements IOperation {

    @Override
    public String name() {
        return "GET_3D_FPS";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        // 无参数操作
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        float fps = renderer.getScene().get3DFPS();
        return "3D FPS=" + OperationParams.fmtFps(fps);
    }
}
