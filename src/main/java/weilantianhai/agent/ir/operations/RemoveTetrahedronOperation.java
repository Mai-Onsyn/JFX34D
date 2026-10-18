package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

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
