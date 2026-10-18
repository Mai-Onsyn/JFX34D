package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

import java.util.List;

public class ListModelOperation extends GuardedOperation {
    @Override
    public String name() {
        return "LIST_MODEL";
    }

    @Override
    protected void parse(JSONObject data) {
        // 无参数操作
    }

    @Override
    protected String run(RendererInterface renderer) throws IRException {
        List<String> paths = renderer.getModel().listAllModels();
        if (paths.isEmpty()) {
            return "No models in scene";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i).append(". \"").append(paths.get(i)).append('"');
        }
        return sb.toString();
    }
}
