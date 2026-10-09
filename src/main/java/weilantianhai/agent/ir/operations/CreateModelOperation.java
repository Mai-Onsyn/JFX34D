package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * CREATE_MODEL：创建一个**空模型**（{@code kind = GROUP}），追加到场景末尾（文档 §4.1.2）。
 *
 * <p>参数：{@code name} — 模型路径，必须未被占用。
 *
 * <pre>
 * # CREATE_MODEL(id=7)
 * Successfully create "Tower"
 * Now models: 2
 * </pre>
 *
 * <p>空模型本身没有几何，把形状建在它下面即可（{@code CREATE_*} 系列）。
 */
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
                + "Now models: " + ModelQuery.rootCount(renderer);
    }
}
