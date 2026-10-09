package weilantianhai.agent.ir.operations;

import com.alibaba.fastjson2.JSONObject;
import mai_onsyn.renderer.interfaces.RendererInterface;
import weilantianhai.agent.ir.IRException;

/**
 * RENAME_MODEL：重命名一个模型节点的**叶名**（文档 §4.1.7）。
 *
 * <p>参数：
 * <ul>
 *   <li>{@code path} — 现有完整路径，必须精确存在（也可以是中间/分组路径，按前缀改名整段）</li>
 *   <li>{@code newName} — 新的叶名，**是名字不是路径**</li>
 * </ul>
 *
 * <pre>
 * # RENAME_MODEL(id=12)
 * Successfully rename "Tower/Base" to "Tower/Lid"
 * </pre>
 *
 * <p>重命名会**连整棵子树一起改名**，父子关系保持不变：{@code Tower → Tower2} 之后，
 * {@code Tower/Base} 会变成 {@code Tower2/Base}，不会断链。
 * {@code newName} 里带 {@code /} 也不会新建层级，会被原样拼到父路径后面。
 */
public class RenameModelOperation extends GuardedOperation {

    private String path;
    private String newName;

    @Override
    public String name() {
        return "RENAME_MODEL";
    }

    @Override
    protected void parse(JSONObject data) throws IRException {
        this.path = OperationParams.getRequiredPath(data, "path", "PATH");
        this.newName = OperationParams.getRequiredString(data, "newName", "NEW_NAME");
    }

    @Override
    protected String run(RendererInterface renderer) {
        renderer.getModel().renameModel(path, newName);
        String newPath = ModelQuery.renamedPath(path, newName);
        if (newPath.equals(path)) {
            return "Successfully rename \"" + path + "\" to \"" + newPath + "\" (unchanged)";
        }
        return "Successfully rename \"" + path + "\" to \"" + newPath + "\"";
    }
}
