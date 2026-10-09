package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * DELETE_MODEL：删除该路径**及其整棵子树**（{@code name} 和所有 {@code name/...}）（文档 §4.1.3）。
 *
 * <p>参数：{@code name} — 要删除的前缀路径，必须存在。
 *
 * <pre>
 * # DELETE_MODEL(id=8)
 * Successfully delete "Tower"
 * Now models: 1
 * </pre>
 */
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
                + "Now models: " + ModelQuery.rootCount(renderer);
    }
}
