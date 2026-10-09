package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * SET_CAMERA_POS：直接设置摄像机坐标（文档 §3.1.3）。
 *
 * <p>参数：{@code pos} — 四维坐标，数组 {@code [0,0,0,0]} 或字符串 {@code "(0 0 0 0)"}。
 */
public class SetCameraPosOperation extends GuardedOperation {

    private Vector4f pos;

    @Override
    public String name() {
        return "SET_CAMERA_POS";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.pos = OperationParams.getRequiredVector4(data, "pos", "POS");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getCamera().setPosition(new Vector4f(pos));
        return "Success\nNow Pos="
                + OperationParams.formatVector4(renderer.getCamera().getPosition());
    }
}
