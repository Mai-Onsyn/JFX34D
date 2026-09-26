package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * GET_3D_1PERCENT_LOW_FPS：获取 3D 渲染器最近约 1 秒的 1% Low 帧率。
 * 无参数。（v2 文档 §8.3）
 *
 * <p>1% Low 即最慢的 1% 帧的平均帧率，用来判断卡顿。返回格式：
 * <pre>
 * 3D 1% Low FPS=41.20
 * </pre>
 */
public class Get3D1PercentLowFpsOperation implements IOperation {

    @Override
    public String name() {
        return "GET_3D_1PERCENT_LOW_FPS";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        // 无参数操作
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        float fps = renderer.getScene().get3D1PercentLowFPS();
        return "3D 1% Low FPS=" + OperationParams.fmtFps(fps);
    }
}
