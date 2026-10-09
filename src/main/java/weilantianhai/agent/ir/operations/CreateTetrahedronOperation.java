package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_TETRAHEDRON：创建正四面超平面（文档 §5.2.1）。
 *
 * <p>参数：{@code target}（可带多级路径，不必已存在；空字符串表示根路径）、
 * {@code name}（新部件名）、{@code center}、{@code radius}（{@code > 0}）。
 *
 * <p>四个顶点在四条坐标轴上距中心 {@code radius} 处；落在超平面
 * {@code x+y+z+w = radius} 上，四维体积为 0，是一个**三维片元**（1 个胞）。
 */
public class CreateTetrahedronOperation extends GuardedOperation {

    private ShapeTarget target;
    private Vector4f center;
    private float radius;

    @Override
    public String name() {
        return "CREATE_TETRAHEDRON";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_TETRAHEDRON");
        this.center = OperationParams.getRequiredVector4(data, "center", "CENTER");
        this.radius = OperationParams.getRequiredPositiveFloat(data, "radius", "RADIUS");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getShape().createTetrahedron(target.target, target.name, center, radius);
        return "Successfully create \"" + target.path + "\" (TETRAHEDRON)\n"
                + "Center=" + OperationParams.formatVector4(center)
                + ", radius=" + OperationParams.fmt(radius) + "\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
