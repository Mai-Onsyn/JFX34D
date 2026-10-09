package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;

/**
 * GET_CAMERA_VIEW：获取摄像机四个基向量在世界中的指向。无参数。
 *
 * <p>返回格式（文档 §3.2.1）：
 * <pre>
 * vx=(1, 0, 0, 0)
 * vy=(0, 1, 0, 0)
 * vz=(0, 0, 1, 0)
 * vw=(0, 0, 0, 1)
 * </pre>
 */
public class GetCameraViewOperation extends GuardedOperation {

    @Override
    public String name() {
        return "GET_CAMERA_VIEW";
    }

    @Override
    protected void parse(JSONObject data) {
        // 无参数操作
    }

    @Override
    protected String run(RendererInterface renderer) {
        return CameraViewFormatter.format(renderer.getCamera().getView());
    }
}
