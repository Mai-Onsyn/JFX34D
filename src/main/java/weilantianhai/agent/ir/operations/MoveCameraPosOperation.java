package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.util.JsonParamUtil;

/**
 * MOVE_CAMERA_POS：按单轴移动摄像机（v2 文档 §3.1.2）。
 *
 * <p>参数：
 * <ul>
 *   <li>{@code axis} — {@code x} | {@code y} | {@code z} | {@code w}</li>
 *   <li>{@code distance} — 移动距离，可正可负</li>
 * </ul>
 *
 * <p>移动只相对于当前摄像机坐标系，即 {@code pos += v_axis * distance}，不改变视角。
 */
public class MoveCameraPosOperation implements IOperation {

    private String axis;
    private float distance;

    @Override
    public String name() {
        return "MOVE_CAMERA_POS";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        this.axis = JsonParamUtil.getRequiredEnum(data, "axis", "AXIS", OperationParams.AXES);
        this.distance = JsonParamUtil.getRequiredFloatInRange(
                data, "distance", "DISTANCE", -1000f, 1000f
        );
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        CameraInterface camera = renderer.getCamera();
        switch (axis) {
            case "x" -> camera.moveRight(distance);
            case "y" -> camera.moveUp(distance);
            case "z" -> camera.moveAna(distance);
            case "w" -> camera.moveForward(distance);
            default -> throw new IRException("INVALID_AXIS", "非法轴：" + axis);
        }
        Vector4f pos = camera.getPosition();
        return "Success\nNow Pos=" + OperationParams.formatVector4(pos);
    }
}
