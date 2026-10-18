package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;

public class GetCameraPosOperation extends GuardedOperation {
    @Override
    public String name() {
        return "GET_CAMERA_POS";
    }

    @Override
    protected void parse(JSONObject data) {
        // 无参数操作，data 允许省略
    }

    @Override
    protected String run(RendererInterface renderer) {
        return "Pos=" + OperationParams.formatVector4(renderer.getCamera().getPosition());
    }
}
