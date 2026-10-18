package weilantianhai.agent.ir.operations;

import mai_onsyn.renderer.utils.Coordinate4D;

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
