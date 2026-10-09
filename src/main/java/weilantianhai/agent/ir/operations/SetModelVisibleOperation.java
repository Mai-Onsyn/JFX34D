package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * SET_MODEL_VISIBLE：控制某个节点是否参与渲染（文档 §4.1.9）。
 *
 * <p>参数：{@code name}（目标路径，必须存在）、{@code visible}（布尔）。
 *
 * <pre>
 * # SET_MODEL_VISIBLE(id=14)
 * Success
 * "Tower" visible = false
 * </pre>
 *
 * <p>只写 {@code Mesh4D.visible} 标志，**不改** {@code kind} / {@code params}，
 * 也**不动几何**；作用范围是目标节点**及其整棵子树**，一次就能隐藏一整棵子树。
 */
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
