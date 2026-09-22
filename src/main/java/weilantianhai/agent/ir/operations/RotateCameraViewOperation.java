package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.util.JsonParamUtil;

import java.util.Set;

public class RotateCameraViewOperation implements IOperation {
    public static final Set<String> PLANES = Set.of("xy","xz","xw","yz","yw","zw");

    private String axis;
    private float angleRad;

    @Override
    public String name(){
        return "ROTATE_CAMERA_VIEW";
    }

    @Override
    public void load(JSONObject data)throws IRException {
        this.axis = JsonParamUtil.getRequiredEnum(data,"axis","PLANE",PLANES);
        float angleDeg = JsonParamUtil.getRequiredFloat(data,"angle","ANGLE");
        this.angleRad = (float)Math.toRadians(angleDeg);
    }

    @Override
    public void execute(RendererInterface renderer) throws IRException {
        CameraInterface camera = renderer.getCamera();
        switch (axis){
            case "xy"-> camera.rotateXY(angleRad);
            case "xz"-> camera.rotateXZ(angleRad);
            case "xw"-> camera.rotateXW(angleRad);
            case "yz"-> camera.rotateYZ(angleRad);
            case "yw"-> camera.rotateYW(angleRad);
            case "zw"-> camera.rotateZW(angleRad);
            default -> throw new IRException("INVALID_PLANE","非法旋转面：" + axis);
        }
    }
}
