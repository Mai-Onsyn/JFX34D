package weilantianhai.agent.ir.operations;

import mai_onsyn.renderer.utils.Coordinate4D;

/**
 * 摄像机坐标系（四个基向量）的统一格式化工具。
 *
 * <p>{@link GetCameraViewOperation}、{@link SetCameraViewOperation}、
 * {@link RotateCameraViewOperation} 都用它的输出，保证回给 AI 的文本完全一致：
 * <pre>
 * vx=(1, 0, 0, 0)
 * vy=(0, 1, 0, 0)
 * vz=(0, 0, 1, 0)
 * vw=(0, 0, 0, 1)
 * </pre>
 */
final class CameraViewFormatter {

    private CameraViewFormatter() {}

    /** 把四个基向量格式化为多行文本（不含前导的 Success/Current is 行） */
    static String format(Coordinate4D view) {
        return "vx=" + OperationParams.formatVector4(view.getVx()) + "\n"
                + "vy=" + OperationParams.formatVector4(view.getVy()) + "\n"
                + "vz=" + OperationParams.formatVector4(view.getVz()) + "\n"
                + "vw=" + OperationParams.formatVector4(view.getVw());
    }
}
