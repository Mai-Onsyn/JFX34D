package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * REMOVE_TETRAHEDRON：按索引删除四面体（文档 §6.6）。
 *
 * <p>参数：{@code name}、{@code tet}（模型内下标，从 0 开始）。
 *
 * <pre>
 * # REMOVE_TETRAHEDRON(id=27)
 * Successfully remove tetrahedron #0 from "Sculpt/Patch"
 * Tetrahedrons: 0
 * Now kind is CARVED
 * </pre>
 *
 * <p><b>下标语义</b>：删除后其后的下标会**顺移**，所以删除之后要重新查询，
 * 不要沿用旧下标。
 */
public class RemoveTetrahedronOperation extends GuardedOperation {

    private String name;
    private int tet;

    @Override
    public String name() {
        return "REMOVE_TETRAHEDRON";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.tet = OperationParams.getRequiredInt(data, "tet", "TET");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getGeometry().removeTetrahedron(name, tet);
        return "Successfully remove tetrahedron #" + tet + " from \"" + name + "\"\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, name) + "\n"
                + "Now kind is CARVED";
    }
}
