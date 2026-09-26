package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.CameraInterface;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.utils.Coordinate4D;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * SET_CAMERA_VIEW：直接设置摄像机坐标系（v2 文档 §3.2.3）。
 *
 * <p>参数：{@code vx}、{@code vy}、{@code vz}、{@code vw} 四个四维向量。
 *
 * <p>按文档要求，解析器层负责校验：四个向量都必须是单位向量，且两两正交
 * （{@code CameraInterfaceImpl.setView} 自身是直接赋值，不做检查）。
 */
public class SetCameraViewOperation implements IOperation {

    private Vector4f vx;
    private Vector4f vy;
    private Vector4f vz;
    private Vector4f vw;

    @Override
    public String name() {
        return "SET_CAMERA_VIEW";
    }

    @Override
    public void load(JSONObject data) throws IRException {
        this.vx = OperationParams.getRequiredVector4(data, "vx", "VX");
        this.vy = OperationParams.getRequiredVector4(data, "vy", "VY");
        this.vz = OperationParams.getRequiredVector4(data, "vz", "VZ");
        this.vw = OperationParams.getRequiredVector4(data, "vw", "VW");

        // 单位性校验
        OperationParams.requireUnit(vx, "vx", "VECTOR");
        OperationParams.requireUnit(vy, "vy", "VECTOR");
        OperationParams.requireUnit(vz, "vz", "VECTOR");
        OperationParams.requireUnit(vw, "vw", "VECTOR");

        // 两两正交校验（四个向量四四正交）
        OperationParams.requireOrthogonal(vx, vy, "vx", "vy", "VECTOR");
        OperationParams.requireOrthogonal(vx, vz, "vx", "vz", "VECTOR");
        OperationParams.requireOrthogonal(vx, vw, "vx", "vw", "VECTOR");
        OperationParams.requireOrthogonal(vy, vz, "vy", "vz", "VECTOR");
        OperationParams.requireOrthogonal(vy, vw, "vy", "vw", "VECTOR");
        OperationParams.requireOrthogonal(vz, vw, "vz", "vw", "VECTOR");
    }

    @Override
    public String execute(RendererInterface renderer) throws IRException {
        CameraInterface camera = renderer.getCamera();
        camera.setView(new Coordinate4D(
                new Vector4f(vx),
                new Vector4f(vy),
                new Vector4f(vz),
                new Vector4f(vw)
        ));
        return "Success\nCurrent is:\n" + CameraViewFormatter.format(camera.getView());
    }
}
