package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.ogl3d.data.Mesh;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

public class CreateCone4Operation extends GuardedOperation {
    private ShapeTarget target;
    private MeshBaseSpec base;
    private Vector4f apex;

    @Override
    public String name() {
        return "CREATE_CONE4";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_CONE4");
        this.base = MeshBaseSpec.parse(data.get("base"), "BASE");
        this.apex = OperationParams.getRequiredVector4(data, "apex", "APEX");
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        Mesh mesh = base.build();
        MeshBaseSpec.requireTriangles(mesh, "Cone");
        renderer.getShape().createCone4(target.target, target.name, mesh, apex);
        return "Successfully create \"" + target.path + "\" (CONE4)\n"
                + "Base: " + base.summary(mesh) + "\n"
                + "Apex=" + OperationParams.formatVector4(apex) + "\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
