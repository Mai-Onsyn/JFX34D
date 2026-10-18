package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;

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
