package weilantianhai.agent.ir.resolver;

import weilantianhai.agent.ir.operations.IOperation;
import weilantianhai.agent.ir.IRException;
import weilantianhai.agent.ir.operations.Get3D1PercentLowFpsOperation;
import weilantianhai.agent.ir.operations.Get3DFpsOperation;
import weilantianhai.agent.ir.operations.Get4DFpsOperation;
import weilantianhai.agent.ir.operations.GetCameraPosOperation;
import weilantianhai.agent.ir.operations.GetCameraViewOperation;
import weilantianhai.agent.ir.operations.MoveCameraPosOperation;
import weilantianhai.agent.ir.operations.RotateCameraViewOperation;
import weilantianhai.agent.ir.operations.Set3DMaxFpsOperation;
import weilantianhai.agent.ir.operations.Set4DMaxFpsOperation;
import weilantianhai.agent.ir.operations.SetCameraPosOperation;
import weilantianhai.agent.ir.operations.SetCameraViewOperation;
import weilantianhai.agent.ir.operations.SetLightRenderingOperation;
import weilantianhai.agent.ir.operations.SetTriangleLineRenderingOperation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 操作注册表：把 JSON 里的 {@code type} 字符串映射到具体的 {@link IOperation} 实例。
 *
 * <p>注册的 {@code type} 必须与对应操作类 {@code name()} 的返回值完全一致，
 * 否则 {@link #creat(String)} 会抛 {@code UNKNOWN_OP}。
 *
 * <p>当前已接入的操作共 13 个，分两类：
 * <ul>
 *   <li>摄像机：位置与视角的查询 / 设置 / 移动 / 旋转</li>
 *   <li>渲染设置：帧率限制与查询、线框渲染、光照渲染开关</li>
 * </ul>
 *
 * <p>模型管理、几何体、雕刻三类已取消接入，相关操作类不再存在，故此处不注册。
 */
public class OperationRegistry {
    private final Map<String, Supplier<IOperation>> map = new HashMap<>();

    //注册操作
    public OperationRegistry() {
        // ---------- 摄像机：位置 ----------
        //获取摄像机位置
        register("GET_CAMERA_POS", GetCameraPosOperation::new);
        //移动摄像机位置（沿单轴）
        register("MOVE_CAMERA_POS", MoveCameraPosOperation::new);
        //设置摄像机位置
        register("SET_CAMERA_POS", SetCameraPosOperation::new);

        // ---------- 摄像机：视角 ----------
        //获取摄像机视角（四基向量）
        register("GET_CAMERA_VIEW", GetCameraViewOperation::new);
        //旋转摄像机视角（在指定平面内）
        register("ROTATE_CAMERA_VIEW", RotateCameraViewOperation::new);
        //设置摄像机视角（四基向量，需单位且两两正交）
        register("SET_CAMERA_VIEW", SetCameraViewOperation::new);

        // ---------- 渲染设置：帧率 ----------
        //设置 3D 最大帧率
        register("SET_3D_MAX_FPS", Set3DMaxFpsOperation::new);
        //获取 3D 帧率
        register("GET_3D_FPS", Get3DFpsOperation::new);
        //获取 3D 1% Low 帧率
        register("GET_3D_1PERCENT_LOW_FPS", Get3D1PercentLowFpsOperation::new);
        //设置 4D 最大帧率
        register("SET_4D_MAX_FPS", Set4DMaxFpsOperation::new);
        //获取 4D 帧率
        register("GET_4D_FPS", Get4DFpsOperation::new);

        // ---------- 渲染设置：渲染模式 ----------
        //开关仅线框渲染
        register("SET_TRIANGLE_LINE_RENDERING", SetTriangleLineRenderingOperation::new);
        //开关光照渲染
        register("SET_LIGHT_RENDERING", SetLightRenderingOperation::new);
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
