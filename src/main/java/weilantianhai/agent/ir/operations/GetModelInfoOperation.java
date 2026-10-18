package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class GetModelInfoOperation extends GuardedOperation {
    private String name;

    @Override
    public String name() {
        return "GET_MODEL_INFO";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        return renderer.getGeometry().getModelInfos(name);
    }
}
