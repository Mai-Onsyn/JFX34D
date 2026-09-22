package weilantianhai.agent.ir.resolver;

import weilantianhai.agent.ir.operations.IOperation;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.ir.operations.MoveCameraPosOperation;
import weilantianhai.agent.ir.operations.RotateCameraViewOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class OperationRegistry {
    private final Map<String, Supplier<IOperation>> map = new HashMap<>();

    //注册操作
    public OperationRegistry() {
        //移动相机位置
        register("MOVE_CAMERA_POS", MoveCameraPosOperation::new);
        //旋转摄像机视角
        register("ROTATE_CAMERA_VIEW", RotateCameraViewOperation::new);
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
