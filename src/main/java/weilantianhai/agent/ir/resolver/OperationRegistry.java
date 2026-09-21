package weilantianhai.agent.ir.resolver;

import weilantianhai.agent.ir.IOperation;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.ir.operations.MoveCameraPosOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class OperationRegistry {
    private final Map<String, Supplier<IOperation>> map = new HashMap<>();

    public OperationRegistry() {
        register("MOVE_CAMERA_POS", MoveCameraPosOperation::new);
    }

    public void register(String type,Supplier<IOperation> supplier){
        map.put(type,supplier);
    }

    public IOperation creat(String type) throws IRException {
        Supplier<IOperation> s = map.get(type);
        if(s == null){
            throw new IRException("UNKNOWN_OP","未知操作类型：" + type);
        }
        return s.get();
    }


}
