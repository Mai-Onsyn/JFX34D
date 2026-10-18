package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class RotateCameraViewOperation extends GuardedOperation {
    private String axis;
    private float angleDeg;

    @Override
    public String name() {
        return "ROTATE_CAMERA_VIEW";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.axis = OperationParams.getRequiredEnum(data, "axis", "PLANE", OperationParams.PLANES);
        this.angleDeg = OperationParams.getRequiredFloat(data, "angle", "ANGLE");
    }

    @Override
    protected String run(RendererInterface renderer) {
        CameraInterface camera = renderer.getCamera();
        switch (axis) {
            case "xy" -> camera.rotateXY(angleDeg);
            case "xz" -> camera.rotateXZ(angleDeg);
            case "xw" -> camera.rotateXW(angleDeg);
            case "yz" -> camera.rotateYZ(angleDeg);
            case "yw" -> camera.rotateYW(angleDeg);
            case "zw" -> camera.rotateZW(angleDeg);
            default -> throw new IllegalArgumentException("Invalid rotation plane: " + axis);
        }
        return "Success\nCurrent is:\n" + CameraViewFormatter.format(camera.getView());
    }
}
