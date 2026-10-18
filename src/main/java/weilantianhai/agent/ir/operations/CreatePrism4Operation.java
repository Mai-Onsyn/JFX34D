package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.ogl3d.data.Mesh;
import weilantianhai.agent.ir.IRException;

public class CreatePrism4Operation extends GuardedOperation {
    private ShapeTarget target;
    private MeshBaseSpec base;
    private float ws;
    private float we;

    @Override
    public String name() {
        return "CREATE_PRISM4";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.target = ShapeTarget.parse(data, "CREATE_PRISM4");
        this.base = MeshBaseSpec.parse(data.get("base"), "BASE");
        this.ws = OperationParams.getRequiredFloat(data, "ws", "WS");
        this.we = OperationParams.getRequiredFloat(data, "we", "WE");
        if (ws == we) {
            throw new IRException("W_RANGE_EMPTY",
                    "Prism w range must not be empty, got ws = we = " + OperationParams.fmt(ws));
        }
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        Mesh mesh = base.build();
        MeshBaseSpec.requireTriangles(mesh, "Prism");
        renderer.getShape().createPrism4(target.target, target.name, mesh, ws, we);

        float lo = Math.min(ws, we);
        float hi = Math.max(ws, we);
        return "Successfully create \"" + target.path + "\" (PRISM4)\n"
                + "Base: " + base.summary(mesh) + "\n"
                + "w range=(" + OperationParams.fmt(lo) + ", " + OperationParams.fmt(hi) + ")\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, target.path);
    }
}
