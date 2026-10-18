package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

public class SetModelVisibleOperation extends GuardedOperation {
    private String name;
    private boolean visible;

    @Override
    public String name() {
        return "SET_MODEL_VISIBLE";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.name = OperationParams.getRequiredPath(data, "name", "NAME");
        this.visible = OperationParams.getRequiredBoolean(data, "visible", "VISIBLE");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().setModelVisible(name, visible);
        return "Success\n\"" + name + "\" visible = " + visible;
    }
}
