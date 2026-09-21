package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IOperation;
import weilantianhai.agent.ir.IRException;

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
        this.axis = data.getString("axis");
        Float d = data.getFloat("distance");

        if(axis == null || !AXES.contains(this.axis)){
            throw new IRException("INVALID_AXIS","非法轴："+ axis +"，允许 x/y/z/w");
        }
        if(d == null || !d.isNaN() || d.isInfinite()){
            throw new IRException("INVALID_DISTANCE","distance 非法:" + d);
        }
        if(Math.abs(d) > 1000f){
            throw new IRException("DISTANCE_TOO_LARGE","distance 超过上限：1000");
        }
        this.distance = d;
    }

    @Override
    public void execute(Object renderer){
        // TODO: 对接RendererInterface.INSTANCE.camera
    }

}
