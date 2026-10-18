package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.cpu4dkt.Matrix5f;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class GetModelMatrixOperation extends GuardedOperation {
    private String name;

    @Override
    public String name() {
        return "GET_MODEL_MATRIX";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        Matrix5f matrix = renderer.getTransform().getModelMatrix(name);
        return "\"" + name + "\" current is:\n"
                + OperationParams.formatMatrix5Rows(matrix.getData());
    }
}
