package weilantianhai.agent.ir.operation;

import com.alibaba.fastjson2.JSONObject;
import weilantianhai.agent.ir.IOperation;
import weilantianhai.agent.ir.IRException;

import java.util.Set;

public class MoveOperation implements IOperation {
    public static final Set<String> DIRECTIONS = Set.of("right", "up", "ana", "forward");

    private String direction;
    private float distance;

    @Override
    public String name() {
        return "move";
    }

    @Override
    public void load(JSONObject args) throws IRException {
        this.direction = args.getString("direction");
        this.distance = args.getFloat("distance");

        //参数校验
        if(direction == null||!DIRECTIONS.contains(direction)){
            throw new IRException("INVALID_DIRECTION","非法方向"+direction);
        }
    }

    @Override
    public void execute(Object obj) {
        switch (direction){

        }
    }



}
