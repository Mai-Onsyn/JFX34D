package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * GET_CAMERA_VIEW：获取摄像机四个基向量在世界中的指向。无参数。
 *
 * <p>返回格式（v2 文档 §3.2.1）：
 * <pre>
 * vx=(1, 0, 0, 0)
 * vy=(0, 1, 0, 0)
 * vz=(0, 0, 1, 0)
 * vw=(0, 0, 0, 1)
 * </pre>
 */
public class GetCameraViewOperation implements IOperation {

    @Override
    public String name() {
        return "GET_CAMERA_VIEW";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        // 无参数操作
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        Coordinate4D view = renderer.getCamera().getView();
        return CameraViewFormatter.format(view);
    }
}
