package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.util.JsonParamUtil;

import java.util.Set;

public class MoveCameraPosOperation implements IOperation {
    public static final Set<String> AXES = Set.of("x", "y", "z", "w");

    private String axis;
    private float distance;

    @Override
    public String name(){
        return "MOVE_CAMERA_POS";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        this.axis = JsonParamUtil.getRequiredEnum(data,"axis","AXIS",AXES);
        this.distance = JsonParamUtil.getRequiredFloatInRange(
                data,"distance","DISTANCE",-1000f,1000f
        );
    }

    @Override
    public void execute(RendererInterface renderer) throws IRException{
        CameraInterface camera = renderer.getCamera();
        switch (axis){
            case "x" -> camera.moveRight(distance);
            case "y" -> camera.moveUp(distance);
            case "z" -> camera.moveAna(distance);
            case "w" -> camera.moveForward(distance);
            default -> throw new IRException("INVALID_AXIS","非法轴：" + axis);
        }
    }

}
