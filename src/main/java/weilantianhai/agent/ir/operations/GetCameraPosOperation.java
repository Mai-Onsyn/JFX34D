package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * GET_CAMERA_POS：获取当前摄像机坐标。无参数。
 *
 * <p>返回格式（v2 文档 §3.1.1）：
 * <pre>
 * # GET_CAMERA_POS(id=1)
 * Pos=(0, 0, 0, 0)
 * </pre>
 */
public class GetCameraPosOperation implements IOperation {

    @Override
    public String name() {
        return "GET_CAMERA_POS";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        // 无参数操作，data 允许省略
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        CameraInterface camera = renderer.getCamera();
        Vector4f pos = camera.getPosition();
        return "Pos=" + OperationParams.formatVector4(pos);
    }
}
