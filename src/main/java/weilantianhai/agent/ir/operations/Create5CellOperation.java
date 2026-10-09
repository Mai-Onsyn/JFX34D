package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_5CELL：创建四维单纯形（5-cell / 超四面体）（文档 §5.2.2）。
 *
 * <p>参数：{@code target}、{@code name}、{@code center}、{@code size}（棱长，{@code > 0}）。
 *
 * <p>标准 4-单纯形缩放到给定棱长，共 5 个胞，每胞"去掉一个顶点"。
 */
public class Create5CellOperation extends GuardedOperation {

    private ShapeTarget target;
    private Vector4f center;
    private float size;

    @Override
    public String name() {
        return "CREATE_5CELL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_5CELL");
        this.center = OperationParams.getRequiredVector4(data, "center", "CENTER");
        this.size = OperationParams.getRequiredPositiveFloat(data, "size", "SIZE");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getShape().create5Cell(target.target, target.name, center, size);
        return "Successfully create \"" + target.path + "\" (5CELL)\n"
                + "Center=" + OperationParams.formatVector4(center)
                + ", size=" + OperationParams.fmt(size) + "\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
