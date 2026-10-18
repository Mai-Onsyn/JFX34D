package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class ApplyTransformToVertexOperation extends GuardedOperation {
    private String name;
    private String dest;

    @Override
    public String name() {
        return "APPLY_TRANSFORM_TO_VERTEX";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.dest = OperationParams.getRequiredPath(data, "dest", "DEST");
        if (name.equals(dest)) {
            throw new IRException("DEST_EQUALS_NAME",
                    "dest 不能等于 name（不支持原地烘焙），请换一个新路径：" + dest);
        }
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().applyTransformToVertex(name, dest);
        return "Successfully apply transform of \"" + name + "\"\n"
                + "Created \"" + dest + "\", matrix is identity\n"
                + "Tetrahedrons: " + ModelQuery.tetrahedronText(renderer, dest);
    }
}
