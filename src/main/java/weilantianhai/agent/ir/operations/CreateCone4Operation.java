package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import mai_onsyn.renderer.ogl3d.data.Mesh;
import org.joml.Vector4f;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_CONE4：以 3D 网格为底面（提升到 {@code w = 0}），以四维点 {@code apex} 为顶点（文档 §5.2.6）。
 *
 * <p>参数：{@code target}、{@code name}、{@code base}（3D 网格 DSL，见 {@link MeshBaseSpec}）、
 * {@code apex}。
 *
 * <pre>
 * # CREATE_CONE4(id=19)
 * Successfully create "Tower/Tip" (CONE4)
 * Base: CUBE edge=1 (12 triangles), transforms=[]
 * Apex=(0, 0, 0, 2.5)
 * Tetrahedrons: 12
 * </pre>
 *
 * <p>base 的每个三角形与 {@code apex} 组成一个四面体（胞数 = base 三角形数）。
 */
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
