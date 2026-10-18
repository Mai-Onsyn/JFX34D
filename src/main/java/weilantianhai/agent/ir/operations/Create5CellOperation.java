package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

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
