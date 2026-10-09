package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_16CELL：创建超八面体（16-cell）（文档 §5.2.3）。
 *
 * <p>参数：{@code target}、{@code name}、{@code center}、{@code radius}（{@code > 0}）。
 *
 * <p>8 个顶点位于四条坐标轴的 {@code ±radius} 处，16 个胞。
 */
public class Create16CellOperation extends GuardedOperation {

    private ShapeTarget target;
    private Vector4f center;
    private float radius;

    @Override
    public String name() {
        return "CREATE_16CELL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_16CELL");
        this.center = OperationParams.getRequiredVector4(data, "center", "CENTER");
        this.radius = OperationParams.getRequiredPositiveFloat(data, "radius", "RADIUS");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getShape().create16Cell(target.target, target.name, center, radius);
        return "Successfully create \"" + target.path + "\" (16CELL)\n"
                + "Center=" + OperationParams.formatVector4(center)
                + ", radius=" + OperationParams.fmt(radius) + "\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
