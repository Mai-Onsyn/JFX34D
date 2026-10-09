package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * MOVE_CAMERA_POS：按单轴移动摄像机（文档 §3.1.2）。
 *
 * <p>参数：
 * <ul>
 *   <li>{@code axis} — {@code x} | {@code y} | {@code z} | {@code w}</li>
 *   <li>{@code distance} — 移动距离，可正可负</li>
 * </ul>
 *
 * <p>移动只相对于当前摄像机坐标系，即 {@code pos += v_axis * distance}，不改变视角。
 */
public class MoveCameraPosOperation extends GuardedOperation {

    /** 单次移动距离上限，纯粹防呆，避免 LLM 给一个夸张值把模型甩出视野 */
    private static final float MAX_DISTANCE = 10000f;

    private String axis;
    private float distance;

    @Override
    public String name() {
        return "MOVE_CAMERA_POS";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.axis = OperationParams.getRequiredEnum(data, "axis", "AXIS", OperationParams.AXES);
        this.distance = OperationParams.getRequiredFloat(data, "distance", "DISTANCE");
        if (Math.abs(distance) > MAX_DISTANCE) {
            throw new IRException("DISTANCE_OUT_OF_RANGE",
                    "distance 超出范围 ±" + OperationParams.fmt(MAX_DISTANCE) + "：" + distance);
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        CameraInterface camera = renderer.getCamera();
        switch (axis) {
            case "x" -> camera.moveRight(distance);
            case "y" -> camera.moveUp(distance);
            case "z" -> camera.moveAna(distance);
            case "w" -> camera.moveForward(distance);
            default -> throw new IllegalArgumentException("Invalid axis: " + axis);
        }
        return "Success\nNow Pos=" + OperationParams.formatVector4(camera.getPosition());
    }
}
