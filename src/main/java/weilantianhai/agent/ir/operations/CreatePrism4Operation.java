package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.ogl3d.data.Mesh;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_PRISM4：以 3D 网格为底面，沿 w 轴从 {@code ws} 拉伸到 {@code we}（文档 §5.2.5）。
 *
 * <p>参数：{@code target}、{@code name}、{@code base}（3D 网格 DSL，见 {@link MeshBaseSpec}）、
 * {@code ws}、{@code we}。
 *
 * <pre>
 * # CREATE_PRISM4(id=18)
 * Successfully create "Tower/Body" (PRISM4)
 * Base: PRISM sides=6 radius=1 height=2 (20 triangles), transforms=[ROTATE]
 * w range=(-1.5, 1.5)
 * Tetrahedrons: 60
 * </pre>
 *
 * <p>要求 {@code ws != we}；{@code ws > we} 时实现会自动交换，返回文案里给出归一化后的区间。
 */
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
