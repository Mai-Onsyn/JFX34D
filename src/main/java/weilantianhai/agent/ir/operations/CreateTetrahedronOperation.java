package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

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
