package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.util.JsonParamUtil;

/**
 * ROTATE_CAMERA_VIEW：旋转摄像机视角（v2 文档 §3.2.2）。
 *
 * <p>参数：
 * <ul>
 *   <li>{@code axis} — 旋转平面 {@code xy} | {@code xz} | {@code xw} | {@code yz} | {@code yw} | {@code zw}</li>
 *   <li>{@code angle} — 旋转角度（角度制）</li>
 * </ul>
 *
 * <p>旋转的是摄像机的四个基向量（不是模型）。接口层 {@code CameraInterfaceImpl}
 * 内部会用 {@code toRadians} 转换，所以这里保持文档约定的角度制传入，不要自行转弧度。
 */
public class RotateCameraViewOperation implements IOperation {

    private String axis;
    private float angleDeg;

    @Override
    public String name() {
        return "ROTATE_CAMERA_VIEW";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        this.axis = JsonParamUtil.getRequiredEnum(data, "axis", "PLANE", OperationParams.PLANES);
        this.angleDeg = JsonParamUtil.getRequiredFloat(data, "angle", "ANGLE");
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        CameraInterface camera = renderer.getCamera();
        switch (axis) {
            case "xy" -> camera.rotateXY(angleDeg);
            case "xz" -> camera.rotateXZ(angleDeg);
            case "xw" -> camera.rotateXW(angleDeg);
            case "yz" -> camera.rotateYZ(angleDeg);
            case "yw" -> camera.rotateYW(angleDeg);
            case "zw" -> camera.rotateZW(angleDeg);
            default -> throw new IRException("INVALID_PLANE", "非法旋转面：" + axis);
        }
        return "Success\nCurrent is:\n" + CameraViewFormatter.format(camera.getView());
    }
}
