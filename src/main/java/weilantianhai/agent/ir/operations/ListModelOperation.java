package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

import java.util.List;

/**
 * LIST_MODEL：列出场景中所有**根路径**（文档 §4.1.1）。无参数。
 *
 * <pre>
 * # LIST_MODEL(id=6)
 * 0. "Tower"
 * 1. "Gallery"
 * </pre>
 *
 * <p>要列出某个模型下面的部件，用 {@code GET_MODEL_INFO}。
 */
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
        List<String> roots = renderer.getModel().listModel();
        if (roots.isEmpty()) {
            return "No models in scene";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roots.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(i).append(". \"").append(roots.get(i)).append('"');
        }
        return sb.toString();
    }
}
