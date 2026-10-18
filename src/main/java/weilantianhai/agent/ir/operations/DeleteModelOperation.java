package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class DeleteModelOperation extends GuardedOperation {
    private String name;

    @Override
    public String name() {
        return "DELETE_MODEL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().removeModel(name);
        return "Successfully delete \"" + name + "\"\n"
                + "Now models: " + ModelQuery.modelCount(renderer);
    }
}
