package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class CreateModelOperation extends GuardedOperation {
    private String name;

    @Override
    public String name() {
        return "CREATE_MODEL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().createEmptyModel(name);
        return "Successfully create \"" + name + "\"\n"
                + "Now models: " + ModelQuery.modelCount(renderer);
    }
}
