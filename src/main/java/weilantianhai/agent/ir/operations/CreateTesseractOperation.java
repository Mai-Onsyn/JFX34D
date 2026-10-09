package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_TESSERACT：创建超立方体（tesseract）（文档 §5.2.4）。
 *
 * <p>参数：{@code target}、{@code name}、{@code center}、{@code edgeLength}（{@code > 0}）。
 *
 * <p>16 个顶点、8 个立方体胞，每胞 Kuhn 分解为 6 个四面体 = **48 个胞**。
 * 8 个胞使用固定调色板，顺序 {@code +X, -X, +Y, -Y, +Z, -Z, +W, -W}。
 */
public class CreateTesseractOperation extends GuardedOperation {

    private ShapeTarget target;
    private Vector4f center;
    private float edgeLength;

    @Override
    public String name() {
        return "CREATE_TESSERACT";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_TESSERACT");
        this.center = OperationParams.getRequiredVector4(data, "center", "CENTER");
        this.edgeLength = OperationParams.getRequiredPositiveFloat(data, "edgeLength", "EDGE_LENGTH");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getShape().createTesseract(target.target, target.name, center, edgeLength);
        return "Successfully create \"" + target.path + "\" (TESSERACT)\n"
                + "Center=" + OperationParams.formatVector4(center)
                + ", edgeLength=" + OperationParams.fmt(edgeLength) + "\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
