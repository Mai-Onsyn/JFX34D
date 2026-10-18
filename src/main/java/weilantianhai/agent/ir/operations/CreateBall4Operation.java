package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

public class CreateBall4Operation extends GuardedOperation {
    /** density 上限；文档里渲染侧默认细分上限是 64（约 1260 万四面体），这里收紧到 24 防呆 */
    private static final float MAX_DENSITY = 24f;

    private ShapeTarget target;
    private Vector4f center;
    private float radius;
    private float density;

    @Override
    public String name() {
        return "CREATE_BALL4";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_BALL4");
        this.center = OperationParams.getRequiredVector4(data, "center", "CENTER");
        this.radius = OperationParams.getRequiredPositiveFloat(data, "radius", "RADIUS");
        this.density = OperationParams.getRequiredPositiveFloat(data, "density", "DENSITY");
        if (density > MAX_DENSITY) {
            throw new IRException("DENSITY_TOO_LARGE",
                    "density 不能超过 " + OperationParams.fmt(MAX_DENSITY)
                            + "（胞数是 O(k³)，过大会耗光内存）：" + density);
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getShape().createBall4(target.target, target.name, center, radius, density);
        return "Successfully create \"" + target.path + "\" (BALL4)\n"
                + "Center=" + OperationParams.formatVector4(center)
                + ", radius=" + OperationParams.fmt(radius)
                + ", density=" + OperationParams.fmt(density) + "\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
